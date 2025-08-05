package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.api.Log
import android.os.Handler
import android.os.Looper

@CloudstreamPlugin
class DirtyShipPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as? AppCompatActivity ?: run {
            Log.e("DirtyShipPlugin", "Context is not an AppCompatActivity")
            return
        }
        Log.d("DirtyShipPlugin", "Resources available: ${resources != null}")
        registerMainAPI(DirtyShip(this))
    }

    fun loadChapter(chapterName: String, pages: List<String>) {
        val currentActivity = activity ?: run {
            Log.e("DirtyShipPlugin", "Activity is null, cannot show fragment")
            showToast("Unable to display gallery: Activity not available")
            return
        }

        // UI işlemlerini main thread'de yap
        Handler(Looper.getMainLooper()).post {
            try {
                // Filtrelenmiş sayfa listesi (thumbnail'leri çıkar)
                val filteredPages = pages.filter { !it.contains("100x140") }

                if (filteredPages.isEmpty()) {
                    Log.e("DirtyShipPlugin", "Gösterilecek geçerli görüntü yok")
                    showToast("No valid images to display")
                    return@post
                }

                Log.d("DirtyShipPlugin", "Galeri gösteriliyor: $chapterName, ${filteredPages.size} görüntü")
                val frag = DirtyShipChapterFragment(this, chapterName, filteredPages)

                val fragmentManager = currentActivity.supportFragmentManager

                // Önceki fragment'ları temizle
                val existingFragment = fragmentManager.findFragmentByTag("DirtyShipChapter")
                if (existingFragment != null) {
                    fragmentManager.beginTransaction().remove(existingFragment).commit()
                }

                frag.show(fragmentManager, "DirtyShipChapter")
            } catch (e: Exception) {
                Log.e("DirtyShipPlugin", "Fragment gösterilirken hata: ${e.message}")
                showToast("Failed to display gallery: ${e.message}")
            }
        }
    }
}