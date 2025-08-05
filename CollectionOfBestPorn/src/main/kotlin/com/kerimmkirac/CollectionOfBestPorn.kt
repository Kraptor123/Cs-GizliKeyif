// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class CollectionOfBestPorn : MainAPI() {
    override var mainUrl              = "https://collectionofbestporn.com"
    override var name                 = "CollectionOfBestPorn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/most-recent"      to "All Videos",
        "${mainUrl}/most-viewed/month"   to "Most Viewed Videos",
        
        "${mainUrl}/category/big-ass"  to "Big Ass Videos",
        "${mainUrl}/category/big-tits" to "Big Tits Videos",
        "${mainUrl}/category/latin" to "Latin Videos",
        "${mainUrl}/category/family" to "Family Videos",
        "${mainUrl}/category/lingerie" to "Lingerie Videos",
        "${mainUrl}/category/milf" to "Milf Videos",
        "${mainUrl}/category/asian" to "Asian Videos",
        
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = app.get("${request.data}/page/$page").document
    val home = document.select("div.video-item").mapNotNull { it.toMainPageResult() }

    return newHomePageResponse(request.name, home)
}

private fun Element.toMainPageResult(): SearchResponse? {
    val anchor = this.selectFirst("div.video-thumb a") ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null

    val img = anchor.selectFirst("img")
    val posterUrl = fixUrlNull(img?.attr("src"))

    val title = this.selectFirst("div.video-desc div.title span")?.text()
        ?: img?.attr("alt") 
        ?: return null

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
}


    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/search/${query}").document

        return document.select("div.video-item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("div.video-thumb a") ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null

    val img = anchor.selectFirst("img")
    val posterUrl = fixUrlNull(img?.attr("src"))

    val title = this.selectFirst("div.video-desc div.title span")?.text()
        ?: img?.attr("alt") 
        ?: return null

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document
    val title = document.selectFirst("h1.video-title")?.text()?.trim() ?: return null
    val poster = fixUrlNull(
    document.selectFirst("div.vjs-poster")?.attr("style")
     ?.let { Regex("""url\((?:&quot;|")?(.*?)(?:&quot;|")?\)""").find(it)?.groupValues?.getOrNull(1) }
    ?: document.selectFirst("div.video-thumb img")?.attr("src"))
    val description = document.selectFirst("h1.video-title")?.text()?.trim() ?: return null
    val tags = document.select("div.tags ul.item-list li").map { it.text() }
    val recommendations = document.select("div.video-item").mapNotNull { it.toRecommendationResult() }
    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = poster
        this.plot = description
        this.tags = tags
        this.recommendations = recommendations
    }
}

    private fun Element.toRecommendationResult(): SearchResponse? {
     val anchor = this.selectFirst("div.video-thumb a") ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null

    val img = anchor.selectFirst("img")
    val posterUrl = fixUrlNull(img?.attr("src"))

    val title = this.selectFirst("div.video-desc div.title span")?.text()
    ?: img?.attr("alt") 
    ?: return null
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
    Log.d("STF", "data » ${data}")
    val document = app.get(data).document
    
    
    val sources = document.select("video source")
    Log.d("STF", "Found ${sources.size} sources")
    
    sources.forEach { source ->
        val videoUrl = source.attr("src")
        val quality = source.attr("res") 
        val label = source.attr("label")
        
        if (videoUrl.isNotEmpty()) {
            Log.d("STF", "Video URL: $videoUrl")
            Log.d("STF", "Quality: $quality, Label: $label")
            
            
            callback.invoke(
                newExtractorLink(
                    name = this.name,
                    source = this.name,
                    url = videoUrl,
                    
                    
                    
                    type = if (videoUrl.endsWith(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                ){
                    this.referer = "https://collectionofbestporn.com/"
                    this.quality = when (quality) {
                        "360" -> Qualities.P360.value
                        "480" -> Qualities.P480.value
                        "720" -> Qualities.P720.value
                        "1080" -> Qualities.P1080.value
                        else -> getQualityFromName(label) ?: Qualities.Unknown.value
                    }
                    this.headers =  mapOf(
                        "Host" to "videos.collectionofbestporn.com",
                        "Connection" to "keep-alive",
                        "sec-ch-ua-platform" to "\"Android\"",
                        "Accept-Encoding" to "identity;q=1, *;q=0",
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36",
                        "sec-ch-ua" to "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Brave\";v=\"138\"",
                        "sec-ch-ua-mobile" to "?1",
                        "Accept" to "*/*",
                        "Sec-GPC" to "1",
                        "Accept-Language" to "tr-TR,tr;q=0.9",
                        "Sec-Fetch-Site" to "same-site",
                        "Sec-Fetch-Mode" to "no-cors",
                        "Sec-Fetch-Dest" to "video",
                        "Referer" to "https://collectionofbestporn.com/",
                        "Range" to "bytes=0-"
                    )
                }
            )
        }
    }
    
    return sources.isNotEmpty()
}


private fun getQualityFromName(label: String?): Int? {
    return when (label?.uppercase()) {
        "SD" -> Qualities.P360.value
        "HD" -> Qualities.P720.value
        "FHD", "FULL HD" -> Qualities.P1080.value
        else -> null
    }
}}