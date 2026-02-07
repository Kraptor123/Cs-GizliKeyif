// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Fyptt : MainAPI() {
    override var mainUrl = "https://fyptt.to"
    override var name = "Fyptt"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/tiktok-nudes/" to "Nudes",
        "${mainUrl}/tiktok-porn/" to "TikTok",
        "${mainUrl}/tiktok-boobs/" to "Boobs",
        "${mainUrl}/instagram-porn/" to "Instagram",
        "${mainUrl}/tiktok-sex/" to "Sex",
        "${mainUrl}/nsfw-tiktok/" to "NSFW",
        "${mainUrl}/tiktok-xxx/" to "XXX",
        "${mainUrl}/tiktok-ass/" to "Ass",
        "${mainUrl}/tiktok-pussy/" to "Pussy",
        "${mainUrl}/tiktok-live/" to "Live",
        "${mainUrl}/sexy-tiktok/" to "Sexy",
        "${mainUrl}/tiktok-thots/" to "Thots"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val base = request.data.trimEnd('/')
        val url = if (page > 1) "$base/page/$page/" else base
        val response = app.get(url)

        val home =
            response.document.select("div.fl-post-column").mapNotNull { it.toMainPageResult() }
        val hasNext =
            response.document.selectFirst(".next.page-numbers, a.next, .pagination .next") != null
        return newHomePageResponse(request.name, home, hasNext = hasNext)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = this.selectFirst(".fl-post-grid-image a") ?: return null
        val title = anchor.attr("title").ifBlank { anchor.text() }.ifBlank { return null }
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val q = query.trim()
        val url = if (page > 1) "${mainUrl}/page/$page/?s=$q" else "${mainUrl}/?s=$q"
        val response = app.get(url)
        val results =
            response.document.select("div.fl-post-column").mapNotNull { it.toMainPageResult() }
        val hasNext =
            response.document.selectFirst(".next.page-numbers, a.next, .pagination .next") != null
        return newSearchResponseList(results, hasNext = hasNext)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    private var cerez: String? = null
    private val useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0"

    private fun cookieal(referans: String? = null): Map<String, String> {
        return mutableMapOf(
            "User-Agent" to useragent,
            "Cookie" to (cerez ?: "")
        ).apply {
            referans?.let { put("Referer", it) }
        }
    }

    private fun cerezitemizle(adres: String, donencerezler: Map<String, String>) {
        val yol = "/" + adres.substringAfter("fyptt.to/").substringBeforeLast("/") + "/"
        cerez = buildString {
            append("domain=fyptt.to; path=$yol; ")
            donencerezler.filter { it.key != "domain" && it.key != "path" }
                .forEach { append("${it.key}=${it.value}; ") }
        }.removeSuffix("; ")
    }

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(url, headers = cookieal())
        cerezitemizle(url, response.cookies)

        val document = response.document
        val title = document.select("h1").firstOrNull()?.text()?.trim() ?: return null

        val recommendations = document.select("div.fl-post-column").mapNotNull { element ->
            val recTitle = element.select("a img").firstOrNull()?.attr("alt") ?: return@mapNotNull null
            val recHref = fixUrlNull(element.select("a").firstOrNull()?.attr("href")) ?: return@mapNotNull null
            newMovieSearchResponse(recTitle, recHref, TvType.NSFW) {
                this.posterUrl = fixUrlNull(element.select("img").firstOrNull()?.attr("src"))
            }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = fixUrlNull(document.select("meta[property=og:image]").firstOrNull()?.attr("content"))
            this.plot = document.select("meta[property=og:description]").firstOrNull()?.attr("content")?.trim()
            this.tags = document.select("div.fl-html .entry-category a").map { it.text().trim() }
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        suspend fun linkbul(adres: String, secici: String, ref: String? = null): String? {
            return app.get(adres, headers = cookieal(ref)).document
                .select(secici).firstOrNull()?.attr("src")
        }

        val anaurl = data
        val iframeadresi = linkbul(anaurl, "iframe[src*='fyptt']") ?: return false
        val videolinki = linkbul(iframeadresi, "video-js source, video source, video, source", anaurl) ?: return false

        newExtractorLink(
            source = name,
            name = name,
            url = videolinki,
        ) {
            this.referer = anaurl
            this.headers = mapOf("Cookie" to (cerez ?: ""))
            this.type = ExtractorLinkType.VIDEO
        }.let { callback(it) }

        return true
    }
}