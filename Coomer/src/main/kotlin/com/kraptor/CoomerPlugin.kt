package com.kraptor

import android.annotation.SuppressLint
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.api.Log
import android.os.Handler
import android.os.Looper
import coil3.ImageLoader
import coil3.bitmapFactoryExifOrientationStrategy
import coil3.bitmapFactoryMaxParallelism
import coil3.decode.BitmapFactoryDecoder
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.allowConversionToBitmap
import coil3.request.maxBitmapSize
import coil3.util.DebugLogger

@CloudstreamPlugin
class CoomerPlugin: Plugin() {
    var activity: AppCompatActivity? = null
    var context: Context? = null
    lateinit var imageLoader: ImageLoader

    @SuppressLint("SuspiciousIndentation")
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
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            .maxBitmapSize(coil3.size.Size(screenWidth, screenHeight * 2))
            .bitmapFactoryMaxParallelism(2)
            .bitmapFactoryExifOrientationStrategy(coil3.decode.ExifOrientationStrategy.RESPECT_PERFORMANCE)
            .allowConversionToBitmap(true)
            .components {
                add(BitmapFactoryDecoder.Factory())
                // HTTP Headers ekle (HTTP 500 hatalarını azaltmak için)
                add(Interceptor { chain ->
                    val headers = NetworkHeaders.Builder()
                        .add(
                            "User-Agent",
                            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
                        )
                        .add("Referer", "https://coomer.st/")
                        .add("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                        .build()

                    val newRequest = chain.request.newBuilder()
                        .httpHeaders(headers)
                        .build()

                    // Chain'i güncelle ve proceed et
                    chain.withRequest(newRequest).proceed()
                })
            }
            .build()
            registerMainAPI(Coomer(this))
        }

    suspend fun loadChapter(manga: List<String>) {
        if (activity == null) {
//            Log.e("kraptor_DEBUG", "Activity is null, cannot show fragment")
            return
        }
        val frag = CoomerChapterFragment(this, manga)
        frag.show(activity!!.supportFragmentManager, "HentaizmChapter")
    }
}