// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class XXXChina : MainAPI() {
    override var mainUrl              = "https://3xchina.net"
    override var name                 = "3XChina"
    override val hasMainPage          = true
    override var lang                 = "zh"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/category/chinese-av/?filter=latest" to "Latest",
        "${mainUrl}/category/chinese-av/?filter=most-viewed" to "Most Viewed",
        "${mainUrl}/category/chinese-av/?filter=popular" to "Popular",
        "${mainUrl}/category/chinese-av/?filter=random" to "Random"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val baseUrl = request.data.trimEnd('/')
        val url = if (page == 1) {
            baseUrl
        } else {
            val base = baseUrl.substringBefore("?")
            val filter = baseUrl.substringAfter("?")
            Log.d("Ayzen", base)
            Log.d("Ayzen", filter)
            "$base/page/$page/?$filter"
        }
        Log.d("Ayzen", url)
        val document = app.get(url).document
        val home = document.select("article.loop-video").mapNotNull { it.toMainPageResult() }
        Log.d("Ayzen", "${home.size}")
        if (home.isNotEmpty()) {
            Log.d("Ayzen", home.first().name)
            Log.d("Ayzen", home.last().name)
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
        val title = this.selectFirst("a[data-title]")?.attr("data-title")
            ?: this.selectFirst(".entry-header span")?.text()
            ?: return null

        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null

        val img = this.selectFirst(".post-thumbnail-container img")
        val posterUrl = fixUrlNull(
            img?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("src")?.takeIf { it.isNotBlank() }
        )

        return newMovieSearchResponse(title, fixUrl(href), TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page == 1) {
            "$mainUrl/?s=$query"
        } else {
            "$mainUrl/page/$page/?s=$query"
        }
        val document = app.get(url).document

        val aramaCevap = document.select("article.loop-video").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = aramaCevap.isNotEmpty())
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags            = document.select("div.tags .tags-list a.label[title]").map { it.text() }
        val score           = document.selectFirst("div.rating-result div.percentage")?.text()?.trim()?.replace("%", "")
        val actors          = document.select("#video-actors a").map { Actor(it.text()) }
        val recommendations = document.select("div.under-video-block article.loop-video").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.score           = Score.from10(score)
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a[data-title]")?.attr("data-title")
            ?: this.selectFirst(".entry-header span")?.text()
            ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst(".post-thumbnail-container img")?.attr("data-src")
                ?: this.selectFirst(".post-thumbnail-container img")?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("Ayzen", "data = $data")
        val document = app.get(data).document
        val iframes = document.select("iframe, IFRAME").mapNotNull { it.attr("src").takeIf { s -> s.isNotBlank() } }
        Log.d("Ayzen", "tüm iframeler = $iframes")

        val downloadUrl = document.selectFirst("a#tracking-url")?.attr("href")
        Log.d("Ayzen", "download url = $downloadUrl")

        val allLinks = iframes + listOfNotNull(downloadUrl)
        Log.d("Ayzen", "toplam link sayısı = ${allLinks.size}")

        allLinks.forEach { link ->
            Log.d("Ayzen", "işleniyor = $link")
            loadExtractor(link, mainUrl, subtitleCallback, callback)
        }
        return allLinks.isNotEmpty()
    }
}