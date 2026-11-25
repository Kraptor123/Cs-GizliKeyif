// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class TurkHub : MainAPI() {
    override var mainUrl              = "https://turkhub6c.lat"
    override var name                 = "TurkHub"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/"                             to "TurkHub En Son",
        "${mainUrl}/kategori/18-yasinda-genc"     to "18 Yaşında / Genç",
        "${mainUrl}/kategori/amator"              to "Amatör",
        "${mainUrl}/kategori/turbanli"            to "Türbanlı",
        "${mainUrl}/kategori/konulu"              to "Konulu",
        "${mainUrl}/kategori/sahibe-kole"         to "Sahibe Köle",
        "${mainUrl}/kategori/webcam"              to "Webcam",
        "${mainUrl}/kategori/masturbation"        to "Masturbation",
        "${mainUrl}/kategori/sakso"               to "Sakso",
        "${mainUrl}/kategori/esmer"               to "Esmer",
        "${mainUrl}/kategori/sarisin"             to "Sarışın",
        "${mainUrl}/kategori/hard"                to "Hard",
        "${mainUrl}/kategori/milf"                to "Milf",
        "${mainUrl}/kategori/anal"                to "Anal",
        "${mainUrl}/kategori/grup"                to "Grup",
        "${mainUrl}/kategori/lezbiyen"            to "Lezbiyen",
        "${mainUrl}/kategori/dildo"               to "Dildo",
        "${mainUrl}/kategori/am-yalama"           to "Am Yalama",
        "${mainUrl}/kategori/sisman"              to "Şişman",
        "${mainUrl}/kategori/asmr"                to "ASMR",
        "${mainUrl}/kategori/travesti"            to "Travesti",
        "${mainUrl}/kategori/zenci"               to "Zenci",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}?sayfa=$page"
        val document = app.get(url).document

        val home = document.select("article.video-card").mapNotNull {
            it.toMainPageResult()
        }.distinctBy { it.url }

        return newHomePageResponse(request.name, home, hasNext = true)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val entrytitle = this.selectFirst("a.card-title")
        val title      = entrytitle?.attr("title")?.ifBlank { null } ?: entrytitle?.text()?.trim() ?: return null
        val href       = fixUrlNull(entrytitle?.attr("href")) ?: return null
        val posterurl  = fixUrlNull(this.selectFirst("a.card-thumb img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterurl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page == 1) "$mainUrl/ara?q=$query" else "$mainUrl/ara?q=$query&sayfa=$page"
        val document = app.get(url).document

        val aramacevap = document.select("article.video-card").mapNotNull {
            it.toMainPageResult()
        }.distinctBy { it.url }

        val hasnext = document.selectFirst("a[href*=sayfa=${page + 1}]") != null

        return newSearchResponseList(aramacevap, hasnext)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        // eski kayıtlı içerikler "href|poster" formatında
        val url      = url.substringBefore("|")
        val document = app.get(url).document

        val title  = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val plot   = document.selectFirst("meta[name=description]")?.attr("content")?.trim()
        val tags   = document.select("a.chip[href^=/kategori/]").map { it.text().trim() }
        val recs   = document.select("article.video-card").mapNotNull { it.toMainPageResult() }
            .filter { it.url != url }.distinctBy { it.url }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = plot
            this.tags            = tags
            this.recommendations = recs
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data.substringBefore("|")).document
        // og:video -> /v/<token>, 302 ile mp4 CDN adresine yönlendiriyor
        val gate = fixUrlNull(document.selectFirst("meta[property=og:video]")?.attr("content")) ?: return false
        val video = app.get(gate, referer = "$mainUrl/", allowRedirects = false).headers["location"] ?: gate

        callback(
            newExtractorLink(
                source = this.name,
                name   = this.name,
                url    = video,
                type   = ExtractorLinkType.VIDEO
            ) {
                this.referer = "$mainUrl/"
                this.quality = Qualities.Unknown.value
            }
        )
        return true
    }
}
