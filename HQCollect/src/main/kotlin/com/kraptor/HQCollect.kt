// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class HQCollect : MainAPI() {
    override var mainUrl              = "https://hqcollect.com"
    override var name                 = "HQCollect"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    private val posterCache = mutableMapOf<String, String>()

    override val mainPage = mainPageOf(
        "${mainUrl}/categories/facial-abuse/"    to      "Facial Abuse",
        "${mainUrl}/categories/ghetto-gaggers/"  to      "Ghetto Gaggers",
        "${mainUrl}/categories/latina-abuse/"    to      "Latina Abuse",
        "${mainUrl}/categories/black-payback/"   to      "Black Payback"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}page/$page/").document
        val home     = document.select("div.col-xs-6").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3 a")?.text() ?: this.selectFirst("img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        posterUrl?.let { posterCache[href] = it }

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.col-xs-6").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("h3 a")?.text() ?: this.selectFirst("img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = posterCache[url]
        val description     = document.selectFirst("div.post-entry p")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("span.meta a").map { it.text() }
        val rating          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()?.toRatingInt()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.col-xs-6").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(rating)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("h3 a")?.text() ?: this.selectFirst("img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val iframe   = document.selectFirst("div.player.player-small iframe")?.attr("src")

        val videoSrc = document.selectFirst("video.wp-video-shortcode a")?.attr("href").toString()
        Log.d("kraptor_$name", "iframe = ${iframe} videoSrc = $videoSrc")

        val video =  if (iframe.isNullOrBlank()){
            videoSrc
        }else{
            iframe
        }

        if (video.contains("hqcollect.com")) {
            callback.invoke(newExtractorLink(
                source = this.name,
                name   = this.name,
                url    = video,
                type   = ExtractorLinkType.VIDEO,
                {
                    this.referer = "${mainUrl}/"
                    this.quality = Qualities.Unknown.value
                }
            ))
        }else {
            loadExtractor(video, "${mainUrl}/", subtitleCallback, callback)
        }
        return true
    }
}