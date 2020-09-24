package com.airatlovesmusic.scanner.ui.scan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.airatlovesmusic.scanner.entity.Corners
import com.airatlovesmusic.scanner.entity.Point
import org.opencv.core.Point as OpenCVPoint
import kotlin.math.abs

class DocumentCornersView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
) : View(context, attributes) {

    private val rectPaint = Paint()
    private val circlePaint = Paint()
    private val fillPaint = Paint()
    private val path: Path = Path()

    private var ratioX: Double = 1.0
    private var ratioY: Double = 1.0
    private var topLeft: Point = Point()
    private var topRight: Point = Point()
    private var bottomRight: Point = Point()
    private var bottomLeft: Point = Point()
    private var point2Move = Point()
    private val points: List<Point>
        get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    private var latestDownX = 0.0F
    private var latestDownY = 0.0F

    var cropMode = false
        set(value) {
            field = value
            invalidate()
        }

    init {
        with(rectPaint) {
            color = Color.parseColor("#3454D1")
            isAntiAlias = true
            isDither = true
            strokeWidth = 6F
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            pathEffect = CornerPathEffect(10f)
        }

        with(fillPaint) {
            color = Color.parseColor("#3454D1")
            alpha = 60
            isAntiAlias = true
            isDither = true
            strokeWidth = 6F
            style = Paint.Style.FILL
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            pathEffect = CornerPathEffect(10f)
        }

        with(circlePaint) {
            color = Color.LTGRAY
            isDither = true
            isAntiAlias = true
            strokeWidth = 4F
            style = Paint.Style.STROKE
        }
    }

    fun onCornersDetected(corners: List<Point>) {
        topLeft = corners.getOrNull(0) ?: Point()
        topRight = corners.getOrNull(1) ?: Point()
        bottomRight = corners.getOrNull(2) ?: Point()
        bottomLeft = corners.getOrNull(3) ?: Point()
        resize()
        with(path) {
            reset()
            moveTo(topLeft.x.toFloat(), topLeft.y.toFloat())
            lineTo(topRight.x.toFloat(), topRight.y.toFloat())
            lineTo(bottomRight.x.toFloat(), bottomRight.y.toFloat())
            lineTo(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())
            close()
        }
        invalidate()
    }

    fun onCornersNotDetected() {
        path.reset()
        circlePaint.reset()
        invalidate()
    }

    fun getOpenCVPoints() = points.map { OpenCVPoint(it.x, it.y) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        with(canvas) {
            drawPath(path, fillPaint)
            drawPath(path, rectPaint)
            if (cropMode) {
                drawCircle(topLeft.x.toFloat(), topLeft.y.toFloat(), 20F, circlePaint)
                drawCircle(topRight.x.toFloat(), topRight.y.toFloat(), 20F, circlePaint)
                drawCircle(bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), 20F, circlePaint)
                drawCircle(bottomRight.x.toFloat(), bottomRight.y.toFloat(), 20F, circlePaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (!cropMode) {
            return false
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                latestDownX = event.x
                latestDownY = event.y
                calculatePoint2Move(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                point2Move.x = (event.x - latestDownX) + point2Move.x
                point2Move.y = (event.y - latestDownY) + point2Move.y
                movePoints()
                latestDownY = event.y
                latestDownX = event.x
            }
        }
        return true
    }

    private fun calculatePoint2Move(downX: Float, downY: Float) {
        point2Move = points.minByOrNull { abs((it.x - downX).times(it.y - downY)) } ?: topLeft
    }

    private fun movePoints() {
        with(path) {
            reset()
            moveTo(topLeft.x.toFloat(), topLeft.y.toFloat())
            lineTo(topRight.x.toFloat(), topRight.y.toFloat())
            lineTo(bottomRight.x.toFloat(), bottomRight.y.toFloat())
            lineTo(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())
            close()
        }
        invalidate()
    }

    private fun resize() {
        topLeft.x = topLeft.x.div(ratioX)
        topLeft.y = topLeft.y.div(ratioY)
        topRight.x = topRight.x.div(ratioX)
        topRight.y = topRight.y.div(ratioY)
        bottomRight.x = bottomRight.x.div(ratioX)
        bottomRight.y = bottomRight.y.div(ratioY)
        bottomLeft.x = bottomLeft.x.div(ratioX)
        bottomLeft.y = bottomLeft.y.div(ratioY)
    }
}