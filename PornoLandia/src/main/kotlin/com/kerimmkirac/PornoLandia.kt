// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class PornoLandia : MainAPI() {
    override var mainUrl              = "https://www.pornolandia.xxx"
    override var name                 = "PornoLandia"
    override val hasMainPage          = true
    override var lang                 = "es"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/videos"      to "All Porn Videos",
        "${mainUrl}/videos?o=vp"   to "Popular Porn Videos",
        "${mainUrl}/videos/gostosas" to "Hot Porn Videos",
        "${mainUrl}/videos/bundudas"  to "Big Ass Porn Videos"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?page=$page").document
        val home = document.select(".col-xs-12.col-sm-6.col-md-3.col-lg-3.video-title-color").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2.video-title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null

        val img = this.selectFirst("img")
        val posterRaw = img?.attr("data-src")?.takeIf { it.isNotBlank() } ?: img?.attr("src")
        val posterUrl = fixUrlNull(posterRaw)

       

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
    val results = mutableListOf<SearchResponse>()

    for (page in 1..5) {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "${mainUrl}/search/videos?search_query=$encodedQuery&page=$page"
        val document = app.get(url).document
        val items = document.select(".col-xs-12.col-sm-6.col-md-4.col-lg-3.video-title-color").mapNotNull { it.toSearchResult() }

        
        if (items.isEmpty()) break

        results.addAll(items)
    }

    return results
}
    

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2.video-title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null

        val img = this.selectFirst("img")
        val posterRaw = img?.attr("data-src")?.takeIf { it.isNotBlank() } ?: img?.attr("src")
        val posterUrl = fixUrlNull(posterRaw)

        

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(document.selectFirst("video")?.attr("poster"))
        val description = document.selectFirst("p.descricao")?.text()?.trim()
        
        val tags        = document.select("div.m-t-20 a").map { it.text().trim() }
       
        

        val recommendations = document.select(".col-xs-12.col-sm-6.col-md-3.col-lg-3.video-title-color")
            .mapNotNull { it.toRecommendationResult() }

        
        val videoUrl = document.selectFirst("video source")?.attr("src")?.let { fixUrl(it) } ?: return null

        return newMovieLoadResponse(title, url, TvType.NSFW, videoUrl) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
           
            this.recommendations = recommendations
        }
    }

   

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("h2.video-title, h3.video-title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null

        val img = this.selectFirst("img")
        val posterRaw = img?.attr("data-src")?.takeIf { it.isNotBlank() } ?: img?.attr("src")
        val posterUrl = fixUrlNull(posterRaw)

        

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
           
            val videoUrl = data
            

            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = videoUrl,
                    
                    type = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO,
                ){
                        this.referer = mainUrl
                        this.quality = Qualities.Unknown.value
                    }
                
            )
            return true
        } catch (e: Exception) {
            Log.e("PornoLandia", "Error : ${e.message}")
        }

        return false
    }
}