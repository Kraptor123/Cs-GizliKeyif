// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Element

class FreeUsePorn : MainAPI() {
    override var mainUrl = "https://www.freeuseporn.com"
    override var name = "FreeUsePorn"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/mind-control" to "Mind Control",
        "${mainUrl}/videos/general-freeuse" to "General Freeuse",
        "${mainUrl}/videos/free-service" to "Free Service",
        "${mainUrl}/videos/forced" to "Forced",
        "${mainUrl}/videos/time-stop" to "Time Stop",
        "${mainUrl}/videos/japanese" to "Japanese",
        "${mainUrl}/videos/ignored-sex" to "Ignored Sex",
        "${mainUrl}/videos/glory-hole" to "Glory Hole",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?page=$page").document
        val home = document.select("ul#videos-list li").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("span.v-name")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/videos/${query}?page=$page").document

        val aramaCevap = document.select("ul#videos-list li").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags = document.select("div.tags a").map { it.text() }
        val score =
            document.selectFirst("ul.data li:contains(Rating)")?.text()?.substringAfter(" ")?.replace("%", "")?.trim()
        val recommendations = document.select("div.col-12.col-md-2").mapNotNull { it.toMainPageResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.score = Score.from100(score)
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

        val videolar = document.select("source")

        videolar.forEach { video ->
            val link = video.attr("src")
            val res = video.attr("res").toIntOrNull() ?: 0

            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = link,
                    type = ExtractorLinkType.VIDEO,
                    initializer = {
                        this.referer = "${mainUrl}/"
                        this.quality = res
                    }
                ))
        }

        return true
    }
}