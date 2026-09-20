// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class VideoCelebs : MainAPI() {
    override var mainUrl = "https://videocelebs.net"
    override var name = "VideoCelebs"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)


    override val mainPage = mainPageOf(
        "${mainUrl}/tag/nude" to "Nude",
        "${mainUrl}/tag/topless" to "Topless",
        "${mainUrl}/tag/sex" to "Sex",
        "${mainUrl}/tag/butt" to "Butt",
        "${mainUrl}/tag/sexy" to "Sexy",
        "${mainUrl}/tag/full-frontal" to "Full Frontal",
        "${mainUrl}/tag/underwear" to "Underwear",
        "${mainUrl}/tag/bush" to "Bush",
        "${mainUrl}/tag/cleavage" to "Cleavage",
        "${mainUrl}/tag/bikini" to "Bikini",
        "${mainUrl}/tag/side-boob" to "Side boob",
        "${mainUrl}/tag/lesbian" to "Lesbian",
        "${mainUrl}/tag/see-thru" to "See Thru",
        "${mainUrl}/tag/thong" to "Thong",
        "${mainUrl}/tag/explicit" to "Explicit",
        "${mainUrl}/tag/nipslip" to "Nipslip",
        "${mainUrl}/tag/striptease" to "Striptease",
        "${mainUrl}/tag/implied-nudity" to "Implied Nudity",
        "${mainUrl}/tag/nude-debut" to "Nude Debut",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home = document.select("div.item.big").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val rating = this.selectFirst("div.rating.positive")?.text()?.replace("%", "")?.trim()

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(rating)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}/page/$page").document

        val aramaCevap = document.select("div.item.big").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("a.item img")?.attr("src"))
        val description = document.selectFirst("div.singl > div:nth-child(4)")?.text()?.trim()
        val year = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.entry-utility strong:contains(Tags) ~ a").map { it.text() }
        val score =
            document.selectFirst("div.rating span.voters")?.text()?.trim()?.replace("%", "")?.substringBefore(" ")
                ?.trim()
        val duration = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.pupular_video ul.wpp-list li").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.entry-utility strong:contains(Actress) + a").map { Actor(it.text()) }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from100(score)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a img")?.attr("alt") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
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
