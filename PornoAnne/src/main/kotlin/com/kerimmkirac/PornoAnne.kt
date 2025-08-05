// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class PornoAnne : MainAPI() {
    override var mainUrl              = "https://pornoanne.com"
    override var name                 = "PornoAnne"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "Tüm Videolar",
        "${mainUrl}/kategori/buyuk-got-porno-izle"   to "Büyük Göt",
        "${mainUrl}/kategori/buyuk-memeli-porno-izle" to "Büyük Meme",
        "${mainUrl}/kategori/spor-porno-izle"  to "Spor",
        "${mainUrl}/kategori/ensest-porno-izle"  to "Ensest",
        "${mainUrl}/kategori/1080p-porno-izle"  to "1080p",
        "${mainUrl}/kategori/4k-porno-izle"  to "4k",
        "${mainUrl}/kategori/brezilya-porno-izle"  to "Brezilyalı",
        "${mainUrl}/kategori/koreli-porno-izle"  to "Koreli",
        "${mainUrl}/kategori/brazzers-porno-izle"  to "Brazzers",
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

    
    val poster = fixUrlNull(
        selectFirst("source")?.attr("data-srcset")
            ?: selectFirst("img")?.attr("src")
            ?: selectFirst("img")?.attr("data-src")
    )

    return newMovieSearchResponse(title, "$href|$poster", TvType.Movie) {
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
        val poster = fixUrlNull(
        selectFirst("source")?.attr("data-srcset")
            ?: selectFirst("img")?.attr("src")
            ?: selectFirst("img")?.attr("data-src")
    )

        return newMovieSearchResponse(title, "$href|$poster", TvType.Movie) {
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

    

    return newMovieLoadResponse(title, data, TvType.Movie, data) {
        this.posterUrl = realPoster
        this.plot = description
        this.tags = tags
        
    }
}

    

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("STF", "data » ${data}")
        
        
        val url = data.split("|")[0]
        Log.d("STF", "extracted url » ${url}")
        
        val document = app.get(url).document

        
        val iframe = document.selectFirst("iframe")?.attr("src") ?: return false
        Log.d("STF", "iframe » ${iframe}")

        
        val iframeDocument = app.get(iframe, referer = mainUrl).document
        
        
        val scriptText = iframeDocument.select("script").joinToString("\n") { it.html() }
        val fileRegex = """file:\s*["']([^"']+)["']""".toRegex()
        val fileMatch = fileRegex.find(scriptText)
        val m3u8Url = fileMatch?.groupValues?.get(1) ?: return false
        
       
        val fullM3u8Url = if (m3u8Url.startsWith("http")) {
            m3u8Url
        } else {
            "${iframe.substringBefore("/player.php")}$m3u8Url"
        }
        
        Log.d("STF", "m3u8Url » ${fullM3u8Url}")

        
        val playlistResponse = app.get(fullM3u8Url, referer = iframe)
        val playlistContent = playlistResponse.text
        
        Log.d("STF", "playlist content » ${playlistContent.take(200)}")
        
       
        callback.invoke(
        newExtractorLink(
            name = "PornoAnne",
            source = "PornoAnne",
            url = fullM3u8Url,
            type = ExtractorLinkType.M3U8
        ){
            this.referer = iframe
            this.quality = Qualities.Unknown.value
        }
    )
    return true
}
}