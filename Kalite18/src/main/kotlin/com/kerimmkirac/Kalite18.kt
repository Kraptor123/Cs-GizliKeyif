// ! Bu araç @kerimmkirac tarafından | @Cs-Gizlikeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import org.json.JSONObject
import org.json.JSONArray
import com.lagradost.cloudstream3.utils.*
import okhttp3.RequestBody.Companion.toRequestBody

import kotlinx.serialization.json.*

import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Kalite18 : MainAPI() {
    override var mainUrl              = "https://www.kalite18.net"
    override var name                 = "Kalite18"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)


    private val posterCache = mutableMapOf<String, String>()
    override val mainPage = mainPageOf(
    "?filter=latest" to "Tüm Videolar",
    "${mainUrl}/kategori/buyuk-got" to "Büyük Göt",
    "${mainUrl}/kategori/buyuk-meme" to "Büyük Meme",
    "${mainUrl}/kategori/esmer-porno" to "Esmer Porno",
    "${mainUrl}/kategori/latin-porno" to "Latin Porno",
    "${mainUrl}/kategori/milf-porno" to "MILF Porno",
    "${mainUrl}/kategori/uvey-anne-porno" to "Üvey Anne",
    "${mainUrl}/kategori/uvey-kardes-porno" to "Üvey Kardeş"
)

override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val url = if (request.data.startsWith("?")) {
        
        "${mainUrl}/page/$page${request.data}"
    } else {
        
        "${request.data}/page/$page/"
    }
    
    val document = app.get(url).document
    val home = document.select("article").mapNotNull { it.toMainPageResult() }
    
    return newHomePageResponse(request.name, home)
}

    private fun Element.toMainPageResult(): SearchResponse? {
    val title     = this.selectFirst("header.entry-header span")?.text() ?: return null
    val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src"))
    posterUrl?.let { posterCache[href] = it }

    return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
}


    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("article").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("header.entry-header span")?.text() ?: return null
    val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src"))
    posterUrl?.let { posterCache[href] = it }

    return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document

    val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
    val poster = posterCache[url]
    val description     = document.selectFirst("div.desc p")?.text()?.trim()
    
    val tags            = document.select("div.tags-list a").map { it.text() }.take(5)
    
    
    val recommendations = document.select("article.loop-video").mapNotNull { it.toRecommendationResult() }
    

    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl       = poster
        this.plot            = description
        
        this.tags            = tags
       
        this.recommendations = recommendations
        
    }
}


    fun Element.toRecommendationResult(): SearchResponse? {
    val aTag = this.selectFirst("a") ?: return null
    val imgTag = aTag.selectFirst("img")

    val title = imgTag?.attr("alt") ?: aTag.attr("title") ?: return null
    val href = fixUrlNull(aTag.attr("href")) ?: return null
    val posterUrl = fixUrlNull(imgTag?.attr("data-src"))

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
    Log.d("Kalite18", "data: $data")

    val html = app.get(data).text

    val iframeUrl = Regex("""<iframe[^>]+src=["']([^"']+)["']""")
        .find(html)
        ?.groupValues?.get(1)

   
    if (iframeUrl == null) {
        
        return false
    }

    val vid = Regex("vid=([a-zA-Z0-9]+)").find(iframeUrl)?.groupValues?.get(1)
    
    if (vid == null) {
        
        return false
    }

    val postHeaders = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
        "Referer" to iframeUrl,
        "Origin" to "https://play.vidvod.xyz",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    )

    
    val alternatives = listOf("mp4")
    
    for (alt in alternatives) {
        val postData = mapOf(
            "vid" to vid,
            "alternative" to alt,
            "ord" to "0"
        )

        val responseText = try {
            app.post(
                url = "https://play.vidvod.xyz/ajax_sources.php",
                headers = postHeaders,
                data = postData
            ).text.also {
                
            }
        } catch (e: Exception) {
            
            continue
        }

        
        val json = try {
            org.json.JSONObject(responseText)
        } catch (e: Exception) {
           
            continue
        }

        val sourcesArray = try {
            json.getJSONArray("source")
        } catch (e: Exception) {
            
            continue
        }

        if (sourcesArray.length() == 0) {
            
            continue
        }

        for (i in 0 until sourcesArray.length()) {
            val sourceObj = sourcesArray.getJSONObject(i)
            val rawFile = sourceObj.optString("file")
            val label = sourceObj.optString("label", alt.uppercase())

            if (rawFile.isEmpty()) continue

            
            val file = try {
                java.net.URLDecoder.decode(rawFile, "UTF-8")
            } catch (e: Exception) {
                
                rawFile
            }

            

            val linkType = when {
                file.contains(".m3u8") || alt == "m3u8" -> ExtractorLinkType.M3U8
                else -> ExtractorLinkType.VIDEO
            }

            callback.invoke(
                newExtractorLink(
                    name = "Kalite18",
                    source = "Kalite18",
                    url = file,
                    type = linkType
                ) {
                    this.referer = "https://play.vidvod.xyz/"
                    this.quality = getQualityFromName(label)
                }
            )
            
            
        }
    }

    return true
}


}