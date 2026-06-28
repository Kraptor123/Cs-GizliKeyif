// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer


class CamCaps : MainAPI() {
    override var mainUrl = "https://camcaps.io"
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
        val document = if (page == 1) {
            app.get(request.data).document
        } else {
            app.get("${request.data}&page=$page").document
        }
        val home = document.select("article.thumb").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.text() ?: return null
        val rating = this.selectFirst("span.like-icon")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(
            title,
            "$href-=-${posterUrl?.substringAfter("//")}",
            TvType.NSFW
        ) {
            this.posterUrl = posterUrl
            this.score = Score.from100(rating.replace("%", ""))
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/videos/$query?page=$page").document

        val aramaCevap = document.select("article.thumb").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val split = url.split("-=-")
        val document = app.get(split[0]).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = "https://${split[1]}"
        val description = document.selectFirst("article.about p")?.text()?.trim()
        val year = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.group a").map { it.text() }
        val scoreText = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("article.thumb").mapNotNull { it.toMainPageResult() }
        val actors = document.select("span.valor a").map { Actor(it.text()) }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

       return newMovieLoadResponse(title, url, TvType.Movie, url) {
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

        val iframe = document.selectFirst("iframe")?.attr("src").toString()

        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}