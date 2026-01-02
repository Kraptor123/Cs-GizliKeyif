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

//        Log.d("kraptor_DEBUG", "Toplam sayfa sayısı: ${manga.mangaResim.size}")
        if (manga.mangaResim.isEmpty()) {
//            Log.w("kraptor_DEBUG", "Uyarı: Sayfa listesi boş!")
            return
        }

        val recyclerView = view.findView<RecyclerView>("page_list")
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(3)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        // Doğrudan plugin context'ini kullan
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
//                Log.d("kraptor_DEBUG", "ViewHolder initialized successfully")
            } catch (e: Exception) {
//                Log.e("kraptor_DEBUG", "ViewHolder initialization failed: ${e.message}")
                throw e
            }
        }

        @SuppressLint("DiscouragedApi")
        private fun <T : View> View.findView(name: String): T {
            val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
            if (id == 0) {
//                Log.e("kraptor_DEBUG", "View ID '$name' not found in package ${BuildConfig.LIBRARY_PACKAGE_NAME}")
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
//                Log.e("kraptor_DEBUG", "Layout 'page' not found in package com.kraptor")
                throw RuntimeException("Layout 'page' not found")
            }

            val pageLayout = plugin.resources!!.getLayout(pageLayoutId)
            // Plugin context'ini kullan
            val view = LayoutInflater.from(adapterContext).inflate(pageLayout, viewGroup, false)

//            Log.d("kraptor_DEBUG", "ViewHolder created successfully with plugin context")
            return ViewHolder(plugin, view)
        } catch (e: Exception) {
//            Log.e("kraptor_DEBUG", "Failed to create ViewHolder: ${e.message}")
            throw e
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        try {
            if (position < 0 || position >= imageUrls.size) {
//                Log.e("kraptor_DEBUG", "Invalid position: $position, list size: ${imageUrls.size}")
                return
            }

            val imageUrl = imageUrls[position]
//            Log.d("kraptor_DEBUG", "Binding position $position with URL: $imageUrl")

            if (imageUrl.isBlank()) {
//                Log.w("kraptor_DEBUG", "Empty image URL at position $position")
                viewHolder.imageView.setImageResource(android.R.drawable.ic_dialog_alert)
                return
            }

            // Coil yüklemesini özel ImageLoader ile yap
            val request = ImageRequest.Builder(adapterContext)
                .data(imageUrl)
                .target(viewHolder.imageView)
                // view boyutuna göre decode et (ÖNEMLİ)
                .size(ViewSizeResolver(viewHolder.imageView))
                // büyük görsellerde crossfade kapat (tek bitmap tutulur)
                .crossfade(false)
                // Bellek kullanımını azalt
                .bitmapConfig(Bitmap.Config.RGB_565)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .build()


            // Plugin'deki ImageLoader'ı kullan
            plugin.imageLoader.enqueue(request)

//            Log.d("kraptor_DEBUG", "Coil load initiated successfully for position $position")

        } catch (e: Exception) {
//            Log.e("kraptor_DEBUG", "General error in onBindViewHolder at position $position: ${e.message}")
            e.printStackTrace()

            // Fallback
            try {
                viewHolder.imageView.setImageResource(android.R.drawable.ic_dialog_alert)
            } catch (ignored: Exception) {
                // Ignore if even this fails
            }
        }
    }

    override fun getItemCount(): Int {
//        Log.d("kraptor_DEBUG", "Item count: ${imageUrls.size}")
        return imageUrls.size
    }
}