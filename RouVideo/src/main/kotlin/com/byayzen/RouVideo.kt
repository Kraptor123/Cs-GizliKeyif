// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class RouVideo : MainAPI() {
    override var mainUrl = "https://rou.video"
    override var name = "RouVIDEO"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/home" to "Home",
        "${mainUrl}/v" to "All Videos",
        "${mainUrl}/t/國產AV" to "Chinese AV",
        "${mainUrl}/t/自拍流出" to "Leaked/Amateur",
        "${mainUrl}/t/探花" to "Tanhua",
        "${mainUrl}/t/OnlyFans" to "OnlyFans",
        "${mainUrl}/t/日本" to "Japanese"

    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1 || request.data.endsWith("/home")) {
            request.data
        } else {
            if (request.data.contains("?")) {
                "${request.data}&page=$page"
            } else {
                "${request.data}?page=$page"
            }
        }

        val document = app.get(url).document
        val items = document.select("div[data-slot='card'], div.text-card-foreground").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = true
            ),
            hasNext = if (request.data.endsWith("/home")) false else items.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search?q=$query"
        } else {
            "$mainUrl/search?q=$query&page=$page"
        }

        val document = app.get(url).document
        val items = document.select("div[data-slot='card'], div.text-card-foreground").mapNotNull {
            it.toSearchResult()
        }

        return newSearchResponseList(items, hasNext = items.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null

        val images = this.select("img")
        val posterUrl = if (images.size > 1) {
            fixUrlNull(images[1].attr("src"))
        } else {
            fixUrlNull(images.firstOrNull()?.attr("src"))
        }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = document.selectFirst("meta[property='og:image']")?.attr("content")
        val description =
            document.selectFirst("meta[property='og:description']")?.attr("content")?.trim()

        // Etiketler (Tags)
        val tags =
            document.select("div[data-slot='card-content'] a[href^='/t/']").map { it.text().trim() }

        // Önerilen Videolar (Recommendations)
        val recommendations = document.select("div.group.relative.p-2").mapNotNull {
            it.toRecommendationResult()
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val videoId = data.split("/").lastOrNull() ?: return false
        val apiUrl = "$mainUrl/api/v/$videoId"

        val res = app.get(apiUrl).text
        val m3u8Url =
            Regex("""\"videoUrl\"\s*:\s*\"(.*?)\"""").find(res)?.groupValues?.get(1) ?: return false

        callback.invoke(
            newExtractorLink(
                name,
                name,
                m3u8Url,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = "$mainUrl/"
                this.quality = Qualities.Unknown.value
            }
        )

        return true
    }
}