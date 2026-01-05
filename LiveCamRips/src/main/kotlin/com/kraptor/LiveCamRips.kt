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

            Log.d("kraptor_LiveCamRips", "🔄 Oturum başlatılıyor: Sadece interceptor (cloudflareKiller) ile HTTP GET denenecek (tek adım)")

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

                val doc = resp.document
                val title = doc?.title() ?: ""
                val bodyLen = doc?.html()?.length ?: 0

                Log.d("kraptor_LiveCamRips", "Interceptor GET tamamlandı. title='$title' bodyLength=$bodyLen cookies=${resp.cookies}")

                sessionCookies = resp.cookies ?: emptyMap()

                if (sessionCookies!!.isNotEmpty()) {
                    Log.d("kraptor_LiveCamRips", "✅ Interceptor ile cookie alindi = $sessionCookies")
                } else {
                    Log.w("kraptor_LiveCamRips", "⚠️ Interceptor GET yapildi ama cookie bulunamadi. sessionCookies boş set ediliyor.")
                }

            } catch (e: Exception) {
                Log.e("kraptor_LiveCamRips", "❌ Interceptor GET hata: ${e.message}. sessionCookies boş set ediliyor.")
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
                val doc = Jsoup.parse(bodyStr)
                if (doc.html().contains("Just a moment") || doc.title().contains("Attention Required") || bodyStr.contains("cloudflare")) {
                    Log.d("kraptor_livecamrips", "Cloudflare içerigi tespit edildi in interceptor -> cloudflareKiller devreye giriyor")
                    return cloudflareKiller.intercept(chain)
                }
            } catch (e: Exception) {
                Log.w("kraptor_livecamrips", "Interceptor parse hatasi: ${e.message}")
            }
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
        Log.d("kraptor_LiveCamRips", "getMainPage page=$page request=${request.data}")
        initSession()
        val document = if (page==1){
            app.get("${request.data}", cookies = sessionCookies ?: emptyMap(), interceptor = interceptor, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language" to "en-US,en;q=0.5",
                "Referer" to "${mainUrl}/",
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
            ), allowRedirects = false).document
        } else{
            app.get("${request.data}/$page", cookies = sessionCookies ?: emptyMap(), interceptor = interceptor, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language" to "en-US,en;q=0.5",
                "Referer" to "${mainUrl}/",
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
            ), allowRedirects = false).document
        }

//        Log.d("kraptor_LiveCamRips", "document = ${document.title()} / body length = ${document.html().length}")
        val home = document.select("div.col-xl-3").mapNotNull { it.toMainPageResult() }
//        Log.d("kraptor_LiveCamRips", "Found ${home.size} main items")
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("span.tm-text-gray-light")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.img-fluid")?.attr("src"))
        val posterHeaders = mapOf("Referer" to "${mainUrl}/", "User-Agent" to getRandomUserAgent())

        return newMovieSearchResponse(title, "$href:kraptor:${posterUrl?.substringAfter("//")}", TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = posterHeaders
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
//        Log.d("kraptor_LiveCamRips", "search query=$query")
        initSession()
        val document = if (page==1){
            app.get("${mainUrl}/search/$query", headers =  mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language" to "en-US,en;q=0.5",
                "Referer" to "${mainUrl}/",
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
            )).document
        } else {
            app.get("${mainUrl}/search/$query/$page", headers =  mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language" to "en-US,en;q=0.5",
                "Referer" to "${mainUrl}/",
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
            )).document
        }
        Log.d("kraptor_LiveCamRips", "search document title=${document}")
        val aramaCevap = document.select("div.tm-gallery div.col-12").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span.tm-text-gray-light")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val posterHeaders = mapOf("Referer" to "${mainUrl}/", "User-Agent" to getRandomUserAgent())

        return newMovieSearchResponse(title, "$href:kraptor:${posterUrl?.substringAfter("//")}", TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = posterHeaders
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        Log.d("kraptor_LiveCamRips", "url =$url")
        val split = url.split(":kraptor:")
        Log.d("kraptor_LiveCamRips", "split url=$split")
        val url = split[0]
        val poster = "https://" + split[1]
        Log.d("kraptor_LiveCamRips", "load url=$url")
        initSession()
        val document = app.get(url, interceptor = interceptor, cookies = sessionCookies ?: emptyMap(), headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Referer" to url,
            "User-Agent" to getRandomUserAgent()
        )).document

//        Log.d("kraptor_LiveCamRips", "load document title=${document.title()} body size=${document.html().length}")
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val description = document
            .selectFirst("div.video-caption p")
            ?.textNodes()
            ?.joinToString(" ") { it.text().trim() }
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
        val recommendations = document.select("div.col-xl-3").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("span.valor a").map { Actor(it.text()) }
        val tags    = document.select("div.video-caption a").map { it.text() }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        val model = url.contains("/model/")

        val episodes = document.select("div.tm-gallery div.col-12").mapNotNull { bolum ->
            val title     = bolum.selectFirst("span.tm-text-gray-light")?.text() ?: return null
            val href      = fixUrlNull(bolum.selectFirst("a")?.attr("href")) ?: return null
            val posterUrl = fixUrlNull(bolum.selectFirst("img")?.attr("src"))
            newEpisode(href, {
                this.name = title
                this.posterUrl = posterUrl
            })
        }

        return if (model){
            newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.recommendations = recommendations
                this.tags = tags
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.NSFW, url) {
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
        val posterHeaders = mapOf("Referer" to "${mainUrl}/", "User-Agent" to getRandomUserAgent())
        return newMovieSearchResponse(title, "$href:kraptor:${posterUrl?.substringAfter("//")}", TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = posterHeaders
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "loadLinks data=$data")
        initSession()
        val document = app.get(data, interceptor = interceptor, cookies = sessionCookies ?: emptyMap(), headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Referer" to data,
            "User-Agent" to getRandomUserAgent()
        )).document

        val iframe = fixUrlNull(document.selectFirst("iframe.embed-responsive-item")?.attr("src")).toString()
        val iframelink = if (iframe.contains("mdzsmutpcvykb")) iframe.replace("mdzsmutpcvykb.net","mixdrop.my") else iframe
        Log.d("kraptor_$name", "iframelink = $iframelink")
        loadExtractor(iframelink, referer = "${mainUrl}/", subtitleCallback, callback)
        return true
    }
}

