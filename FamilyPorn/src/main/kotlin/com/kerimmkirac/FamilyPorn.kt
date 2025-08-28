// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
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

    return newHomePageResponse(
        list = HomePageList(
            name = request.name,
            list = home,
            isHorizontalImages = true
        ),
        hasNext = true
    )
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
    
    
    val iframeElement = document.selectFirst("div.embed-container iframe")
    
    
    val iframeUrl = iframeElement?.attr("src")
        ?.takeIf { it.contains("bestwish.lol") }
    
    
    
    if (iframeUrl == null) {
        
        return null
    }

   
    try {
       
        
        val iframeResponse = app.get(iframeUrl, headers = mapOf(
            "referer" to url,
            "user-agent" to "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36"
        ))
        
        val iframeContent = iframeResponse.text
        
        
        
        val streamingUrlRegex = Regex("streaming_url\\s*:\\s*[\"']([^\"']+)[\"']")
        val thumbnailRegex = Regex("thumbnail\\s*:\\s*[\"']([^\"']+)[\"']")
        
        val streamingUrlMatch = streamingUrlRegex.find(iframeContent)
        val thumbnailMatch = thumbnailRegex.find(iframeContent)
        
        val videoUrl = streamingUrlMatch?.groupValues?.get(1)
        val thumbnail = thumbnailMatch?.groupValues?.get(1)
        
        
        
        if (videoUrl != null) {
            return newMovieLoadResponse(title, url, TvType.NSFW, videoUrl) {
                this.posterUrl = thumbnail
                this.tags = tags
                this.plot = description
            }
        } else {
           
            
            
            val alternativePatterns = listOf(
                Regex("file\\s*:\\s*[\"']([^\"']+\\.m3u8[^\"']*)[\"']"),
                Regex("[\"']([^\"']*\\.m3u8[^\"']*)[\"']"),
                Regex("source\\s*:\\s*[\"']([^\"']+)[\"']")
            )
            
            for (pattern in alternativePatterns) {
                val match = pattern.find(iframeContent)
                if (match != null) {
                    val altVideoUrl = match.groupValues[1]
                    
                    
                    return newMovieLoadResponse(title, url, TvType.NSFW, altVideoUrl) {
                        this.posterUrl = thumbnail
                        this.tags = tags
                        this.plot = description
                    }
                }
            }
            
            return null
        }
        
    } catch (e: Exception) {
        Log.e("FamilyPorn", "iframe extraction failed: ${e.message}")
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