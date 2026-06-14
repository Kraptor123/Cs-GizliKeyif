package com.kraptor

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil3.BitmapImage
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.size.Scale
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.BitmapDrawable

class GalleryGridAdapter(
    private val plugin: SimpCityPlugin,
    private val images: List<String>,
    private val onImageClick: (Int) -> Unit
) : RecyclerView.Adapter<GalleryGridAdapter.GridViewHolder>() {

    @SuppressLint("DiscouragedApi")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val layoutId = plugin.resources!!.getIdentifier("gallery_grid_item", "layout", plugin.resPackageName)
        val view = LayoutInflater.from(parent.context).inflate(plugin.resources!!.getLayout(layoutId), parent, false)
        val width = parent.width / 3
        view.layoutParams.height = width
        return GridViewHolder(view)
    }

    @SuppressLint("DiscouragedApi")
    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val url = ImageUrlFilter.toThumbnail(images[position])
        val imageView = holder.itemView.findViewById<ImageView>(
            plugin.resources!!.getIdentifier("gridImage", "id", plugin.resPackageName)
        )

        val dp = holder.itemView.resources.displayMetrics.density
        val focusedDrawable = GradientDrawable().apply {
            setStroke((3 * dp).toInt(), 0xFFFFD600.toInt())
            setColor(0x00000000)
        }
        val normalDrawable = ColorDrawable(0x00000000)

        holder.itemView.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            background = normalDrawable
            setOnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focusedDrawable else normalDrawable
            }
            setOnKeyListener { v, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (position < 3) { // Top row
                            v.rootView.findViewById<View>(plugin.resources!!.getIdentifier("btnBack", "id", plugin.resPackageName))?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val rowCount = (itemCount + 2) / 3
                        val currentRow = position / 3
                        if (currentRow == rowCount - 1) { // Bottom row
                            v.rootView.findViewById<View>(plugin.resources!!.getIdentifier("btnDownloadAll", "id", plugin.resPackageName))?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (position % 3 == 0) {
                            v.rootView.findViewById<View>(plugin.resources!!.getIdentifier("btnBack", "id", plugin.resPackageName))?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (position % 3 == 2 || position == itemCount - 1) {
                            v.rootView.findViewById<View>(plugin.resources!!.getIdentifier("btnDownloadAll", "id", plugin.resPackageName))?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                }
                false
            }
            setOnClickListener { onImageClick(position) }
        }

        imageView.setImageDrawable(null)
        imageView.colorFilter = null // Ensure no tint

        val request = ImageRequest.Builder(holder.itemView.context)
            .data(url)
            .scale(Scale.FILL)
            .target { resultImage ->
                val drawable = when (resultImage) {
                    is BitmapImage -> BitmapDrawable(holder.itemView.resources, resultImage.bitmap)
                    else -> resultImage.asDrawable(holder.itemView.resources)
                }
                imageView.setImageDrawable(drawable)
            }
            .build()
        plugin.imageLoader.enqueue(request)
    }

    override fun getItemCount() = images.size

    class GridViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
