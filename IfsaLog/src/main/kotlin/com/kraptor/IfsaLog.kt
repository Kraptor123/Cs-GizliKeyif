// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class IfsaLog : MainAPI() {
    override var mainUrl              = "https://ifsalog4.club"
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
        val home     = document.select("div.magpal-grid-post").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title")?.substringAfter("to ") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.magpal-grid-post").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("span.singular-entry-title-inside a")?.text()?.trim() ?: return null
        val video    = fixUrlNull(document.selectFirst("video source, div.chs-iframe-container iframe")?.attr("src") ?: fixUrlNull(document.selectFirst("div.separator a")
            ?.attr("href"))).toString()
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = if (video.contains("blogger")) {
            "Bu Video Ne Yazık ki Oynatılamıyor!"
        }else {"18 Yaş ve Üzeri İçin Uygundur!"}
        val tags            = listOf("+18","IfsaLog")


        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val video    = fixUrlNull(document.selectFirst("video source, div.chs-iframe-container iframe")?.attr("src") ?: fixUrlNull(document.selectFirst("div.separator a")
            ?.attr("href"))).toString()
        Log.d("kraptor_$name", "video = ${video}")
        if (video.contains("gundemexpress")) {
            callback.invoke(newExtractorLink(
                "IfsaLog",
                "IfsaLog",
                video,
                type = ExtractorLinkType.VIDEO,
                {
                    this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
                    this.referer = "${mainUrl}/"
                }
            ))
        } else {
            loadExtractor(video, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }
}