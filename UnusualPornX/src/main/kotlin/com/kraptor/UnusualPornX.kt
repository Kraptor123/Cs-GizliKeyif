// ! This Extension Made By @Kraptor123 for GizliKeyif

package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class UnusualPornX(context: Context) : MainAPI() {
    override var mainUrl              = "https://unusualpornx.com"
    override var name                 = "UnusualPornX"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/latest-updates" to "Latest Updates",
        "${mainUrl}/categories/demons" to "Demons",
        "${mainUrl}/categories/time-stop" to "Time Stop",
        "${mainUrl}/categories/mind-control" to "Mind Control",
        "${mainUrl}/categories/vampires" to "Vampires",
        "${mainUrl}/categories/monsters" to "Monsters",
        "${mainUrl}/categories/sci-fi-experiments" to "Sci-Fi Experiments",
        "${mainUrl}/categories/ghosts" to "Monsters",
    )

    private val context = context

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page/").document
        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(list = HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-webp"))
        val trailer   = this.selectFirst("img")?.attr("data-preview")?.replace("https://","")

        return newMovieSearchResponse(title, href + "|" + trailer, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}").document
        val searchAnswer = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(searchAnswer, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        Log.d(tag, "Load : $url")
        val split    = url.split("|")
        val trailer  = "https://${split[1]}"
        val document = app.get(split[0]).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.info-content a[href*=tags]").map { it.text() }
        val scoreText       = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("div.item span em:contains(:)")
            ?.text()
            ?.split(":")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.let { parts ->
                val size = parts.size
                val seconds = if (size >= 1) parts[size - 1] else 0
                val minutes = if (size >= 2) parts[size - 2] else 0
                val hours   = if (size >= 3) parts[size - 3] else 0

                hours * 60 + minutes + if (seconds >= 30) 1 else 0
            }
        val recommendations = document.select("div.item").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("div.info-content a.link[href*=models] span.name").map { Actor(it.text()) }

        return newMovieLoadResponse(title, split[0], TvType.NSFW, split[0]) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(scoreText)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer, "${mainUrl}/", true)
        }
    }


    private fun cleanupWebView(wv: WebView) {
        try {
            wv.stopLoading()
            wv.setWebChromeClient(null)
            wv.webViewClient = object : WebViewClient() {}
            wv.removeAllViews()
            wv.clearHistory()
            wv.loadUrl("about:blank")
            wv.destroy()
        } catch (ignored: Throwable) {
        }
    }

    // WebView oluşturup video URL'sini çıkar
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun createWebViewAndExtractVideo(
        context: Context,
        html: String,
        onResult: (String?) -> Unit
    ): WebView = withContext(Dispatchers.Main) {

        val wv = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    extractVideoWithDelay(view, { result ->
                        onResult(result)
                        // İş bitince temizle
                        Handler(Looper.getMainLooper()).post {
//                            Log.d("kraptor_UnusualPornX", "WebView temizlendi")
                            cleanupWebView(this@apply)
                        }
                    }, 0)
                }
            }

            // HTML'i yükle
            loadDataWithBaseURL("${mainUrl}/", html, "text/html", "UTF-8", null)
        }

        return@withContext wv
    }

    // Video URL'sini gecikmeyle çıkar
    private fun extractVideoWithDelay(webView: WebView?, onResult: (String?) -> Unit, attempt: Int) {
        if (webView == null || attempt > 3) {
//            Log.d("kraptor_UnusualPornX", "Timeout reached or WebView is null")
            onResult(null)
            return
        }

//        Log.d("kraptor_UnusualPornX", "Attempt $attempt - kt_player video URL araniyor...")

        val extractScript = """
    (function() {
        try {
            var results = [];
            
            if (typeof flashvars !== 'undefined' && flashvars) {
                // 360p
                if (flashvars.video_url) {
                    var videoUrl = flashvars.video_url;
                    if (videoUrl.startsWith('function/0/')) {
                        videoUrl = videoUrl.substring(11);
                    }
                    // Sondaki / varsa temizle
                    if (videoUrl.endsWith('/')) {
                        videoUrl = videoUrl.slice(0, -1);
                    }
                    var quality = flashvars.video_url_text || '360p';
                    results.push({
                        url: videoUrl,
                        quality: quality
                    });
                }
                
                // 720p
                if (flashvars.video_alt_url) {
                    var altUrl = flashvars.video_alt_url;
                    if (altUrl.startsWith('function/0/')) {
                        altUrl = altUrl.substring(11);
                    }
                    // Sondaki / varsa temizle
                    if (altUrl.endsWith('/')) {
                        altUrl = altUrl.slice(0, -1);
                    }
                    var altQuality = flashvars.video_alt_url_text || '720p';
                    results.push({
                        url: altUrl,
                        quality: altQuality
                    });
                }
            }
            
            return results.length > 0 ? JSON.stringify(results) : null;
            
        } catch (e) {
            console.log('Extract error:', e);
            return null;
        }
    })();
""".trimIndent()

        webView.evaluateJavascript(extractScript) { resultJson ->
//            Log.d("kraptor_UnusualPornX", "Raw result: '$resultJson'")

            val cleanResult = resultJson?.let { raw ->
                if (raw == "null" || raw == "\"null\"") {
                    null
                } else {
                    raw.removePrefix("\"").removeSuffix("\"")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                }
            }

            if (cleanResult.isNullOrEmpty() || cleanResult == "null") {
                if (attempt < 20) {
//                    Log.d("kraptor_UnusualPornX", "Video URL bulunamadi, 1 saniye bekleyip tekrar deniyor...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractVideoWithDelay(webView, onResult, attempt + 1)
                    }, 1000)
                } else {
//                    Log.d("kraptor_UnusualPornX", "Max deneme sayisina ulasildi, basarisiz")
                    onResult(null)
                }
            } else {
                // function/0/ prefix'ini temizle
                val finalUrl = if (cleanResult.startsWith("function/0/")) {
                    cleanResult.removePrefix("function/0/")
                } else {
                    cleanResult
                }

//                Log.d("kraptor_UnusualPornX", "SUCCESS! Video URL bulundu: $finalUrl")
                onResult(finalUrl)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_UnusualPornX", "data » $data")
        val pageHtml = app.get(data).text

        Log.d("kraptor_UnusualPornX", "WebView ile kt_player video URL'si çıkarılıyor...")

        val videoResultJson = suspendCancellableCoroutine { continuation ->
            runBlocking {
                createWebViewAndExtractVideo(context = context, pageHtml) { result ->
                    continuation.resume(result)
                }
            }
        }

        Log.d("kraptor_UnusualPornX", "Video JSON = $videoResultJson")

        videoResultJson?.let { jsonResult ->
            try {
                val videoList = try { mapper.readValue<List<VideoQuality>>(jsonResult) } catch (e: Exception) { null }

                videoList?.forEach { video ->
                    if (video.url.startsWith("http")) {
                        Log.d("kraptor_UnusualPornX", "${video.quality} - ${video.url}")

                        callback.invoke(
                            newExtractorLink(
                                source = "UnusualPornX",
                                name = "UnusualPornX",
                                url = video.url,
                                type = ExtractorLinkType.VIDEO,
                            ) {
                                this.referer = "${mainUrl}/"
                                quality = getQualityFromName(video.quality)
                            })
                    }
                }
                return videoList?.isNotEmpty() == true

            } catch (e: Exception) {
                Log.e("kraptor_UnusualPornX", "Parse Hata verdi: ${e.message}")
                return false
            }
        }

        return false
    }
}

data class VideoQuality(
    @param:JsonProperty("url") val url: String,
    @param:JsonProperty("quality") val quality: String
)