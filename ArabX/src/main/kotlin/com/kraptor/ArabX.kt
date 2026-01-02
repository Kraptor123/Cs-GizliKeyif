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
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ArabX(context: Context) : MainAPI() {
    override var mainUrl              = "https://www.arabx.cam"
    override var name                 = "ArabX"
    override val hasMainPage          = true
    override var lang                 = "ar"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    private val appcontext = context

    override val mainPage = mainPageOf(
        "${mainUrl}/categories/افلام-سكس-مترجمه/" to "افلام سكس مترجمة",
        "${mainUrl}/categories/سكس-بزاز-كبيرة/" to "سكس بزاز كبيرة",
        "${mainUrl}/categories/سكس-محارم/" to "سكس محارم مترجم",
        "${mainUrl}/categories/سكس-امهات-مترجم/" to "سكس امهات مترجم",
        "${mainUrl}/categories/سكس-عائلي/" to "سكس عائلي",
        "${mainUrl}/categories/سكس-مراهقات/" to "سكس مراهقات",
        "${mainUrl}/categories/سكس-اخوات/" to "سكس اخوات مترجم",
        "${mainUrl}/categories/سكس-نيك-الطيز/" to "سكس نيك الطيز",
        "${mainUrl}/categories/سكس/" to "سكس",
        "${mainUrl}/categories/سكس-عربي/" to "سكس عربي",
        "${mainUrl}/categories/سكس-سحاق/" to "سكس سحاق",
        "${mainUrl}/categories/سكس-نيك-سمراوات/" to "سكس نيك سمراوات",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?mode=async&function=get_block&block_id=list_videos_common_videos_list&sort_by=post_date&from=$page").document
        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-webp"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}/").document

        val aramaCevap = document.select("div.list-videos div.item").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div#tab_video_info div.item em")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("meta[property=video:tag]").map { it.attr("content") }
        val score          = document.selectFirst("span.voters")?.text()?.trim()?.substringBefore("%")?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.list-videos div.item").mapNotNull { it.toMainPageResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from100(score)
            this.duration        = duration
            this.recommendations = recommendations
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
                            Log.d("kraptor_Arabx", "WebView temizlendi")
                            cleanupWebView(this@apply)
                        }
                    }, 0)
                }
            }

            // HTML'i yükle
            loadDataWithBaseURL("$mainUrl/", html, "text/html", "UTF-8", null)
        }

        return@withContext wv
    }

    // Video URL'sini gecikmeyle çıkar
    private fun extractVideoWithDelay(webView: WebView?, onResult: (String?) -> Unit, attempt: Int) {
        if (webView == null || attempt > 20) {
            Log.d("kraptor_Arabx", "Timeout reached or WebView is null")
            onResult(null)
            return
        }

        Log.d("kraptor_Arabx", "Attempt $attempt - Extracting video URLs...")

        val extractScript = """
(function() {
    try {
        var results = [];
        
        // tea01d2ec94 değişkenini bul
        var flashvars = null;
        
        for (var key in window) {
            if (key.indexOf('tea') === 0 && typeof window[key] === 'object') {
                flashvars = window[key];
                console.log('Found flashvars variable:', key);
                break;
            }
        }
        
        if (!flashvars && window.player_obj && window.player_obj.conf) {
            flashvars = window.player_obj.conf;
            console.log('Found flashvars via player_obj');
        }
        
        if (!flashvars) {
            console.log('Flashvars not found');
            return null;
        }
        
        console.log('Flashvars found, checking URLs...');
        
        var urlMappings = [
            {key: 'video_url', qualityKey: 'video_url_text', default: '480p'},
            {key: 'video_alt_url', qualityKey: 'video_alt_url_text', default: '720p'},
            {key: 'video_alt_url2', qualityKey: 'video_alt_url2_text', default: '1080p'}
        ];
        
        // Önce URL'lerin işlenip işlenmediğini kontrol et
        var hasUnprocessedUrl = false;
        
        urlMappings.forEach(function(item) {
            if (flashvars[item.key]) {
                var url = flashvars[item.key];
                if (url.indexOf('function/0/') === 0) {
                    hasUnprocessedUrl = true;
                    console.log('URL still has function/0/ prefix:', item.key);
                }
            }
        });
        
        // Eğer hala işlenmemiş URL varsa, henüz hazır değil
        if (hasUnprocessedUrl) {
            console.log('URLs not processed yet, need to wait...');
            return null;
        }
        
        // URL'ler işlenmiş, topla
        urlMappings.forEach(function(item) {
            if (flashvars[item.key]) {
                var url = flashvars[item.key];
                var quality = flashvars[item.qualityKey] || item.default;
                
                // function/0/ prefix'i olmamalı artık ama yine de kontrol et
                if (url.indexOf('function/0/') === 0) {
                    url = url.substring(11);
                }
                
                url = url.replace(/\/+$/, '');
                
                if (url.indexOf('http') === 0) {
                    results.push({
                        url: url,
                        quality: quality
                    });
                    console.log('Added:', quality, '-', url.substring(0, 60) + '...');
                }
            }
        });
        
        if (results.length === 0) {
            console.log('No valid URLs found');
            return null;
        }
        
        console.log('Total URLs found:', results.length);
        
        // Tek satır JSON döndür (newline yok)
        var resultString = '[';
        for (var i = 0; i < results.length; i++) {
            if (i > 0) resultString += ',';
            resultString += '{"url":"' + results[i].url + '","quality":"' + results[i].quality + '"}';
        }
        resultString += ']';
        return resultString;
        
    } catch (e) {
        console.log('Error:', e.toString());
        return null;
    }
})();
""".trimIndent()

        webView.evaluateJavascript(extractScript) { resultJson ->
            Log.d("kraptor_Arabx", "Raw result: '$resultJson'")

            val cleanResult = resultJson?.let { raw ->
                if (raw == "null" || raw == "\"null\"") null
                else raw.removePrefix("\"").removeSuffix("\"")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\/", "/")
            }

            if (cleanResult.isNullOrEmpty() || cleanResult == "null") {
                if (attempt < 20) {
                    // İlk 5 denemede 500ms, sonra 2 saniye bekle
                    val delay = if (attempt < 5) 500L else 2000L
                    Log.d("kraptor_Arabx", "Not ready, retrying in ${delay}ms...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractVideoWithDelay(webView, onResult, attempt + 1)
                    }, delay)
                } else {
                    Log.d("kraptor_Arabx", "Max attempts reached")
                    onResult(null)
                }
            } else {
                Log.d("kraptor_Arabx", "SUCCESS: $cleanResult")
                onResult(cleanResult)
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val iframe = document.selectFirst("iframe")?.attr("src")

        Log.d("kraptor_$name", "iframe = ${iframe}")

        if (iframe != null){
            loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        } else {
            val videoResultJson = suspendCoroutine { continuation ->
                runBlocking {
                    createWebViewAndExtractVideo(context = appcontext, document.html()) { result ->
                        continuation.resume(result)
                    }
                }
            }

        Log.d("kraptor_Arabx", "Video JSON Result = $videoResultJson")

            videoResultJson?.let { jsonResult ->
                try {
                    // JSON parse et
                    val videoList = parseJson<List<VideoQuality>>(jsonResult)

                    videoList?.forEach { video ->
                        if (video.url.startsWith("http")) {
                        Log.d("kraptor_Arabx", "Adding: ${video.quality} - ${video.url}")

                            val videoUrl = app.get(video.url, referer = "${mainUrl}/", allowRedirects = true).url

                        Log.d("kraptor_Arabx", "videoUrl = $videoUrl")

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
                Log.e("kraptor_Arabx", "Parse error: ${e.message}")
                    return false
                }
            }
        }
        return true
    }
}

data class VideoQuality(
    val url: String,
    val quality: String
)