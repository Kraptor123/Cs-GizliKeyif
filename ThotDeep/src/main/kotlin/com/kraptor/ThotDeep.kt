// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.nicehttp.cookies
import okhttp3.Interceptor
import java.net.URLDecoder

class ThotDeep : MainAPI() {
    override var mainUrl              = "https://thotdeep.com"
    override var name                 = "ThotDeep"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Main Page",
        "${mainUrl}/popular" to "Popular",
        "${mainUrl}/tags/actress" to "Actress",
        "${mainUrl}/categories/tv-personalities" to "Tv Personalities",
        "${mainUrl}/celebrities" to "Celebrities",
    )

    private var cookie = mapOf("" to "")

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?page=$page").document

        val home = document.select("div.post, div.model-card").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, true))

    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3 a, h3.model-card-name")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img[loading=lazy]")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search?type=title&s=$query&page=$page").document

        val aramaCevap = document.select("div.post, div.model-card").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div#player-wrap img[src*=http], div.model-avatar img")?.attr("src"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("span:contains(cate) ~ a").map { it.text() }
        val score          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.post").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("span:contains(celeb) ~ a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        val episodes        = document.select("div.post").mapNotNull { bolum ->
            val title     = bolum.selectFirst("a")?.text() ?: return null
            val href      = fixUrlNull(bolum.selectFirst("a")?.attr("href")) ?: return null
            val posterUrl = fixUrlNull(bolum.selectFirst("img[loading=lazy]")?.attr("src"))
            newEpisode(href,{
                this.posterUrl = posterUrl
                this.name = title
            })
        }

        val celeb = (url.contains("celebrities"))

        return if (!celeb) {
            newMovieLoadResponse(title, url, TvType.NSFW, url) {
                this.posterUrl       = poster
                this.plot            = description
                this.year            = year
                this.tags            = tags
                this.score           = Score.from10(score)
                this.duration        = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes) {
                this.posterUrl       = poster
                this.plot            = description
                this.year            = year
                this.tags            = tags
                this.score           = Score.from10(score)
                this.duration        = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    private var capturedCookies: Map<String, String> = emptyMap()

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data, referer = "${mainUrl}/")
        capturedCookies = document.cookies

        val data = document.document.selectFirst("div#player-wrap")?.attr("data-source") ?: ""

        val video = base64Decode(data.drop(16).reversed().drop(16)).trim()

        callback.invoke(newExtractorLink(
            this.name,
            this.name,
            video,
            initializer = {
                this.referer = "$mainUrl/"
            }
        ))

        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val cookieString = capturedCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            val newRequest = originalRequest.newBuilder()
                .header("Referer", "$mainUrl/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")

            if (cookieString.isNotEmpty()) {
                newRequest.header("Cookie", cookieString)
            }

            val builtRequest = newRequest.build()
            val response = chain.proceed(builtRequest)
            if (!response.isSuccessful) {
                Log.e("kraptor_error", "Video oynatılamadı. Kod: ${response.code}")
            }

            response
        }
    }
}