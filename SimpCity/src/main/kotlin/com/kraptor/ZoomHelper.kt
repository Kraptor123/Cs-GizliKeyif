package com.kraptor

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.OverScroller
import kotlin.math.abs
import kotlin.math.min

@SuppressLint("ClickableViewAccessibility")
class ZoomHelper(
    private val imageView: ImageView,
    private val onSingleTap: (() -> Unit)? = null
) {

    private var currentScale = 1f
    private var baseScale = 1f
    private val minScale = 1f
    private val maxScale = 5f

    private var panX = 0f
    private var panY = 0f

    private val scroller = OverScroller(imageView.context)
    private var velocityTracker: VelocityTracker? = null
    private val flingRunnable = Runnable { flingStep() }

    private val touchSlop = ViewConfiguration.get(imageView.context).scaledTouchSlop

    private var isZooming = false
    private var isDragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var pointerId = -1
    private var isSetup = true

    private val gestureDetector = GestureDetector(
        imageView.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onSingleTap?.invoke()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                isSetup = false
                cancelFling()
                if (currentScale > minScale * 1.5f) {
                    animateReset()
                } else {
                    animateZoomTo(2.5f, e.x, e.y)
                }
                return true
            }
        }
    )

    private val scaleDetector = ScaleGestureDetector(
        imageView.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
                isZooming = true
                cancelFling()
                return true
            }

            override fun onScale(d: ScaleGestureDetector): Boolean {
                isSetup = false
                val newScale = (currentScale * d.scaleFactor).coerceIn(minScale, maxScale)
                val applied = newScale / currentScale
                if (abs(newScale - currentScale) > 0.001f) {
                    val fx = d.focusX
                    val fy = d.focusY
                    panX = applied * panX + (1f - applied) * (fx - imageView.width / 2f)
                    panY = applied * panY + (1f - applied) * (fy - imageView.height / 2f)
                    currentScale = newScale
                    applyTransform()
                }
                return true
            }

            override fun onScaleEnd(d: ScaleGestureDetector) {
                isZooming = false
                if (currentScale < minScale * 1.05f) {
                    animateReset()
                }
            }
        }
    ).apply { isQuickScaleEnabled = false }

    init {
        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> onTouchDown(event)
                MotionEvent.ACTION_MOVE -> onTouchMove(event)
                MotionEvent.ACTION_UP -> onTouchUp(event)
                MotionEvent.ACTION_CANCEL -> onTouchCancel()
                MotionEvent.ACTION_POINTER_UP -> onPointerUp(event)
            }
            true
        }

        imageView.viewTreeObserver.addOnGlobalLayoutListener {
            if (imageView.drawable != null && imageView.width > 0 && isSetup) {
                resetBaseScale()
            }
        }
    }

    private fun onTouchDown(event: MotionEvent) {
        cancelFling()
        pointerId = event.getPointerId(0)
        lastTouchX = event.x
        lastTouchY = event.y

        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(event)
    }

    private fun onTouchMove(event: MotionEvent) {
        velocityTracker?.addMovement(event)
        if (isZooming) return

        val pointerIndex = event.findPointerIndex(pointerId)
        if (pointerIndex < 0) return

        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        val dx = x - lastTouchX
        val dy = y - lastTouchY

        if (!isDragging && currentScale > minScale * 1.05f) {
            if (abs(dx) > touchSlop || abs(dy) > touchSlop) isDragging = true
        }

        if (isDragging) {
            isSetup = false
            panX += dx
            panY += dy
            constrainPan()
            applyTransform()
        }
        lastTouchX = x
        lastTouchY = y
    }

    private fun onTouchUp(event: MotionEvent) {
        if (isDragging && currentScale > minScale * 1.05f) {
            velocityTracker?.addMovement(event)
            velocityTracker?.computeCurrentVelocity(1000)
            val vx = velocityTracker?.xVelocity ?: 0f
            val vy = velocityTracker?.yVelocity ?: 0f
            if (abs(vx) > 100 || abs(vy) > 100) startFling(-vx, -vy)
        }
        isDragging = false
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun onTouchCancel() {
        isDragging = false
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun onPointerUp(event: MotionEvent) {
        val newIndex = if (event.actionIndex == 0) 1 else 0
        if (newIndex < event.pointerCount) {
            pointerId = event.getPointerId(newIndex)
            lastTouchX = event.getX(newIndex)
            lastTouchY = event.getY(newIndex)
        }
    }

    private fun startFling(vx: Float, vy: Float) {
        val maxX = getMaxPanX(); val maxY = getMaxPanY()
        scroller.fling(panX.toInt(), panY.toInt(), vx.toInt(), vy.toInt(),
            -maxX.toInt(), maxX.toInt(), -maxY.toInt(), maxY.toInt(), 50, 50)
        imageView.post(flingRunnable)
    }

    private fun flingStep() {
        if (scroller.computeScrollOffset()) {
            panX = scroller.currX.toFloat()
            panY = scroller.currY.toFloat()
            constrainPan(); applyTransform()
            imageView.post(flingRunnable)
        }
    }

    private fun cancelFling() {
        scroller.forceFinished(true)
        imageView.removeCallbacks(flingRunnable)
    }

    fun resetBaseScale() {
        isSetup = false
        val d = imageView.drawable ?: return
        val vw = imageView.width.toFloat(); val vh = imageView.height.toFloat()
        if (vw <= 0f || vh <= 0f) return
        val dw = d.intrinsicWidth.toFloat(); val dh = d.intrinsicHeight.toFloat()
        if (dw <= 0f || dh <= 0f) return

        baseScale = min(vw / dw, vh / dh)
        currentScale = 1f
        panX = 0f; panY = 0f
        applyTransform()
    }

    fun isZoomed(): Boolean = currentScale > minScale * 1.05f

    fun toggleZoom(focusX: Float? = null, focusY: Float? = null) {
        isSetup = false; cancelFling()
        if (currentScale > minScale * 1.5f) {
            animateReset()
        } else {
            val fx = focusX ?: (imageView.width / 2f)
            val fy = focusY ?: (imageView.height / 2f)
            animateZoomTo(2.5f, fx, fy)
        }
    }

    fun panByDirection(dx: Float, dy: Float): Boolean {
        if (!isZoomed()) return false
        panX += dx; panY += dy
        constrainPan(); applyTransform()
        return true
    }

    private val animateRunnable = Runnable { animateStep() }
    private var animStartScale = 1f; private var animEndScale = 1f
    private var animStartPanX = 0f; private var animStartPanY = 0f
    private var animEndPanX = 0f; private var animEndPanY = 0f
    private var animStartTime = 0L

    private fun animateReset() {
        animateZoomTo(minScale, imageView.width / 2f, imageView.height / 2f, true)
    }

    private fun animateZoomTo(targetScale: Float, focusX: Float, focusY: Float, isReset: Boolean = false) {
        cancelFling()
        animStartScale = currentScale
        animEndScale = targetScale
        animStartPanX = panX
        animStartPanY = panY

        if (isReset) {
            animEndPanX = 0f
            animEndPanY = 0f
        } else {
            val applied = targetScale / currentScale
            
            // Correct math for pan offset from center:
            // panX_new = applied * panX + (1 - applied) * (focusX - width/2)
            animEndPanX = applied * panX + (1f - applied) * (focusX - imageView.width / 2f)
            animEndPanY = applied * panY + (1f - applied) * (focusY - imageView.height / 2f)
            
            val oldScale = currentScale
            val oldPanX = panX
            val oldPanY = panY
            
            currentScale = targetScale
            panX = animEndPanX
            panY = animEndPanY
            constrainPan()
            
            animEndPanX = panX
            animEndPanY = panY
            
            currentScale = oldScale
            panX = oldPanX
            panY = oldPanY
        }

        animStartTime = System.currentTimeMillis()
        imageView.post(animateRunnable)
    }

    private fun animateStep() {
        val elapsed = System.currentTimeMillis() - animStartTime
        val t = (elapsed.toFloat() / 250L).coerceIn(0f, 1f)
        val ease = 1f - (1f - t) * (1f - t)

        currentScale = animStartScale + (animEndScale - animStartScale) * ease
        panX = animStartPanX + (animEndPanX - animStartPanX) * ease
        panY = animStartPanY + (animEndPanY - animStartPanY) * ease
        constrainPan(); applyTransform()

        if (t < 1f) imageView.post(animateRunnable)
        else if (currentScale <= minScale * 1.05f) {
            currentScale = minScale; panX = 0f; panY = 0f; applyTransform()
        }
    }

    private fun applyTransform() {
        val d = imageView.drawable ?: return
        val vw = imageView.width.toFloat(); val vh = imageView.height.toFloat()
        val s = baseScale * currentScale
        val dw = d.intrinsicWidth * s; val dh = d.intrinsicHeight * s
        val matrix = android.graphics.Matrix()
        matrix.setScale(s, s)
        matrix.postTranslate((vw - dw) / 2f + panX, (vh - dh) / 2f + panY)
        imageView.imageMatrix = matrix
    }

    private fun constrainPan() {
        val d = imageView.drawable ?: return
        val vw = imageView.width.toFloat(); val vh = imageView.height.toFloat()
        val s = baseScale * currentScale
        val dw = d.intrinsicWidth * s; val dh = d.intrinsicHeight * s

        if (dw <= vw) panX = 0f else {
            val maxPanX = (dw - vw) / 2f
            panX = panX.coerceIn(-maxPanX, maxPanX)
        }
        if (dh <= vh) panY = 0f else {
            val maxPanY = (dh - vh) / 2f
            panY = panY.coerceIn(-maxPanY, maxPanY)
        }
    }

    private fun getMaxPanX(): Float {
        val d = imageView.drawable ?: return 0f
        val dw = d.intrinsicWidth * baseScale * currentScale
        return if (dw > imageView.width) (dw - imageView.width) / 2f else 0f
    }

    private fun getMaxPanY(): Float {
        val d = imageView.drawable ?: return 0f
        val dh = d.intrinsicHeight * baseScale * currentScale
        return if (dh > imageView.height) (dh - imageView.height) / 2f else 0f
    }
}
