// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
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
    override var mainUrl              = "https://livecamrips.su"
    override var name                 = "LiveCamRips"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override var sequentialMainPage   = true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay       = 550L // ? 0.25 saniye
    override var sequentialMainPageScrollDelay = 550L // ? 0.25 saniye
    private var sessionCookies: Map<String, String>? = null
    private val initMutex = Mutex()
    private val posterCache = mutableMapOf<String, String>()

    private suspend fun initSession() {
        if (sessionCookies != null) return
        initMutex.withLock {
            if (sessionCookies != null ) return@withLock

            Log.d("kraptor_LiveCamRips", "🔄 Oturum başlatılıyor: cookie aliniyor")
            val resp = app.get(mainUrl, interceptor = interceptor, timeout = 120, headers =  mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
                "Referer" to "${mainUrl}/",
            ))
            sessionCookies = resp.cookies
            Log.d("kraptor_LiveCamRips", "🔄 cookie alindi = $sessionCookies")
        }
    }

    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

            if (doc.html().contains("Just a moment")) {
                Log.d("kraptor_livecamrips", "cloudflare geldi!")
                return cloudflareKiller.intercept(chain)
            }

            return response
        }
    }

    override val mainPage = mainPageOf(
//        "${mainUrl}/category/Female/" to "Latest",
        "${mainUrl}/tag/18/"                      to "18",
        "${mainUrl}/tag/petite/"                  to "Petite",
//        "${mainUrl}/tag/smalltits/"               to "Small Tits",
//        "${mainUrl}/tag/milf/"                    to "Milf",
//        "${mainUrl}/tag/skinny/"                  to "Skinny",
        "${mainUrl}/tag/cute/"                    to "Cute",
//        "${mainUrl}/tag/latina/"                  to "Latina",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        initSession()
        val document = app.get("${request.data}/$page", cookies = sessionCookies!!, interceptor = interceptor, headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Cache-Control" to "no-cache",
            "Connection" to "keep-alive",
            "DNT" to "1",
            "Host" to "livecamrips.su",
            "Pragma" to "no-cache",
            "Priority" to "u=0, i",
            "Referer" to "${request.data}/$page",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-User" to "?1",
            "Sec-GPC" to "1",
            "Upgrade-Insecure-Requests" to "1",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        )).document
        val home     = document.select("div.col-xl-3").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div:nth-child(2) > a:nth-child(1) > span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.img-fluid")?.attr("src"))
        posterUrl?.let { posterCache[href] = it }
        val posterHeaders   = mapOf(
            "Referer" to "${mainUrl}/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        )

        return newMovieSearchResponse(title, href, TvType.NSFW){
            this.posterUrl = posterUrl
            this.posterHeaders = posterHeaders
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        initSession()
        val document = app.get("${mainUrl}/search/${query}/1",interceptor = interceptor , cookies = sessionCookies!!, headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Referer" to "${mainUrl}/",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "same-origin",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        )).document
        return document.select("div.col-xl-3").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div:nth-child(2) > a:nth-child(1) > span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val posterHeaders   = mapOf(
            "Referer" to "${mainUrl}/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        )
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = posterHeaders
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        initSession()
        val document = app.get(url, interceptor = interceptor, cookies = sessionCookies!!, headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Cache-Control" to "no-cache",
            "Connection" to "keep-alive",
            "DNT" to "1",
            "Host" to "livecamrips.su",
            "Pragma" to "no-cache",
            "Priority" to "u=0, i",
            "Referer" to url,
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-User" to "?1",
            "Sec-GPC" to "1",
            "Upgrade-Insecure-Requests" to "1",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        )).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = posterCache[url]
        val posterHeaders = poster?.let {
            mapOf(
                "Referer" to "${mainUrl}/",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
            )
        }
        val description     = document.selectFirst("div.wp-content p")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.video-caption a").map { it.text().replace("#","") }
        val rating          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()?.toRatingInt()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.col-xl-3").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.posterHeaders   = posterHeaders
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(rating)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("div:nth-child(2) > a:nth-child(1) > span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val posterHeaders   = mapOf(
            "Referer" to "${mainUrl}/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = posterHeaders
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data » ${data}")
        val document = app.get(data, interceptor = interceptor, cookies = sessionCookies!!, headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Cache-Control" to "no-cache",
            "Connection" to "keep-alive",
            "DNT" to "1",
            "Host" to "livecamrips.su",
            "Pragma" to "no-cache",
            "Priority" to "u=0, i",
            "Referer" to data,
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-User" to "?1",
            "Sec-GPC" to "1",
            "Upgrade-Insecure-Requests" to "1",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        )).document
        val iframe   = fixUrlNull(document.selectFirst("iframe.embed-responsive-item")?.attr("src")).toString()
        val iframelink = if (iframe.contains("mdzsmutpcvykb")) {
            iframe.replace("mdzsmutpcvykb.net","mixdrop.my")
        } else {
            iframe
        }
        Log.d("kraptor_$name", "iframelink = ${iframelink}")
        loadExtractor(iframelink, referer = "${mainUrl}/" , subtitleCallback, callback)

        return true
    }
}