// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Motherless : MainAPI() {
    override var mainUrl              = "https://motherless.com"
    override var name                 = "Motherless"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/porn/public/videos"    to "Public",
        "${mainUrl}/GV02DE763"             to "Best Bodies",
        "${mainUrl}/GV0DC9FD6"             to "Group Sex",
        "${mainUrl}/porn/gothic/videos"    to "Gothic",
        "${mainUrl}/porn/japanese/videos"  to "Japanese",
        "${mainUrl}/porn/cfnm/videos"      to "CFNM",
        "${mainUrl}/GV2B87965"             to "Oral",
        "${mainUrl}/GV637AC0A"             to "SHSY",
        "${mainUrl}/GV1AD0514"             to "Best Ama Webcams",
        "${mainUrl}/GV5DBB206"             to "Jerk Off Instructions",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get("${request.data}").document
        } else {
            app.get("${request.data}?page=$page").document
        }
        val home     = document.select("div.thumb-container.video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.captions a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.static")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/term/${query}").document
        val videolar = fixUrlNull(document.selectFirst("div.content-footer a")?.attr("href")).toString()
        Log.d("kraptor_$name", "videolar = $videolar")
        val videoSayfa = app.get(videolar).document

        return videoSayfa.select("div.thumb-container.video").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a.caption.title.pop.plain")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.static")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.media-meta-title h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = "Sadece 18 Yaş ve Üzeri İçin Uygundur!"
        val tags            = document.select("div.media-meta-tags a").map { it.text() }
        val recommendations = document.select("div.thumb-container.video").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("div.captions a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.static")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).text

        val regex = Regex(pattern = "__fileurl = '([^']*)';", options = setOf(RegexOption.IGNORE_CASE))

        val videoUrl = regex.find(document)?.groupValues[1].toString()

        Log.d("kraptor_$name", "videoUrl = ${videoUrl}")

        callback.invoke(newExtractorLink(
            source = "Motherless",
            name   = "Motherless",
            url    = videoUrl,
            type   = INFER_TYPE
        ))
        return true
    }
}