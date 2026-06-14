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

    var resPackageName: String = "com.kraptor.simpcity"
        private set

    @SuppressLint("SuspiciousIndentation")
    override fun load(context: Context) {
        appContext = context
        activity = context as AppCompatActivity

        try {
            val testId = context.resources.getIdentifier("galleryPager", "id", "com.kraptor.simpcity")
            if (testId != 0) {
                resPackageName = "com.kraptor.simpcity"
            } else {
                val testId2 = context.resources.getIdentifier("galleryPager", "id", "com.kraptor")
                if (testId2 != 0) {
                    resPackageName = "com.kraptor"
                }
            }
        } catch (e: Exception) {
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

        val sharedPref = context.getSharedPreferences(Settings.PREFS_NAME, Context.MODE_PRIVATE)

        registerMainAPI(SimpCity(this))
        registerExtractorAPI(TurboCr())
        registerExtractorAPI(BunkrCrExtractor())
        registerExtractorAPI(CDNBunkrExtractor())
        registerExtractorAPI(BunkrExtractor())
        registerExtractorAPI(FiledItchFilesExtractor())

        this.openSettings = { ctx ->
            val simpApi = SimpCity(this)
            val dialog = SettingsDialog(ctx as AppCompatActivity) {
                com.lagradost.cloudstream3.MainActivity.reloadHomeEvent.invoke(true)
            }
            dialog.show(simpApi.mainUrl)
        }
    }

    fun loadGallery(title: String, images: List<String>) {
        if (images.isEmpty()) return
        val act = activity ?: return

        Handler(Looper.getMainLooper()).post {
            try {
                val frag = GalleryFragment(this, title, images)
                frag.show(act.supportFragmentManager, "SimpGallery")
            } catch (e: Exception) {
            }
        }
    }

}
