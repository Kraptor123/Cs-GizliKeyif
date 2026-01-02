// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class XRares : MainAPI() {
    override var mainUrl              = "https://www.xrares.com"
    override var name                 = "XRares"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/videos?type=public"      to "Videos",
        "${mainUrl}/videos?type=public&o=bw"      to "Being Watched",
        "${mainUrl}/videos?type=public&o=tr"      to "Top Rated",
        "${mainUrl}/videos?type=public&o=mr"      to "Most Recent",
        "${mainUrl}/videos?type=public&o=mv"      to "Most Viewed",
        "${mainUrl}/videos?type=public&o=md"      to "Most Commented",
        "${mainUrl}/videos?type=public&o=tf"      to "Top Favorites",
        "${mainUrl}/videos?type=public&o=lg"      to "Longest",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page==1){
            app.get("${request.data}").document
        } else {
            app.get("${request.data}&page=$page").document
        }
        val home     = document.select("div.col-sm-6").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("span.video-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val rating    = this.selectFirst("div.video-rating b")?.text()?.replace("%","")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score     = Score.from100(rating)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page==1){
            app.get("${mainUrl}/search/porn_videos?search_query=$query&type=public").document
        } else {
            app.get("${mainUrl}/search/porn_videos?search_query=$query&type=public}&page=$page").document
        }

        val aramaCevap = document.select("div.col-sm-6").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.m-t-10 p")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.m-t-10 a").map { it.text() }
        val vidId           = document.selectFirst("a[href*=related_videos][id]")?.attr("id")?.substringAfterLast("_") ?: ""
        val postRecommend   = recommendation(vidId)
        val recPost         = mapper.readValue<Recommend>(postRecommend)
        val recDoc          = Jsoup.parse(recPost.videos ?:"")
        val recommendations = recDoc.select("div.col-lg-3").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.recommendations = recommendations
        }
    }

    private suspend fun recommendation(vidId: String): String{
        val response = app.post("$mainUrl/ajax/related_videos", headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Accept" to "application/json, text/javascript, */*; q=0.01",
            "Accept-Language" to "en-US,en;q=0.5",
            "Referer" to "${mainUrl}/",
            "X-Requested-With" to "XMLHttpRequest",
        ), data = mapOf(
            "video_id" to vidId,
            "page" to "1",
            "move" to "next"
        )).text
        return response
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("span.video-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val private   = this.selectFirst("div.label-private")?.text() ?: ""
        val rating    = this.selectFirst("div.video-rating b")?.text()?.replace("%","")

        if (private.contains("private", true)){
            return null
        }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score     = Score.from100(rating)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val source = document.select("source")

        source.forEach { video ->
            val res = video.attr("res")
            val mp4 = video.attr("src")
            callback.invoke(newExtractorLink(
                this.name,
                this.name,
                mp4,
                type = ExtractorLinkType.VIDEO
            ) {
                this.referer = "${mainUrl}/"
                this.quality = getQualityFromName(res)
            }
            )
        }

        return true
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Recommend(
        val videos: String?
    )
}
