// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Evooli : MainAPI() {
    override var mainUrl              = "https://www.evooli.com"
    override var name                 = "Evooli"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "Tüm videolar",
        "${mainUrl}/anne-x"   to "Üvey Anne",
        "${mainUrl}/ensest-x"   to "Ensest",
        "${mainUrl}/asyali-x"   to "Asyalı",
        "${mainUrl}/esmer-x"   to "Esmer",
        "${mainUrl}/milf-x"   to "Milf",
        
        "${mainUrl}/amator-porno-x"  to "Amatör"
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

    val poster = fixUrlNull(this.selectFirst("a.clip-link img")?.attr("data-lazy-src"))
        ?: fixUrlNull(this.selectFirst("a.clip-link img")?.attr("src")) 

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

    val poster = fixUrlNull(this.selectFirst("a.clip-link img")?.attr("data-lazy-src"))
        ?: fixUrlNull(this.selectFirst("a.clip-link img")?.attr("src"))

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

    val iframeSrc = fixUrlNull(doc.selectFirst("div.player iframe")?.attr("src")) ?: return null
    Log.i("Evooli", "iframe URL: $iframeSrc")

    val iframeDoc = app.get(iframeSrc).document

    val script = iframeDoc.select("script").find {
        it.data().contains("jwplayer(\"player\").setup")
    }?.data() ?: return null.also {
        Log.e("Evooli", "JW setup script bulunamadı")
    }

    Log.i("Evooli", "JW Player setup script:\n$script")

    val posterRegex = Regex("""image\s*:\s*"(.*?)"""")
    val videoRegex = Regex("""file\s*:\s*"(.*?)"""")

    val poster = posterRegex.find(script)?.groupValues?.get(1)
    val videoUrl = videoRegex.find(script)?.groupValues?.get(1)

    Log.i("Evooli", "Poster URL: $poster")
    Log.i("Evooli", "Video URL: $videoUrl")

    if (videoUrl == null) return null

    val recommendations = doc.select("div.related-posts div.item-video")
        .mapNotNull { it.toRecommendationResult() }

    return newMovieLoadResponse(title, data, TvType.NSFW, data) {
        this.posterUrl = poster
        this.posterHeaders = mapOf("Referer" to mainUrl)
        this.plot = description
        this.tags = tags
        this.recommendations = recommendations
    }
}





    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val doc = app.get(data).document
    val iframeSrc = fixUrlNull(doc.selectFirst("div.player iframe")?.attr("src")) ?: return false
    val iframeDoc = app.get(iframeSrc).document

    val script = iframeDoc.select("script").find {
        it.data().contains("jwplayer(\"player\").setup")
    }?.data() ?: return false

    val videoRegex = Regex("""file\s*:\s*"(.*?)"""")
    val videoUrl = videoRegex.find(script)?.groupValues?.get(1) ?: return false

    callback.invoke(
        newExtractorLink(
            name = this.name,
            source = this.name,
            url = videoUrl,


            type = ExtractorLinkType.VIDEO
        ){
            this.referer = mainUrl
            this.quality = Qualities.P1080.value
        }
    )

    return true
}}