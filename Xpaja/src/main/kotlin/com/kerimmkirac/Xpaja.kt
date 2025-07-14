// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.


package com.kerimmkirac

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Xpaja : MainAPI() {
    override var mainUrl              = "https://www.xpaja.net"
    override var name                 = "Xpaja"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/videos"      to "All Porn Videos",
        "${mainUrl}/exclusive"      to "Exclusive Porn Videos",
        "${mainUrl}/most-viewed"   to "Most Viewed Porn Videos",
        "${mainUrl}/top-rated" to "Top Rated Porn Videos",
        "${mainUrl}/hot-porn"  to "Hottest Porn Videos",
        "${mainUrl}/category/latinas"  to "Latina Porn Videos",
        "${mainUrl}/category/big-tits"  to "Big Tits Porn Videos",
        "${mainUrl}/category/big-ass"  to "Big Ass Porn Videos",
        "${mainUrl}/category/asian"  to "Asian Porn Videos",
        "${mainUrl}/category/amateur"  to "Amateur Porn Videos",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home     = document.select("article").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val title = aTag.attr("title").ifBlank { 
            this.selectFirst("h3.title-post")?.text() 
        } ?: return null

        val href = fixUrlNull(aTag.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
    val results = mutableListOf<SearchResponse>()

    for (page in 1..5) {
        val url = "$mainUrl/search/videos/$query/page/$page"
        val document = app.get(url).document
        val items = document.select("article").mapNotNull { it.toSearchResult() }

        
        if (items.isEmpty()) break

        results.addAll(items)
    }

    return results
}


    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val title = aTag.attr("title").ifBlank { 
            this.selectFirst("h3.title-post")?.text() 
        } ?: return null

        val href = fixUrlNull(aTag.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        
        val title = document.selectFirst("h1.TitlePostSingle")?.text()?.trim() ?: return null

        
        val poster = fixUrlNull(
            
            document.selectFirst("meta[property=og:image]")?.attr("content")
            
            ?: document.selectFirst("video#videoPlayer_html5_api")?.attr("poster")
            
            ?: document.selectFirst("div.vjs-poster")?.attr("style")?.let {
                Regex("""url\(['"]?(.*?)['"]?\)""").find(it)?.groupValues?.get(1)
            }
            
            ?: document.selectFirst("div.poster img")?.attr("src")
        )

        
        val tags = document.select("ul.taglist > li a").map { it.text().trim() }.distinct()

        
        val recommendations = document.select("div.owl-item div.item").mapNotNull {
            it.toRecommendationResult()
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val title = aTag.attr("title").ifBlank {
            this.selectFirst("h3")?.text()
        } ?: return null

        val href = fixUrlNull(aTag.attr("href")) ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
    Log.d("Xpaja", "data ${data}")
    val document = app.get(data).document
    
    Log.d("Xpaja", "Document title: ${document.title()}")
    Log.d("Xpaja", "Document body length: ${document.body().text().length}")

    
    val videoElement = document.selectFirst("video#videoPlayer")
    
    if (videoElement != null) {
        Log.d("Xpaja", "Video element ")
        
        
        
        val sources = videoElement.select("source")
        
        
        sources.forEachIndexed { index, source ->
           
            
            val videoUrl = source.attr("src")
            val quality = source.attr("res") ?: source.attr("label") ?: "Unknown"
            
           
            
            if (videoUrl.isNotEmpty()) {
               
                val fullUrl = if (videoUrl.startsWith("http")) {
                    videoUrl
                } else {
                    "$mainUrl/${videoUrl.removePrefix("/")}"
                }
                
                
                
                
                val qualityInt = quality.replace("p", "").toIntOrNull() ?: 0
                
                
                callback.invoke(
                    newExtractorLink(
                        name = "$name ",
                        source = name,
                        url = fullUrl,
                        type = if (fullUrl.contains(".mp4")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                    ) {
                        this.referer = mainUrl
                        this.quality = qualityInt
                    }
                )
                
                
            } else {
                Log.d("Xpaja", "Source $index - videoUrl boş")
            }
        }
        
        
        if (sources.isEmpty()) {
            
            
            val mainVideoUrl = videoElement.attr("src")
            
            
            if (mainVideoUrl.isNotEmpty()) {
                val fullUrl = if (mainVideoUrl.startsWith("http")) {
                    mainVideoUrl
                } else {
                    "$mainUrl/${mainVideoUrl.removePrefix("/")}"
                }
                
               
                
                
                val qualityFromUrl = when {
                    fullUrl.contains("1080p") -> 1080
                    fullUrl.contains("720p") -> 720
                    fullUrl.contains("480p") -> 480
                    fullUrl.contains("240p") -> 240
                    else -> 0
                }
                
                val qualityLabel = if (qualityFromUrl > 0) "${qualityFromUrl}p" else "Unknown"
                Log.d("Xpaja", "Ana video qualityFromUrl: $qualityFromUrl, qualityLabel: $qualityLabel")
                
                callback.invoke(
                    newExtractorLink(
                        name = "$name - $qualityLabel",
                        source = name,
                        url = fullUrl,
                        type = if (fullUrl.contains(".mp4")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                    ) {
                        this.referer = mainUrl
                        this.quality = qualityFromUrl
                    }
                )
                
               
            } else {
                Log.d("Xpaja", "Ana video src de boş")
            }
        }
    } else {
       
        
        
        val alternativeVideo = document.selectFirst("video") 
        
        
       
        val allVideos = document.select("video")
        
        
        allVideos.forEachIndexed { index, video ->
            Log.d("Xpaja", "Video $index: ${video.outerHtml()}")
        }
    }

    return true
}}
