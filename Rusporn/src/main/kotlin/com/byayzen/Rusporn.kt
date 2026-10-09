// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Rusporn(private val context: Context) : MainAPI() {
    override var mainUrl            = "https://en.rusporn.center"
    override var name               = "Rusporn"
    override val hasMainPage        = true
    override var lang               = "ru"
    override val hasQuickSearch     = false
    override val supportedTypes     = setOf(TvType.NSFW)
    override var sequentialMainPage = true

    private var sessionHeaders: Map<String, String>? = null
    private val initMutex = Mutex()

    private suspend fun getBypassedDocument(url: String): Document {
        sessionHeaders?.let { headers ->
            try {
                val res   = app.get(url, headers = headers)
                val doc   = res.document
                val title = doc.title()
                if (!title.contains("Just a moment", ignoreCase = true) &&
                    !title.contains("Cloudflare", ignoreCase = true) &&
                    !title.contains("Attention Required", ignoreCase = true)
                ) {
                    return doc
                }
            } catch (e: Exception) {
                Log.d("Rusporn", "e=$e")
            }
        }

        return initMutex.withLock {
            sessionHeaders?.let { headers ->
                try {
                    val res   = app.get(url, headers = headers)
                    val doc   = res.document
                    val title = doc.title()
                    if (!title.contains("Just a moment", ignoreCase = true) &&
                        !title.contains("Cloudflare", ignoreCase = true) &&
                        !title.contains("Attention Required", ignoreCase = true)
                    ) {
                        return@withLock doc
                    }
                } catch (_: Exception) {}
            }

            Log.d("Rusporn", "url=$url")
            val html = loadUrlInNativeWebView(url)
            Jsoup.parse(html, url)
        }
    }

    private suspend fun loadUrlInNativeWebView(url: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                var isCompleted   = false
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                val webView = WebView(context.applicationContext).apply {
                    settings.javaScriptEnabled     = true
                    settings.domStorageEnabled      = true
                    settings.useWideViewPort       = true
                    settings.loadWithOverviewMode = true
                }

                val handler  = Handler(Looper.getMainLooper())
                var attempts = 0

                fun cleanup() {
                    try {
                        handler.removeCallbacksAndMessages(null)
                        webView.stopLoading()
                        webView.webViewClient = object : WebViewClient() {}
                        webView.destroy()
                    } catch (_: Throwable) {}
                }

                fun finishWithHtml(html: String) {
                    if (isCompleted) return
                    isCompleted = true
                    handler.removeCallbacksAndMessages(null)

                    val cookies = cookieManager.getCookie(url)
                    val ua      = webView.settings.userAgentString
                    if (!cookies.isNullOrBlank()) {
                        sessionHeaders = mapOf(
                            "Cookie"     to cookies,
                            "User-Agent" to ua,
                            "Referer"    to "$mainUrl/"
                        )
                    }

                    cleanup()
                    if (continuation.isActive) {
                        continuation.resume(html)
                    }
                }

                fun checkStatus() {
                    if (isCompleted) return
                    attempts++

                    webView.evaluateJavascript("document.title + '|||' + document.documentElement.outerHTML") { result ->
                        if (isCompleted) return@evaluateJavascript

                        val raw = result?.removePrefix("\"")?.removeSuffix("\"")
                            ?.replace("\\\"", "\"")
                            ?.replace("\\n", "\n")
                            ?.replace("\\u003C", "<")
                            ?.replace("\\u003E", ">")
                            ?: ""

                        val parts = raw.split("|||", limit = 2)
                        val title = parts.getOrNull(0) ?: ""
                        val html  = parts.getOrNull(1) ?: ""

                        val isCfTitle = title.contains("Just a moment", ignoreCase = true) ||
                                title.contains("Cloudflare", ignoreCase = true) ||
                                title.contains("Attention Required", ignoreCase = true)

                        val hasContent = html.contains("preview") || html.contains("video-categories") || html.contains("ivideo_info")

                        Log.d("Rusporn", "attempts=$attempts title=$title hasContent=$hasContent")

                        if (!isCfTitle && (hasContent || title.isNotBlank())) {
                            finishWithHtml(html)
                        } else if (attempts < 20) {
                            handler.postDelayed({ checkStatus() }, 1000)
                        } else {
                            finishWithHtml(html)
                        }
                    }
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                        super.onPageFinished(view, loadedUrl)
                        handler.postDelayed({ checkStatus() }, 1500)
                    }
                }

                webView.loadUrl(url)

                continuation.invokeOnCancellation {
                    handler.post { cleanup() }
                }

            } catch (e: Exception) {
                Log.e("Rusporn", "e=$e")
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/domashneye/"        to "Amateur",
        "${mainUrl}/anal/"              to "Anal",
        "${mainUrl}/aziatki/"           to "Asians",
        "${mainUrl}/bolshiye-popki/"    to "Big Ass",
        "${mainUrl}/bolshiye-chleny/"   to "Big Dick",
        "${mainUrl}/bolshiye-doyki/"    to "Big Tits",
        "${mainUrl}/blondinki/"         to "Blondes",
        "${mainUrl}/lesbiyanki/"        to "Lesbians",
        "${mainUrl}/massazh/"           to "Massage",
        "${mainUrl}/masturbatsiya/"     to "Masturbation",
        "${mainUrl}/mamki/"             to "MILF",
        "${mainUrl}/negry/"             to "Blacked",
        "${mainUrl}/molodyye/"          to "Teen"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data}page-${page}/"
        }
        Log.d("Rusporn", "page=$page url=$url")

        val document = getBypassedDocument(url)
        val home     = document.select("div#preview, div.preview, div.preview-images").mapNotNull { it.toSearchResult() }
        Log.d("Rusporn", "home=${home.size} page=$page title=${document.title()}")

        val nextPageLink = document.select("a[href*='page-${page + 1}']").firstOrNull()
        val hasNext      = nextPageLink != null

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = hasNext
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.preview-name a, div.title a, h1, h2")?.text()?.trim()
            ?: return null
        val href = fixUrlNull(
            this.selectFirst("div.preview-images a, div.title a, a[href*='/video/'], a")?.attr("href")
        ) ?: return null

        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("src")
                ?: this.selectFirst("div.preview-images img")?.attr("src")
        ) ?: return null

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl    = if (page == 1) {
            "${mainUrl}/search/?text=${encodedQuery}"
        } else {
            "${mainUrl}/search/?text=${encodedQuery}&page=$page"
        }

        val document = getBypassedDocument(searchUrl)
        val results  = document.select("div#preview, div.preview, div.preview-images").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(results, hasNext = true)
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = getBypassedDocument(url)

        val title = document.selectFirst("h1")?.text()?.trim()
            ?: document.selectFirst("title")?.text()?.trim()?.substringBefore(" - HD porn online")
            ?: return null

        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
            ?: fixUrlNull(document.selectFirst("div.story-description img")?.attr("src"))
            ?: fixUrlNull(document.selectFirst("div.preview-images img")?.attr("src"))

        val description = document.selectFirst("div.story-description#ivideo_info")?.text()?.trim()
            ?: document.selectFirst("meta[name=description]")?.attr("content")?.trim()
        val tags        = document.select("div.video-categories a").map { it.text().trim() }

        val recommendations = document.select("div#preview, div.preview").mapNotNull { element ->
            val recTitle  = element.selectFirst("div.preview-name a")?.text()?.trim()
                ?: return@mapNotNull null
            val recHref   = fixUrlNull(element.selectFirst("div.preview-images a")?.attr("href"))
                ?: return@mapNotNull null
            val recPoster = fixUrlNull(element.selectFirst("div.preview-images img")?.attr("src"))
            newMovieSearchResponse(recTitle, recHref, TvType.NSFW) { this.posterUrl = recPoster }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document  = getBypassedDocument(data)
        val scriptTag = document.select("script").find { it.data().contains("var player") }?.data()
            ?: return false

        var count = 0
        Regex("""\[([\d+p]+)]\s*([^,]]+)""").findAll(scriptTag)
            .map { match -> Pair(match.groupValues[1], match.groupValues[2].trim()) }
            .sortedByDescending { (quality, _) -> quality.replace(Regex("\\D"), "").toIntOrNull() ?: 0 }
            .forEach { (quality, url) ->
                callback(
                    newExtractorLink(
                        source = name,
                        name   = "$name - $quality",
                        url    = url,
                        type   = ExtractorLinkType.VIDEO
                    )
                )
                count++
            }
        return count > 0
    }
}