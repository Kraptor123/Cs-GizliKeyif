// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class CamWh : MainAPI() {
    override var mainUrl              = "https://camwh.com"
    override var name                 = "CamWh"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/latest-updates/?mode=async&function=get_block&block_id=list_videos_latest_videos_list&sort_by=post_date&from=1" to "Latest Videos",
        "$mainUrl/top-rated/?mode=async&function=get_block&block_id=list_videos_common_videos_list&sort_by=rating&from=1" to "Top Rated Videos",
        "$mainUrl/most-popular/?mode=async&function=get_block&block_id=list_videos_common_videos_list&sort_by=video_viewed&from=1" to "Most Viewed Videos"
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val newUrl = request.data.replace(Regex("from=\\d+"), "from=$page")
        val document = app.get(newUrl).document

        val items = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, items)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = this.selectFirst("a") ?: return null
        val title = anchor.attr("title")?.trim() ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("data-original"))
        

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
            
        }
    }



    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}/?from_videos=$page").document

        val aramaCevap = document.select("div.item").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("a") ?: return null
        val title = anchor.attr("title")?.trim() ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("data-original"))
        

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
            
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document
    
    val json = document.selectFirst("script[type=application/ld+json]")?.data() ?: return null
    val jsonObj = JSONObject(json)
    
    val title = jsonObj.optString("name") ?: return null
    val description = jsonObj.optString("description")
    val poster = fixUrlNull(jsonObj.optString("thumbnailUrl"))
    val contentUrl = jsonObj.optString("contentUrl") 
    val recommendations = document.select("div.list-videos div.item").mapNotNull { it.toRecommendationResult() }
    
    return newMovieLoadResponse(title, url, TvType.NSFW, contentUrl) { 
        this.posterUrl = poster
        this.plot = description
        this.recommendations = recommendations
    }
}

private fun Element.toRecommendationResult(): SearchResponse? {
    val title = this.selectFirst("a img")?.attr("alt") ?: return null
    val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-webp"))
    
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
   
    
    try {
        
        val response = app.get(data, allowRedirects = false)
        
        val finalUrl = if (response.code in 300..399) {
            
            response.headers["Location"] ?: response.headers["location"] ?: data
        } else {
            
            data
        }
        
        
        
       
        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                finalUrl,
                
               type = if (finalUrl.endsWith(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            ){
                this.quality = Qualities.Unknown.value
                this.referer = ""
            }
        )
        
        return true
        
    } catch (e: Exception) {
        
        return false
    }
}}
