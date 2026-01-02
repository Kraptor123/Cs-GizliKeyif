// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AllClassicPorn(context: Context) : MainAPI() {
    override var mainUrl              = "https://allclassic.porn"
    override var name                 = "AllClassicPorn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    private val appContext = context

    override val mainPage = mainPageOf(
        "${mainUrl}/categories/amateur/"            to  "Amateur Classic",
        "${mainUrl}/categories/anal/"               to  "Anal",
        "${mainUrl}/categories/antique/"            to  "Antique",
        "${mainUrl}/categories/asian/"              to  "Asian",
        "${mainUrl}/categories/babe/"               to  "Babe",
        "${mainUrl}/categories/bbw/"                to  "BBW And Fat",
        "${mainUrl}/categories/big-ass/"            to  "Big Ass",
        "${mainUrl}/categories/big-dick/"           to  "Big Dick",
        "${mainUrl}/categories/big-tits/"           to  "Big Tits",
        "${mainUrl}/categories/blonde/"             to  "Blondes",
        "${mainUrl}/categories/blowjob/"            to  "Blowjobs",
        "${mainUrl}/categories/bondage/"            to  "Bondage and BDSM",
        "${mainUrl}/categories/brunette/"           to  "Brunettes",
        "${mainUrl}/categories/compilation/"        to  "Compilation",
        "${mainUrl}/categories/cuckold/"            to  "Cuckold",
        "${mainUrl}/categories/cumshot/"            to  "Cumshots",
        "${mainUrl}/categories/cunnilingus/"        to  "Cunnilingus",
        "${mainUrl}/categories/deepthroat/"         to  "Deepthroat",
        "${mainUrl}/categories/double-penetration/" to  "Double Penetration",
        "${mainUrl}/categories/ebony/"              to  "Ebony",
        "${mainUrl}/categories/european/"           to  "European",
        "${mainUrl}/categories/family/"             to  "Family",
        "${mainUrl}/categories/female-orgasm/"      to  "Female orgasm",
        "${mainUrl}/categories/fetish/"             to  "Fetish",
        "${mainUrl}/categories/fisting/"            to  "Fisting",
        "${mainUrl}/categories/full-movie/"         to  "Full Movies",
        "${mainUrl}/categories/gangbang/"           to  "Gangbang",
//        "${mainUrl}/categories/gay/"                to  "Gay",
        "${mainUrl}/categories/hairy/"              to  "Hairy",
        "${mainUrl}/categories/handjob/"            to  "Handjob",
        "${mainUrl}/categories/hardcore/"           to  "Hardcore",
        "${mainUrl}/categories/hd/"                 to  "HD",
        "${mainUrl}/categories/historical/"         to  "Historical",
        "${mainUrl}/categories/interracial/"        to  "Interracial",
        "${mainUrl}/categories/lesbian/"            to  "Lesbians",
        "${mainUrl}/categories/lingerie/"           to  "Lingerie",
        "${mainUrl}/categories/masturbation/"       to  "Masturbation",
        "${mainUrl}/categories/mature/"             to  "Mature",
        "${mainUrl}/categories/milf/"               to  "MILF",
        "${mainUrl}/categories/old-and-young/"      to  "Old and Young",
        "${mainUrl}/categories/orgy/"               to  "Orgy",
        "${mainUrl}/categories/petite/"             to  "Petite",
        "${mainUrl}/categories/pissing/"            to  "Pissing",
        "${mainUrl}/categories/pornstars/"          to  "Pornstars",
        "${mainUrl}/categories/rare/"               to  "Rare",
        "${mainUrl}/categories/redhead/"            to  "Redhead",
        "${mainUrl}/categories/riding/"             to  "Riding",
        "${mainUrl}/categories/school/"             to  "School",
        "${mainUrl}/categories/skinny/"             to  "Skinny",
        "${mainUrl}/categories/small-tits/"         to  "Small Tits",
        "${mainUrl}/categories/softcore/"           to  "Softcore",
        "${mainUrl}/categories/solo/"               to  "Solo",
        "${mainUrl}/categories/stockings/"          to  "Stockings",
        "${mainUrl}/categories/striptease/"         to  "Striptease",
        "${mainUrl}/categories/teen/"               to  "Teens",
        "${mainUrl}/categories/threesome/"          to  "Threesomes",
        "${mainUrl}/categories/toys/"               to  "Toys",
        "${mainUrl}/categories/vintage/"            to  "Vintage",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page/").document
        val home     = document.select("a.th.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.th-description")?.text() ?: return null
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val puan = this.selectFirst("span.th-rating")?.text()?.replace("%","")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(puan)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query.replace(" ","-")}/$page/").document

        val aramaCevap = document.select("a.th.item").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.th-description")?.text() ?: return null
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val puan = this.selectFirst("span.th-rating")?.text()?.replace("%","")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(puan)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1.h2")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.description-container")?.text()?.substringAfter(":")?.trim()
        val year            = document.selectFirst("span.move-right:has(strong:matchesOwn(^Released:))")?.text()?.substringAfter(" ")?.trim()?.toIntOrNull()
        val tags            = document.select("div.video-links p:has(strong:matchesOwn(^Tags:)) a").map { it.text().trim() }
        val rating          = document.selectFirst("div.voters strong")?.text()?.replace("%","")?.trim()
        val duration        = document.selectFirst("meta[property=video:duration]")?.attr("content")?.split("M")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("a.th.item").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.video-links p:has(strong:matchesOwn(^Models:))")
            .flatMap { aktorler ->
                aktorler.select("a.btn[itemprop=actor]").map { aTag ->
                    val aktorIsim = aTag.text().trim()
                    val aktorPoster = aTag.selectFirst("img")?.attr("src")
                    Actor(aktorIsim, aktorPoster)
                }
            }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from100(rating)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("div.th-description")?.text() ?: return null
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val puan = this.selectFirst("span.th-rating")?.text()?.replace("%","")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(puan)
        }
    }

    // Minimal WebView temizleme
    private fun cleanupWebView(wv: WebView) {
        try {
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.destroy()
        } catch (ignored: Throwable) {}
    }

    // Ultra minimal WebView - gereksiz her şey kaldırıldı
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun createWebViewAndExtractVideo(
        context: Context,
        html: String,
        onResult: (String?) -> Unit
    ): WebView = withContext(Dispatchers.Main) {

        val wv = WebView(context.applicationContext).apply {
            // Sadece gerekli ayarlar
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                blockNetworkImage = true // Hız için resimler yüklenmesin
                cacheMode = WebSettings.LOAD_NO_CACHE
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Direkt çıkar - bekleme yok
                    extractVideoUrl(view, onResult)
                }
            }

            loadDataWithBaseURL("https://allclassic.porn/", html, "text/html", "UTF-8", null)
        }

        return@withContext wv
    }

    // Tek seferlik video URL çıkarma - retry yok
    private fun extractVideoUrl(webView: WebView?, onResult: (String?) -> Unit) {
        webView?.evaluateJavascript("""
    (function() {
        // En muhtemel yerler - ilk bulduğunu döndür
        if (window.player_obj?.config?.video_url) return window.player_obj.config.video_url;
        if (window.flashvars?.video_url) return window.flashvars.video_url;
        
        // Video element kontrolü
        var video = document.querySelector('video');
        if (video?.src?.includes('.mp4')) return video.src;
        if (video?.currentSrc?.includes('.mp4')) return video.currentSrc;
        
        // Script içinde .mp4 ara
        var scripts = document.querySelectorAll('script');
        for (var script of scripts) {
            var content = script.innerHTML;
            if (content.includes('.mp4')) {
                var match = content.match(/video_url['"]\s*:\s*['"]([^'"]+\.mp4[^'"]*)['"]/i) ||
                           content.match(/(https?:\/\/[^\s'"]+\.mp4[^\s'"]*)/i);
                if (match) return match[1];
            }
        }
        return null;
    })();
    """.trimIndent()) { result ->

            val videoUrl = result?.takeIf { it != "null" && it.isNotEmpty() }
                ?.removePrefix("\"")?.removeSuffix("\"")
                ?.replace("\\/", "/")

            Log.d("kraptor_${this.name}", if (videoUrl != null) "SUCCESS! Video URL: $videoUrl" else "Video URL bulunamadı")

            onResult(videoUrl)
            cleanupWebView(webView)
        }
    }

    suspend fun getVideoUrl(appContext: Context, pageHtml: String): String? {
        return suspendCoroutine { continuation ->
            // Main thread'de çalıştır - WebView için gerekli
            CoroutineScope(Dispatchers.Main).launch {
                createWebViewAndExtractVideo(appContext, pageHtml) { result ->
                    continuation.resume(result)
                }
            }
        }
    }


    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
       Log.d("kraptor_${this.name}", "data » $data")
        val pageHtml = app.get(data).text

       Log.d("kraptor_${this.name}", "WebView ile kt_player video URL'si çıkarılıyor...")

        val videoUrl = getVideoUrl(appContext, pageHtml)

       Log.d("kraptor_${this.name}", "Final video URL = $videoUrl")

        videoUrl?.let { url ->
            if (url.startsWith("http")) {
                callback.invoke(newExtractorLink(
                    source = name,
                    name = name,
                    url = url,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.referer = "${mainUrl}/"
                })
                return true
            }
        }

        return true
    }
}