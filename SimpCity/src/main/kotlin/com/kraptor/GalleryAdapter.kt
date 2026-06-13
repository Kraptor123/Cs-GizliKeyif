package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.collection.LruCache
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.BitmapImage
import coil3.asDrawable
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Scale
import com.lagradost.api.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// BuildConfig YOK — plugin.resPackageName kullanılıyor
class GalleryAdapter(
    private val plugin: SimpCityPlugin,
    private val imageUrls: List<String>,
    private val context: Context
) : RecyclerView.Adapter<GalleryAdapter.ImageViewHolder>() {

    private val screenWidth: Int
    private val screenHeight: Int
    private val viewHolders = mutableMapOf<Int, ImageViewHolder>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val imageCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount / 1024
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
        private val plugin: SimpCityPlugin,
        view: View,
        private val adapter: GalleryAdapter
    ) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findView("page")
        private val zoomHelper = ZoomHelper(imageView)
        private var currentRequestId: String? = null
        private var currentDisposable: Disposable? = null

        init {
            imageView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            imageView.scaleType = ImageView.ScaleType.MATRIX
            imageView.setBackgroundColor(0xFF121212.toInt())

            view.isFocusable = true
            view.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        zoomHelper.toggleZoom()
                        true
                    }
                    else -> false
                }
            }
        }

        fun loadImage(targetPosition: Int) {
            cancelLoad()

            if (targetPosition !in 0 until imageUrls.size) return
            val primaryUrl = imageUrls[targetPosition]

            val requestId = "$primaryUrl-$targetPosition-${System.currentTimeMillis()}"
            currentRequestId = requestId
            imageView.tag = requestId

            imageCache.get(primaryUrl)?.let { bitmap ->
                if (imageView.tag == requestId) {
                    imageView.setImageBitmap(bitmap)
                    imageView.post { zoomHelper.resetBaseScale() }
                }
                return
            }

            imageView.setImageResource(android.R.drawable.progress_indeterminate_horizontal)

            enqueueImage(
                url = primaryUrl,
                requestId = requestId,
                onSuccess = { drawable ->
                    if (imageView.tag == requestId) {
                        val bmp = drawableToBitmap(drawable)
                        imageCache.put(primaryUrl, bmp)
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
                        val drawable = when (resultImage) {
                            is BitmapImage -> BitmapDrawable(context.resources, resultImage.bitmap)
                            else -> resultImage.asDrawable(context.resources)
                        }
                        if (imageView.tag == requestId) {
                            try { onSuccess(drawable) } catch (_: Throwable) {}
                        }
                    }
                    .listener(onError = { _, _ ->
                        if (imageView.tag == requestId) onError()
                    })
                    .build()

                currentDisposable = plugin.imageLoader.enqueue(request)
            } catch (e: Exception) {
                imageView.post { if (imageView.tag == requestId) onError() }
            }
        }

        fun cancelLoad() {
            currentRequestId = null
            imageView.tag = null
            try { currentDisposable?.dispose() } catch (_: Throwable) {}
            currentDisposable = null
        }

        private fun drawableToBitmap(drawable: Drawable): Bitmap {
            if (drawable is BitmapDrawable) {
                drawable.bitmap?.let { return it }
            }
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            return androidx.core.graphics.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }

        @SuppressLint("DiscouragedApi")
        private fun <T : View> View.findView(name: String): T {
            val id = plugin.resources!!.getIdentifier(name, "id", plugin.resPackageName)
            require(id != 0) { "View ID '$name' not found in ${plugin.resPackageName}" }
            return findViewById(id)
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val layoutId = plugin.resources!!.getIdentifier("page", "layout", plugin.resPackageName)
        require(layoutId != 0) { "Layout 'page' not found in ${plugin.resPackageName}" }

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
        if (position == 0) preloadAdjacent(holder.itemView.parent as? RecyclerView)
    }

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelLoad()
        val toRemove = viewHolders.entries.firstOrNull { it.value == holder }?.key
        if (toRemove != null) viewHolders.remove(toRemove)
    }

    fun preloadAdjacent(recyclerView: RecyclerView?) {
        if (recyclerView == null || recyclerView.layoutManager !is LinearLayoutManager) return
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

        val preloadRange = maxOf(0, firstVisible - 1)..minOf(itemCount - 1, lastVisible + 1)
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
            } else {
                try { bitmap.recycle() } catch (_: Throwable) {}
            }
        } catch (_: Exception) {}
    }

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
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        val width = drawable.intrinsicWidth.coerceAtLeast(1)
                        val height = drawable.intrinsicHeight.coerceAtLeast(1)
                        androidx.core.graphics.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                            val canvas = Canvas(this)
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

    override fun getItemCount() = imageUrls.size
}

private fun getNetworkHeaders() = NetworkHeaders.Builder()
    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    .set("Referer", "https://simpcity.cr/")
    .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
    .build()
