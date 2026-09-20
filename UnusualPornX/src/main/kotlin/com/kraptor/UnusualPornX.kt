// ! This Extension Made By @Kraptor123 for GizliKeyif

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class UnusualPornX : MainAPI() {
    override var mainUrl              = "https://unusualpornx.com"
    override var name                 = "UnusualPornX"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/latest-updates" to "Latest Updates",
        "${mainUrl}/categories/demons" to "Demons",
        "${mainUrl}/categories/time-stop" to "Time Stop",
        "${mainUrl}/categories/mind-control" to "Mind Control",
        "${mainUrl}/categories/vampires" to "Vampires",
        "${mainUrl}/categories/monsters" to "Monsters",
        "${mainUrl}/categories/sci-fi-experiments" to "Sci-Fi Experiments",
        "${mainUrl}/categories/ghosts" to "Monsters",
    )


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page/").document
        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(list = HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-webp"))
        val trailer   = this.selectFirst("img")?.attr("data-preview")?.replace("https://","")

        return newMovieSearchResponse(title, href + "|" + trailer, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}").document
        val searchAnswer = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(searchAnswer, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        Log.d(tag, "Load : $url")
        val split    = url.split("|")
        val trailer  = "https://${split[1]}"
        val document = app.get(split[0]).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.info-content a[href*=tags]").map { it.text() }
        val scoreText       = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("div.item span em:contains(:)")
            ?.text()
            ?.split(":")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.let { parts ->
                val size = parts.size
                val seconds = if (size >= 1) parts[size - 1] else 0
                val minutes = if (size >= 2) parts[size - 2] else 0
                val hours   = if (size >= 3) parts[size - 3] else 0

                hours * 60 + minutes + if (seconds >= 30) 1 else 0
            }
        val recommendations = document.select("div.item").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("div.info-content a.link[href*=models] span.name").map { Actor(it.text()) }

        return newMovieLoadResponse(title, split[0], TvType.NSFW, split[0]) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(scoreText)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer, "${mainUrl}/", true)
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val pageHtml = app.get(data).text
        return KtPlayerExtractor.getLinks(name, mainUrl, data, pageHtml, callback = callback)
    }
}
