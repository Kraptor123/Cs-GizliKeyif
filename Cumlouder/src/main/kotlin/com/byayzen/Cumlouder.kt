// ! Bu araç @ByAyzen tarafından | @cs-kraptor için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Cumlouder : MainAPI() {
    override var mainUrl = "https://www.cumlouder.com"
    override var name = "Cumlouder"
    override val hasMainPage = true
    override var lang = "en"
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.NSFW)
    //NSFW,

    override val mainPage = mainPageOf(
        "${mainUrl}/series/newest/" to "Newest",
        "${mainUrl}/porn-videos/latina/"        to "Latina",
        "${mainUrl}/porn-videos/cowgirl/"       to "Cowgirl",
        "${mainUrl}/porn-videos/anal/"          to "Anal",
        "${mainUrl}/porn-videos/milf/"          to "MILF",
        "${mainUrl}/porn-videos/tattoo/"        to "Tattoo",
        "${mainUrl}/porn-videos/doggystyle/"    to "Doggystyle",
        "${mainUrl}/porn-videos/threesome/"     to "Threesome",
        "${mainUrl}/porn-videos/orgy/"          to "Orgy",
        "${mainUrl}/porn-videos/redhead/"       to "Redhead",
        "${mainUrl}/porn-videos/lesbian/"       to "Lesbian",
        "${mainUrl}/porn-videos/russian/"       to "Russian",
        "${mainUrl}/porn-videos/blowjob/"       to "Blowjob",
        "${mainUrl}/porn-videos/amateur/"       to "Amateur",
        "${mainUrl}/porn-videos/facial/"        to "Facial",
        "${mainUrl}/porn-videos/ass/"           to "Ass",
        "${mainUrl}/porn-videos/busty/"         to "Busty",
        "${mainUrl}/porn-videos/hardcore/"      to "Hardcore",
        "${mainUrl}/porn-videos/party/"         to "Party",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data.trimEnd('/') + "/$page/"
        val document = app.get(url).document
        val home = document.select("div.medida a.muestra-escena").filter {
            !it.parents().any { parent -> parent.hasClass("related-sites") }
        }.mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }
    private fun Element.toMainPageResult(): SearchResponse? {
        val title = runCatching {
            this.selectFirst("h2")!!.text() + " " + (this.selectFirst("span.minutos")?.ownText()
                ?.replace("m", "")?.trim() ?: "")
        }.getOrNull() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1) {
            app.get("${mainUrl}/search/?q=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=${query}").document
        }

        val aramaCevap =
            document.select("div.medida a.muestra-escena").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div video")?.attr("poster"))
        val description = document.selectFirst("div.sub-video p")?.text()
            ?.replace(Regex("^\\s*Description\\s*[:\\-–—]?\\s*", RegexOption.IGNORE_CASE), "")
            ?.trim()
        val year = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("ul.tags a.tag-link").map { it.text().replaceFirstChar { char -> char.uppercase() } }
        val duration =
            document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.medida a.muestra-escena")
            .mapNotNull { it.toRecommendationResult() }
        val actors = document.select("a.pornstar-link").map { it.text().trim() }
        Log.d("ByAyzen_${this.name}", "Actors list: $actors")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
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
        val title = runCatching {
            (this.selectFirst("img")?.attr("alt") ?: return null) + " " + (this.selectFirst("span.minutos")?.ownText()
                ?.replace("m", "")?.trim() ?: "")
        }.getOrNull() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val videoUrl = document.selectFirst("video source")?.attr("src")?.toString() ?: return false


        callback(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = videoUrl,
                type = INFER_TYPE
            )
        )

        return true
    }
}