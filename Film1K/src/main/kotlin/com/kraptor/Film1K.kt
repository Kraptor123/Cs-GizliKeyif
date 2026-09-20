// ! This Extension Made By @kraptor for GizliKeyif

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Film1K : MainAPI() {
    override var mainUrl              = "https://www.film1k.com"
    override var name                 = "Film1K"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    //cloudflare v2
    override var sequentialMainPage = true
    override var sequentialMainPageDelay       = 50L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 50L  // ? 0.05 saniye

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest",
        "${mainUrl}/tag/eng-sub" to "English Subs",
        "${mainUrl}/tag/english" to "English",
        "${mainUrl}/tag/french" to "French",
        "${mainUrl}/tag/german" to "German",
        "${mainUrl}/tag/spanish" to "Spanish",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1){
            app.get("${request.data}").document
        } else {
            app.get("${request.data}/page/$page").document
        }
        val home     = document.select("article.post").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(list = HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        var poster    = this.selectFirst("img")?.attr("data-src")
        if (poster.isNullOrEmpty()){
            poster = this.selectFirst("img")?.attr("src")
        }
        val posterUrl = fixUrlNull(poster)

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page?s=${query}").document
        }
        val searchAnswer = document.select("article.post").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(searchAnswer, hasNext = true)
    }

    

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    
    override suspend fun load(url: String): LoadResponse? {
        Log.d(name, "Load aşaması: $url")
        val document = app.get(url).document
        
        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = title + document.selectFirst("div.MoreInfo div.Description")
            ?.ownText()
            ?.substringBefore("Stream this")
            ?.trim()
        val tags            = document.select("div.MoreInfo p.EzLinks a").map { it.text() }
        val year            = title.substringAfter(" (").substringBefore(")")
        Log.d(name, tags.toString())
        val scoreText       = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("div.MoreInfo div.Description strong:contains(Runtime)")
                ?.nextSibling()
                .toString().substringBeforeLast(" mins").substringAfterLast(" ").trim()
                .toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors          = document.selectFirst("div.MoreInfo div.Description strong:contains(Actors)")
            ?.nextSibling()
            .toString()
            .replace(":", "").split(", ")
            .map { Actor(it) }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title.substringBefore("("), url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year.toIntOrNull()
            this.tags            = tags
            this.score           = Score.from10(scoreText)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document
        val source = document.selectFirst("source")?.attr("src").toString().replace(".mp4","")

        Log.d(tag, source)

        loadExtractor(source, "$mainUrl/", subtitleCallback, callback)

        return true
    }
}