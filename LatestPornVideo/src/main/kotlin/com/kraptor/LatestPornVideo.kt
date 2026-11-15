// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class LatestPornVideo : MainAPI() {
    override var mainUrl              = "https://latestpornvideo.com"
    override var name                 = "LatestPornVideo"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/"                     to "Latest",
        "${mainUrl}/category/mixed-porn/" to "Mixed Porn",
        "${mainUrl}/category/porn-movie/" to "Porn Movie",
        "${mainUrl}/category/jav/"        to "JAV",
        "${mainUrl}/category/webcam/"     to "Webcam",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url      = if (page == 1) request.data else "${request.data}page/$page/"
        val home     = app.get(url).document.select("article.loop-video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, isHorizontalImages = true), hasNext = home.isNotEmpty())
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("header span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src")?.ifBlank { null } ?: this.attr("data-main-thumb"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url        = if (page == 1) "${mainUrl}/?s=${query}" else "${mainUrl}/page/$page/?s=${query}"
        val aramaCevap = app.get(url).document.select("article.loop-video").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = aramaCevap.isNotEmpty())
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()?.ifBlank { null }
        val tags            = document.select("div.tags-list a").map { it.text() }
        val recommendations = document.select("article.loop-video").mapNotNull { it.toMainPageResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val iframes = app.get(data).document.select("div.responsive-player iframe, div.video-player iframe")
            .mapNotNull { fixUrlNull(it.attr("src")) }.distinct()

        iframes.forEach { loadExtractor(it, "${mainUrl}/", subtitleCallback, callback) }
        return iframes.isNotEmpty()
    }
}
