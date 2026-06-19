// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.


package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Xpaja : MainAPI() {
    override var mainUrl              = "https://www.xpaja.net"
    override var name                 = "Xpaja"
    override val hasMainPage          = true
    override var lang                 = "es"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/videos"         to "Todos los Videos Porno",
        "${mainUrl}/exclusive"      to "Videos Porno Exclusivos",
        "${mainUrl}/most-viewed"   to "Videos Porno Más Vistos",
        "${mainUrl}/top-rated" to "Videos Porno Mejor Valorados",
        "${mainUrl}/hot-porn"  to "Videos Porno Más Calientes",
        "${mainUrl}/category/latinas"  to "Videos Porno de Latinas",
        "${mainUrl}/category/big-tits"  to "Videos Porno de Tetas Grandes",
        "${mainUrl}/category/big-ass"  to "Videos Porno de Culo Grande",
        "${mainUrl}/category/asian"  to "Videos Porno Asiáticos",
        "${mainUrl}/category/amateur"  to "Videos Porno Amateur",
        "${mainUrl}/category/sexual-follies"  to "Locuras Sexuales",
        "${mainUrl}/category/hardcore"  to "Videos Porno Hardcore",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home     = document.select("article").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
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

    override suspend fun search(query: String, page: Int): SearchResponseList {
    val url = "$mainUrl/search/videos/$query/page/$page"
    val document = app.get(url).document

    val aramaCevap = document.select("article").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
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

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1.TitlePostSingle")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val tags = document.select("ul.taglist > li a").map { it.text().trim() }.distinct()
        val year = document.selectFirst("span.p-by")?.text()?.trim()?.let {
            Regex("""\b(\d{4})\b""").find(it)?.groupValues?.get(1)?.toIntOrNull()
        }
        val recommendations = document.select("div.media-body article").mapNotNull {
            it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.tags = tags
            this.year = year
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val aTag = this.selectFirst("a.thumb-unit > a") ?: this.selectFirst("a") ?: return null
        val title = aTag.attr("title").ifBlank {
            this.selectFirst("h3.title-post")?.text()
        } ?: return null
        val href = fixUrlNull(aTag.attr("href")) ?: return null
        val poster = fixUrlNull(this.selectFirst("img.thumb-post")?.let { img -> img.attr("data-src").takeIf { it.isNotBlank() && !it.contains("img-lazy") } ?: img.attr("src").takeIf { it.isNotBlank() && !it.contains("img-lazy") } })
        Log.d("Ayzen", "Title: $title | Href: $href | Poster: $poster")
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("Xpaja", "data $data")
        val document = app.get(data).document
        Log.d("Xpaja", "Document title: ${document.title()}")
        Log.d("Xpaja", "Document body length: ${document.body().text().length}")
        val videoElement = document.selectFirst("video#videoPlayer")
        if (videoElement != null) {
            Log.d("Xpaja", "Video element ")
            val sources = videoElement.select("source")
            sources.forEachIndexed { index, source ->
                val videoUrl = source.attr("src")
                val quality = source.attr("res")
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
    }
}
