package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
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
import coil3.size.Size
import com.lagradost.api.Log
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class SimpCityPlugin : Plugin() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    var activity: AppCompatActivity? = null
    lateinit var imageLoader: ImageLoader
        private set

    // BuildConfig YOK — runtime'da bulunuyor
    var resPackageName: String = "com.kraptor.simpcity"
        private set

    @SuppressLint("SuspiciousIndentation")
    override fun load(context: Context) {
        appContext = context
        activity = context as AppCompatActivity

        // Resource package name'i kesin olarak bul
        try {
            val testId = context.resources.getIdentifier("page_list", "id", "com.kraptor.simpcity")
            if (testId != 0) {
                resPackageName = "com.kraptor.simpcity"
                Log.d("kraptor_SimpCity", "resPackageName = com.kraptor.simpcity (found page_list)")
            } else {
                val testId2 = context.resources.getIdentifier("page_list", "id", "com.kraptor")
                if (testId2 != 0) {
                    resPackageName = "com.kraptor"
                    Log.d("kraptor_SimpCity", "resPackageName = com.kraptor (found page_list)")
                } else {
                    Log.e("kraptor_SimpCity", "page_list ID not found in any package!")
                }
            }
        } catch (e: Exception) {
            Log.e("kraptor_SimpCity", "resPackageName detection failed: ${e.message}")
        }

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        imageLoader = ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("simp_image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            .maxBitmapSize(Size(screenWidth, screenHeight * 2))
            .bitmapFactoryMaxParallelism(2)
            .bitmapFactoryExifOrientationStrategy(coil3.decode.ExifOrientationStrategy.RESPECT_PERFORMANCE)
            .allowConversionToBitmap(true)
            .components {
                add(BitmapFactoryDecoder.Factory())
                add(Interceptor { chain ->
                    val headers = NetworkHeaders.Builder()
                        .add("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                        .add("Referer", "https://simpcity.cr/")
                        .add("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                        .build()
                    chain.withRequest(chain.request.newBuilder().httpHeaders(headers).build()).proceed()
                })
            }
            .build()

        registerMainAPI(SimpCity(this))
        registerExtractorAPI(TurboCr())
        registerExtractorAPI(BunkrCrExtractor())
        registerExtractorAPI(CDNBunkrExtractor())
        registerExtractorAPI(BunkrExtractor())
        registerExtractorAPI(FiledItchFilesExtractor())
    }

    fun loadGallery(images: List<String>) {
        Log.d("kraptor_SimpCity", "loadGallery called with ${images.size} images")

        if (images.isEmpty()) {
            Log.e("kraptor_SimpCity", "loadGallery: images list is empty")
            return
        }

        val act = activity
        if (act == null) {
            Log.e("kraptor_SimpCity", "loadGallery: activity is NULL!")
            return
        }

        Log.d("kraptor_SimpCity", "loadGallery: activity exists, posting fragment to main thread")

        Handler(Looper.getMainLooper()).post {
            try {
                Log.d("kraptor_SimpCity", "loadGallery: creating GalleryFragment")
                val frag = GalleryFragment(this, images)
                Log.d("kraptor_SimpCity", "loadGallery: calling frag.show()")
                frag.show(act.supportFragmentManager, "SimpGallery")
                Log.d("kraptor_SimpCity", "loadGallery: frag.show() completed")
            } catch (e: Exception) {
                Log.e("kraptor_SimpCity", "loadGallery: frag.show() FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
