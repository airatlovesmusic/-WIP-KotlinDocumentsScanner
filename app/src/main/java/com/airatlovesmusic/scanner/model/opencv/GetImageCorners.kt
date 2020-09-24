package com.airatlovesmusic.scanner.model.opencv

import android.graphics.Bitmap
import com.airatlovesmusic.scanner.entity.Corners
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY
import kotlin.math.abs
import kotlin.math.sqrt
import com.airatlovesmusic.scanner.entity.Point as PPoint
import com.airatlovesmusic.scanner.entity.Size as PSize


class GetImageCorners {

    fun getDocumentEdges(bitmap: Bitmap): Corners? {
        val image = Mat().also { Utils.bitmapToMat(bitmap, it) }
        // downscale image
        val ratio = 680.0 / image.width().coerceAtLeast(image.height())
        val downscaledSize = Size(image.width() * ratio, image.height() * ratio)
        val downscaled = Mat(downscaledSize, image.type())
        Imgproc.resize(image, downscaled, downscaledSize)
        val edged = Mat().also {
            Imgproc.cvtColor(downscaled, it, COLOR_BGR2GRAY)
            Imgproc.GaussianBlur(it, it, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(it, it, 75.0, 200.0)
        }
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            edged,
            contours,
            Mat(),
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        val sorted = contours.sortedBy { Imgproc.contourArea(it) }.reversed()
        var documentContours: MatOfPoint2f? = null
        for (mat in sorted) {
            val contourFloat: MatOfPoint2f = MatOfPoint2f().apply { mat.convertTo(
                this,
                CvType.CV_32FC2
            ) }
            val arcLen = Imgproc.arcLength(contourFloat, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(contourFloat, approx, 0.02 * arcLen, true)
            if (isRectangle(approx, edged.rows() * edged.cols())) {
                documentContours = approx
                break
            }
        }
        return documentContours?.let {
            Corners(
                it.toList().map { PPoint(it.x.times(1.0 / ratio), it.y.times(1.0 / ratio)) },
                image.size().let { PSize(it.width, it.height) }
            )
        }
    }

    private fun isRectangle(mat: MatOfPoint2f, srcArea: Int): Boolean {
        val polygonInt: MatOfPoint = MatOfPoint().also { mat.convertTo(it, CvType.CV_32S) }
        if (mat.rows() != 4) {
            return false
        }
        val area = abs(Imgproc.contourArea(mat))
        if (area < srcArea * 0.1 || area > srcArea * 0.9) {
            return false
        }
        if (!Imgproc.isContourConvex(polygonInt)) {
            return false
        }
        var maxCosine = 0.0
        val approxPoints = mat.toArray()

        for (i in 2..4) {
            val cosine: Double = abs(
                angle(
                    approxPoints[i % 4],
                    approxPoints[i - 2],
                    approxPoints[i - 1]
                )
            )
            maxCosine = cosine.coerceAtLeast(maxCosine)
        }
        return maxCosine < 0.3
    }

    private fun angle(p1: Point, p2: Point, p0: Point): Double {
        val dx1 = p1.x - p0.x
        val dy1 = p1.y - p0.y
        val dx2 = p2.x - p0.x
        val dy2 = p2.y - p0.y
        return (dx1 * dx2 + dy1 * dy2) / sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10)
    }

}