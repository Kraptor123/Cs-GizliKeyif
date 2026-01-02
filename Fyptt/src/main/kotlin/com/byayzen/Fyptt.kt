// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Fyptt : MainAPI() {
    override var mainUrl = "https://fyptt.to"
    override var name = "Fyptt"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/tiktok-nudes/"   to "Nudes",
        "${mainUrl}/tiktok-porn/"    to "TikTok",
        "${mainUrl}/tiktok-boobs/"   to "Boobs",
        "${mainUrl}/instagram-porn/" to "Instagram",
        "${mainUrl}/tiktok-sex/"     to "Sex",
        "${mainUrl}/nsfw-tiktok/"    to "NSFW",
        "${mainUrl}/tiktok-xxx/"     to "XXX",
        "${mainUrl}/tiktok-ass/"     to "Ass",
        "${mainUrl}/tiktok-pussy/"   to "Pussy",
        "${mainUrl}/tiktok-live/"    to "Live",
        "${mainUrl}/sexy-tiktok/"    to "Sexy",
        "${mainUrl}/tiktok-thots/"   to "Thots"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val base = request.data.trimEnd('/')
        val url = if (page > 1) "$base/page/$page/" else base

        val document = app.get(url).document
        val home = document.select("div.fl-post-column").mapNotNull { it.toMainPageResult() }

        val hasNext = document.selectFirst(".next.page-numbers, a.next, .pagination .next") != null
        return newHomePageResponse(request.name, home, hasNext = hasNext)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = this.selectFirst(".fl-post-grid-image a") ?: return null
        val title = anchor.attr("title").ifBlank { anchor.text() }.ifBlank { return null }
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val q = query.trim()
        val url = if (page > 1) "${mainUrl}/page/$page/?s=$q" else "${mainUrl}/?s=$q"
        val document = app.get(url).document

        val results = document.select("div.fl-post-column").mapNotNull { it.toMainPageResult() }
        val hasNext = document.selectFirst(".next.page-numbers, a.next, .pagination .next") != null

        return newSearchResponseList(results, hasNext = hasNext)
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description =
            document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags = document.select("div.fl-html .entry-category a")
            .map { it.text().trim() }
        val recommendations =
            document.select("div.fl-post-column").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a img")?.attr("alt") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val headers = mapOf("Referer" to "$mainUrl/", "User-Agent" to "Mozilla/5.0")
        val doc = app.get(data, headers = headers).document

        var url = doc.selectFirst("video source[type='video/mp4']")?.attr("src")
            ?: doc.selectFirst("video")?.attr("src")
            ?: doc.selectFirst("iframe[src*='fypttstr']")?.attr("src")?.let {
                it.substringAfter("fileid=").substringBefore("&").takeIf { id -> id.isNotBlank() }?.let { id ->
                    "https://stream.fyptt.to/$id.mp4"
                }
            }

        if (url.isNullOrBlank()) return false

        newExtractorLink(
            source = "AZNude",
            name = "AZNude",
            url = url,
        ) {
            this.quality = Qualities.Unknown.value
            this.referer = "$mainUrl/"
            this.headers = headers
        }?.let { callback(it) } ?: return false

        return true
    }
}