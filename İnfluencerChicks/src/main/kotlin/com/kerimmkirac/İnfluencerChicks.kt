// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.


package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class İnfluencerChicks : MainAPI() {
    override var mainUrl              = "https://influencerchicks.com"
    override var name                 = "İnfluencerChicks"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "Tüm Videolar",
        "${mainUrl}/category/youtube-3"   to "Youtuber",
        "${mainUrl}/category/celebrity-2" to "Ünlü",
        "${mainUrl}/category/twitch-1"  to "Twitch",
        "${mainUrl}/category/instagram-4"  to "İnstagram",
        "${mainUrl}/category/patreon-2"  to "Patreon",
        "${mainUrl}/category/twitch-1"  to "Twitch",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = app.get("${request.data}/page/$page").document
    val home = document.select("ul.g1-collection-items li.g1-collection-item").mapNotNull { it.toMainPageResult() }

    return newHomePageResponse(request.name, home)
}

private fun Element.toMainPageResult(): SearchResponse? {
    val href = fixUrlNull(this.selectFirst("article .entry-featured-media a")?.attr("href")) ?: return null
    val title = this.selectFirst("article .entry-title a")?.text()?.trim() ?: return null

    val posterImg = this.selectFirst("article .entry-featured-media img")
    val posterUrl = fixUrlNull(posterImg?.attr("data-src") ?: posterImg?.attr("src"))

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
}



    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=${query}").document
        }

        val aramaCevap = document.select("ul.g1-collection-items li.g1-collection-item").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val href = fixUrlNull(this.selectFirst("article .entry-featured-media a")?.attr("href")) ?: return null
    val title = this.selectFirst("article .entry-title a")?.text()?.trim() ?: return null

    val posterImg = this.selectFirst("article .entry-featured-media img")
    val posterUrl = fixUrlNull(posterImg?.attr("data-src") ?: posterImg?.attr("src"))

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    
override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document

    val title = document.selectFirst("h1")?.text()?.trim() ?: return null

    val poster = fixUrlNull(document.selectFirst("video")?.attr("poster"))
        ?: fixUrlNull(document.selectFirst("div.g1-content-narrow img")?.let { img ->
            img.attr("data-src").takeIf { it.isNotBlank() } ?: img.attr("src")
        })

    val description = document.selectFirst("div.g1-content-narrow p")?.text()?.trim()

    val tags = document.select("p.entry-tags a").map { it.text().trim() }.take(5)

    val recommendations = document.select("a.g1-frame").mapNotNull { it.toRecommendationResult() }

    val videoExists = document.selectFirst("video")?.attr("src").isNullOrBlank().not()
        || document.selectFirst("video source")?.attr("src").isNullOrBlank().not()

    return if (videoExists) {
        newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    } else {
        val dummyTitle = "Bu bir fotograf galerisi o yüzden çalışmıyor."
        val dummyDescription = "Bu bir fotograf galerisi o yüzden çalışmıyor."

        newTvSeriesLoadResponse(dummyTitle, url, TvType.NSFW, emptyList()) {
            this.posterUrl = poster
            this.plot = dummyDescription
            
            this.recommendations = recommendations
        }
    }
}


private fun Element.toRecommendationResult(): SearchResponse? {
    
    val href = fixUrlNull(this.attr("href")) ?: return null
    
    
    val title = this.attr("title").takeIf { it.isNotBlank() }
        ?: this.selectFirst("h3.entry-title a")?.text()?.trim()
        ?: return null
    
    
    val img = this.selectFirst("img")
    val posterUrl = img?.let { 
        fixUrlNull(it.attr("data-src").takeIf { src -> src.isNotBlank() } 
            ?: it.attr("src"))
    }
    
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
    val document = app.get(data).document

    val video = document.selectFirst("video")
val videoUrl = video?.attr("src")?.takeIf { it.isNotBlank() }
    ?: video?.selectFirst("source")?.attr("src")?.takeIf { it.isNotBlank() }
    ?: run {
        Log.e("InfluencerChicks", "Video src bulunamadı: $data")
        return false
    }

Log.i("InfluencerChicks", "Video URL bulundu: $videoUrl")



    Log.i("InfluencerChicks", "Video URL bulundu: $videoUrl")

    callback.invoke(
        newExtractorLink(
            name = "InfluencerChicks",
            source = "InfluencerChicks",
            url = videoUrl,
            
            type = ExtractorLinkType.VIDEO
        ){
            this.referer = "$mainUrl/"
            this.quality = Qualities.Unknown.value
        }
    )

    return true
}
}

