package com.airatlovesmusic.scanner.ui.scan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.airatlovesmusic.scanner.entity.Corners
import com.airatlovesmusic.scanner.entity.Point
import kotlin.math.abs

class DocumentCornersView : View {

    constructor(context: Context) : super(context)
    constructor(context: Context, attributes: AttributeSet) : super(context, attributes)
    constructor(context: Context, attributes: AttributeSet, defTheme: Int) : super(context, attributes, defTheme)

    private val rectPaint = Paint()
    private val circlePaint = Paint()
    private val fillPaint = Paint()
    private var ratioX: Double = 1.0
    private var ratioY: Double = 1.0
    private var topLeft: Point = Point()
    private var topRight: Point = Point()
    private var bottomRight: Point = Point()
    private var bottomLeft: Point = Point()
    private val path: Path = Path()
    private var point2Move = Point()
    private var latestDownX = 0.0F
    private var latestDownY = 0.0F
    var cropMode = false
        set(value) {
            field = value
            invalidate()
        }

    init {
        rectPaint.color = Color.parseColor("#3454D1")
        rectPaint.isAntiAlias = true
        rectPaint.isDither = true
        rectPaint.strokeWidth = 6F
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        rectPaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        rectPaint.pathEffect = CornerPathEffect(10f)

        fillPaint.color = Color.parseColor("#3454D1")
        fillPaint.alpha = 60
        fillPaint.isAntiAlias = true
        fillPaint.isDither = true
        fillPaint.strokeWidth = 6F
        fillPaint.style = Paint.Style.FILL
        fillPaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        fillPaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        fillPaint.pathEffect = CornerPathEffect(10f)

        circlePaint.color = Color.LTGRAY
        circlePaint.isDither = true
        circlePaint.isAntiAlias = true
        circlePaint.strokeWidth = 4F
        circlePaint.style = Paint.Style.STROKE
    }

    fun onCornersDetected(corners: Corners) {
        ratioX = corners.size.width.div(measuredWidth)
        ratioY = corners.size.height.div(measuredHeight)
        topLeft = corners.corners.getOrNull(0) ?: Point()
        topRight = corners.corners.getOrNull(1) ?: Point()
        bottomRight = corners.corners.getOrNull(2) ?: Point()
        bottomLeft = corners.corners.getOrNull(3) ?: Point()
        resize()
        path.reset()
        path.moveTo(topLeft.x.toFloat(), topLeft.y.toFloat())
        path.lineTo(topRight.x.toFloat(), topRight.y.toFloat())
        path.lineTo(bottomRight.x.toFloat(), bottomRight.y.toFloat())
        path.lineTo(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())
        path.close()

        invalidate()
    }

    fun onCornersNotDetected() {
        path.reset()
        invalidate()
    }

    fun getPoints() =
        listOf(topLeft, topRight, bottomRight, bottomLeft).map { org.opencv.core.Point(it.x, it.y) }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawPath(path, fillPaint)
        canvas?.drawPath(path, rectPaint)
        if (cropMode) {
            canvas?.drawCircle(topLeft.x.toFloat(), topLeft.y.toFloat(), 20F, circlePaint)
            canvas?.drawCircle(topRight.x.toFloat(), topRight.y.toFloat(), 20F, circlePaint)
            canvas?.drawCircle(bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), 20F, circlePaint)
            canvas?.drawCircle(bottomRight.x.toFloat(), bottomRight.y.toFloat(), 20F, circlePaint)
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
        val points = listOf(topLeft, topRight, bottomRight, bottomLeft)
        point2Move = points.minByOrNull { abs((it.x - downX).times(it.y - downY)) } ?: topLeft
    }

    private fun movePoints() {
        path.reset()
        path.moveTo(topLeft.x.toFloat(), topLeft.y.toFloat())
        path.lineTo(topRight.x.toFloat(), topRight.y.toFloat())
        path.lineTo(bottomRight.x.toFloat(), bottomRight.y.toFloat())
        path.lineTo(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())
        path.close()
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