// ! Bu araç @ByAyzen tarafından | @cs-kraptor için yazılmış, yavşak kerime özel yapılmıştır.

package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Koreanpornmovie : MainAPI() {
    override var mainUrl = "https://koreanpornmovie.com"
    override var name = "Koreanpornmovie"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "latest" to "Ana Sayfa",
        "longest" to "En Uzun Videolar",
        "random" to "Rastgele Videolar",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val filter = request.data

        val pagePath = if (page <= 1) "" else "/page/$page/"
        val url = "$mainUrl$pagePath?filter=$filter"

        val document = app.get(url).document
        val home = document.select("div.videos-list article").mapNotNull { it.toSearchResponse() }

        return newHomePageResponse(
            list = HomePageList(name = request.name, list = home, isHorizontalImages = true),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = this.selectFirst("header.entry-header span")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl =
            this.attr("data-main-thumb").takeIf { it.isNotEmpty() }?.let { fixUrlNull(it) }
                ?: this.selectFirst("img")?.attr("src")?.let { fixUrlNull(it) }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) "$mainUrl/?s=$query" else "$mainUrl/page/$page/?s=$query"
        val items = app.get(url).document.select("div article")
            .mapNotNull { it.toSearchResponse() }

        return newSearchResponseList(items, hasNext = items.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title =
            document.selectFirst("header.entry-header h1, header.entry-header span")?.text()?.trim()
                ?: return null

        val iframeSrc = document.selectFirst("div.responsive-player iframe")?.attr("src")
        val poster = if (!iframeSrc.isNullOrEmpty()) {
            val iframeDoc = app.get(iframeSrc).document
            iframeDoc.selectFirst("video")?.attr("poster")?.let { fixUrlNull(it) }
        } else {
            null
        } ?: ""
        val description = document.select("div.desc p")
            .joinToString(" ") { it.text().trim() }
            .replace("\\s+".toRegex(), " ")
            .split(Regex("\\bsynopsis\\b\\s*:?", RegexOption.IGNORE_CASE), limit = 2)
            .getOrNull(1)?.trim() ?: ""

        val actors = document.select("div#video-actors a").map { Actor(it.text().trim()) }
        val year = document.selectFirst("div#video-date")?.text()
            ?.let { Regex("\\d{4}").find(it)?.value?.toIntOrNull() }
        val tags = document.select("a[rel=category], a[rel=tag]").map { it.text() }
        val recommendations = document.select("div.under-video-block div article")
            .mapNotNull { it.toSearchResponse() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
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
        val document = app.get(data).document
        val title =
            document.selectFirst("header.entry-header h1, header.entry-header span")?.text()?.trim()
                ?: return false
        val videoUrl = "https://koreanporn.stream/${title}.mp4"

        document.select("iframe").mapNotNull { it.attr("abs:src") }.forEach {
            loadExtractor(it, data, subtitleCallback, callback)
        }

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = videoUrl,
                type = ExtractorLinkType.VIDEO
            ) {
                this.referer = data
            }
        )

        return true
    }
}