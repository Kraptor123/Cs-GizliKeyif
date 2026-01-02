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
import com.lagradost.cloudstream3.network.CloudflareKiller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.text.ifEmpty
import kotlin.text.isNotEmpty
import kotlin.text.startsWith

class FullPorner(private val context: Context) : MainAPI() {
    override var mainUrl              = "https://fullporner.com"
    override var name                 = "FullPorner"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/home/"    to "Featured",
        "${mainUrl}/category/hd-porn/"    to "HD",
        "${mainUrl}/category/amateur/"    to "Amateur",
        "${mainUrl}/category/teen/"       to "Teen",
        "${mainUrl}/category/cumshot/"    to "CumShot",
        "${mainUrl}/category/deepthroat/" to "DeepThroat",
        "${mainUrl}/category/orgasm/"     to "Orgasm",
        "${mainUrl}/category/threesome/"  to "ThreeSome",
        "${mainUrl}/category/group-sex/"  to "Group Sex",
    )

    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

            if (doc.html().contains("Just a moment")) {
                return cloudflareKiller.intercept(chain)
            }

            return response
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}", interceptor = interceptor).document
        val home     = document.select("div.video-block div.video-card").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.video-card div.video-card-body div.video-title a")?.text() ?: return null
        val href      = fixUrl(this.selectFirst("div.video-card div.video-card-body div.video-title a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.select("div.video-card div.video-card-image a img").attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {

            val document = app.get("${mainUrl}/search?q=${query.replace(" ", "+")}&p=$page", interceptor = interceptor).document

            val aramaCevap = document.select("div.video-block div.video-card").mapNotNull { it.toSearchResult() }

            return newSearchResponseList(aramaCevap, hasNext = true)
        }
    private fun cleanupWebView(wv: WebView) {
        try {
            wv.stopLoading()
            wv.setWebChromeClient(null)   // nullable setter kullan
            wv.webViewClient = object : WebViewClient() {}
            wv.removeAllViews()
            wv.clearHistory()
            wv.loadUrl("about:blank")
            wv.destroy()
        } catch (ignored: Throwable) {}
    }

    suspend fun createWebViewAndExtract(
        context: Context,
        baseUrl: String,
        html: String,
        onResult: (String?) -> Unit
    ): WebView = withContext(Dispatchers.Main) {

        val modifiedHtml = html.replace(
            Regex("""jwplayer\s*\(\s*["']player["']\s*\)\s*\.setup\s*\(\s*configs\s*\)\s*;"""),
            """
        window.configs = configs;
        console.log('jwplayer configs set:', JSON.stringify(configs));
        jwplayer("player").setup(configs);
        """.trimIndent()
        )

        val wv = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    extractWithDelay(view, { result ->
                        onResult(result)
                        // işi bitince temizle (main thread)
                        Handler(Looper.getMainLooper()).post {
                            Log.d("kraptor_filmmak", "webview temizlendi")
                            cleanupWebView(this@apply)
                        }
                    }, 0)
                }
            }
            loadDataWithBaseURL(baseUrl, modifiedHtml, "text/html", "UTF-8", null)
        }

        return@withContext wv
    }

    private fun extractWithDelay(webView: WebView?, onResult: (String?) -> Unit, attempt: Int) {
        if (webView == null || attempt > 15) {
            Log.d("kraptor_webview", "Timeout reached or WebView is null")
            onResult(null)
            return
        }

//        Log.d("kraptor_webview", "Attempt $attempt - Checking for configs...")

        val extractScript = """
            (function() {
                var result = {};
                
                // 1. window.configs (JWPlayer)
                if (typeof window.configs !== 'undefined' && window.configs) {
                    result.configs = window.configs;
                    result.type = 'jwplayer_configs';
                    return JSON.stringify(result);
                }
                
                // 2. JWPlayer instance
                if (typeof jwplayer !== 'undefined') {
                    try {
                        var instances = jwplayer().getPlaylist();
                        if (instances && instances.length > 0) {
                            result.playlist = instances;
                            result.type = 'jwplayer';
                            return JSON.stringify(result);
                        }
                    } catch(e) {}
                }
                
                // 3. VideoJS
                if (typeof videojs !== 'undefined') {
                    try {
                        // Tüm video elementlerini kontrol et
                        var videoElements = document.querySelectorAll('video');
                        for (var i = 0; i < videoElements.length; i++) {
                            var el = videoElements[i];
                            if (el.id && videojs.getPlayer) {
                                var player = videojs.getPlayer(el.id);
                                if (player && player.currentSources) {
                                    result.sources = player.currentSources();
                                    result.type = 'videojs';
                                    return JSON.stringify(result);
                                }
                            }
                        }
                    } catch(e) {}
                }
                
                // 4. Generic video source extraction
                var videos = document.querySelectorAll('video source, video');
                if (videos.length > 0) {
                    var sources = [];
                    videos.forEach(function(v) {
                        if (v.src) sources.push({src: v.src, type: v.type});
                    });
                    if (sources.length > 0) {
                        result.sources = sources;
                        result.type = 'html5';
                        return JSON.stringify(result);
                    }
                }
                
                return null;
            })();
        """.trimIndent()

        webView.evaluateJavascript(extractScript) { resultJson ->
//            Log.d("kraptor_webview", "=== CONFIG JSON DEBUG ===")
//            Log.d("kraptor_webview", "Raw resultJson: '$resultJson'")

            val cleanResult = resultJson?.let { raw ->
                if (raw == "null" || raw == "\"null\"") {
                    null
                } else {
                    raw.removePrefix("\"").removeSuffix("\"")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                }
            }

//            Log.d("kraptor_webview", "Cleaned result length: ${cleanResult?.length ?: 0}")

            if (cleanResult.isNullOrEmpty() || cleanResult == "null") {
                if (attempt < 15) {
//                    Log.d("kraptor_webview", "Config is null/empty, retrying in 800ms...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractWithDelay(webView, onResult, attempt + 1)
                    }, 200)
                } else {
//                    Log.d("kraptor_webview", "Max attempts reached, giving up")
                    onResult(null)
                }
            } else {
//                Log.d("kraptor_webview", "SUCCESS! Found config")
                onResult(cleanResult)
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, interceptor = interceptor).document

        val title     = document.selectFirst("div.video-block div.single-video-left div.single-video-title h2")?.text()?.trim().toString()
        val iframeUrl      = fixUrlNull(document.selectFirst("div.video-block div.single-video-left div.single-video iframe")?.attr("src")) ?: ""
