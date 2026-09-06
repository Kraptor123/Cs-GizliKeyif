// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Horny69 : MainAPI() {
    override var mainUrl              = "https://www.horny69.com"
    override var name                 = "Horny69"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/videos" to "Newly Added",
        "${mainUrl}/videos?sort_by=video_viewed" to "Most Viewed",
        "${mainUrl}/videos?sort_by=video_duration" to "Longest",
        "${mainUrl}/categories/blowjob" to "BlowJob",
        "${mainUrl}/categories/cowgirl" to "Cowgirl",
        "${mainUrl}/categories/big-tits" to "Big Tits",
        "${mainUrl}/categories/milf" to "Milf",
        "${mainUrl}/categories/big-ass" to "Big Ass",
        "${mainUrl}/categories/missionary" to "Missionary",
        "${mainUrl}/categories/pov" to "POV",
        "${mainUrl}/categories/doggystyle" to "Doggystyle",
        "${mainUrl}/categories/deep-throat" to "Deep Throat",
        "${mainUrl}/categories/blonde" to "Blonde",
        "${mainUrl}/categories/threesome" to "Threesome",
        "${mainUrl}/categories/bbc" to "BBC",
        "${mainUrl}/categories/hardcore" to "Hardcore",
        "${mainUrl}/categories/pussy-licking" to "Pussy Licking",
        "${mainUrl}/categories/average-male-body" to "Average Male Body",
        "${mainUrl}/categories/anal" to "Anal",
        "${mainUrl}/categories/big-dick" to "Big Dick",
        "${mainUrl}/categories/tattoo" to "Tattoo"
    )


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            if (request.data.contains("?")) "${request.data}&page=$page" else "${request.data}?page=$page"
        }

        val document = app.get(url).document
        val home     = document.select("article.group").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search?q=$query"
        } else {
            "$mainUrl/search?q=$query&page=$page"
        }

        val document     = app.get(url).document
        val searchAnswer = document.select("article.group").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(searchAnswer)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkTag   = this.selectFirst("a[href*=/videos/]") ?: return null
        val title     = linkTag.selectFirst("h2, h3")?.text()?.trim() ?: return null
        val href      = fixUrlNull(linkTag.attr("href").ifEmpty { return null }) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src")?.ifEmpty { null })

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)


    override suspend fun load(url: String): LoadResponse? {
        Log.d(name, "Load aşaması: $url")
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("button.ptape-play-gate img")?.attr("src")?.ifEmpty { null })
        val plot            = document.selectFirst("div.mt-5.space-y-4 > p")?.text()?.trim()
        val duration        = getDurationFromString(document.selectFirst("div.grid span:has(svg):matches(\\d+:\\d+)")?.text())
        val tags            = document.select("a[href*=/categories/]").map { it.text().trim() }
        val actors          = document.select("a[href*=/pornstars/]").map { Actor(it.text().trim()) }
        val recommendations = document.select("article.group").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = plot
            this.duration        = duration
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(name, "data = $data")
        val document = app.get(data).document
        val videoUrl = document.selectFirst("div[data-src]")?.attr("data-src")?.ifEmpty { null } ?: return false

        callback.invoke(
            newExtractorLink(
                source      = this.name,
                name        = this.name,
                url         = videoUrl,
                type        = ExtractorLinkType.VIDEO,
                initializer = {
                    this.referer = mainUrl
                }
            )
        )
        return true
    }
}