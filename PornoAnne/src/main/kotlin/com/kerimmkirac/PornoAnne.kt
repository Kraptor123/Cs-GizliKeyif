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
    val anchor = selectFirst("a.clip-link") ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null
    val title = anchor.attr("title")?.trim() ?: return null

    
    val poster = fixUrlNull(
        selectFirst("source")?.attr("data-srcset")
            ?: selectFirst("img")?.attr("src")
            ?: selectFirst("img")?.attr("data-src")
    )

    return newMovieSearchResponse(title, href, TvType.NSFW) {
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

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            posterUrl = poster
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(data: String): LoadResponse? {
        val doc = app.get(data).document

        val title = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val description = doc.selectFirst("div.entry-content")?.text()?.trim()
        val tags = doc.select("div#extras a").map { it.text().trim() }
        val posterUrl = doc.selectFirst("meta[property=og:image]")?.attr("content").toString()


    return newMovieLoadResponse(title, data, TvType.NSFW, data) {
        this.posterUrl = posterUrl
        this.plot = description
        this.tags = tags
        
    }
}

    

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {

        Log.d("kraptor_${this.name}", "data » ${data}")


        val document = app.get(data, referer = "${mainUrl}/").document


        val iframe = document.selectFirst("div#video iframe")?.attr("src").toString()

        Log.d("kraptor_${this.name}", "iframe » ${iframe}")

        loadExtractor(iframe, referer = "${mainUrl}/" , subtitleCallback, callback)

        return true
    }
}