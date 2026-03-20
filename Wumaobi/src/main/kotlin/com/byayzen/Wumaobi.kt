// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Wumaobi : MainAPI() {
    override var mainUrl              = "https://wumaobi.com"
    override var name                 = "Wumaobi"
    override val hasMainPage          = true
    override var lang                 = "zh"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "hits" to "Popular",
        "id" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "${mainUrl}/recommend" else "${mainUrl}/recommend/page-$page.html"
        val document = app.get(url, cookies = mapOf("sort" to request.data)).document
        val home = document.select(".card").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.card-title, .card-title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) "${mainUrl}/search/$query/" else "${mainUrl}/search/$query/page-$page.html"

        val response = app.get(
            url,
            cookies = mapOf("sort" to "id"),
            headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "Upgrade-Insecure-Requests" to "1"
            )
        )

        val results = response.document.select(".card").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst(".video-title a")?.text()
            ?: document.selectFirst("h1")?.text()
            ?: return null
        val poster = fixUrlNull(document.selectFirst("video#playerCnt")?.attr("poster")
            ?: document.selectFirst("meta[property=og:image]")?.attr("content"))
        val tags = document.select(".post-meta a, .video-tag, .card-text")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        val videoSource = document.selectFirst("video#playerCnt source.video-source")?.attr("src")
            ?: document.selectFirst("video source")?.attr("src")

        if (videoSource != null) {
            callback.invoke(
                newExtractorLink(
                    source  = name,
                    name    = name,
                    url     = videoSource,
                    type    = ExtractorLinkType.VIDEO
                ) {
                    this.quality = Qualities.Unknown.value
                    this.referer = data
                    this.headers = mapOf(
                        "Origin" to mainUrl,
                        "Referer" to "$mainUrl/"
                    )
                }
            )
            return true
        }
        return false
    }
}