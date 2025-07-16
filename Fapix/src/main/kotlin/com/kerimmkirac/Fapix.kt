// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Fapix : MainAPI() {
    override var mainUrl              = "https://fapix.porn"
    override var name                 = "Fapix"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/latest"      to "All Porn Videos",
        "${mainUrl}/bro-sis"   to "Step Sister Porn Videos",
        "${mainUrl}/mother-and-son"  to "StepMom Porn Videos",
        
        
        
        
        
        "${mainUrl}/rus-porn-2025"  to "Russian Porn Videos",
        
        
        
        
       
        
        


    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = app.get("${request.data}?page=$page").document
    val home = document.select("div.video.trailer").mapNotNull { it.toMainPageResult() }

    return newHomePageResponse(request.name, home)
}

private fun Element.toMainPageResult(): SearchResponse? {
    val anchor = this.selectFirst("a") ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null
    val title = anchor.selectFirst("div.title")?.attr("title")?.trim()
        ?: anchor.attr("alt")?.trim()
        ?: return null
    val posterUrl = fixUrlNull(anchor.selectFirst("img")?.attr("src"))
    

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
        
        
    }
}


    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/search?q=${query}").document

        return document.select("div.video.trailer").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("a") ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null
    val title = anchor.selectFirst("div.title")?.attr("title")?.trim()
        ?: anchor.attr("alt")?.trim()
        ?: return null
    val posterUrl = fixUrlNull(anchor.selectFirst("img")?.attr("src"))
    

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
        
        
    }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document

    val title = document.selectFirst("h1[itemprop=name]")?.ownText()?.trim() ?: return null


    val poster = fixUrlNull(
        document.selectFirst("video#player")?.attr("poster")
            ?: document.selectFirst("video#player")?.attr("data-poster")
    )

    val tags = document.select("span.info-row a.button").map { it.text().trim() }.take(5)

    

    val actors = document.select("a.tag-modifier[itemprop=actor]").map {
    val name = it.selectFirst("span[itemprop=name]")?.text()?.trim() ?: return@map null
    val image = fixUrlNull(it.selectFirst("img")?.attr("src"))
    Actor(name, image)
}.filterNotNull()


    val recommendations = document.select("div.video.trailer").mapNotNull { it.toRecommendationResult() }

    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = poster
        this.tags = tags
        
        this.recommendations = recommendations
        addActors(actors)
    }
}


    private fun Element.toRecommendationResult(): SearchResponse? {
    val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    val title = this.selectFirst("div.title")?.attr("title")?.trim() ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

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
    val sourceUrl = document.selectFirst("video#player source")?.attr("src") ?: return false

   
    val finalUrl = app.get(
        sourceUrl,
        headers = mapOf("Referer" to mainUrl),
        allowRedirects = false
    ).headers["Location"] ?: sourceUrl 

    callback.invoke(
        newExtractorLink(
            name = "Fapix",
            source = "Fapix",
            url = finalUrl,
            
            type = if (finalUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
        ){
            this.referer = mainUrl
            this.quality = Qualities.Unknown.value
        }
    )

    return true
}
}