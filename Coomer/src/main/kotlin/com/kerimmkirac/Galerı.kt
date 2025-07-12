package com.kraptor

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.view.Gravity
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.WindowManager
import android.view.ScaleGestureDetector
import android.os.Handler
import android.os.Looper
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.PagerSnapHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import com.kerimmkirac.CoomerPlugin
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sqrt
import com.lagradost.cloudstream3.*
import androidx.core.graphics.toColorInt

class CoomerChapterFragment(
    private val plugin: CoomerPlugin,
    private val chapterName: String,
    private val pages: List<String>
) : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var titleText: TextView
    private lateinit var pageIndicator: TextView
    private var fullscreenDialog: Dialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )
        return dialog
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()

        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 16)
            setBackgroundColor("#AA000000".toColorInt())
        }

        titleText = TextView(context).apply {
            text = chapterName
            textSize = 18f
            setTextColor(Color.WHITE)
        }

        pageIndicator = TextView(context).apply {
            text = "${pages.size} images"
            textSize = 14f
            setTextColor("#CCCCCC".toColorInt())
        }

        headerLayout.addView(titleText)
        headerLayout.addView(pageIndicator)

        recyclerView = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutManager = LinearLayoutManager(context)
            adapter = ImageListAdapter(pages)
        }

        mainLayout.addView(headerLayout)
        mainLayout.addView(recyclerView)

        return mainLayout
    }

    private inner class ImageListAdapter(
        private val imageUrls: List<String>
    ) : RecyclerView.Adapter<ImageListAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(container: FrameLayout) : RecyclerView.ViewHolder(container) {
            val imageView: ImageView = container.getChildAt(0) as ImageView
            val progressBar: ProgressBar = container.getChildAt(1) as ProgressBar
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val context = parent.context
            val container = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    400
                )
                setBackgroundColor(Color.BLACK)
            }
            val imageView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val progressBar = ProgressBar(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.gravity = Gravity.CENTER }
            }
            container.addView(imageView)
            container.addView(progressBar)
            return ImageViewHolder(container)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            loadImage(imageUrls[position], holder.imageView, holder.progressBar)
            holder.imageView.setOnClickListener { showFullscreenImage(position) }
        }

        override fun getItemCount(): Int = imageUrls.size
    }

    private fun loadImage(
        url: String,
        imageView: ImageView,
        progressBar: ProgressBar
    ) {
        progressBar.visibility = View.VISIBLE
        imageView.visibility = View.GONE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bmp = downloadImage(url)
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bmp)
                    progressBar.visibility = View.GONE
                    imageView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("Coomer", "Error loading image: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    imageView.visibility = View.VISIBLE
                }
            }
        }
    }

    private suspend fun downloadImage(url: String): Bitmap = withContext(Dispatchers.IO) {
        val response = app.get(url)
        val body = response.body
        val imageBytes = body.bytes()
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun showFullscreenImage(startPos: Int) {
        val context = requireContext()
        fullscreenDialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val layout = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val rv = RecyclerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = FullscreenImageAdapter(pages)
        }
        PagerSnapHelper().attachToRecyclerView(rv)

        layout.addView(rv)
        fullscreenDialog?.setContentView(layout)
        fullscreenDialog?.show()
        rv.scrollToPosition(startPos)
    }

    private inner class FullscreenImageAdapter(
        private val imageUrls: List<String>
    ) : RecyclerView.Adapter<FullscreenImageAdapter.FullscreenViewHolder>() {

        inner class FullscreenViewHolder(container: FrameLayout) : RecyclerView.ViewHolder(container) {
            val imageView: ZoomableImageView = container.getChildAt(0) as ZoomableImageView
            val progressBar: ProgressBar = container.getChildAt(1) as ProgressBar
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullscreenViewHolder {
            val context = parent.context
            val container = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.BLACK)
            }
            val zoomView = ZoomableImageView(context, this@CoomerChapterFragment).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.MATRIX
            }
            val progressBar = ProgressBar(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.gravity = Gravity.CENTER }
            }
            container.addView(zoomView)
            container.addView(progressBar)
            return FullscreenViewHolder(container)
        }

        override fun onBindViewHolder(holder: FullscreenViewHolder, position: Int) {
            loadFullscreenImage(imageUrls[position], holder.imageView, holder.progressBar)
        }

        override fun getItemCount(): Int = imageUrls.size
    }

    private fun loadFullscreenImage(
        url: String,
        imageView: ZoomableImageView,
        progressBar: ProgressBar
    ) {
        progressBar.visibility = View.VISIBLE
        imageView.visibility = View.GONE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bmp = downloadImage(url)
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bmp)
                    imageView.resetZoom()
                    progressBar.visibility = View.GONE
                    imageView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("Coomer", "Error loading image: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    imageView.visibility = View.VISIBLE
                }
            }
        }
    }

    fun closeFullscreen() {
        fullscreenDialog?.dismiss()
        fullscreenDialog = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private inner class ZoomableImageView(
        context: android.content.Context,
        private val fragment: CoomerChapterFragment
    ) : AppCompatImageView(context) {

        private var minScale = 1f
        private var maxScale = 4f
        private var currentScale = 1f
        private var baseScale = 1f
        private val mediumScale = 2f

        private val matrix = Matrix()
        private val displayRect = RectF()
        private val matrixValues = FloatArray(9)

        private val scaleGestureDetector: ScaleGestureDetector
        private val gestureDetector: GestureDetector
        private val handler = Handler(Looper.getMainLooper())

        private var lastTouchX = 0f
        private var lastTouchY = 0f
        private var isDragging = false
        private var isScaling = false
        private var allowDrag = true

        // Double tap handling
        private var doubleTapRunnable: Runnable? = null
        private var tapCount = 0
        private val doubleTapTimeout = 300L

        init {
            scaleType = ScaleType.MATRIX
            imageMatrix = matrix

            scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
            gestureDetector = GestureDetector(context, GestureListener())
        }

        private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val newScale = currentScale * scaleFactor

                if (newScale in minScale..maxScale) {
                    matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                    currentScale = newScale
                    checkMatrixBounds()
                    imageMatrix = matrix
                    allowDrag = currentScale > minScale

                    // Immediately block parent scrolling when scaling
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScaling = true
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isScaling = false
                // Only allow parent scrolling if we're at minimum scale
                parent.requestDisallowInterceptTouchEvent(currentScale > minScale)

                // Snap to nearest scale level
                snapToNearestScale()
            }
        }

        private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                handleTap()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                handleDoubleTap(e.x, e.y)
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (currentScale <= minScale * 1.1f && e1 != null) {
                    val dy = e2.y - e1.y
                    val dx = e2.x - e1.x

                    // Vertical swipe to close
                    if (kotlin.math.abs(dy) > kotlin.math.abs(dx) && dy > 200 && kotlin.math.abs(velocityY) > 500) {
                        fragment.closeFullscreen()
                        return true
                    }
                }
                return false
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Critical: Handle parent scrolling properly
            val shouldBlockParent = currentScale > minScale || isScaling ||
                    scaleGestureDetector.isInProgress
            parent.requestDisallowInterceptTouchEvent(shouldBlockParent)

            var handled = scaleGestureDetector.onTouchEvent(event)
            handled = gestureDetector.onTouchEvent(event) || handled

            if (!isScaling && allowDrag) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.x
                        lastTouchY = event.y
                        isDragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (currentScale > minScale) {
                            val dx = event.x - lastTouchX
                            val dy = event.y - lastTouchY

                            if (!isDragging && (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10)) {
                                isDragging = true
                                parent.requestDisallowInterceptTouchEvent(true)
                            }

                            if (isDragging) {
                                matrix.postTranslate(dx, dy)
                                checkMatrixBounds()
                                imageMatrix = matrix
                                handled = true
                            }

                            lastTouchX = event.x
                            lastTouchY = event.y
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                        // Re-evaluate whether to block parent after touch ends
                        parent.requestDisallowInterceptTouchEvent(currentScale > minScale)
                    }
                }
            }

            return handled || super.onTouchEvent(event)
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        private fun handleTap() {
            tapCount++
            doubleTapRunnable?.let { handler.removeCallbacks(it) }

            if (tapCount == 1) {
                doubleTapRunnable = Runnable {
                    tapCount = 0
                    // Single tap - do nothing or close if needed
                }
                handler.postDelayed(doubleTapRunnable!!, doubleTapTimeout)
            } else if (tapCount == 2) {
                doubleTapRunnable?.let { handler.removeCallbacks(it) }
                tapCount = 0
                handleDoubleTap(width / 2f, height / 2f)
            }
        }

        private fun handleDoubleTap(focusX: Float, focusY: Float) {
            val targetScale = when {
                currentScale < mediumScale -> mediumScale
                currentScale < maxScale -> maxScale
                else -> minScale
            }

            animateScaleTo(targetScale, focusX, focusY)
        }

        private fun animateScaleTo(targetScale: Float, focusX: Float, focusY: Float) {
            val startScale = currentScale
            val animator = ValueAnimator.ofFloat(0f, 1f)

            animator.duration = 300
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val scale = startScale + (targetScale - startScale) * progress
                val scaleFactor = scale / currentScale

                matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                currentScale = scale
                checkMatrixBounds()
                imageMatrix = matrix

                allowDrag = currentScale > minScale
                // Keep parent blocked during animation if scaling up
                parent.requestDisallowInterceptTouchEvent(currentScale > minScale)
            }

            animator.start()
        }

        private fun snapToNearestScale() {
            val targetScale = when {
                currentScale < (minScale + mediumScale) / 2 -> minScale
                currentScale < (mediumScale + maxScale) / 2 -> mediumScale
                else -> maxScale
            }

            if (kotlin.math.abs(currentScale - targetScale) > 0.1f) {
                animateScaleTo(targetScale, width / 2f, height / 2f)
            }
        }

        private fun checkMatrixBounds() {
            val rect = getDisplayRect()
            val height = rect.height()
            val width = rect.width()
            var deltaX = 0f
            var deltaY = 0f
            val viewHeight = getHeight()
            val viewWidth = getWidth()

            if (height <= viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top
            } else {
                when {
                    rect.top > 0 -> deltaY = -rect.top
                    rect.bottom < viewHeight -> deltaY = viewHeight - rect.bottom
                }
            }

            if (width <= viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left
            } else {
                when {
                    rect.left > 0 -> deltaX = -rect.left
                    rect.right < viewWidth -> deltaX = viewWidth - rect.right
                }
            }

            matrix.postTranslate(deltaX, deltaY)
        }

        private fun getDisplayRect(): RectF {
            val drawable = this.drawable ?: return RectF()
            displayRect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            matrix.mapRect(displayRect)
            return displayRect
        }

        override fun setImageBitmap(bm: Bitmap?) {
            super.setImageBitmap(bm)
            bm ?: return

            post {
                val viewWidth = width.toFloat()
                val viewHeight = height.toFloat()
                val bmWidth = bm.width.toFloat()
                val bmHeight = bm.height.toFloat()

                if (viewWidth == 0f || viewHeight == 0f) return@post

                val scale = min(viewWidth / bmWidth, viewHeight / bmHeight)

                minScale = scale
                baseScale = scale
                maxScale = scale * 4f
                currentScale = scale

                matrix.reset()
                matrix.postScale(scale, scale)
                matrix.postTranslate(
                    (viewWidth - bmWidth * scale) / 2f,
                    (viewHeight - bmHeight * scale) / 2f
                )

                imageMatrix = matrix
                allowDrag = false
            }
        }

        fun resetZoom() {
            currentScale = baseScale
            matrix.reset()
            matrix.postScale(baseScale, baseScale)

            val drawable = this.drawable
            if (drawable != null) {
                val dx = (width - drawable.intrinsicWidth * baseScale) / 2
                val dy = (height - drawable.intrinsicHeight * baseScale) / 2
                matrix.postTranslate(dx, dy)
            }

            imageMatrix = matrix
            allowDrag = false

            // Important: Allow parent scrolling when reset to normal scale
            parent.requestDisallowInterceptTouchEvent(false)
        }
    }
}