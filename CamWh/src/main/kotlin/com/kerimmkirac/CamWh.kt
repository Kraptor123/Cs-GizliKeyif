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
        "$mainUrl/latest-updates/" to "Latest Videos",
        "$mainUrl/top-rated/" to "Top Rated Videos",
        "$mainUrl/most-popular/" to "Most Viewed Videos"
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val newUrl = request.data.replace(Regex("from=\\d+"), "from=$page")
        val document = app.get(newUrl).document

        val items = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, items)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = this.selectFirst("a") ?: return null
        val title = anchor.attr("title").trim()
        val href = fixUrlNull(anchor.attr("href")) ?: return null

        val imgElement = this.selectFirst("img")
        val poster = fixUrlNull(imgElement?.attr("data-original").takeIf { !it.isNullOrBlank() })



        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }



    override suspend fun search(query: String, page: Int): SearchResponseList {
        val searchUrl = "$mainUrl/search/$query/?mode=async&function=get_block&block_id=list_videos_videos_list_search_result&q=$query&category_ids=&sort_by=&from_videos=$page&from_albums=1"

        val document = app.get(searchUrl).document

        val aramaCevap = document.select("div.item").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = aramaCevap.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("a") ?: return null
        val title = anchor.attr("title").trim()
        val href = fixUrlNull(anchor.attr("href")) ?: return null

        val imgElement = this.selectFirst("img")
        val poster = fixUrlNull(
            imgElement?.attr("data-original").takeIf { !it.isNullOrBlank() }
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("div.headline h1")?.text()?.trim() ?: return null
        val poster = document.selectFirst("div.fp-poster img")?.attr("src")
        val description = document.selectFirst("div.item:contains(Description:) em")?.text()

        val actor = document.select("div.item:contains(Tags:) a").map { it.text() }
        val tags = document.select("div.item:contains(Categories:) a").map { it.text() }

        val recommendations = document.select("div.list-videos div.item").mapNotNull {
            it.toRecommendationResult()
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
            this.addActors(actor)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val anchor = this.selectFirst("a") ?: return null
        val title = anchor.attr("title").trim()
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val img = anchor.selectFirst("img")
        val poster = fixUrlNull(img?.attr("data-webp") ?: img?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
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
