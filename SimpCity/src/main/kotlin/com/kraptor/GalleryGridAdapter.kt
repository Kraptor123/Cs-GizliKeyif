package com.kraptor

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil3.BitmapImage
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.size.Scale

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

        imageView.setImageDrawable(null)
        val request = ImageRequest.Builder(holder.itemView.context)
            .data(url)
            .scale(Scale.FILL)
            .target { resultImage ->
                val drawable = when (resultImage) {
                    is BitmapImage -> android.graphics.drawable.BitmapDrawable(
                        holder.itemView.resources, resultImage.bitmap
                    )
                    else -> resultImage.asDrawable(holder.itemView.resources)
                }
                imageView.setImageDrawable(drawable)
            }
            .build()
        plugin.imageLoader.enqueue(request)

        holder.itemView.setOnClickListener { onImageClick(position) }
    }

    override fun getItemCount() = images.size

    class GridViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
