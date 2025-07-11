// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class DoedaOrijinal : MainAPI() {
    override var mainUrl              = "https://www.doeda.com"
    override var name                 = "DoedaOrijinal"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/eda"      to "Tüm Videolar",
        "${mainUrl}/kap/anne-1"   to "Üvey Anne",
        "${mainUrl}/kap/buyuk-meme-1"   to "Büyük Meme",
        "${mainUrl}/kap/esmer"   to "Esmer",
        "${mainUrl}/kap/milf-1"   to "Milf",
        "${mainUrl}/latin"   to "Latin",
        "${mainUrl}/kap/amator"  to "Amatör"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home     = document.select("div.item-video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = selectFirst("a.clip-link") ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val title = anchor.attr("title")?.trim() ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, "$href|$poster", TvType.NSFW) {
            posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        
        for (page in 1..3) { 
            val document = app.get("${mainUrl}/page/$page/?s=${query}").document
            val pageResults = document.select("div.item-video").mapNotNull { it.toSearchResult() }
            
            if (pageResults.isEmpty()) break 

            results.addAll(pageResults)
        }
        
        return results
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = selectFirst("a.clip-link") ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val title = anchor.attr("title")?.trim() ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, "$href|$poster", TvType.NSFW) {
            posterUrl = poster
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(data: String): LoadResponse? {
    val (url, poster) = data.split("|").let {
        it[0] to it.getOrNull(1)
    }

    val doc = app.get(url).document

    val title = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
    val description = doc.selectFirst("div.entry-content")?.text()?.trim()
    val tags = doc.select("div#extras a").map { it.text().trim() }

    val realPoster = poster ?: fixUrlNull(doc.selectFirst("img.wp-post-image")?.attr("src"))

    val recommendations = doc.select("div.related-posts div.item-video")
        .mapNotNull { it.toRecommendationResult() }

    return newMovieLoadResponse(title, data, TvType.NSFW, data) {
        this.posterUrl = realPoster
        this.plot = description
        this.tags = tags
        this.recommendations = recommendations
    }
}


    private fun Element.toRecommendationResult(): SearchResponse? {
        val aTag = selectFirst("a.clip-link") ?: return null
        val href = fixUrlNull(aTag.attr("href")) ?: return null
        val title = aTag.attr("title")?.trim() ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, "$href|$poster", TvType.NSFW) {
            posterUrl = poster
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val (url, _) = data.split("|").let {
            it[0] to it.getOrNull(1)
        }
        
        val document = app.get(url).document
        

        val iframeSrc = document.selectFirst("div.screen iframe")?.attr("src") ?: return false
        Log.d("Doeda", "iframe : $iframeSrc")
        
        
        val vid = Regex("vid=([^&]+)").find(iframeSrc)?.groupValues?.get(1) ?: return false
        Log.d("Doeda", "vid: $vid")
        
        
        val postData = mapOf(
            "vid" to vid,
            "alternative" to "ankacdn",
            "ord" to "0"
        )
        
        try {
            
            val response = app.post(
                url = "https://www.canlitvnews.xyz/player/ajax_sources.php",
                data = postData,
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                    "Referer" to iframeSrc,
                    "Origin" to "https://www.canlitvnews.xyz",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
                )
            )
            
            val jsonResponse = response.parsedSafe<VideoResponse>()
            Log.d("Doeda", "response: ${response.text}")
            
            if (jsonResponse?.status == "true" && jsonResponse.source?.isNotEmpty() == true) {
                jsonResponse.source.forEach { source ->
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "${name}",
                            url = source.file,

                            type = ExtractorLinkType.VIDEO
                        ){
                            this.referer = "https://www.canlitvnews.xyz/"
                            this.quality = Qualities.Unknown.value
                        }
                    )
                }
                return true
            }
        } catch (e: Exception) {
            Log.e("Doeda", "hatacık : ${e.message}")
        }
        
        return false
    }
    
    data class VideoResponse(
        val source: List<VideoSource>? = null,
        val status: String? = null,
        val iframe: String? = null,
        val download: String? = null,
        val order: Int? = null,
        val image: String? = null,
        val alternative: String? = null
    )
    
    data class VideoSource(
        val file: String,
        val label: String,
        val type: String
    )
}