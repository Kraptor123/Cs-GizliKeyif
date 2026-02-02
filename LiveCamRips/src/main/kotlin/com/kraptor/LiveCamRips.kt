package com.kraptor

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.mapOf

class LiveCamRips : MainAPI() {
    override var mainUrl              = "https://livecamrips.to"
    override var name                 = "LiveCamRips"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override var sequentialMainPage   = true
    override var sequentialMainPageDelay       = 550L
    override var sequentialMainPageScrollDelay = 550L

    private var sessionCookies: Map<String, String>? = null
    private val initMutex = Mutex()
    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

    private fun getRandomUserAgent(): String {
        val agents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 OPR/120.0.0.0 (Edition developer)",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        return agents.random()
    }

    private suspend fun initSession() {
        if (sessionCookies != null) return
        initMutex.withLock {
            if (sessionCookies != null) return@withLock

            try {
                val resp = app.get("${mainUrl}/", interceptor = interceptor, headers = mapOf(
                    "Host" to "livecamrips.to",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                    "Accept-Language" to "en-US,en;q=0.5",
                    "Accept-Encoding" to "gzip, deflate, br, zstd",
                    "Referer" to "https://theporndude.com/",
                    "Sec-GPC" to "1",
                    "Connection" to "keep-alive",
                    "Upgrade-Insecure-Requests" to "1",
                    "Sec-Fetch-Dest" to "document",
                    "Sec-Fetch-Mode" to "navigate",
                    "Sec-Fetch-Site" to "cross-site",
                    "Sec-Fetch-User" to "?1",
                    "Priority" to "u=0, i",
                    "Pragma" to "no-cache",
                    "Cache-Control" to "no-cache",
                    "TE" to "trailers"
                ))

                sessionCookies = resp.cookies ?: emptyMap()

            } catch (e: Exception) {
                sessionCookies = emptyMap()
            }
        }
    }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            try {
                val bodyStr = response.peekBody(1024 * 1024).string()
                if (bodyStr.contains("Just a moment") || bodyStr.contains("cloudflare")) {
                    return cloudflareKiller.intercept(chain)
                }
            } catch (e: Exception) { }
            return response
        }
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/tag/18" to "18",
        "${mainUrl}/tag/petite"  to "Petite",
        "${mainUrl}/tag/cute"    to "Cute",
        "${mainUrl}/tag/couple"  to "Couple",
        "${mainUrl}/tag/goth"    to "Goth",
        "${mainUrl}/tag/elegant" to "Elegant",
        "${mainUrl}/tag/milf"    to "Milf",
        "${mainUrl}/tag/shy"     to "Shy",
        "${mainUrl}/tag/latina"  to "Latina",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        initSession()

        val url = if (page == 1) request.data else "${request.data}/$page"
        val response = app.get(url, cookies = sessionCookies ?: emptyMap(), interceptor = interceptor, headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Referer" to "${mainUrl}/"
        ), allowRedirects = false)

        val document = response.document
        val items = document.select("div.col-xl-3").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, items)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("span.tm-text-gray-light")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.img-fluid")?.attr("src"))

        val dataString = "$href:kraptor:${posterUrl?.substringAfter("//")}"

        return newMovieSearchResponse(title, dataString, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Referer" to "${mainUrl}/", "User-Agent" to getRandomUserAgent())
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        initSession()

        val url = if (page == 1) "${mainUrl}/search/$query" else "${mainUrl}/search/$query/$page"
        val document = app.get(url, headers = mapOf("Referer" to "${mainUrl}/")).document

        val results = document.select("div.tm-gallery div.col-12").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(results, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span.tm-text-gray-light")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        val dataString = "$href:kraptor:${posterUrl?.substringAfter("//")}"

        return newMovieSearchResponse(title, dataString, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Referer" to "${mainUrl}/", "User-Agent" to getRandomUserAgent())
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val split = url.split(":kraptor:")
        val targetUrl = split[0]
        val poster = if (split.size > 1) "https://" + split[1] else null

        initSession()
        val document = app.get(targetUrl, interceptor = interceptor, cookies = sessionCookies ?: emptyMap(), headers = mapOf(
            "Referer" to targetUrl,
            "User-Agent" to getRandomUserAgent()
        )).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val description = document.selectFirst("div.video-caption p")?.text()
        val recommendations = document.select("div.col-xl-3").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("span.valor a").map { Actor(it.text()) }
        val tags = document.select("div.video-caption a").map { it.text() }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        val isModelPage = targetUrl.contains("/model/")

        val episodes = document.select("div.tm-gallery div.col-12").mapNotNull { bolum ->
            val epTitle = bolum.selectFirst("span.tm-text-gray-light")?.text() ?: return@mapNotNull null
            val epHref = fixUrlNull(bolum.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            newEpisode(epHref) {
                this.name = epTitle
                this.posterUrl = fixUrlNull(bolum.selectFirst("img")?.attr("src"))
            }
        }

        return if (isModelPage) {
            newTvSeriesLoadResponse(title, targetUrl, TvType.NSFW, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.recommendations = recommendations
                this.tags = tags
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, targetUrl, TvType.NSFW, targetUrl) {
                this.posterUrl = poster
                this.plot = description
                this.recommendations = recommendations
                this.tags = tags
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("div:nth-child(2) > a:nth-child(1) > span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        val dataString = "$href:kraptor:${posterUrl?.substringAfter("//")}"

        return newMovieSearchResponse(title, dataString, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Referer" to "${mainUrl}/", "User-Agent" to getRandomUserAgent())
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        initSession()

        val document = app.get(data, interceptor = interceptor, cookies = sessionCookies ?: emptyMap(), headers = mapOf(
            "Referer" to data,
            "User-Agent" to getRandomUserAgent()
        )).document

        val iframe = document.selectFirst("iframe.embed-responsive-item")?.attr("src") ?: return false

        val fixedIframe = if (iframe.contains("mdzsmutpcvykb")) iframe.replace("mdzsmutpcvykb.net","mixdrop.my") else iframe

        loadExtractor(fixedIframe, referer = "${mainUrl}/", subtitleCallback, callback)
        return true
    }
}