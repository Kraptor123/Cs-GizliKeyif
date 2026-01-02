package com.kraptor

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import coil3.ImageLoader
import coil3.bitmapFactoryExifOrientationStrategy
import coil3.bitmapFactoryMaxParallelism
import coil3.decode.BitmapFactoryDecoder
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.allowConversionToBitmap
import coil3.request.maxBitmapSize
import coil3.util.DebugLogger
import com.lagradost.api.Log
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class HentaizmMangaPlugin : Plugin() {
    var activity: AppCompatActivity? = null
    var context: Context? = null
    lateinit var imageLoader: ImageLoader

    override fun load(context: Context) {
        this.context = context
        activity = context as AppCompatActivity

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        imageLoader = ImageLoader.Builder(context)
            .logger(DebugLogger())
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20) // önceki %25 yerine %20 (daha güvenli)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            // Burayı makul bir sınıra çekiyoruz — ekran yüksekliğinin 2-3 katı yeterli
            .maxBitmapSize(coil3.size.Size(screenWidth, screenHeight * 2))
            .bitmapFactoryMaxParallelism(2)
            .bitmapFactoryExifOrientationStrategy(coil3.decode.ExifOrientationStrategy.RESPECT_PERFORMANCE)
            .allowConversionToBitmap(true)
            .components {
                add(BitmapFactoryDecoder.Factory())
            }
            .build()
        registerMainAPI(HentaizmManga(this))
    }

    suspend fun loadChapter(manga: Manga) {
        if (activity == null) {
            Log.e("kraptor_DEBUG", "Activity is null, cannot show fragment")
            return
        }
        val frag = HentaizmChapterFragment(this, manga)
        frag.show(activity!!.supportFragmentManager, "HentaizmChapter")
    }
}