//        Log.d("kraptor_Fullporner","iframeUrl = $iframeUrl")
        val videoID          = iframeUrl.substringAfter("video/").substringBefore("/")
//        Log.d("kraptor_Fullporner","videoID = $videoID")
        val reverseId        = videoID.reversed()
        val poster           = "https://xiaoshenke.net/vid/$reverseId/720/i"
//        Log.d("kraptor_Fullporner","poster = $poster")
        val posterAl         = app.get(poster, referer = "https://xiaoshenke.net/", allowRedirects = true).url

        val tags            = document.select("div.video-block div.single-video-left div.single-video-title p.tag-link span a").map { it.text() }
        val description     = document.selectFirst("div.video-block div.single-video-left div.single-video-title h2")?.text()?.trim().toString()
        val actors          = document.select("div.video-block div.single-video-left div.single-video-info-content p a").map { it.text() }
        val recommendations = document.select("div.video-block div.video-recommendation div.video-card").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = posterAl
            this.posterHeaders   = mapOf("Referer" to "https://xiaoshenke.net/")
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val document       = app.get(data, interceptor = interceptor).document
        val iframeUrl      = fixUrlNull(document.selectFirst("div.video-block div.single-video-left div.single-video iframe")?.attr("src")) ?: ""
        Log.d("kraptor_$name","iframeurl = $iframeUrl")

        val iframeText = app.get(iframeUrl, interceptor = interceptor).text

        val configJson = suspendCoroutine<String?> { continuation ->
            runBlocking {
                createWebViewAndExtract(context = context, iframeUrl, iframeText) { result ->
                    continuation.resume(result)
                }
            }
        }

        configJson?.let { configStr ->

        val configObj = JSONObject(configStr)

            Log.d("kraptor_Fullporner","configobj = $configObj")

        if (configObj.has("sources")) {
            val sources = configObj.getJSONArray("sources")
            for (i in 0 until sources.length()) {
                val sourceObj = sources.getJSONObject(i)
                val videoUrl = sourceObj.optString("file", "").ifEmpty {
                    sourceObj.optString("src", "")
                }
                val quality = sourceObj.optString("src","").substringAfterLast("/")

                if (videoUrl.isNotEmpty() && videoUrl.startsWith("http")) {
                    callback.invoke(
                        newExtractorLink(
                            source = "FullPorner",
                            name = "FullPorner",
                            url = videoUrl,
                            type = ExtractorLinkType.VIDEO,
                            {
                                this.referer = "${mainUrl}/"
                                this.quality = getQualityFromName(quality)
                            }
                        ))
                }
            }
        }
        else {
            loadExtractor(iframeUrl, subtitleCallback, callback)
        }
    }
return@withContext true
}
}

