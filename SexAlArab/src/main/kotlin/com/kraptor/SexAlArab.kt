// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SexAlArab(context: Context) : MainAPI() {
    override var mainUrl              = "https://sexalarab.com"
    override var name                 = "SexAlArab"
    override val hasMainPage          = true
    override var lang                 = "ar"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    private val appContext = context

    override val mainPage = mainPageOf(
        "${mainUrl}/category/افلام-سكس-بنات-سحاق/"  to "سكس سحاق",
        "${mainUrl}/category/سكس-ميلف/"  to "سكس ميلف",
        "${mainUrl}/category/سكس-امهات/"  to "سكس امهات",
        "${mainUrl}/category/مسلسلات-سكس-مترجم/"  to "مسلسلات",
        "${mainUrl}/category/سكس-عائلي/"  to "سكس عائلي",
        "${mainUrl}/category/سكس-نيك-طيز/"  to "سكس طيز",
        "${mainUrl}/category/سكس-نيك-اخوات/"  to "سكس اخوات",
        "${mainUrl}/category/سكس-نيك-مراهقات/"  to "سكس مراهقات",
        "${mainUrl}/category/سكس-مترجم/"  to "سكس مترجم",
        "${mainUrl}/category/سكس-مدبلج/"  to "سكس مدبلج",

    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?mode=async&function=get_block&block_id=list_videos_common_videos_list&sort_by=post_date&is_private=&from=$page").document
        val home     = document.select("div.item.private").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("strong.title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original")) ?:  fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Referer" to "${mainUrl}/")
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/?mode=async&function=get_block&block_id=list_videos_videos_list_search_result&q=$query&category_ids=&sort_by=&is_private=&from_videos=$page").document

        val aramaCevap = document.select("div.item.private").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.headline h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.item.drtl em")?.text()?.trim()
        val tags            = document.select("div.video-info div.item:contains(التصنيفات) > a").map { it.text() }
        val recommendations = document.select("div.item.private").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("div.item a.link[href*=models]").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
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
                        Handler(Looper.getMainLooper()).post {
                            cleanupWebView(this@apply)
                        }
                    }, 0)
                }
            }

            loadDataWithBaseURL("https://sexalarab.com/", html, "text/html", "UTF-8", null)
        }

        return@withContext wv
    }

    private fun extractVideoWithDelay(webView: WebView?, onResult: (String?) -> Unit, attempt: Int) {
        if (webView == null || attempt > 20) {
            onResult(null)
            return
        }


        val extractScript = """
    (function() {
        try {
            var results = [];
            
            // kt_player.js yüklendi mi kontrol et
            var ktPlayerLoaded = typeof kt_player === 'function';
            console.log('kt_player loaded:', ktPlayerLoaded);
            
            // flashvars var mı kontrol et
            if (typeof flashvars === 'undefined' || !flashvars) {
                console.log('flashvars not found yet');
                return null;
            }
            
            console.log('flashvars found, checking URLs...');
            
            // Video URL'lerini topla
            var urls = [
                {key: 'video_url', qualityKey: 'video_url_text', default: '360p'},
                {key: 'video_alt_url', qualityKey: 'video_alt_url_text', default: '480p'},
                {key: 'video_alt_url2', qualityKey: 'video_alt_url2_text', default: '720p'},
                {key: 'video_alt_url3', qualityKey: 'video_alt_url3_text', default: '1080p'}
            ];
            
            var hasUnprocessedUrl = false;
            
            urls.forEach(function(item) {
                if (flashvars[item.key]) {
                    var url = flashvars[item.key];
                    
                    // Eğer hala function/0/ prefix'i varsa, kt_player.js henüz işlememiş demektir
                    if (url.startsWith('function/0/')) {
                        hasUnprocessedUrl = true;
                        console.log('Found unprocessed URL:', item.key);
                    }
                }
            });
            
            // kt_player yüklenmişse VE hala işlenmemiş URL varsa, biraz daha bekle
            if (ktPlayerLoaded && hasUnprocessedUrl) {
                console.log('kt_player loaded but URLs not processed yet, waiting...');
                return null;
            }
            
            // kt_player yüklenmemişse ve işlenmemiş URL varsa, kendi işleyelim
            if (!ktPlayerLoaded && hasUnprocessedUrl) {
                console.log('kt_player not loaded, processing URLs manually...');
            }
            
            // URL'leri topla
            urls.forEach(function(item) {
                if (flashvars[item.key]) {
                    var url = flashvars[item.key];
                    var quality = flashvars[item.qualityKey] || item.default;
                    
                    // function/0/ prefix'ini kaldır
                    if (url.startsWith('function/0/')) {
                        url = url.substring(11);
                    }
                    
                    // Sondaki / varsa kaldır
                    url = url.replace(/\/+$/, '');
                    
                    // Geçerli URL kontrolü
                    if (url.startsWith('http')) {
                        results.push({
                            url: url,
                            quality: quality
                        });
                        console.log('Added:', quality, url.substring(0, 50) + '...');
                    }
                }
            });
            
            if (results.length === 0) {
                console.log('No valid URLs found');
                return null;
            }
            
            console.log('Total URLs found:', results.length);
            return JSON.stringify(results);
            
        } catch (e) {
            console.log('Error:', e.toString());
            return null;
        }
    })();
""".trimIndent()

        webView.evaluateJavascript(extractScript) { resultJson ->

            val cleanResult = resultJson?.let { raw ->
                if (raw == "null" || raw == "\"null\"") null
                else raw.removePrefix("\"").removeSuffix("\"")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\/", "/")
            }

            if (cleanResult.isNullOrEmpty() || cleanResult == "null") {
                if (attempt < 20) {
                    val delay = if (attempt < 5) 500L else 2000L
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractVideoWithDelay(webView, onResult, attempt + 1)
                    }, delay)
                    onResult(null)
                }
            } else {
                onResult(cleanResult)
            }
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val pageHtml = app.get(data).text


        val videoResultJson = suspendCoroutine { continuation ->
            runBlocking {
                createWebViewAndExtractVideo(appContext, pageHtml) { result ->
                    continuation.resume(result)
                }
            }
        }


        videoResultJson?.let { jsonResult ->
            try {
                val videoList = parseJson<List<VideoQuality>>(jsonResult)

                videoList?.forEach { video ->
                    if (video.url.startsWith("http")) {

                        val videoUrl = app.get(video.url, referer = "${mainUrl}/", allowRedirects = true).url


                        callback.invoke(
                            newExtractorLink(
                                source = this.name,
                                name = this.name,
                                url = videoUrl,
                                type = ExtractorLinkType.VIDEO,
                            ) {
                                this.referer = "${mainUrl}/"
                                quality = getQualityFromName(video.quality)
                            })
                    }
                }
                return videoList.isNotEmpty()

            } catch (e: Exception) {
                return false
            }
        }

        return false
    }
}

data class VideoQuality(
    val url: String,
    val quality: String
)