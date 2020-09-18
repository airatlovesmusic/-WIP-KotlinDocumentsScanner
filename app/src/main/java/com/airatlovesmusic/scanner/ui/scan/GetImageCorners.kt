package com.airatlovesmusic.scanner.ui.scan

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


/**
 * A good initial number to use during the white-color filter operation
 * see [GetImageCorners.run]
 */
internal const val THRESHOLD_BASE = 48.0

/**
 * Tries to find an A4 document inside the provided image.
 * Returns the corners of the A4 document, if found
 */
class GetImageCorners {

    suspend fun findDocumentCorners(bitmap: Bitmap): Corners? {
        return runCatching {
            val result = getPoint(bitmap)
            result?.let {
                Corners(
                    it.toList(),
                    it.size()
                )
            }
        }.getOrNull()
    }

    private val areaDescendingComparator: Comparator<MatOfPoint2f> =
        Comparator { m1, m2 ->
            val area1 = Imgproc.contourArea(m1)
            val area2 = Imgproc.contourArea(m2)
            Math.ceil(area2 - area1).toInt()
        }

    private fun getPoint(bitmap: Bitmap): MatOfPoint2f? {
        val src: Mat = bitmapToMat(bitmap)

        // Downscale image for better performance.
        val ratio: Double = 600.toDouble() / Math.max(src.width(), src.height())
        val downscaledSize = Size(src.width() * ratio, src.height() * ratio)
        val downscaled = Mat(downscaledSize, src.type())
        Imgproc.resize(src, downscaled, downscaledSize)
        val rectangles = getPoints(downscaled)
        if (rectangles.isNullOrEmpty()) { return null }
        Collections.sort(rectangles, areaDescendingComparator)
        val largestRectangle = rectangles[0]
        return scaleRectangle(largestRectangle, 1f / ratio)
    }

    private fun getPoints(src: Mat): List<MatOfPoint2f>? {

        // Blur the image to filter out the noise.
        val blurred = Mat()
        Imgproc.medianBlur(src, blurred, 9)

        // Set up images to use.
        val gray0 = Mat(blurred.size(), CvType.CV_8U)
        val gray = Mat()

        // For Core.mixChannels.
        val contours: List<MatOfPoint> = ArrayList()
        val rectangles: MutableList<MatOfPoint2f> = ArrayList()
        val sources: MutableList<Mat> = ArrayList()
        sources.add(blurred)
        val destinations: MutableList<Mat> = ArrayList()
        destinations.add(gray0)

        // To filter rectangles by their areas.
        val srcArea = src.rows() * src.cols()

        // Find squares in every color plane of the image.
        for (c in 0..2) {
            val ch = intArrayOf(c, 0)
            val fromTo = MatOfInt(*ch)
            Core.mixChannels(sources, destinations, fromTo)

            // Try several threshold levels.
            for (l in 0 until 2) {
                if (l == 0) {
                    // HACK: Use Canny instead of zero threshold level.
                    // Canny helps to catch squares with gradient shading.
                    // NOTE: No kernel size parameters on Java API.
                    Imgproc.Canny(gray0, gray, 10.0, 20.0)

                    // Dilate Canny output to remove potential holes between edge segments.
                    Imgproc.dilate(gray, gray, Mat.ones(Size(3.toDouble(), 3.toDouble()), 0))
                } else {
                    val threshold: Int = (l + 1) * 255 / 2
                    Imgproc.threshold(
                        gray0,
                        gray,
                        threshold.toDouble(),
                        255.0,
                        Imgproc.THRESH_BINARY
                    )
                }

                // Find contours and store them all as a list.
                Imgproc.findContours(
                    gray,
                    contours,
                    Mat(),
                    Imgproc.RETR_LIST,
                    Imgproc.CHAIN_APPROX_SIMPLE
                )
                for (contour in contours) {
                    val contourFloat: MatOfPoint2f = MatOfPoint2f().apply { contour.convertTo(
                        this,
                        CvType.CV_32FC2
                    ) }
                    val arcLen = Imgproc.arcLength(contourFloat, true) * 0.02

                    // Approximate polygonal curves.
                    val approx = MatOfPoint2f()
                    Imgproc.approxPolyDP(contourFloat, approx, arcLen, true)
                    if (isRectangle(approx, srcArea)) {
                        rectangles.add(approx)
                    }
                }
            }
        }
        return rectangles
    }

    private fun isRectangle(polygon: MatOfPoint2f, srcArea: Int): Boolean {
        val polygonInt: MatOfPoint = MatOfPoint().apply { polygon.convertTo(this, CvType.CV_32S) }
        if (polygon.rows() != 4) {
            return false
        }
        val area = abs(Imgproc.contourArea(polygon))
        if (area < srcArea * 0.02 || area > srcArea * 0.98) {
            return false
        }
        if (!Imgproc.isContourConvex(polygonInt)) {
            return false
        }

        // Check if the all angles are more than 72.54 degrees (cos 0.3).
        var maxCos = 0.0
        val approxPoints = polygon.toArray()
        for (i in 2..4) {
            val cosine: Double = abs(
                angle(
                    approxPoints[i % 4],
                    approxPoints[i - 2], approxPoints[i - 1]
                )
            )
            maxCos = cosine.coerceAtLeast(maxCos)
        }
        return maxCos < 1
    }

}

fun angle(p1: Point, p2: Point, p0: Point): Double {
    val dx1 = p1.x - p0.x
    val dy1 = p1.y - p0.y
    val dx2 = p2.x - p0.x
    val dy2 = p2.y - p0.y
    return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10)
}

fun bitmapToMat(bitmap: Bitmap): Mat {
    val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8U, Scalar(4.toDouble()))
    val bitmap32 = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    Utils.bitmapToMat(bitmap32, mat)
    return mat
}

val MatOfPoint.shape: Array<Point>
    get() {
        val c2f = MatOfPoint2f(*toArray())
        val peri = Imgproc.arcLength(c2f, true)
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
        return approx.toArray()
    }

fun scaleRectangle(original: MatOfPoint2f, scale: Double): MatOfPoint2f? {
    val originalPoints = original.toList()
    val resultPoints: MutableList<Point> = ArrayList()
    for (point in originalPoints) {
        resultPoints.add(Point(point.x * scale, point.y * scale))
    }
    val result = MatOfPoint2f()
    result.fromList(resultPoints)
    return result
}
