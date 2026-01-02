package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.collection.LruCache
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.asDrawable
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Scale
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kraptor.BuildConfig
import com.lagradost.api.Log
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Full working fragment + adapter + zoom helper combined.
 * Uses Coil enqueue + target for safe ImageView updates and cancellation.
 */
class CoomerChapterFragment(
    private val plugin: CoomerPlugin,
    private val manga: List<String>
) : BottomSheetDialogFragment() {

    @SuppressLint("DiscouragedApi")
    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        require(id != 0) { "View ID '$name' not found" }
        return findViewById(id)
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = plugin.resources!!.getIdentifier("chapter", "layout", BuildConfig.LIBRARY_PACKAGE_NAME)
        val layout = plugin.resources!!.getLayout(layoutId)
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setOnShowListener { dialogInterface ->
            (dialogInterface as? BottomSheetDialog)?.apply {
                findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                    BottomSheetBehavior.from(sheet).apply {
                        state = BottomSheetBehavior.STATE_EXPANDED
                        peekHeight = resources.displayMetrics.heightPixels
                        isDraggable = false
                    }
                    sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }

        if (manga.isEmpty()) {
//            Log.w("CoomerChapter", "Empty manga list")
            dismiss()
            return
        }

        val recyclerView = view.findView<RecyclerView>("page_list")
        recyclerView.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false).apply {
                initialPrefetchItemCount = 3
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        (recyclerView.adapter as? ImageAdapter)?.preloadVisibleAndAdjacentImages(recyclerView)
                    }
                }
            })
        }

        PagerSnapHelper().attachToRecyclerView(recyclerView)
        recyclerView.adapter = ImageAdapter(plugin, manga, requireContext())
    }
}

/**
 * ImageAdapter using Coil.enqueue + target. Handles thumbnail then full-res replacement,
 * uses requestId + imageView.tag to avoid stale updates, and LruCache for bitmaps.
 */
