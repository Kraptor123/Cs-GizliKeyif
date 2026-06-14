package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil3.BitmapImage
import coil3.asDrawable
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.size.Scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GalleryAdapter(
    private val plugin: SimpCityPlugin,
    private val imageUrls: List<String>,
    private val context: Context,
    private val onPageChanged: ((Int) -> Unit)? = null,
    private val onImageClick: (() -> Unit)? = null
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    private val screenWidth: Int
    private val screenHeight: Int
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var currentPosition: Int = 0
        private set

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

    inner class GalleryViewHolder(
        private val plugin: SimpCityPlugin,
        val containerView: View,
        private val adapter: GalleryAdapter
    ) : RecyclerView.ViewHolder(containerView) {

        val imageView: ImageView
        private val zoomHelper: ZoomHelper
        private val pageLoading: View
        private val pageError: View

        private var currentDisposable: Disposable? = null
        private var currentUrl: String? = null

        init {
            containerView.isFocusable = true
            containerView.isFocusableInTouchMode = true
            containerView.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)

            imageView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.MATRIX
                setBackgroundColor(android.graphics.Color.BLACK)
                isFocusable = false
                isFocusableInTouchMode = false
                colorFilter = null
                background = null
            }
            
            zoomHelper = ZoomHelper(imageView, onSingleTap = { 
                onImageClick?.invoke()
            })

            val container = containerView.findViewById<ViewGroup>(
                plugin.resources!!.getIdentifier("pageContainer", "id", plugin.resPackageName)
            )
            (container as? ViewGroup)?.addView(imageView, 0)

            pageLoading = containerView.findView("pageLoading")
            pageError = containerView.findView("pageError")
        }

        fun handleDpadKey(keyCode: Int): Boolean {
            val step = imageView.width * 0.15f
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    zoomHelper.toggleZoom()
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (zoomHelper.isZoomed()) {
                        zoomHelper.panByDirection(step, 0f)
                        true
                    } else false
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (zoomHelper.isZoomed()) {
                        zoomHelper.panByDirection(-step, 0f)
                        true
                    } else false
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (zoomHelper.isZoomed()) {
                        zoomHelper.panByDirection(0f, step)
                        true
                    } else false
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (zoomHelper.isZoomed()) {
                        zoomHelper.panByDirection(0f, -step)
                        true
                    } else false
                }
                else -> false
            }
        }

        fun loadImage(targetPosition: Int) {
            cancelLoad()
            if (targetPosition !in 0 until imageUrls.size) return
            val primaryUrl = imageUrls[targetPosition]
            currentUrl = primaryUrl

            val requestId = "$primaryUrl-$targetPosition"
            containerView.tag = requestId

            pageLoading.visibility = View.VISIBLE
            pageError.visibility = View.GONE
            imageView.setImageDrawable(null)

            enqueueImage(
                url = primaryUrl,
                requestId = requestId,
                onSuccess = { drawable ->
                    if (containerView.tag == requestId) {
                        pageLoading.visibility = View.GONE
                        pageError.visibility = View.GONE
                        imageView.setImageDrawable(drawable)
                        imageView.post { zoomHelper.resetBaseScale() }
                    }
                },
                onError = {
                    if (containerView.tag == requestId) {
                        pageLoading.visibility = View.GONE
                        pageError.visibility = View.VISIBLE
                    }
                }
            )
        }

        private fun enqueueImage(
            url: String,
            requestId: String,
            onSuccess: (android.graphics.drawable.Drawable) -> Unit,
            onError: () -> Unit
        ) {
            try {
                currentDisposable?.dispose()
                val headers = getNetworkHeaders()
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(screenWidth, screenHeight)
                    .scale(Scale.FIT)
                    .httpHeaders(headers)
                    .target { resultImage ->
                        val drawable = when (resultImage) {
                            is BitmapImage -> android.graphics.drawable.BitmapDrawable(
                                context.resources, resultImage.bitmap
                            )
                            else -> resultImage.asDrawable(context.resources)
                        }
                        if (containerView.tag == requestId) {
                            try { onSuccess(drawable) } catch (_: Throwable) {}
                        }
                    }
                    .listener(onError = { _, _ ->
                        if (containerView.tag == requestId) onError()
                    })
                    .build()

                currentDisposable = plugin.imageLoader.enqueue(request)
            } catch (e: Exception) {
                containerView.post { if (containerView.tag == requestId) onError() }
            }
        }

        fun cancelLoad() {
            containerView.tag = null
            currentUrl = null
            try { currentDisposable?.dispose() } catch (_: Throwable) {}
            currentDisposable = null
        }

        @SuppressLint("DiscouragedApi")
        private fun <T : View> View.findView(name: String): T {
            val id = plugin.resources!!.getIdentifier(name, "id", plugin.resPackageName)
            require(id != 0) { "View ID '$name' not found in ${plugin.resPackageName}" }
            return findViewById(id)
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val layoutId = plugin.resources!!.getIdentifier("gallery_page", "layout", plugin.resPackageName)
        return GalleryViewHolder(
            plugin,
            LayoutInflater.from(context).inflate(plugin.resources!!.getLayout(layoutId), parent, false),
            this
        )
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.loadImage(position)
    }

    override fun onViewRecycled(holder: GalleryViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelLoad()
        holder.imageView.setImageDrawable(null)
    }

    override fun getItemCount() = imageUrls.size

    fun onPageSelected(position: Int) {
        currentPosition = position
        onPageChanged?.invoke(position)
        preloadAdjacent(position)
    }

    private fun preloadAdjacent(position: Int, preloadRange: Int = 2) {
        val start = (position - preloadRange).coerceAtLeast(0)
        val end = (position + preloadRange).coerceAtMost(imageUrls.lastIndex)

        coroutineScope.launch(Dispatchers.IO) {
            for (i in start..end) {
                if (i == position) continue
                preloadSingleImage(i)
            }
        }
    }

    private suspend fun preloadSingleImage(position: Int) {
        if (position !in 0 until imageUrls.size) return
        val url = imageUrls[position]
        try {
            val headers = getNetworkHeaders()
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(screenWidth, screenHeight)
                .scale(Scale.FIT)
                .httpHeaders(headers)
                .build()
            plugin.imageLoader.execute(request)
        } catch (_: Exception) {}
    }
}

private fun getNetworkHeaders() = NetworkHeaders.Builder()
    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    .set("Referer", "https://simpcity.cr/")
    .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
    .build()
