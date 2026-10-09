// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Javtiful : MainAPI() {
    override var mainUrl              = "https://javtiful.com"
    override var name                 = "Javtiful"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "$mainUrl/videos" to "Newest",
        "$mainUrl/videos?sort=most_viewed" to "Most Viewed",
        "$mainUrl/videos?sort=top_rated" to "Top Rated",
        "$mainUrl/category/chinese-av" to "Chinese AV",
        "$mainUrl/category/mature-woman" to "Mature Woman",
        "$mainUrl/category/amateur" to "Amateur",
        "$mainUrl/category/beautiful-girl" to "Beautiful Girl",
        "$mainUrl/category/married-woman" to "Married Woman",
        "$mainUrl/category/big-tits" to "Big Tits",
        "$mainUrl/category/school-girls" to "School Girls",
        "$mainUrl/category/drama" to "Drama",
        "$mainUrl/category/affair" to "Affair",
        "$mainUrl/category/office-lady" to "Office Lady",
        "$mainUrl/category/female-student" to "Female Student",
        "$mainUrl/category/cosplay" to "Cosplay",
        "$mainUrl/category/female-teacher" to "Female Teacher",
        "$mainUrl/category/milf" to "Milf",
        "$mainUrl/category/nurse" to "Nurse",
        "$mainUrl/category/female-boss" to "Female Boss",
        "$mainUrl/category/sister-in-law" to "Sister-in-law",
        "$mainUrl/category/female-investigator" to "Female Investigator",
        "$mainUrl/category/housekeeper" to "Housekeeper",
        "$mainUrl/category/hypnosis" to "Hypnosis",
        "$mainUrl/category/bbw" to "BBW"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            if (request.data.contains("?")) "${request.data}&page=$page" else "${request.data}?page=$page"
        }

        val document = app.get(url).document
        val home = document.select("article.video-card").mapNotNull { it.toSearchResponse() }
        val hasNext = document.selectFirst("nav.pagination a.pagination__link:contains(Next):not(.is-disabled)") != null

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = hasNext
        )
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        if (this.hasClass("video-card--partner")) return null

        val titleLink = this.selectFirst("a.video-card__title") ?: return null
        val title = titleLink.text().trim().ifEmpty { return null }
        val href = fixUrlNull(titleLink.attr("href").ifEmpty { return null }) ?: return null

        if (!href.startsWith(mainUrl) && !href.startsWith("/")) return null
        if (!href.contains("/video/")) return null

        val img = this.selectFirst("img")
        val posterUrl = fixUrlNull(
            img?.attr("data-front-lazy-current-src")?.ifEmpty { null }
                ?: img?.attr("data-front-lazy-src")?.ifEmpty { null }
                ?: img?.attr("src")?.ifEmpty { null }
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Referer" to "$mainUrl/", "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search?q=$query"
        } else {
            "$mainUrl/search?page=$page&q=$query"
        }

        val document = app.get(url).document
        val searchResults = document.select("article.video-card").mapNotNull { it.toSearchResponse() }
        val hasNext = document.selectFirst("nav.pagination a.pagination__link:contains(Next):not(.is-disabled)") != null

        return newSearchResponseList(searchResults, hasNext = hasNext)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.watch-title h1")?.text()?.trim()?.ifEmpty { null } ?: return null

        val poster = fixUrlNull(
            document.selectFirst("video#front-player")?.attr("data-poster")?.ifEmpty { null }
                ?: document.selectFirst("video#front-player")?.attr("poster")?.ifEmpty { null }
                ?: document.selectFirst("meta[property=og:image]")?.attr("content")?.ifEmpty { null }
        )

        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()?.ifEmpty { null }

        val datetime = document.selectFirst("div.watch-detail:contains(Added on) time")?.attr("datetime")?.ifEmpty { null }
        val year = datetime?.split("-")?.firstOrNull()?.toIntOrNull()

        val tags = document.select("div.watch-detail:contains(Tags) a, div.watch-detail:contains(Categories) a").map { it.text().trim() }

        val actorList = document.select("a.watch-actor-card").mapNotNull { el ->
            val actorName = el.selectFirst("span")?.text()?.trim()?.ifEmpty { return@mapNotNull null } ?: return@mapNotNull null
            val actorImg = fixUrlNull(el.selectFirst("img")?.attr("src")?.ifEmpty { null })
            ActorData(Actor(actorName, actorImg))
        }

        val recommendations = document.select("div.related-grid article.video-card").mapNotNull {
            it.toSearchResponse()
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.posterHeaders = mapOf("Referer" to "$mainUrl/", "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            this.plot = description
            this.year = year
            this.tags = tags
            this.actors = actorList
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val sources = document.select("video source[src]")

        var found = false
        sources.forEach { sourceEl ->
            val src = fixUrlNull(sourceEl.attr("src").ifEmpty { return@forEach }) ?: return@forEach
            val qualityStr = sourceEl.attr("size")
            val quality = qualityStr.toIntOrNull() ?: Qualities.Unknown.value
            val linkType = if (src.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO

            callback(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = src,
                    type = linkType
                ) {
                    this.quality = quality
                    this.referer = "$mainUrl/"
                    this.headers = mapOf("Referer" to "$mainUrl/", "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                }
            )
            found = true
        }

        return found
    }
}