class ImageAdapter(
    private val plugin: CoomerPlugin,
    private val imageUrls: List<String>,
    private val context: Context
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private val screenWidth: Int
    private val screenHeight: Int
    private val viewHolders = mutableMapOf<Int, ImageViewHolder>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Memory-aware cache (KB)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val imageCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount / 1024
        }
    }

    // Precomputed alternative URLs (thumbnails)
    private val altUrls = imageUrls.map { url ->
        url.substringAfter("/data").takeIf { it.isNotEmpty() }?.let {
            "https://img.coomer.st/thumbnail/data$it"
        }
    }

    init {
        context.resources.displayMetrics.let {
            screenWidth = it.widthPixels
            screenHeight = it.heightPixels
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        coroutineScope.cancel()
    }

    inner class ImageViewHolder(
        private val plugin: CoomerPlugin,
        view: View,
        private val adapter: ImageAdapter
    ) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findView("page")
        private val zoomHelper = ZoomHelper(imageView)
        private var currentRequestId: String? = null
        private var currentDisposable: Disposable? = null

        init {
            imageView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            imageView.scaleType = ImageView.ScaleType.MATRIX
            imageView.setBackgroundColor(0xFF121212.toInt())
        }

        fun loadImage(targetPosition: Int) {
            // Cancel any previous Coil request
            cancelLoad()

            if (targetPosition !in 0 until imageUrls.size) return
            val primaryUrl = imageUrls[targetPosition]
            val altUrl = if (targetPosition < altUrls.size) altUrls[targetPosition] else null

            // Unique ID for this load; store on both holder and imageView tag
            val requestId = "$primaryUrl-$targetPosition-${System.currentTimeMillis()}"
            currentRequestId = requestId
            imageView.tag = requestId

            // 1) If full-res cached -> show immediately and return
            imageCache.get(primaryUrl)?.let { bitmap ->
                if (imageView.tag == requestId) {
                    imageView.setImageBitmap(bitmap)
                    imageView.post { zoomHelper.resetBaseScale() }
                }
                return
            }

            // 2) If thumbnail cached -> show it quickly (still start full-res)
            altUrl?.let { thumbUrl ->
                imageCache.get(thumbUrl)?.let { thumbBitmap ->
                    if (imageView.tag == requestId) {
                        imageView.setImageBitmap(thumbBitmap)
                        imageView.post { zoomHelper.resetBaseScale() }
                    }
                }
            }

            // placeholder
            imageView.setImageResource(android.R.drawable.progress_indeterminate_horizontal)

            // If thumbnail exists and not cached -> enqueue thumbnail request first; on success enqueue full
            if (!altUrl.isNullOrEmpty() && imageCache.get(altUrl) == null) {
                enqueueImage(
                    url = altUrl,
                    requestId = requestId,
                    onSuccess = { drawable ->
                        // cache and show thumb (only if still valid)
                        if (imageView.tag == requestId) {
                            val bmp = drawableToBitmap(drawable)
                            imageCache.put(altUrl, bmp)
                            imageView.setImageBitmap(bmp)
                            imageView.post { zoomHelper.resetBaseScale() }
                        }
                        // after thumbnail shown/handled, start full-res (if not cached)
                        if (imageCache.get(primaryUrl) == null) {
                            enqueueFull(primaryUrl, requestId)
                        } else {
                            // already cached while thumb was fetched
                            imageCache.get(primaryUrl)?.let { fullBmp ->
                                if (imageView.tag == requestId) {
                                    imageView.setImageBitmap(fullBmp)
                                    imageView.post { zoomHelper.resetBaseScale() }
                                }
                            }
                        }
                    },
                    onError = {
                        // thumb failed - still try full
                        enqueueFull(primaryUrl, requestId)
                    }
                )
            } else {
                // No thumb to fetch or thumb already cached -> start full directly (or enqueue)
                enqueueFull(primaryUrl, requestId)
            }
        }

        private fun enqueueFull(url: String, requestId: String) {
            // If full already cached, show and return
            imageCache.get(url)?.let { bmp ->
                if (imageView.tag == requestId) {
                    imageView.setImageBitmap(bmp)
                    imageView.post { zoomHelper.resetBaseScale() }
                }
                return
            }

            enqueueImage(
                url = url,
                requestId = requestId,
                onSuccess = { drawable ->
                    if (imageView.tag == requestId) {
                        val bmp = drawableToBitmap(drawable)
                        imageCache.put(url, bmp)
                        imageView.setImageBitmap(bmp)
                        imageView.post { zoomHelper.resetBaseScale() }
                    }
                },
                onError = {
                    if (imageView.tag == requestId) {
                        imageView.setImageResource(android.R.drawable.stat_notify_error)
                        imageView.post { zoomHelper.resetBaseScale() }
                    }
                }
            )
        }

        /**
         * Enqueue a Coil request that sets drawable via target lambda.
         * It stores disposable into currentDisposable and ensures stale updates are ignored using imageView.tag.
         */
        private fun enqueueImage(
            url: String,
            requestId: String,
            onSuccess: (Drawable) -> Unit,
            onError: () -> Unit
        ) {
            try {
                currentDisposable?.dispose()
                val headers = getNetworkHeaders()
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(screenWidth, screenHeight)
                    .scale(Scale.FILL)
                    .httpHeaders(headers)
                    .target { resultImage ->
                        // resultImage: coil3.Image (ör. BitmapImage) — drawable'a çeviriyoruz
                        val drawable = when (resultImage) {
                            is BitmapImage -> BitmapDrawable(context.resources, resultImage.bitmap)
                            else -> resultImage.asDrawable(context.resources)
                        }

                        // main thread içinde çalıştırıldığını varsayarak doğrudan çağırıyoruz
                        if (imageView.tag == requestId) {
                            try {
                                onSuccess(drawable)
                            } catch (e: Throwable) {
//                                Log.w("ImageAdapter", "onSuccess handler error: ${e.message}")
                            }
                        } else {
                            // stale - ignore
                        }
                    }
                    .listener(
                        onError = { _, _ ->
                            if (imageView.tag == requestId) {
                                onError()
                            }
                        }
                    )
                    .build()

                val disp = plugin.imageLoader.enqueue(request)
                currentDisposable = disp
            } catch (e: Exception) {
//                Log.w("ImageAdapter", "enqueueImage failed for $url: ${e.message}")
                imageView.post { if (imageView.tag == requestId) onError() }
            }
        }


        fun cancelLoad() {
            currentRequestId = null
            imageView.tag = null
            try {
                currentDisposable?.dispose()
            } catch (_: Throwable) {}
            currentDisposable = null
        }

        // convert drawable to bitmap helper
        private fun drawableToBitmap(drawable: Drawable): Bitmap {
            if (drawable is BitmapDrawable) {
                drawable.bitmap?.let { return it }
            }

            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            return androidx.core.graphics.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                val canvas = android.graphics.Canvas(this)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }

        @SuppressLint("DiscouragedApi")
        private fun <T : View> View.findView(name: String): T {
            val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
            require(id != 0) { "View ID '$name' not found" }
            return findViewById(id)
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val layoutId = plugin.resources!!.getIdentifier("page", "layout", BuildConfig.LIBRARY_PACKAGE_NAME)
        require(layoutId != 0) { "Layout 'page' not found" }

        return ImageViewHolder(
            plugin,
            LayoutInflater.from(context).inflate(
                plugin.resources!!.getLayout(layoutId),
                parent,
                false
            ).apply {
                layoutParams = ViewGroup.LayoutParams(screenWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            },
            this
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        viewHolders[position] = holder
        holder.loadImage(position)
        if (position == 0) preloadVisibleAndAdjacentImages(holder.itemView.parent as? RecyclerView)
    }

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelLoad()
        // Remove by any stored index (bindingAdapterPosition may be NO_POSITION if recycled)
        val toRemove = viewHolders.entries.firstOrNull { it.value == holder }?.key
        if (toRemove != null) viewHolders.remove(toRemove)
    }

    // Preload visible + adjacent images (uses fetchBitmap)
    fun preloadVisibleAndAdjacentImages(recyclerView: RecyclerView?) {
        if (recyclerView == null || recyclerView.layoutManager !is LinearLayoutManager) return
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

        val preloadRange = max(0, firstVisible - 1)..min(itemCount - 1, lastVisible + 1)
        coroutineScope.launch(Dispatchers.IO) {
            preloadRange.forEach { position ->
                preloadSingleImage(position)
            }
        }
    }

    private suspend fun preloadSingleImage(position: Int) {
        if (position !in 0 until imageUrls.size) return
        val url = imageUrls[position]
        if (url.isEmpty() || imageCache.get(url) != null) return

        try {
            val bitmap = fetchBitmap(url)
            if (position in 0 until imageUrls.size && imageUrls[position] == url) {
                imageCache.put(url, bitmap)
//                Log.d("ImageAdapter", "Preloaded image at position $position")
            } else {
                try { bitmap.recycle() } catch (_: Throwable) {}
            }
        } catch (e: Exception) {
//            Log.d("ImageAdapter", "Preload failed for $position: ${e.message}")
        }
    }

    // Suspend helper to fetch bitmap (used for preload). Uses imageLoader.execute
    private suspend fun fetchBitmap(url: String): Bitmap {
        val headers = getNetworkHeaders()
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(screenWidth, screenHeight)
            .scale(Scale.FILL)
            .httpHeaders(headers)
            .build()

        val result = plugin.imageLoader.execute(request)
        if (result is SuccessResult) {
            val image = result.image
            return when (image) {
                is BitmapImage -> image.bitmap
                else -> {
                    val drawable = image.asDrawable(context.resources)
                    // convert drawable -> bitmap
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        val width = drawable.intrinsicWidth.coerceAtLeast(1)
                        val height = drawable.intrinsicHeight.coerceAtLeast(1)
                        androidx.core.graphics.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                            val canvas = android.graphics.Canvas(this)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                        }
                    }
                }
            }
        } else {
            throw Exception("Image request failed for $url")
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        return androidx.core.graphics.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }

    override fun getItemCount() = imageUrls.size
}

