// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.annotation.SuppressLint
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.Actor
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.content.Context



class EU : MainAPI() {
    override var mainUrl = "https://18eu.net"
    override var name = "18EU"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/trending/" to "All Movies",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document
        val home = document.select("div.items article").mapNotNull {
            it.toMainPageResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.data h3 a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/?s=$query"
        } else {
            "$mainUrl/page/$page/?s=$query"
        }

        val response = app.get(url).document
        val aramaCevap = response.select("div.result-item").mapNotNull {
            it.toSearchResult()
        }
        val hasNext = response.selectFirst("div.pagination a.arrow_pag") != null ||
                response.selectFirst("div.resppages a") != null

        return newSearchResponseList(aramaCevap, hasNext = hasNext)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst("div.details div.title a")
        val title = titleElement?.text() ?: return null
        val href = titleElement.attr("href")
        val posterUrl = fixUrlNull(this.selectFirst("div.image img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)


    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("div.data h1")?.text()?.trim() ?: ""
        val poster = document.selectFirst("div.poster img")?.attr("src")
        val banner = document.selectFirst("div.backdrop img")?.attr("src") ?: poster
        val description = document.selectFirst("#info .wp-content p")?.text()?.trim()

        val actors = document.select("#cast .persons .person").mapNotNull {
            val name = it.selectFirst("meta[itemprop=name]")?.attr("content")
                ?: it.selectFirst(".name a")?.text()
            val image = it.selectFirst("img")?.attr("src")
            if (name.isNullOrBlank()) return@mapNotNull null
            Actor(name, image)
        }

        val episodes = document.select("#seasons .se-c .se-a ul li").mapNotNull {
            val epNum = it.selectFirst(".num")?.text()?.toIntOrNull()
            val epName = it.selectFirst(".episodiotitle a")?.text()
            val epUrl = it.selectFirst(".episodiotitle a")?.attr("href")
            if (epUrl == null) return@mapNotNull null
            newEpisode(fixUrl(epUrl)) {
                this.name = epName
                this.episode = epNum
            }
        }

        val watchBtn = document.selectFirst("div.sgeneros a")?.attr("href")

        return if (episodes.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.backgroundPosterUrl = banner
                this.plot = description
                addActors(actors)
            }
        } else if (watchBtn != null) {
            val fullUrl = fixUrl(watchBtn)
            newMovieLoadResponse(title, url, TvType.Movie, fullUrl) {
                this.posterUrl = poster
                this.backgroundPosterUrl = banner
                this.plot = description
                addActors(actors)
            }
        } else {
            val movieSlug = url.trimEnd('/').substringAfterLast("/")
            val constructedUrl = "$mainUrl/watch-$movieSlug?sv=1&ep=1"
            newMovieLoadResponse(title, url, TvType.Movie, constructedUrl) {
                this.posterUrl = poster
                this.backgroundPosterUrl = banner
                this.plot = description
                addActors(actors)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "PrivateApi")
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val context = Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as? Context ?: return false

        val capturedUrl = suspendCancellableCoroutine { cont ->
            Handler(Looper.getMainLooper()).post {
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val url = request.url.toString()
                        if (url.contains(".m3u8") && cont.isActive) {
                            cont.resume(url)
                            view.post { view.destroy() }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        view.evaluateJavascript("""
                        setInterval(() => {
                            if (typeof jwplayer === 'function') jwplayer().play();
                            document.querySelector('.jw-display-icon-display, .videoapi-btn')?.click();
                        }, 1000);
                    """.trimIndent(), null)
                    }
                }

                webView.loadUrl(data)

                Handler(Looper.getMainLooper()).postDelayed({
                    if (cont.isActive) {
                        cont.resume(null)
                        webView.destroy()
                    }
                }, 30_000)
            }
        }

        return capturedUrl?.let {
            callback.invoke(
                newExtractorLink(this.name, this.name, it) {
                    this.referer = "https://pinkueiga.net/"
                    this.type = ExtractorLinkType.M3U8
                    this.quality = Qualities.P720.value
                }
            )
            return true
        } ?: false
    }
}