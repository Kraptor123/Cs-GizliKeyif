// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class MomLover : MainAPI() {
    override var mainUrl              = "https://ilovemommies.com"
    override var name                 = "MomLover"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}" to "Tüm Videolar",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home = document.select("div.post, div.multiple").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div#title-posta > h2 > a")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("div#title-posta > h2 > a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.thumbz img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
     val document = app.get("${mainUrl}/page/$page").document
     val aramaCevap = document.select("div.post, div.multiple").mapNotNull { it.toSearchResult() }

     return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div#title-posta > h2 > a")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("div#title-posta > h2 > a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.thumbz img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>?= search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h2")?.text()?.trim() ?: return null

        val videoElement = document.selectFirst("video")
        val videoUrl = fixUrlNull(videoElement?.selectFirst("source")?.attr("src")) ?: return null
        val poster = fixUrlNull(videoElement?.attr("poster"))

        val yearText = document.selectFirst("div#title-single span")?.ownText()
        val year = Regex("""\b(\d{4})\b""").find(yearText ?: "")?.groupValues?.get(1)?.toIntOrNull()

        val actors = document.select("div#title-single a[rel=tag]").map { Actor(it.text()) }

        val recommendations = document.select("li.recent-post-item").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, videoUrl) {
            this.posterUrl = poster
            this.year = year
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val anchorWithTitle = this.selectFirst("a[title]") ?: return null
        val title = anchorWithTitle.attr("title").trim()
        val href = fixUrlNull(anchorWithTitle.attr("href")) ?: return null

        val posterUrl = fixUrlNull(this.selectFirst("img.wp-post-image")?.attr("src"))

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
//        Log.d("kraptor_MomLover", "data » $data")

        try {
            val videoUrl = if (data.contains(".mp4")) {

//                Log.d("kraptor_MomLover", "Mp4 bulduk: $data")
                val response = app.get(data, allowRedirects = false)
                response.headers["Location"] ?: response.headers["location"] ?: data
            } else {

                val document = app.get(data).document
                val baseUrl = document.selectFirst("video source")?.attr("src") ?: return false
//                Log.d("kraptor_MomLover", "video base » $baseUrl")

                val response = app.get(baseUrl, allowRedirects = false)
                response.headers["Location"] ?: response.headers["location"] ?: baseUrl
            }

//           Log.d("kraptor_MomLover", "En son url: $videoUrl")

            callback.invoke(
                newExtractorLink(
                    name = "MomLover",
                    source = "MomLover",
                    url = videoUrl,
                    type = if (videoUrl.contains(".mp4")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                ) {
                    this.referer = "${mainUrl}/"
                    this.quality = Qualities.Unknown.value
                }
            )

            return true

        } catch (e: Exception) {
            Log.e("Mom", "loadlinks hata: ${e.message}")
            return false
        }
    }
}