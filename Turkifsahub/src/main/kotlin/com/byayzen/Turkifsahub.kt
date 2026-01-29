// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import kotlinx.serialization.json.*

class Turkifsahub : MainAPI() {
    override var mainUrl = "https://turkifsahub.com"
    override var name = "Turkifsahub"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val AnaHeaderlar = mapOf(
        "Origin" to "https://turkifsahub.com",
        "Referer" to "https://turkifsahub.com/",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    )

    override val mainPage = mainPageOf(
        mainUrl to "Ana sayfa",
        "${mainUrl}/kategoriler/amatör" to "Amatör",
        "${mainUrl}/kategoriler/anal" to "Anal",
        "${mainUrl}/kategoriler/cuckold" to "Cuckold",
        "${mainUrl}/kategoriler/fetiş" to "Fetiş",
        "${mainUrl}/kategoriler/genç" to "Genç",
        "${mainUrl}/kategoriler/grup" to "Grup",
        "${mainUrl}/kategoriler/konulu" to "Konulu",
        "${mainUrl}/kategoriler/konuşmalı" to "Konuşmalı",
        "${mainUrl}/kategoriler/lezbiyen" to "Lezbiyen",
        "${mainUrl}/kategoriler/mastürbasyon" to "Mastürbasyon",
        "${mainUrl}/kategoriler/milf" to "Milf",
        "${mainUrl}/kategoriler/sahibe" to "Sahibe",
        "${mainUrl}/kategoriler/şişman" to "Şişman",
        "${mainUrl}/kategoriler/tango" to "Tango",
        "${mainUrl}/kategoriler/türbanlı" to "Türbanlı",
        "${mainUrl}/kategoriler/vip" to "Vip",
        "${mainUrl}/kategoriler/zenci" to "Zenci"
    )

    private fun String?.cleanPoster(): String? {
        return fixUrlNull(this)?.substringBefore("?")
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h1[itemprop=name], h3[itemprop=name]")?.text()
            ?: this.selectFirst("meta[itemprop=name]")?.attr("content") ?: return null

        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null).replace("/v/", "/")
        val poster = (this.selectFirst("meta[itemprop=thumbnailUrl]")?.attr("content")
            ?: this.selectFirst("img")?.attr("src")).cleanPoster()

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
            this.posterHeaders = AnaHeaderlar
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}?page=$page"
        val response = app.get(url, headers = AnaHeaderlar).text
        val home = Jsoup.parse(response).select("article[itemtype*=VideoObject]").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home, hasNext = true)
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val apiUrl = "https://api.turkifsahub.com/ifsa-videos/client/search?searchTerm=$query&page=$page&limit=30"

        val apiRes = app.get(apiUrl, headers = AnaHeaderlar).text
        if (apiRes.trim().startsWith("{")) {
            val json = Json.parseToJsonElement(apiRes).jsonObject
            val results = json["data"]?.jsonArray?.mapNotNull { element ->
                val obj = element.jsonObject
                val slug = obj["slug"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                newMovieSearchResponse(
                    obj["title"]?.jsonPrimitive?.contentOrNull ?: "İsimsiz",
                    fixUrl(slug),
                    TvType.NSFW
                ) {
                    this.posterUrl = obj["thumbnail"]?.jsonPrimitive?.contentOrNull.cleanPoster()
                    this.posterHeaders = AnaHeaderlar
                }
            } ?: emptyList()

            if (results.isNotEmpty()) {
                val hasMore = json["pagination"]?.jsonObject?.get("hasMore")?.jsonPrimitive?.booleanOrNull ?: false
                return newSearchResponseList(results, hasNext = hasMore)
            }
        }

        val htmlDoc = Jsoup.parse(app.get("$mainUrl/search?searchTerm=$query&page=$page", headers = AnaHeaderlar).text)
        val htmlResults = htmlDoc.select("article[itemtype*=VideoObject]").mapNotNull { it.toSearchResult() }
        val hasNext = htmlDoc.selectFirst("a[href*='page=${page + 1}']") != null
        return newSearchResponseList(htmlResults, hasNext = hasNext)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(url, headers = AnaHeaderlar).text
        val document = Jsoup.parse(response)
        val title = document.selectFirst("h1")?.text() ?: return null
        val poster = (document.selectFirst("div.vjs-poster img")?.attr("src")
            ?: document.selectFirst("meta[itemprop=thumbnailUrl]")?.attr("content")).cleanPoster()
        val actorName = document.selectFirst("span[itemprop=actor] meta[itemprop=name]")?.attr("content")
            ?: document.selectFirst("span[itemprop=actor] a")?.text()
        val actors = listOfNotNull(actorName)
        val tags = document.select("nav[aria-label='Etiketler'] a span").map { it.text() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = document.selectFirst("meta[itemprop=description]")?.attr("content")?.trim()
            this.tags = tags
            addActors(actors)
            this.recommendations = emptyList()
            this.posterHeaders = AnaHeaderlar
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data, headers = AnaHeaderlar).text
        val document = Jsoup.parse(response)

        val videoUrl = document.selectFirst("video.vjs-tech")?.attr("src")
            ?: document.selectFirst("meta[itemprop=contentUrl]")?.attr("content")

        videoUrl?.let { link ->
            callback(
                newExtractorLink(
                    name = this.name,
                    source = this.name,
                    url = link,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.referer = "$mainUrl/"
                    this.quality = Qualities.P720.value
                }
            )
        }
        return true
    }
}