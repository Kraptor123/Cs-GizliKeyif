// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class KtPlayerVideo(
    @JsonProperty("url") val url: String,
    @JsonProperty("quality") val quality: String? = null
)

class KtPlayerExtractor(private val context: Context) {

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
    private suspend fun createWebViewAndExtract(
        baseUrl: String,
        html: String,
        onResult: (String?) -> Unit
    ): WebView = withContext(Dispatchers.Main) {

        val wv = WebView(context.applicationContext).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                blockNetworkImage = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }

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

            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }

        return@withContext wv
    }

    private fun extractVideoWithDelay(webView: WebView?, onResult: (String?) -> Unit, attempt: Int) {
        if (webView == null || attempt > 25) {
            onResult(null)
            return
        }

        val extractScript = """
            (function() {
                try {
                    var results = [];
                    if (typeof flashvars !== 'undefined' && flashvars) {
                        var isStillDecrypting = false;
                        var keys = ['video_url', 'video_alt_url', 'video_alt_url2', 'video_alt_url3', 'video_alt_url4'];
                        for (var i = 0; i < keys.length; i++) {
                            var k = keys[i];
                            if (flashvars[k] && typeof flashvars[k] === 'string' && flashvars[k].startsWith('function/')) {
                                isStillDecrypting = true;
                                break;
                            }
                        }
                        if (isStillDecrypting) {
                            return null;
                        }
                        var rnd = flashvars.rnd || '';
                        for (var i = 0; i < keys.length; i++) {
                            var k = keys[i];
                            var vUrl = flashvars[k];
                            if (vUrl && typeof vUrl === 'string' && vUrl.startsWith('http')) {
                                if (rnd && !vUrl.includes('rnd=')) {
                                    vUrl += (vUrl.includes('?') ? '&' : '?') + 'rnd=' + rnd;
                                }
                                var quality = flashvars[k + '_text'] || (k === 'video_url' ? '360p' : (k === 'video_alt_url' ? '480p' : (k === 'video_alt_url2' ? '720p' : '1080p')));
                                results.push({
                                    url: vUrl,
                                    quality: quality
                                });
                            }
                        }
                    }
                    if (results.length === 0 && typeof window.player_obj !== 'undefined' && window.player_obj) {
                        if (window.player_obj.config && window.player_obj.config.video_url && !window.player_obj.config.video_url.startsWith('function/')) {
                            results.push({
                                url: window.player_obj.config.video_url,
                                quality: window.player_obj.config.video_url_text || '360p'
                            });
                        }
                    }
                    if (results.length === 0) {
                        var videos = document.getElementsByTagName('video');
                        for (var v = 0; v < videos.length; v++) {
                            var vid = videos[v];
                            var src = vid.src || vid.currentSrc;
                            if (src && src.startsWith('http') && !src.startsWith('function/')) {
                                results.push({
                                    url: src,
                                    quality: 'HD'
                                });
                                break;
                            }
                        }
                    }
                    return results.length > 0 ? JSON.stringify(results) : null;
                } catch(e) {
                    return null;
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(extractScript) { resultJson ->
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
                if (attempt < 25) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractVideoWithDelay(webView, onResult, attempt + 1)
                    }, 500)
                } else {
                    onResult(null)
                }
            } else {
                onResult(cleanResult)
            }
        }
    }

    suspend fun getLinks(
        sourceName: String,
        mainUrl: String,
        pageUrl: String,
        pageHtml: String,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val rawUrls = Regex("""(video_(?:alt_)?url\d*):\s*['"]([^'"]+)['"]""").findAll(pageHtml)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val texts = Regex("""(video_(?:alt_)?url\d*_text):\s*['"]([^'"]+)['"]""").findAll(pageHtml)
            .associate { it.groupValues[1].removeSuffix("_text") to it.groupValues[2] }

        val hasEncrypted = rawUrls.values.any { it.startsWith("function/") }

        if (rawUrls.isNotEmpty() && !hasEncrypted) {
            rawUrls.forEach { (key, url) ->
                val quality = texts[key] ?: "480p"
                val qual = getQualityInt(quality, url)

                callback.invoke(
                    newExtractorLink(
                        source = sourceName,
                        name = sourceName,
                        url = url,
                        type = ExtractorLinkType.VIDEO,
                        initializer = {
                            this.quality = qual
                            this.referer = "$mainUrl/"
                            this.headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:156.0) Gecko/20100101 Firefox/156.0",
                                "Referer" to pageUrl
                            )
                        }
                    )
                )
            }
            return true
        }

        val videoResultJson = suspendCancellableCoroutine { continuation ->
            runBlocking {
                createWebViewAndExtract(mainUrl, pageHtml) { result ->
                    continuation.resume(result)
                }
            }
        }

        if (!videoResultJson.isNullOrEmpty()) {
            try {
                val videos = AppUtils.tryParseJson<List<KtPlayerVideo>>(videoResultJson)
                if (!videos.isNullOrEmpty()) {
                    videos.forEach { video ->
                        val qual = getQualityInt(video.quality ?: "", video.url)
                        callback.invoke(
                            newExtractorLink(
                                source = sourceName,
                                name = sourceName,
                                url = video.url,
                                type = ExtractorLinkType.VIDEO,
                                initializer = {
                                    this.quality = qual
                                    this.referer = "$mainUrl/"
                                    this.headers = mapOf(
                                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:156.0) Gecko/20100101 Firefox/156.0",
                                        "Referer" to pageUrl
                                    )
                                }
                            )
                        )
                    }
                    return true
                }
            } catch (e: Exception) {
                Log.e("KtPlayerExtractor", "Error parsing video JSON: ${e.message}")
            }
        }

        return false
    }

    private fun getQualityInt(quality: String, url: String): Int {
        return when {
            quality.contains("2160") || quality.contains("4k", ignoreCase = true) || url.contains("2160p") || url.contains("4k", ignoreCase = true) -> Qualities.P2160.value
            quality.contains("1440") || url.contains("1440p") -> Qualities.P1440.value
            quality.contains("1080") || url.contains("1080p") -> Qualities.P1080.value
            quality.contains("720") || url.contains("720p")   -> Qualities.P720.value
            quality.contains("480") || url.contains("480p")   -> Qualities.P480.value
            quality.contains("360") || url.contains("360p")   -> Qualities.P360.value
            quality.contains("240") || url.contains("240p")   -> Qualities.P240.value
            else                                              -> Qualities.P480.value
        }
    }
}
