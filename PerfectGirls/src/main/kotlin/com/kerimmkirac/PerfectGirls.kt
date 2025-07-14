// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class PerfectGirls : MainAPI() {
    override var mainUrl              = "https://www.perfectgirls.xxx"
    override var name                 = "PerfectGirls"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "All Porn Videos",
        "${mainUrl}/popular"   to "Popular Porn Videos",
        "${mainUrl}/trending" to "Trending Porn Videos",
        
        "${mainUrl}/tags/big-ass-stepmom"  to "Stepmom Porn Videos",
        "${mainUrl}/tags/hot-stepsister"  to "Stepsister Porn Videos",
        "${mainUrl}/tags/big-tits-porn"  to "Big Tits Porn Videos",
        "${mainUrl}/tags/latina-amateur"  to "Latina Porn Videos",
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page/").document
        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
    val aTag = this.selectFirst("a") ?: return null
    val title = aTag.attr("title").trim().ifEmpty { null } ?: return null

    val href = fixUrlNull(aTag.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original")
        ?: this.selectFirst("img")?.attr("src"))

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
}

    override suspend fun search(query: String): List<SearchResponse> {
    val results = mutableListOf<SearchResponse>()

    for (page in 1..5) {
        val url = "$mainUrl/search/$query/$page/"
        val document = app.get(url).document
        val items = document.select("div.item").mapNotNull { it.toSearchResult() }

        
        if (items.isEmpty()) break

        results.addAll(items)
    }

    return results
}

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
    val title = aTag.attr("title").trim().ifEmpty { null } ?: return null

    val href = fixUrlNull(aTag.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original")
        ?: this.selectFirst("img")?.attr("src"))

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document

    val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: return null
    val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))

    val description = document.select("div.desc")
        .map { it.text().trim() }
        .firstOrNull { it.startsWith("Description:", ignoreCase = true) }
        ?.removePrefix("Description:")?.trim()

    val tags = document.select("ul.video-tags li a").map { it.text().trim() }

    val duration = document.select("ul.video-meta li")
        .find { it.text().contains(":") }
        ?.text()?.trim()?.split(":")?.let {
            val minutes = it.getOrNull(0)?.toIntOrNull() ?: 0
            val seconds = it.getOrNull(1)?.toIntOrNull() ?: 0
            (minutes * 60 + seconds) / 60
        }

    
    val previewJson = document.selectFirst("a.js-favourites-player")?.attr("data-setup")
    val previewUrl = previewJson?.let {
        val decodedJson = it.replace("&quot;", "\"")
        Regex("\"preview\"\\s*:\\s*\"(.*?)\"").find(decodedJson)?.groupValues?.get(1)
    }

    
     val recommendations = document.select("div.item.thumb-bl").mapNotNull {
            it.toRecommendationResult()
        }

    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = poster
        this.plot = description
        this.tags = tags
        this.duration = duration
        this.recommendations = recommendations
        if (!previewUrl.isNullOrBlank()) {
            addTrailer(previewUrl)
        }
    }
}

    private fun Element.toRecommendationResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
    val title = aTag.attr("title").trim().ifEmpty { null } ?: return null

    val href = fixUrlNull(aTag.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original")
        ?: this.selectFirst("img")?.attr("src"))

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
    Log.d("PerfectGirls", "data  $data")

    val document = app.get(data).document
    val source = document.selectFirst("video#my-video source") ?: return false
    val initialUrl = fixUrl(source.attr("src"))

    if (initialUrl.isNotBlank()) {
        try {
            
            val redirectedM3u = app.get(initialUrl).url
            Log.d("PerfectGirls", "redirectedM3u: $redirectedM3u")

            
            val playlist = app.get(redirectedM3u).text

            
            val regex = Regex("""#EXT-X-STREAM-INF:.*RESOLUTION=\d+x(\d+).*?\n(https[^\n]+)""")
            regex.findAll(playlist).forEach {
                val quality = it.groupValues[1].toIntOrNull() ?: 0
                val streamUrl = it.groupValues[2]

                callback.invoke(
                    newExtractorLink(
                        name = "PerfectGirls",
                        source = "PerfectGirls",
                        url = streamUrl,
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = data
                        this.quality = quality
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("PerfectGirls", "redirect m3u8 parse error ${e.message}")
        }
    }

    return true
}
}
fun getQualityFromString(quality: String): Int {
    return Regex("""(\d{3,4})p""").find(quality)?.groupValues?.get(1)?.toIntOrNull() ?: Qualities.Unknown.value
}

