// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class FamilyPorn : MainAPI() {
    override var mainUrl              = "https://familypornhd.com"
    override var name                 = "FamilyPorn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "All Porn Videos",
        "${mainUrl}/tag/redhead"   to "Red Head Porn Videos",
        "${mainUrl}/tag/cowgirl" to "Cowgirl Porn Videos",
        "${mainUrl}/tag/doggystyle"  to "DoggyStyle Porn Videos",
        "${mainUrl}/tag/latina"   to "Latina Porn Videos",
        "${mainUrl}/tag/milf"   to "Milf Porn Videos",
        "${mainUrl}/tag/natural-tits"   to "Natural Tits Porn Videos",
        "${mainUrl}/tag/stepmomporn"   to "Stepmom Porn Videos",
        "${mainUrl}/tag/stepsisterporn"   to "Step Sister Porn Videos",
        "${mainUrl}/tag/athletic"   to "Athletic Porn Videos",
        "${mainUrl}/tag/asian"   to "Asian Porn Videos",
        "${mainUrl}/tag/big-natural-tits"   to "Big Natural Tits Porn Videos",
        "${mainUrl}/tag/big-tits"   to "Big Tits Porn Videos",

    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val url = if (page == 1) request.data else "${request.data}/page/$page"
    val document = app.get(url).document
    val home = document.select("li.g1-collection-item").mapNotNull { it.toMainPageResult() }

    return newHomePageResponse(request.name, home)
}


    private fun Element.toMainPageResult(): SearchResponse? {
    val anchor = this.selectFirst("article a") ?: return null
    val title = anchor.attr("title")?.trim() ?: return null
    val href = fixUrl(anchor.attr("href"))
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
}


    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("li.g1-collection-item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("article a") ?: return null
    val title = anchor.attr("title")?.trim() ?: return null
    val href = fixUrl(anchor.attr("href"))
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    
    val document = app.get(url).document

    val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
   
    
    val description = document.selectFirst("div.entry-content p")?.text()?.trim() ?: ""
    val tags = document.select("p.entry-tags a").map { it.text().lowercase() }.take(5)
    
    
    
    
    
    
    
    val iframeUrl = document.selectFirst("div.embed-container iframe")?.attr("src")
        ?.takeIf { it.contains("bestwish.lol") }
    
    
    
    if (iframeUrl == null) {
        
        return null
    }

    val fileCode = iframeUrl.substringAfterLast("/")
    val apiUrl = "https://bestwish.lol/data.php?filecode=$fileCode"
    

    try {
        val json = app.get(
            apiUrl,
            headers = mapOf(
                "x-requested-with" to "XMLHttpRequest",
                "referer" to iframeUrl,
                "user-agent" to "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36"
            )
        ).parsedSafe<Map<String, Any>>()
        
       

        if (json == null) {
            
            return null
        }

        val VideoUrl = json["streaming_url"] as? String
        
        
        if (VideoUrl == null) {
           
            return null
        }
        
        val poster = json["thumbnail"] as? String
        val duration = (json["duration"] as? Double)?.toInt()
        
        

        return newMovieLoadResponse(title, url, TvType.NSFW, VideoUrl) {
            this.posterUrl = poster
            this.tags = tags
            this.plot = description
            this.duration = duration
            
        }
    } catch (e: Exception) {
        
        return null
    }
}

    private fun Element.toRecommendationResult(): SearchResponse? {
        val anchor = this.selectFirst("article a") ?: return null
    val title = anchor.attr("title")?.trim() ?: return null
    val href = fixUrl(anchor.attr("href"))
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
    

    
    val VideoUrl = data
    
    if (VideoUrl.isBlank()) {
        
        return false
    }

    

    callback.invoke(
        newExtractorLink(
            name = "BestWish",
            source = "BestWish",
            url = VideoUrl,
            type = if (VideoUrl.endsWith(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
        ) {
            this.referer = "https://bestwish.lol/"
            this.quality = Qualities.Unknown.value
            this.headers = mapOf(
                "origin" to "https://bestwish.lol",
                "referer" to "https://bestwish.lol",
                "user-agent" to "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36",
                "accept-language" to "tr-TR,tr;q=0.8",
                "accept" to "*/*"
            )
        }
    )

    
    return true
}

}