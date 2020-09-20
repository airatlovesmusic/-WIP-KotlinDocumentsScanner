package com.airatlovesmusic.scanner.ui.scan

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY
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

    suspend fun getDocumentEdges(bitmap: Bitmap): Corners? {
        val image = Mat().also { Utils.bitmapToMat(bitmap, it) }
        val edged = Mat().also {
            Imgproc.cvtColor(image, it, COLOR_BGR2GRAY)
            Imgproc.GaussianBlur(it, it, Size(5.toDouble(), 5.toDouble()), 0.toDouble())
            Imgproc.Canny(it, it, 75.toDouble(), 200.toDouble())
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
        sorted.forEach {
            val contourFloat: MatOfPoint2f =
                MatOfPoint2f().apply { it.convertTo(this, CvType.CV_32FC2) }
            val peri = Imgproc.arcLength(contourFloat, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(contourFloat, approx, 0.02 * peri, true)
            if (approx.rows() == 4) {
                documentContours = approx
            }
        }
        return documentContours?.let {
            Corners(
                it.toList(),
                image.size()
            )
        }
    }
}
