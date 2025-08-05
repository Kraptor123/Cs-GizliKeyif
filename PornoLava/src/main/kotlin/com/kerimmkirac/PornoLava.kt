// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class PornoLava : MainAPI() {
    override var mainUrl              = "https://pornolily.click"
    override var name                 = "PornoLava"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/new"      to "En Yeniler",
        "${mainUrl}/cat/3"   to "Pornolar",
        "${mainUrl}/top"   to "En Popüler",
        "${mainUrl}/liked" to "En Beğenilenler",
        "${mainUrl}"  to "Rastgele"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val url = if (request.data == mainUrl) request.data else "${request.data}/$page"
    val document = app.get(url).document
    val home = document.select("div.card").mapNotNull { it.toMainPageResult() }
    return newHomePageResponse(request.name, home)
}


private fun Element.toMainPageResult(): SearchResponse? {
    val aTag = this.selectFirst("a.thumb-container") ?: return null
    val href = fixUrlNull(aTag.attr("href")) ?: return null

    val img = aTag.selectFirst("img") ?: return null
    val posterUrl = fixUrlNull(img.attr("src"))
    
    val titleTag = this.selectFirst("h6.card-title a") ?: return null
    val title = titleTag.text()

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
}


    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.result-item article").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.title a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.title a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document

    val title = document.selectFirst("h4")?.text()?.trim() ?: return null
    val poster = fixUrlNull(document.selectFirst("video")?.attr("poster")) 
    val description = document.selectFirst("h4")?.text()?.trim()
    val recommendations = document.select("div.card").mapNotNull { it.toRecommendationResult() }
   

    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = poster
        this.recommendations = recommendations
        
        
        
    }
}


    private fun Element.toRecommendationResult(): SearchResponse? {
        val aTag = this.selectFirst("a.thumb-container") ?: return null
    val href = fixUrlNull(aTag.attr("href")) ?: return null

    val img = aTag.selectFirst("img") ?: return null
    val posterUrl = fixUrlNull(img.attr("src"))
    
    val titleTag = this.selectFirst("h6.card-title a") ?: return null
    val title = titleTag.text()

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
    val source = document.selectFirst("video > source")?.attr("src") ?: return false
    val videoUrl = fixUrl("$mainUrl$source")

    callback(
        newExtractorLink(
            name = "Pornolava",
            source = "Pornolava",
            url = videoUrl,


            type = if (videoUrl.contains(".mp4")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
        ){
            this.referer = mainUrl
            this.quality = Qualities.P1080.value
        }
    )

    return true
}
}