// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VideoCelebs(context: Context) : MainAPI() {
    override var mainUrl = "https://videocelebs.net"
    override var name = "VideoCelebs"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    private val appContext = context

    override val mainPage = mainPageOf(
        "${mainUrl}/tag/nude" to "Nude",
        "${mainUrl}/tag/topless" to "Topless",
        "${mainUrl}/tag/sex" to "Sex",
        "${mainUrl}/tag/butt" to "Butt",
        "${mainUrl}/tag/sexy" to "Sexy",
        "${mainUrl}/tag/full-frontal" to "Full Frontal",
        "${mainUrl}/tag/underwear" to "Underwear",
        "${mainUrl}/tag/bush" to "Bush",
        "${mainUrl}/tag/cleavage" to "Cleavage",
        "${mainUrl}/tag/bikini" to "Bikini",
        "${mainUrl}/tag/side-boob" to "Side boob",
        "${mainUrl}/tag/lesbian" to "Lesbian",
        "${mainUrl}/tag/see-thru" to "See Thru",
        "${mainUrl}/tag/thong" to "Thong",
        "${mainUrl}/tag/explicit" to "Explicit",
        "${mainUrl}/tag/nipslip" to "Nipslip",
        "${mainUrl}/tag/striptease" to "Striptease",
        "${mainUrl}/tag/implied-nudity" to "Implied Nudity",
        "${mainUrl}/tag/nude-debut" to "Nude Debut",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home = document.select("div.item.big").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val rating = this.selectFirst("div.rating.positive")?.text()?.replace("%", "")?.trim()

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(rating)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}/page/$page").document

        val aramaCevap = document.select("div.item.big").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("a.item img")?.attr("src"))
        val description = document.selectFirst("div.singl > div:nth-child(4)")?.text()?.trim()
        val year = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.entry-utility strong:contains(Tags) ~ a").map { it.text() }
        val score =
            document.selectFirst("div.rating span.voters")?.text()?.trim()?.replace("%", "")?.substringBefore(" ")
                ?.trim()
        val duration = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.entry-utility strong:contains(Actress) + a").map { Actor(it.text()) }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from100(score)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a img")?.attr("alt") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
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
//                            Log.d("kraptor_VideCelebs", "WebView temizlendi")
                            cleanupWebView(this@apply)
                        }
                    }, 0)
                }
            }

            // HTML'i yükle
            loadDataWithBaseURL("https://videocelebs.net/", html, "text/html", "UTF-8", null)
        }

        return@withContext wv
    }

    // Video URL'sini gecikmeyle çıkar
    private fun extractVideoWithDelay(webView: WebView?, onResult: (String?) -> Unit, attempt: Int) {
        if (webView == null || attempt > 3) {
//            Log.d("kraptor_VideCelebs", "Timeout reached or WebView is null")
            onResult(null)
            return
        }

//        Log.d("kraptor_VideCelebs", "Attempt $attempt - kt_player video URL araniyor...")

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
//            Log.d("kraptor_VideCelebs", "Raw result: '$resultJson'")

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
//                    Log.d("kraptor_VideCelebs", "Video URL bulunamadi, 1 saniye bekleyip tekrar deniyor...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractVideoWithDelay(webView, onResult, attempt + 1)
                    }, 1000)
                } else {
//                    Log.d("kraptor_VideCelebs", "Max deneme sayisina ulasildi, basarisiz")
                    onResult(null)
                }
            } else {
                // function/0/ prefix'ini temizle
                val finalUrl = if (cleanResult.startsWith("function/0/")) {
                    cleanResult.removePrefix("function/0/")
                } else {
                    cleanResult
                }

//                Log.d("kraptor_VideCelebs", "SUCCESS! Video URL bulundu: $finalUrl")
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
//        Log.d("kraptor_VideCelebs", "data » $data")
        val pageHtml = app.get(data).text

//        Log.d("kraptor_VideCelebs", "WebView ile kt_player video URL'si çıkarılıyor...")

        val videoResultJson = suspendCoroutine { continuation ->
            runBlocking {
                createWebViewAndExtractVideo(appContext, pageHtml) { result ->
                    continuation.resume(result)
                }
            }
        }

//        Log.d("kraptor_VideCelebs", "Video JSON Result = $videoResultJson")

        videoResultJson?.let { jsonResult ->
            try {
                // JSON parse et
                val videoList = parseJson<List<VideoQuality>>(jsonResult)

                videoList?.forEach { video ->
                    if (video.url.startsWith("http")) {
//                        Log.d("kraptor_VideCelebs", "Adding: ${video.quality} - ${video.url}")

                        callback.invoke(
                            newExtractorLink(
                                source = "VideCelebs",
                                name = "VideCelebs",
                                url = video.url,
                                type = ExtractorLinkType.VIDEO,
                            ) {
                                this.referer = "${mainUrl}/"
                                quality = getQualityFromName(video.quality)
                            })
                    }
                }
                return videoList.isNotEmpty()

            } catch (e: Exception) {
//                Log.e("kraptor_VideCelebs", "Parse error: ${e.message}")
                return false
            }
        }

        return false
    }
}

 // Data class ekle
 data class VideoQuality(
     val url: String,
     val quality: String
 )