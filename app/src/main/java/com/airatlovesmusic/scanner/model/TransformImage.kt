package com.airatlovesmusic.scanner.model

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*


class TransformImage {

    fun getTranformedDocument(
        bitmap: Bitmap,
        points: List<Point>
    ): Bitmap? {
        val rectangle = MatOfPoint2f()
        rectangle.fromList(points)
        val dstMat: Mat = transform(bitmapToMat(bitmap), rectangle)
        return matToBitmap(dstMat)
    }

    private fun transform(src: Mat, corners: MatOfPoint2f): Mat {
        val sortedCorners = sortCorners(corners)
        val size = getRectangleSize(sortedCorners)
        val result = Mat.zeros(size, src.type())
        val imageOutline = getOutline(result)
        val transformation = Imgproc.getPerspectiveTransform(sortedCorners, imageOutline)
        Imgproc.warpPerspective(src, result, transformation, size)
        return result
    }

    private fun getRectangleSize(rectangle: MatOfPoint2f): Size {
        val corners = rectangle.toArray()
        val top = getDistance(corners[0], corners[1])
        val right = getDistance(corners[1], corners[2])
        val bottom = getDistance(corners[2], corners[3])
        val left = getDistance(corners[3], corners[0])
        val averageWidth = (top + bottom) / 2f
        val averageHeight = (right + left) / 2f
        return Size(Point(averageWidth, averageHeight))
    }

    private fun getDistance(p1: Point, p2: Point): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return Math.sqrt(dx * dx + dy * dy)
    }

    private fun getOutline(image: Mat): MatOfPoint2f {
        val topLeft = Point(0.0, 0.0)
        val topRight = Point(image.cols().toDouble(), 0.0)
        val bottomRight = Point(
            image.cols().toDouble(),
            image.rows().toDouble()
        )
        val bottomLeft = Point(0.0, image.rows().toDouble())
        val points = arrayOf(topLeft, topRight, bottomRight, bottomLeft)
        val result = MatOfPoint2f()
        result.fromArray(*points)
        return result
    }

    private fun sortCorners(corners: MatOfPoint2f): MatOfPoint2f {
        val center = getMassCenter(corners)
        val points = corners.toList()
        val topPoints: MutableList<Point> = ArrayList()
        val bottomPoints: MutableList<Point> = ArrayList()
        for (point in points) {
            if (point.y < center.y) {
                topPoints.add(point)
            } else {
                bottomPoints.add(point)
            }
        }
        val topLeft = if (topPoints[0].x > topPoints[1].x) topPoints[1] else topPoints[0]
        val topRight = if (topPoints[0].x > topPoints[1].x) topPoints[0] else topPoints[1]
        val bottomLeft =
            if (bottomPoints[0].x > bottomPoints[1].x) bottomPoints[1] else bottomPoints[0]
        val bottomRight =
            if (bottomPoints[0].x > bottomPoints[1].x) bottomPoints[0] else bottomPoints[1]
        val result = MatOfPoint2f()
        val sortedPoints = arrayOf(topLeft, topRight, bottomRight, bottomLeft)
        result.fromArray(*sortedPoints)
        return result
    }

    private fun getMassCenter(points: MatOfPoint2f): Point {
        var xSum = 0.0
        var ySum = 0.0
        val pointList = points.toList()
        val len = pointList.size
        for (point in pointList) {
            xSum += point.x
            ySum += point.y
        }
        return Point(xSum / len, ySum / len)
    }
}

fun bitmapToMat(bitmap: Bitmap): Mat {
    val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8U, Scalar(4.toDouble()))
    val bitmap32 = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    Utils.bitmapToMat(bitmap32, mat)
    return mat
}

fun matToBitmap(mat: Mat): Bitmap {
    val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, bitmap)
    return bitmap
}