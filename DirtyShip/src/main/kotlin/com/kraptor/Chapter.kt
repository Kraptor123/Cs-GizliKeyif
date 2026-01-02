package com.kraptor

import android.app.Dialog
import android.os.Bundle
import com.lagradost.api.Log
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
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.PagerSnapHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import kotlin.math.min
import kotlin.math.sqrt

class DirtyShipChapterFragment(
    private val plugin: DirtyShipPlugin,
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
            setBackgroundColor(Color.parseColor("#AA000000"))
        }

        titleText = TextView(context).apply {
            text = chapterName
            textSize = 18f
            setTextColor(Color.WHITE)
        }

        pageIndicator = TextView(context).apply {
            text = "${pages.size} images"
            textSize = 14f
            setTextColor(Color.parseColor("#CCCCCC"))
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
                Log.e("DirtyShip", "Error loading image: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    imageView.visibility = View.VISIBLE
                }
            }
        }
    }

    private suspend fun downloadImage(url: String): Bitmap = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection().apply { doInput = true }
        conn.connect()
        BitmapFactory.decodeStream(conn.getInputStream())
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
        // add PagerSnapHelper for true swipe paging
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
            val zoomView = ZoomableImageView(context, this@DirtyShipChapterFragment).apply {
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
                Log.e("DirtyShip", "Error loading image: ${e.message}")
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

    private inner class ZoomableImageView(
        context: android.content.Context,
        private val fragment: DirtyShipChapterFragment
    ) : AppCompatImageView(context) {

        private val NONE = 0
        private val DRAG = 1
        private val ZOOM = 2

        private var mode = NONE
        private var oldDist = 1f
        private var minScale = 1f
        private var maxScale = 3f
        private var currentScale = 1f
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        private val matrix = Matrix()
        private val savedMatrix = Matrix()
        private val startPoint = PointF()
        private val midPoint = PointF()

        private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velX: Float, velY: Float): Boolean {
                if (currentScale <= minScale * 1.1f && e1 != null) {
                    val dy = e2.y - e1.y
                    if (kotlin.math.abs(dy) > swipeThreshold && kotlin.math.abs(velY) > swipeVelocityThreshold && dy > 0) {
                        fragment.closeFullscreen()
                        return true
                    }
                }
                return false
            }
        })

        init {
            scaleType = ScaleType.MATRIX
            imageMatrix = matrix
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // only intercept for pan if zoomed
            parent.requestDisallowInterceptTouchEvent(currentScale > minScale)
            gestureDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    startPoint.set(event.x, event.y)
                    mode = DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    oldDist = calculateSpacing(event)
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix)
                        calculateMidPoint(midPoint, event)
                        mode = ZOOM
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> mode = NONE
                MotionEvent.ACTION_MOVE -> {
                    when (mode) {
                        DRAG -> if (currentScale > minScale) {
                            matrix.set(savedMatrix)
                            matrix.postTranslate(event.x - startPoint.x, event.y - startPoint.y)
                        }
                        ZOOM -> {
                            val newDist = calculateSpacing(event)
                            if (newDist > 10f) {
                                matrix.set(savedMatrix)
                                val factor = newDist / oldDist
                                val target = currentScale * factor
                                if (target in minScale..maxScale) {
                                    matrix.postScale(factor, factor, midPoint.x, midPoint.y)
                                    currentScale = target
                                }
                            }
                        }
                    }
                }
            }
            imageMatrix = matrix
            return true
        }

        override fun setImageBitmap(bm: android.graphics.Bitmap?) {
            super.setImageBitmap(bm)
            bm ?: return
            post {
                val vw = width.toFloat()
                val vh = height.toFloat()
                val bw = bm.width.toFloat()
                val bh = bm.height.toFloat()
                val fit = min(vw / bw, vh / bh)
                minScale = fit
                maxScale = fit * 3f
                currentScale = fit
                matrix.reset()
                matrix.postScale(fit, fit)
                matrix.postTranslate((vw - bw * fit) / 2, (vh - bh * fit) / 2)
                imageMatrix = matrix
            }
        }

        fun resetZoom() {
            matrix.reset()
            currentScale = minScale
            matrix.postScale(minScale, minScale)
            val dx = (width - drawable.intrinsicWidth * minScale) / 2
            val dy = (height - drawable.intrinsicHeight * minScale) / 2
            matrix.postTranslate(dx, dy)
            imageMatrix = matrix
        }

        private fun calculateSpacing(event: MotionEvent): Float {
            val dx = event.getX(0) - event.getX(1)
            val dy = event.getY(0) - event.getY(1)
            return sqrt(dx * dx + dy * dy)
        }

        private fun calculateMidPoint(point: PointF, event: MotionEvent) {
            point.set((event.getX(0) + event.getX(1)) / 2, (event.getY(0) + event.getY(1)) / 2)
        }
    }
}