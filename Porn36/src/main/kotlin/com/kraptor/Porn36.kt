// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Porn36 : MainAPI() {
    override var mainUrl              = "https://www.porn36.com"
    override var name                 = "Porn36"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/latest-updates/"  to  "Latest",
        "${mainUrl}/top-rated/"                   to  "Top Rated",
        "${mainUrl}/most-popular/"                to  "Most Popular",
        "${mainUrl}/networks/mylf-com/"           to  "MYLF",
        "${mainUrl}/networks/teamskeet-com/"      to  "Team Skeet",
        "${mainUrl}/networks/nubiles-porn-com/"   to  "Nubiles Porn",
        "${mainUrl}/networks/tushy-com/"          to  "TUSHY",
        "${mainUrl}/networks/rk-com/"             to  "Reality Kings",
        "${mainUrl}/networks/naughtyamerica-com/" to  "Naughty America",
        "${mainUrl}/networks/blacked/"            to  "BLACKED",
        "${mainUrl}/networks/fakehub/"            to  "FakeHub",
        "${mainUrl}/networks/sexmex/"             to  "SEXMEX",
        "${mainUrl}/networks/pornforce/"          to  "PornForce",
        "${mainUrl}/networks/adult-time/"         to  "Adult Time",
        "${mainUrl}/networks/bangbros/"           to  "Bangbros",
        "${mainUrl}/networks/woodman-casting-x/"  to  "Woodman Casting X",
        "${mainUrl}/networks/oldje-com/"          to  "Oldje",
        "${mainUrl}/networks/private/"            to  "Private",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page/").document
        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl    = this.selectFirst("img")?.attr("src")
        val posterAlt    = this.selectFirst("a.thumb_img img")?.attr("data-src")
        val poster       = if (posterUrl?.contains("data:image/gif;base64") ?: return null) {
            posterAlt
        }else{
            posterUrl
        }

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = poster }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/search/${query}/relevance/").document

        return document.select("div.item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.item a")?.text()?.trim()
        val year            = document.selectFirst("h1")?.text()?.substringAfterLast(".")?.trim()?.toIntOrNull()
        val tags            = document.select("div.hidden_tags a").map { it.text() }
        val recommendations = document.select("div.item a.thumb_img").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("div.info a.btn_model").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

    document.select("source").forEach { video ->
        val videoSrc = video?.attr("src").toString()
        val videoQuality = video?.attr("label")
        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = videoSrc,
                type = ExtractorLinkType.VIDEO,
                {
                    this.referer = "${mainUrl}/"
                    this.quality = getQualityFromName(videoQuality)
                })
        )
    }
     return true
    }
}