package com.kraptor

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.min

/**
 * ZoomHelper — lightweight pinch-zoom + pan + double-tap + TV D-pad for ImageView.
 *
 * TV controls (when the item view has focus):
 *   DPAD_CENTER / ENTER → toggle between 1x and 2.5x
 *   DPAD_UP / DPAD_DOWN  → pan vertically when zoomed
 *   BACK                 → reset zoom (handled by fragment for dismiss)
 */
@SuppressLint("ClickableViewAccessibility")
class ZoomHelper(private val imageView: ImageView) {

    private val matrix = android.graphics.Matrix()
    private var currentScale = 1f
    private var baseScale = 1f

    private val minScale = 1f
    private val maxScale = 4f
    private var isSetup = true

    // ── Scale gesture ──────────────────────────────────────────

    private val scaleDetector = ScaleGestureDetector(
        imageView.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(d: ScaleGestureDetector) = true
            override fun onScale(d: ScaleGestureDetector): Boolean {
                isSetup = false
                val newScale = (currentScale * d.scaleFactor).coerceIn(minScale, maxScale)
                val applied = newScale / currentScale
                if (abs(newScale - currentScale) > 0.01f) {
                    matrix.postScale(applied, applied, d.focusX, d.focusY)
                    currentScale = newScale
                    constrain()
                    imageView.imageMatrix = matrix
                }
                return true
            }
        }
    ).apply { isQuickScaleEnabled = false }

    // ── Gesture (double-tap + scroll) ──────────────────────────

    private val gestureDetector = android.view.GestureDetector(
        imageView.context,
        object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isSetup = false
                currentScale = if (currentScale > 1.5f) 1f else 2.5f
                update(e.x, e.y)
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                if (currentScale > 1.01f) {
                    matrix.postTranslate(-dx, -dy)
                    constrain()
                    imageView.imageMatrix = matrix
                    return true
                }
                return false
            }
        }
    ).apply { setIsLongpressEnabled(false) }

    // ── Touch listener ─────────────────────────────────────────

    init {
        imageView.scaleType = ImageView.ScaleType.MATRIX

        imageView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }

        imageView.viewTreeObserver.addOnGlobalLayoutListener {
            if (imageView.drawable != null && imageView.width > 0 && isSetup) {
                resetBaseScale()
            }
        }
    }

    // ── Public API ─────────────────────────────────────────────

    fun resetBaseScale() {
        isSetup = false
        calcBaseScale()
        currentScale = 1f
        update()
    }

    /** Toggle between fit and 2.5x — ideal for TV remote. */
    fun toggleZoom() {
        isSetup = false
        currentScale = if (currentScale > 1.5f) 1f else 2.5f
        update(imageView.width / 2f, imageView.height / 2f)
    }

    // ── Internal ───────────────────────────────────────────────

    private fun calcBaseScale() {
        val d = imageView.drawable ?: return
        val vw = imageView.width.toFloat()
        val vh = imageView.height.toFloat()
        if (vw <= 0f || vh <= 0f) { baseScale = 1f; return }
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (dw <= 0f || dh <= 0f) { baseScale = 1f; return }
        baseScale = min(vw / dw, vh / dh)
    }

    private fun update(focusX: Float? = null, focusY: Float? = null) {
        val d = imageView.drawable ?: return
        val vw = imageView.width.toFloat()
        val vh = imageView.height.toFloat()
        if (vw <= 0f || vh <= 0f) return
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (dw <= 0f || dh <= 0f) { matrix.reset(); imageView.imageMatrix = matrix; return }

        val s = baseScale * currentScale
        val w = dw * s
        val h = dh * s
        val tx = (vw - w) / 2f
        val ty = (vh - h) / 2f

        matrix.reset()
        matrix.setScale(s, s)

        if (focusX != null && focusY != null) {
            val dx = focusX - vw / 2f
            val dy = focusY - vh / 2f
            matrix.postTranslate(tx - dx * (s - baseScale) / s, ty - dy * (s - baseScale) / s)
        } else {
            matrix.postTranslate(tx, ty)
        }

        constrain()
        imageView.imageMatrix = matrix
    }

    private fun constrain() {
        val d = imageView.drawable ?: return
        val vw = imageView.width.toFloat()
        val vh = imageView.height.toFloat()
        if (vw <= 0f || vh <= 0f) return

        val v = FloatArray(9)
        matrix.getValues(v)
        val s = v[android.graphics.Matrix.MSCALE_X]
        val dw = d.intrinsicWidth * s
        val dh = d.intrinsicHeight * s
        val tx = v[android.graphics.Matrix.MTRANS_X]
        val ty = v[android.graphics.Matrix.MTRANS_Y]

        var dx = 0f
        var dy = 0f

        if (dw < vw) dx = (vw - dw) / 2f - tx
        else {
            if (tx > 0f) dx = -tx
            else if (tx + dw < vw) dx = vw - dw - tx
        }

        if (dh < vh) dy = (vh - dh) / 2f - ty
        else {
            if (ty > 0f) dy = -ty
            else if (ty + dh < vh) dy = vh - dh - ty
        }

        if (abs(dx) > 0.1f || abs(dy) > 0.1f) {
            matrix.postTranslate(dx, dy)
        }
    }
}
