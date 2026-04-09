// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Sexfilm : MainAPI() {
    override var mainUrl              = "https://en.sex-film.biz"
    override var name                 = "Sexfilm"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/movies/" to "Movies",
        "${mainUrl}/porno-video/" to "Videos",
        "${mainUrl}/hd-porno-movies/" to "HD Porn Movies",
        "${mainUrl}/fullhd-porn-movie/" to "FullHD Porn Movies",
        "${mainUrl}/porno-parodies/" to "Parodies",
        "${mainUrl}/vintagexxx/" to "Porn Classic",
        "${mainUrl}/watch/year/2026/" to "2026 Movies"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document
        val home = document.select("div.short").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a.th-title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val searchStart = (page - 1) * 24
        val resultFrom = (page - 1) * 24 + 1

        val document = app.post(
            "${mainUrl}/index.php?do=search",
            data = mapOf(
                "do" to "search",
                "subaction" to "search",
                "search_start" to "$searchStart",
                "full_search" to "0",
                "result_from" to "$resultFrom",
                "story" to query
            )
        ).document

        val results = document.select("div.short").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(results)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a.th-title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1#s-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.fleft img")?.attr("data-src"))
        val description = document.selectFirst("div#s-desc")?.text()?.substringBefore("Watch porn movie")?.trim()

        val year = document.selectFirst("span.gv a[href*='/year/']")?.text()?.toIntOrNull()
        val tags = document.select("ul.flist-col li:contains(Genre) a").map { it.text() }

        val duration = document.selectFirst("ul.flist-col li:contains(Duration)")?.text()?.let {
            val time = it.replace("Duration:", "").trim()
            val parts = time.split(":")
            if (parts.size >= 2) {
                val hours = if (parts.size == 3) parts[0].toIntOrNull() ?: 0 else 0
                val minutes = if (parts.size == 3) parts[1].toIntOrNull() ?: 0 else parts[0].toIntOrNull() ?: 0
                (hours * 60) + minutes
            } else null
        }

        val actors = document.select("ul.flist-col li:contains(Casting) a").map { Actor(it.text()) }
        val recommendations = document.select("div.sect-c div.short").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a.th-title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val scripts = document.select("script").html()

        Regex("""s2\.src\s*=\s*"(https?://.*?)"""").findAll(scripts).forEach { match ->
            val iframeUrl = match.groupValues[1]
            loadExtractor(iframeUrl, data, subtitleCallback, callback)
        }
        return true
    }
}