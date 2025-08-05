// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.


package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class FreePornVideos : MainAPI() {
    override var mainUrl              = "https://www.freepornvideos.xxx"
    override var name                 = "Free Porn Videos"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    

    override val mainPage = mainPageOf(
        "$mainUrl/latest-updates" to "All Videos",
        "$mainUrl/most-popular/week" to "Most Popular",
        "$mainUrl/networks/mom-lover" to "Mom Lover",
        "$mainUrl/networks/brazzers-com" to "Brazzers",
        "$mainUrl/networks/mylf-com" to "MYLF",
        "$mainUrl/networks/brazzers-com" to "Brazzers",
        "$mainUrl/networks/adult-time" to "Adult Time",
        "$mainUrl/networks/rk-com" to "Reality Kings",
        "$mainUrl/categories/jav-uncensored" to "Jav",
        "$mainUrl/networks/mom-lover" to "MILF"

    )

   override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
      val document = app.get("${request.data}/$page/").document
    
    
    val home = document.select("div.item").mapNotNull {
        try {
            it.toSearchResult()
        } catch (e: Exception) {
            
            null
        }
    }

    return newHomePageResponse(
        list = HomePageList(
            name = request.name,
            list = home,
            isHorizontalImages = true
        ),
        hasNext = true
    )
}

private fun Element.toSearchResult(): SearchResponse? {
    try {
        val titleElement = this.select("strong.title").first()
        val title = titleElement?.text()?.takeIf { it.isNotBlank() } ?: return null
        
        val linkElement = this.selectFirst("a[href]")
        val href = linkElement?.attr("href")?.takeIf { it.isNotBlank() } ?: return null
        
       
        val imgElements = this.select("img.thumb")
        var posterUrl: String? = null
        
        for (img in imgElements) {
            
            val dataSrc = img.attr("data-src")?.takeIf { it.isNotBlank() }
            if (dataSrc != null) {
                posterUrl = dataSrc
                break
            }
            
            
            val src = img.attr("src")?.takeIf { it.isNotBlank() }
            if (src != null && !src.contains("data:image")) { 
                posterUrl = src
                break
            }
        }
        
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    } catch (e: Exception) {
        Log.d("FPV", "Error in toSearchResult: ${e.message}")
        return null
    }
}

    fun String?.createSlug(): String? {
        return this?.filter { it.isWhitespace() || it.isLetterOrDigit() }
            ?.trim()
            ?.replace("\\s+".toRegex(), "-")
            ?.lowercase()
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            val searchquery=query.createSlug() ?:""
            val document = app.get(
                "${mainUrl}/search/$searchquery/")
            .document
            val results = document.select("#custom_list_videos_videos_list_search_result_items > div.item").mapNotNull { it.toSearchResult() }
            searchResponse.addAll(results)

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val full_title      = document.selectFirst("div.headline > h1")?.text()?.trim().toString()
        val last_index      = full_title.lastIndexOf(" - ")
        val raw_title       = if (last_index != -1) full_title.substring(0, last_index) else full_title
        val title           = raw_title.removePrefix("- ").trim().removeSuffix("-").trim()

        val poster          = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val tags            = document.selectXpath("//div[contains(text(), 'Categories:')]/a").map { it.text() }
        val description     = document.selectXpath("//div[contains(text(), 'Description:')]/em").text().trim()
        val actors          = document.selectXpath("//div[contains(text(), 'Models:')]/a").map { it.text() }
        val recommendations = document.select("div#list_videos_related_videos_items div.item").mapNotNull { it.toSearchResult() }

        val year            = full_title.substring(full_title.length - 4).toIntOrNull()
        val rating          = document.selectFirst("div.rating span")?.text()?.substringBefore("%")?.trim()?.toFloatOrNull()?.div(10)?.toString()?.toRatingInt()

        val raw_duration    = document.selectXpath("//span[contains(text(), 'Duration')]/em").text().trim()
        val duration_parts  = raw_duration.split(":")
        val duration        = when (duration_parts.size) {
            3 -> {
                val hours   = duration_parts[0].toIntOrNull() ?: 0
                val minutes = duration_parts[1].toIntOrNull() ?: 0

                hours * 60 + minutes
            }
            else -> {
                duration_parts[0].toIntOrNull() ?: 0
            }
        }

        return newMovieLoadResponse(title.removePrefix("- ").removeSuffix("-").trim(), url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.year            = year
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
            this.score           = Score.from10(rating)
            this.duration        = duration
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
    val document = app.get(data).document
    
    document.select("video source").forEach { res ->
        val redirectUrl = res.attr("src")
        val quality = res.attr("label")
        
        try {
            
            val response = app.get(redirectUrl, allowRedirects = true)
            val finalUrl = response.url
            
            callback.invoke(
                newExtractorLink(
                    source = "FPV",
                    name = "FPV",
                    url = finalUrl,
                    
                    type = if (finalUrl.endsWith(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                ){
                    this.referer = data
                    this.quality = getQualityFromName(quality)
                }
            )
        } catch (e: Exception) {
            Log.d("FPV", "Error getting final URL for $quality: ${e.message}")
        }
    }

    return true
}}