// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class IfsaLog : MainAPI() {
    override var mainUrl              = "https://turkifsaalemivip1.blog"
    override var name                 = "IfsaLog"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/"      to "Ana Sayfa",
        "${mainUrl}/category/z-kusagi/"       to "Z Kuşağı",
        "${mainUrl}/category/turk-ifsa/"      to "Türk İfşa",
        "${mainUrl}/category/universiteli/"   to "üniversiteli",
        "${mainUrl}/category/videolar/"       to "videolar",
        "${mainUrl}/category/genc/"           to "genç",
        "${mainUrl}/category/turbanli/"       to "türbanlı",
        "${mainUrl}/category/tango/"          to "Tango",
        "${mainUrl}/category/onlyfans-porno/" to "Onlyfans Porno",
        "${mainUrl}/category/turbanli-ifsa/"  to "Türbanlı İfşa",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page/").document
        val home     = document.select("article.item-list").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("article.item-list").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val recommendations = document.select("a.proseo-related__card").mapNotNull { it.toMainPageResult() }

         return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val video    = fixUrlNull(document.selectFirst("source, iframe")?.attr("src")) ?: ""
        if (video.contains("blogger")){
            loadExtractor(video, subtitleCallback, callback)
        } else {
            callback.invoke(newExtractorLink(
                "IfsaLog",
                "IfsaLog",
                video,
                type = INFER_TYPE,
                {
                    this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
                    this.referer = "${mainUrl}/"
                }
            ))
        }
        return true
    }
}