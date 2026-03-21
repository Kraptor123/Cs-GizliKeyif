// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class DoedaOrijinal : MainAPI() {
    override var mainUrl = "https://www.doeda.com"
    override var name = "DoedaOrijinal"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/eda" to "Tüm Videolar",
        "${mainUrl}/kap/anne-1" to "Üvey Anne",
        "${mainUrl}/kap/buyuk-meme-1" to "Büyük Meme",
        "${mainUrl}/kap/esmer" to "Esmer",
        "${mainUrl}/kap/milf-1" to "Milf",
        "${mainUrl}/latin" to "Latin",
        "${mainUrl}/kap/amator" to "Amatör"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home = document.select("div.item-video").mapNotNull { it.toMainPageResult() }

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
        val url = data.split("|")[0]
        val document = app.get(url).document

        val iframeSrc = document.selectFirst("div.screen iframe, div.fluid-width-video-wrapper iframe")?.attr("src") ?: return false

        val vid = Regex("vid=([^&]+)").find(iframeSrc)?.groupValues?.get(1) ?: return false
        val host = Regex("https://[^/]+").find(iframeSrc)?.value ?: return false

        val postData = mapOf(
            "vid" to vid,
            "alternative" to "ankacdn",
            "ord" to "0"
        )

        try {
            val response = app.post(
                url = "$host/player/ajax_sources.php",
                data = postData,
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                    "Referer" to iframeSrc,
                    "Origin" to host
                )
            ).parsedSafe<VideoResponse>()

            if (response?.status == "true" && !response.source.isNullOrEmpty()) {
                response.source.forEach { source ->
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = name,
                            url = source.file,
                            type = ExtractorLinkType.VIDEO
                        ) {
                            this.referer = "$host/"
                        }
                    )
                }
                return true
            }
        } catch (e: Exception) { }

        return false
    }

    data class VideoSource(
        val file: String,
        val label: String? = null,
        val type: String? = null
    )

    data class VideoResponse(
        val status: String? = null,
        val source: List<VideoSource>? = null
    )
}