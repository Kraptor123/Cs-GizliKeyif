package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import coil3.size.ViewSizeResolver
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.api.Log

class HentaizmChapterFragment(
    val plugin: HentaizmMangaPlugin,
    val manga: Manga,
) : BottomSheetDialogFragment() {

    @SuppressLint("DiscouragedApi")
    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", "com.kraptor")
        if (id == 0) throw RuntimeException("View ID '$name' not found in package com.kraptor")
        return findViewById(id)
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = plugin.resources!!.getIdentifier("chapter", "layout", BuildConfig.LIBRARY_PACKAGE_NAME)
        val layout = plugin.resources!!.getLayout(layoutId)
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (manga.mangaResim.isEmpty()) {
            return
        }

        val recyclerView = view.findView<RecyclerView>("page_list")
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(3)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        recyclerView.adapter = CustomAdapter(plugin, manga.mangaResim, plugin.context!!)
    }
}

class CustomAdapter(
    private val plugin: HentaizmMangaPlugin,
    private val imageUrls: List<String>,
    private val adapterContext: Context
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    class ViewHolder(
        private val plugin: HentaizmMangaPlugin,
        val view: View
    ) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView

        init {
            try {
                imageView = view.findView<ImageView>("page")
            } catch (e: Exception) {
                throw e
            }
        }

        @SuppressLint("DiscouragedApi")
        private fun <T : View> View.findView(name: String): T {
            val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
            if (id == 0) {
                throw RuntimeException("View ID '$name' not found")
            }
            return this.findViewById(id)
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        try {
            val pageLayoutId = plugin.resources!!.getIdentifier("page", "layout", "com.kraptor")
            if (pageLayoutId == 0) {
                throw RuntimeException("Layout 'page' not found")
            }

            val pageLayout = plugin.resources!!.getLayout(pageLayoutId)
            val view = LayoutInflater.from(adapterContext).inflate(pageLayout, viewGroup, false)

            return ViewHolder(plugin, view)
        } catch (e: Exception) {
            throw e
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        try {
            if (position < 0 || position >= imageUrls.size) {
                return
            }

            val imageUrl = imageUrls[position]

            if (imageUrl.isBlank()) {
                viewHolder.imageView.setImageResource(android.R.drawable.ic_dialog_alert)
                return
            }

            val request = ImageRequest.Builder(adapterContext)
                .data(imageUrl)
                .target(viewHolder.imageView)
                .size(ViewSizeResolver(viewHolder.imageView))
                .crossfade(false)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .build()

            plugin.imageLoader.enqueue(request)

        } catch (e: Exception) {
            e.printStackTrace()

            try {
                viewHolder.imageView.setImageResource(android.R.drawable.ic_dialog_alert)
            } catch (ignored: Exception) {
            }
        }
    }

    override fun getItemCount(): Int {
        return imageUrls.size
    }
}