// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class HDStreaming : MainAPI() {
    override var mainUrl              = "https://hdstream.ing"
    override var name                 = "HDStreaming"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/sort/views/" to "Popular",
        "${mainUrl}/participants/couples/" to "Couples",
        "${mainUrl}/participants/group/" to "Group",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?page=$page").document
        val home     = document.select("div.w-full div.overflow-hidden").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a.text-yellow-500")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a.text-yellow-500")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/$query/?page=$page").document

        val aramaCevap = document.select("div.w-full div.overflow-hidden").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          =
            fixUrlNull(document.selectFirst("video#player")?.attr("poster"))
        val description     = document.selectFirst("meta[name=description]")?.attr("content")?.trim()
        val year            = title.substringBefore("-").substringAfterLast(" ").trim() .toIntOrNull()
        val tags            = document.select("div.flex:contains(tags) a").map { it.text() }
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.xl\\:grid-cols-6 div.overflow-hidden").mapNotNull { it.toMainPageResult() }
        val actors          = listOf(Actor(title.substringBefore(" ")))

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val source = document.selectFirst("source")?.attr("src") ?: ""

        callback.invoke(newExtractorLink(
            this.name,
            this.name,
            source,
            type = ExtractorLinkType.VIDEO,
            {
                this.referer = "${mainUrl}/"
            }
        ))
        return true
    }
}