// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer


class CamCaps : MainAPI() {
    override var mainUrl = "https://camcaps.tv"
    override var name = "CamCaps"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/videos?o=mv" to "Most Viewed",
        "$mainUrl/search/videos/onlyfans" to "OnlyFans",
        "$mainUrl/search/videos/manyvids" to "ManyVids",
        "$mainUrl/search/videos/fansly" to "Fansly",
        "$mainUrl/search/videos/loyalfans" to "LoyalFans",
        "$mainUrl/search/videos/youtube" to "YouTube",
        "$mainUrl/search/videos/pornhub" to "PornHub",
        "$mainUrl/search/videos/bongacams" to "Bonga",
        "$mainUrl/search/videos/chaturbate" to "Chaturbate",
        "$mainUrl/search/videos/clips4sale" to "Clips4Sale",
        "$mainUrl/search/videos/mfc" to "MFC",
        "$mainUrl/search/videos/stripchat" to "StripChat",
        "$mainUrl/search/videos/snapchat" to "Snapchat",
    )


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data}?page=$page"
        }

        val document = app.get(url).document
        val home = document.select("div.thumbs > article.thumb").mapNotNull { it.toMainPageResult() }

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
        val image = this.selectFirst("figure img, img") ?: return null
        val title = this.selectFirst("h3")?.text() ?: image.attr("title")
        val href = fixUrlNull(this.selectFirst("a[href*=/video/]")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(image.attr("data-src").ifEmpty { image.attr("src") })

        return newMovieSearchResponse(
            title,
            href,
            TvType.NSFW
        ) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/videos/$query?page=$page").document

        val aramaCevap = document.select("article.thumb").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")
        val description = document.selectFirst("article.about p")?.text()?.trim()
        val year = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.group a").map { it.text() }
        val scoreText = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("article.thumb").mapNotNull { it.toMainPageResult() }
        val actors = document.select("span.valor a").map { Actor(it.text()) }
        val trailer = Regex("""embed/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

       return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(scoreText)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        val iframe = document.selectFirst("div.video-embedded iframe")?.attr("src") ?: return false

        val redirectedUrl = app.get(iframe).url

        loadExtractor(redirectedUrl, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}