/**
 * ZoomHelper - tam işlevsel; orijinal mantığın aynısını korur.
 */
@SuppressLint("ClickableViewAccessibility")
class ZoomHelper(private val imageView: ImageView) {
    private val matrix = Matrix()
    private var currentScale = 1f        // RELATIVE scale (1 = baseScale)
    private var baseScale = 1f          // Scale that fits the image into the view
    private val minScale = 1f
    private val maxScale = 4f
    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    private var isInitialSetup = true

    init {
        imageView.scaleType = ImageView.ScaleType.MATRIX

        scaleGestureDetector = ScaleGestureDetector(
            imageView.context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    isInitialSetup = false
                    val scaleFactor = detector.scaleFactor
                    val newScale = (currentScale * scaleFactor).coerceIn(minScale, maxScale)
                    val appliedFactor = newScale / currentScale
                    if (abs(newScale - currentScale) > 0.01f) {
                        matrix.postScale(appliedFactor, appliedFactor, detector.focusX, detector.focusY)
                        currentScale = newScale
                        constrainTranslation()
                        imageView.imageMatrix = matrix
                    }
                    return true
                }
            }
        ).apply {
            isQuickScaleEnabled = false
        }

        gestureDetector = GestureDetector(
            imageView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    isInitialSetup = false
                    currentScale = if (currentScale > 1.5f) 1f else 2.5f
                    updateScaleAndTranslation(e.x, e.y)
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (currentScale > 1.01f) {
                        matrix.postTranslate(-distanceX, -distanceY)
                        constrainTranslation()
                        imageView.imageMatrix = matrix
                        return true
                    }
                    return false
                }
            }
        ).apply {
            setIsLongpressEnabled(false)
        }

        imageView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }

        imageView.viewTreeObserver.addOnGlobalLayoutListener {
            if (imageView.drawable != null && imageView.width > 0 && imageView.height > 0) {
                if (isInitialSetup) {
                    resetBaseScale()
                }
            }
        }
    }

    fun resetBaseScale() {
        isInitialSetup = false
        calculateBaseScale()
        currentScale = 1f
        updateScaleAndTranslation()
    }

    fun resetZoom() {
        isInitialSetup = false
        currentScale = 1f
        updateScaleAndTranslation()
    }

    private fun calculateBaseScale() {
        val drawable = imageView.drawable ?: return

        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()

        if (viewWidth <= 0f || viewHeight <= 0f) {
            baseScale = 1f
            return
        }

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        if (drawableWidth <= 0f || drawableHeight <= 0f) {
            baseScale = 1f
            return
        }

        val scaleX = viewWidth / drawableWidth
        val scaleY = viewHeight / drawableHeight
        baseScale = min(scaleX, scaleY)
    }

    private fun updateScaleAndTranslation(focusX: Float? = null, focusY: Float? = null) {
        val drawable = imageView.drawable ?: return
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        if (drawableWidth <= 0f || drawableHeight <= 0f) {
            matrix.reset()
            imageView.imageMatrix = matrix
            return
        }

        val finalScale = baseScale * currentScale

        val width = drawableWidth * finalScale
        val height = drawableHeight * finalScale

        val translateX = (viewWidth - width) / 2f
        val translateY = (viewHeight - height) / 2f

        matrix.reset()
        matrix.setScale(finalScale, finalScale)

        if (focusX != null && focusY != null) {
            val dx = focusX - viewWidth / 2f
            val dy = focusY - viewHeight / 2f
            matrix.postTranslate(translateX - dx * (finalScale - baseScale) / finalScale, translateY - dy * (finalScale - baseScale) / finalScale)
        } else {
            matrix.postTranslate(translateX, translateY)
        }

        constrainTranslation()
        imageView.imageMatrix = matrix
    }

    private fun constrainTranslation() {
        val drawable = imageView.drawable ?: return
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val values = FloatArray(9)
        matrix.getValues(values)

        val scale = values[Matrix.MSCALE_X]
        val drawableWidth = drawable.intrinsicWidth * scale
        val drawableHeight = drawable.intrinsicHeight * scale

        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]

        var dx = 0f
        var dy = 0f

        if (drawableWidth < viewWidth) {
            dx = (viewWidth - drawableWidth) / 2f - transX
        } else {
            if (transX > 0f) dx = -transX
            else if (transX + drawableWidth < viewWidth) dx = viewWidth - drawableWidth - transX
        }

        if (drawableHeight < viewHeight) {
            dy = (viewHeight - drawableHeight) / 2f - transY
        } else {
            if (transY > 0f) dy = -transY
            else if (transY + drawableHeight < viewHeight) dy = viewHeight - drawableHeight - transY
        }

        if (abs(dx) > 0.1f || abs(dy) > 0.1f) {
            matrix.postTranslate(dx, dy)
        }
    }
}

/**
 * Network headers used for image requests.
 */
private fun getNetworkHeaders() = NetworkHeaders.Builder()
    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    .set("Referer", "https://coomer.su/")
    .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
    .build()
