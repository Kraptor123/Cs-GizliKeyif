// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class XNalgas : MainAPI() {
    override var mainUrl              = "https://www.xnalgas.com"
    override var name                 = "XNalgas"
    override val hasMainPage          = true
    override var lang                 = "es"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/category/videos"      to "All Porn Videos",
        "${mainUrl}/recommended"   to "Recommended Porn Videos",
        "${mainUrl}/most-viewed" to "Most Viewed Porn Videos",
        "${mainUrl}/most-liked"  to "Most Liked Porn Videos",
        "${mainUrl}/category/latinas"  to "Latina Porn Videos",
        
        "${mainUrl}/tag/brasilenas"  to "Brazilian Porn Videos",
        "${mainUrl}/tag/onlyfans"  to "OnlyFans Porn Videos",
        "${mainUrl}/tag/colombianas"  to "Colombian Porn Videos",
        "${mainUrl}/tag/funny-porn"  to "Funny Porn Videos",
        "${mainUrl}/tag/dominicanas"  to "Dominican Porn Videos",
        "${mainUrl}/tag/street-porn"  to "Street Porn Videos",
        "${mainUrl}/tag/espanolas"  to "Spanish Porn Videos",
        "${mainUrl}/tag/mexicanas"  to "Mexican Porn Videos",
        "${mainUrl}/tag/venezolanas"  to "Venezuelan Porn Videos",
    )
    private val posterCache = mutableMapOf<String, String>()
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page/").document
        val home     = document.select("article").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
    val aTag = this.selectFirst("h2.post-title > a") ?: return null
    val title = aTag.text()
    val href = fixUrlNull(aTag.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
    posterUrl?.let { posterCache[href] = it }

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
}


    override suspend fun search(query: String, page: Int): SearchResponseList {
    val url = ("${mainUrl}/page/$page/?s=${query}")
    val document = app.get(url).document
    val aramaCevap = document.select("article").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = this.selectFirst("h2.post-title > a") ?: return null
    val title = aTag.text()
    val href = fixUrlNull(aTag.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document

    val title = document.selectFirst("h1")?.text()?.trim() ?: return null
    val poster = posterCache[url]
    val description = document.selectFirst("li.descrip > span")?.text()?.trim()

   val recommendations = document.select("div.bBlue").mapNotNull { it.toRecommendationResult() }

    val categories = document.select("li.cat-post").firstOrNull { 
        it.selectFirst("strong")?.text()?.contains("Categorías:") == true 
    }?.select("a")?.map { it.text().trim() } ?: emptyList()

    
    val tags = document.select("li.cat-post").firstOrNull { 
        it.selectFirst("strong")?.text()?.contains("Etiquetas:") == true 
    }?.select("a")?.map { it.text().trim() } ?: emptyList()

    

    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = poster
        this.plot = description
        this.tags = tags
        this.recommendations = recommendations
        
    }
}


    private fun Element.toRecommendationResult(): SearchResponse? {
    val aTag = this.selectFirst("div.header > h2.post-title > a") ?: return null
    val title = aTag.attr("title")?.trim()
    val href = fixUrlNull(aTag.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("div.thumb-unit img")?.attr("src"))

    return newMovieSearchResponse(title ?: return null, href, TvType.NSFW) {
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

    
    val videoUrl = document.selectFirst("video source")?.attr("src")
        ?: document.selectFirst("video")?.attr("src")
        ?: run {
            
            val regex = Regex("""<source[^>]+src=["']([^"']+)["'][^>]*type=['"]video/mp4['"]""")
            regex.find(document.html())?.groupValues?.getOrNull(1)
        }

    

    if (videoUrl.isNullOrEmpty()) {
        
        return false
    }

    
    val finalVideoUrl = if (videoUrl.startsWith("http")) {
        videoUrl
    } else {
        "$mainUrl$videoUrl"
    }

    val type = if (finalVideoUrl.endsWith(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO

    callback.invoke(
        newExtractorLink(
            source = name,
            name = name,
            url = finalVideoUrl,
            type = type
        ) {
            this.referer = mainUrl
            this.quality = Qualities.Unknown.value
        }
    )
    return true
}}