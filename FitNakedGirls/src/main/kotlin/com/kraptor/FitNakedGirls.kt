// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class FitNakedGirls : MainAPI() {
    override var mainUrl = "https://fitnakedgirls.com"
    override var name = "FitNakedGirls"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/category/nude-instagram-girls/" to "Nude Instagram Girls",
        "${mainUrl}/videos/category/nude-models/" to "Nude Models",
        "${mainUrl}/videos/category/naked-yoga/" to "Naked yoga",
        "${mainUrl}/videos/category/nude-fitness-models/" to "Nude fitness models",
        "${mainUrl}/videos/category/naked-girls/" to "Amateur Fit Girls",
        "${mainUrl}/videos/category/nude-influencers/" to "Nude Influencers",
        "${mainUrl}/videos/category/gym-sex/" to "Gym sex",
        "${mainUrl}/videos/category/naked-workout/" to "Naked workout",
        "${mainUrl}/videos/category/nude-internet-girls/" to "Nude Internet Girls",
        "${mainUrl}/videos/category/camgirls/" to "Fit Camgirls",
        "${mainUrl}/videos/category/muscle-porn/" to "Muscle porn",
        "${mainUrl}/videos/category/fitness-lesbians-porn/" to "Fitness lesbians porn",
        "${mainUrl}/videos/category/nude-ballet/" to "Nude ballet",
        "${mainUrl}/videos/category/wwe-porn/" to "WWE Porn",
        "${mainUrl}/videos/category/massage-porn/" to "Massage porn",
        "${mainUrl}/videos/category/nude-sports/" to "Nude sports",
        "${mainUrl}/videos/category/gymnast-porn/" to "Gymnast porn",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get(request.data).document
        } else {
            app.get("${request.data}page/2$page/").document
        }
        val home = document.select("li.g1-collection-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/videos/?s=${query}").document

        val aramaCevap =
            document.select("li.g1-collection-item").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = false)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document        = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.sgeneros a").map { it.text() }
        val score           = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("li.g1-collection-item").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(score)
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

        val video = document.selectFirst("figure.wp-block-video video")?.attr("src")
            ?: document.selectFirst("div.wp-video video")?.text()
            ?: document.selectFirst("a[href*=.mp4]")?.attr("href") ?: ""

        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                video,
                ExtractorLinkType.VIDEO
            ) {
                this.referer = "${mainUrl}/"
            })

        return true
    }
}