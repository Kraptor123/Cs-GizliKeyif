// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Kopeda : MainAPI() {
    override var mainUrl              = "https://www.kopeda.com"
    override var name                 = "Kopeda"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "Tüm Videolar",
        "${mainUrl}/porno/anne"   to "Üvey Anne",
        "${mainUrl}/porno/esmer" to "Esmer",
        "${mainUrl}/porno/ensest"  to "Ensest",
        "${mainUrl}/porno/konulu"  to "Konulu",
        "${mainUrl}/porno/hd"  to "HD 1080P ",
        "${mainUrl}/porno/milf"  to "Milf",
        "${mainUrl}/porno/latin"  to "Latin",
        "${mainUrl}/porno/asyali"  to "Asyalı",
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

        return newMovieSearchResponse(title, "$href|$poster", TvType.Movie){
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

        return newMovieSearchResponse(title, "$href|$poster", TvType.Movie){
          posterUrl = poster  
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(data: String): LoadResponse? {
        val (url, poster) = data.split("|").let {
            it[0] to it.getOrNull(1)
        }

        val doc = app.get(url).document

        val title = doc.selectFirst("div.inner.cf h1")?.text()?.trim() ?: return null
        val description = doc.selectFirst("div.entry-content.rich-content p")?.text()?.trim()
        val tags = doc.select("span.pornoKategori a").map { it.text().trim() }

        

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.plot = description
            this.tags = tags
            this.posterUrl = poster
            
        }
    }

    

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("Kopeda", "data » ${data}")
        val document = app.get(data).document
        
        
        val iframe = document.selectFirst("div.screen.fluid-width-video-wrapper.player iframe")?.attr("src")
        if (iframe.isNullOrEmpty()) return false
        
        
        val idRegex = Regex("pornolar/([^.]+)\\.html")
        val matchResult = idRegex.find(iframe)
        val videoId = matchResult?.groups?.get(1)?.value
        
        if (videoId.isNullOrEmpty()) return false
        
        
        val apiUrl = "https://api.reqcdn.com/url.php?id=$videoId&siteid=2"
        val apiResponse = app.get(apiUrl).text
        
        
        val urlRegex = Regex("\"url\":\"([^\"]+)\"")
        val urlMatch = urlRegex.find(apiResponse)
        val videoUrl = urlMatch?.groups?.get(1)?.value?.replace("\\/", "/")
        
        if (!videoUrl.isNullOrEmpty()) {
            callback.invoke(
                newExtractorLink(
                    name = name,
                    source = name,
                    url = videoUrl,


                    type = if (videoUrl.contains(".mp4")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                ){
                    this.referer = ""
                    this.quality = Qualities.Unknown.value
                }
            )
        }
        
        return true
    }
}