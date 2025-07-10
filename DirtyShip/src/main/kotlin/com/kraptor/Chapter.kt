package com.kraptor

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val PLUGIN_PKG = "com.kraptor"

class DirtyShipChapterFragment(
    private val plugin: DirtyShipPlugin,
    private val chapterName: String,
    private val imageUrls: List<String>
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val context = plugin.activity ?: requireContext()

            // Manuel olarak view oluştur
            val mainLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(16, 16, 16, 16)
            }

            // Title TextView
            val titleView = TextView(context).apply {
                text = chapterName
                textSize = 20f
                setPadding(0, 0, 0, 32)
                setTextColor(android.graphics.Color.BLACK)
            }
            mainLayout.addView(titleView)

            // RecyclerView
            val recyclerView = RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = CustomAdapter(plugin, imageUrls, context)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            }
            mainLayout.addView(recyclerView)

            Log.d("DirtyShipChapterFrag", "View başarıyla oluşturuldu - Manuel")
            mainLayout
        } catch (e: Exception) {
            Log.e("DirtyShipChapterFrag", "View oluşturulurken hata: ${e.message}", e)
            null
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        } catch (e: Exception) {
            Log.e("DirtyShipChapterFrag", "Dialog ayarlanırken hata: ${e.message}")
        }
    }
}

class CustomAdapter(
    private val plugin: DirtyShipPlugin,
    private val imageUrls: List<String>,
    private val context: Context
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        try {
            // Manuel ImageView oluştur
            val imageView = ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT // Yükseklik içeriğe göre ayarlanacak
                )
                // Resmin tamamını göstermek için FIT_CENTER kullan
                scaleType = ImageView.ScaleType.FIT_CENTER

                // Resmin orijinal oranını koruyarak ekrana sığdır
                adjustViewBounds = true

                // Padding'i azalt veya kaldır
                setPadding(4.dpToPx(context), 4.dpToPx(context), 4.dpToPx(context), 4.dpToPx(context))
            }

            Log.d("CustomAdapter", "ViewHolder başarıyla oluşturuldu - Manuel")
            return ViewHolder(imageView)
        } catch (e: Exception) {
            Log.e("CustomAdapter", "ViewHolder oluşturulurken hata: ${e.message}", e)
            throw e
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val url = imageUrls[position]
            Log.d("CustomAdapter", "Yükleniyor: $url")

            // Placeholder göster
            holder.imageView.setImageResource(android.R.drawable.progress_indeterminate_horizontal)

            // Görüntüyü yükle
            ImageLoader(holder.imageView).execute(url)

        } catch (e: Exception) {
            Log.e("CustomAdapter", "Bind sırasında hata: ${e.message}", e)
        }
    }

    // Basit AsyncTask ile image loading
    private class ImageLoader(private val imageView: ImageView) : AsyncTask<String, Void, Bitmap?>() {

        override fun doInBackground(vararg urls: String): Bitmap? {
            val url = urls[0]
            return try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream: InputStream = connection.inputStream
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                Log.e("ImageLoader", "Resim yüklenirken hata: ${e.message}")
                null
            }
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                imageView.setImageBitmap(result)
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }
    }

    override fun getItemCount() = imageUrls.size
}

// Eğer dp cinsinden boyut ayarlamak isterseniz bu extension'ı kullanabilirsiniz
fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}