// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

class Freshporno : MainAPI() {
    override var mainUrl = "https://freshporno.net"
    override var name = "Freshporno"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/channels/brazzers/" to "Brazzers",
        "${mainUrl}/channels/reality-kings/" to "Reality Kings",
        "${mainUrl}/channels/teamskeet/" to "TeamSkeet",
        "${mainUrl}/channels/naughty-america/" to "Naughty America",
        "${mainUrl}/channels/sexyhub/" to "SexyHub",
        "${mainUrl}/channels/mylf/" to "MYLF",
        "${mainUrl}/channels/mofos/" to "Mofos",
        "${mainUrl}/channels/nubiles-porn/" to "Nubiles Porn",
        "${mainUrl}/channels/adulttime/" to "AdultTime",
        "${mainUrl}/channels/evil-angel/" to "Evil Angel",
        "${mainUrl}/channels/private/" to "Private",
        "${mainUrl}/channels/digitalplayground/" to "DigitalPlayground",
        "${mainUrl}/channels/porn-world/" to "Porn World",
        "${mainUrl}/channels/mile-high-media/" to "Mile High Media",
        "${mainUrl}/channels/twistys/" to "Twistys",
        "${mainUrl}/channels/babes-com/" to "Babes.Com",
        "${mainUrl}/channels/nubile-films/" to "Nubile Films",
        "${mainUrl}/channels/fakehub/" to "FakeHub",
        "${mainUrl}/channels/fake-taxi/" to "Fake Taxi",
        "${mainUrl}/channels/sexmex/" to "SexMex",
        "${mainUrl}/channels/sweet-sinner/" to "Sweet Sinner",
        "${mainUrl}/channels/public-agent/" to "Public Agent"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}$page/"

        val document = app.get(url).document
        val home = document.select("div.page-content").mapNotNull {
            it.toMainPageResult()
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse>? {

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "${mainUrl}/search/?q=${encodedQuery}"

        val document = app.get(searchUrl).document

        val results = document.select("div.page-content").mapNotNull {
            it.toSearchResult()
        }

        return results
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags = document.select("ul.video-tags a").map { it.text() }
        val recommendations = document.select("div#list_videos_related_videos div.page-content").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original"))
        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("ul.download-list li").forEach { li ->
            val aTag = li.selectFirst("a") ?: return@forEach
            val url = aTag.attr("href")
            val quality = aTag.text().split(",").first().trim()
            callback(
                newExtractorLink(
                    source = "FreshPorno",
                    name = "FreshPorno - $quality",
                    url = url,
                    type = ExtractorLinkType.VIDEO
                )
            )
        }
        return true
    }
}