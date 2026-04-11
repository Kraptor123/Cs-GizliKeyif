// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Heavy : MainAPI() {
    override var mainUrl = "https://heavy-r.com"
    override var name = "Heavy-R"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/free_porn/amateur.html" to "Amateur",
        "${mainUrl}/free_porn/anal.html" to "Anal",
        "${mainUrl}/free_porn/asian.html" to "Asian",
        "${mainUrl}/free_porn/ass.html" to "Ass",
        "${mainUrl}/free_porn/bdsm.html" to "BDSM",
        "${mainUrl}/free_porn/big-dick.html" to "Big Dick",
        "${mainUrl}/free_porn/big-tits.html" to "Big Tits",
        "${mainUrl}/free_porn/creampie.html" to "Creampie",
        //"${mainUrl}/free_porn/dp.html" to "DP",
        //"${mainUrl}/free_porn/fisting.html" to "Fisting",
        //"${mainUrl}/free_porn/hardcore.html" to "Hardcore",
        //"${mainUrl}/free_porn/lesbian.html" to "Lesbian",
        //"${mainUrl}/free_porn/milf.html" to "MILF"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            request.data.replace(".html", "_$page.html")
        }
        val document = app.get(url).document
        val home = document.select("div.video-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h4.title a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("h4.title a")?.attr("href")) ?: return null
        val posterurl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterurl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = "${mainUrl}/search/${query}_$page.html"
        val document = app.get(url).document

        val results = document.select("div.video-item").mapNotNull {
            it.toMainPageResult()
        }

        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.video-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=\"og:image\"]")?.attr("content"))
        val description = document.selectFirst("div.video-data p")?.text()?.trim()
        val tags = document.select("div.tags a[title]").map { it.text() }

        val recommendations = document.select("div.recent-uploads a.item").mapNotNull {
            it.toRecommendationResult()
        }.distinctBy { it.url }.filter { it.url != url }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("span.title")?.text() ?: return null
        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterurl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterurl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("video#video-file source").forEach { source ->
            val videourl = source.attr("src")
            if (videourl.isNotBlank()) {
                callback(
                    newExtractorLink(
                        this.name,
                        this.name,
                        videourl,
                        type = ExtractorLinkType.VIDEO
                    ) {
                        this.referer = "${mainUrl}/"
                    }
                )
            }
        }

        return true
    }
}