// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class TurkIfsalar : MainAPI() {
    override var mainUrl              = "https://turkifsalar2.site"
    override var name                 = "TurkIfsalar"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/kategori/turk-ifsa"           to "Türk İfşa",
        "${mainUrl}/kategori/amator"              to "Amatör",
        "${mainUrl}/kategori/onlyfans"            to "Onlyfans",
        "${mainUrl}/kategori/fenomen-ifsa"        to "Fenomen İfşa",
        "${mainUrl}/kategori/tango-canli-yayin"   to "Tango & Canlı Yayın",
        "${mainUrl}/kategori/genc"                to "Genç",
        "${mainUrl}/kategori/turbanli"            to "Türbanlı",
        "${mainUrl}/kategori/turk-cift"           to "Türk Çift",
        "${mainUrl}/kategori/ev-yapimi"           to "Ev Yapımı",
        "${mainUrl}/kategori/ev-hanimi"           to "Ev Hanımı",
        "${mainUrl}/kategori/solo"                to "Solo",
        "${mainUrl}/kategori/milf"                to "Milf",
        "${mainUrl}/kategori/olgun"               to "Olgun",
        "${mainUrl}/kategori/buyuk-kalca"         to "Büyük Kalça",
        "${mainUrl}/kategori/buyuk-gogus"         to "Büyük Göğüs",
        "${mainUrl}/kategori/esmer"               to "Esmer",
        "${mainUrl}/kategori/sarisin"             to "Sarışın",
        "${mainUrl}/kategori/dolgun"              to "Dolgun",
        "${mainUrl}/kategori/sakso"               to "Sakso",
        "${mainUrl}/kategori/anal"                to "Anal",
        "${mainUrl}/kategori/hardcore"            to "Hardcore",
        "${mainUrl}/kategori/masturbasyon"        to "Masturbasyon",
        "${mainUrl}/kategori/lezbiyen"            to "Lezbiyen",
        "${mainUrl}/kategori/cuckold"             to "Cuckold",
        "${mainUrl}/kategori/hotwife"             to "Hotwife",
        "${mainUrl}/kategori/bbc"                 to "BBC",
        "${mainUrl}/kategori/aldatan"             to "Aldatan",
        "${mainUrl}/kategori/swinger"             to "Swinger",
        "${mainUrl}/kategori/gizli-cekim"         to "Gizli Çekim",
        "${mainUrl}/kategori/masaj"               to "Masaj",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}").document
        val home     = document.select("a.video-card-hover").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home, false)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val jsonResponse = app.get("${mainUrl}/api/search?q=$query&sort=relevant", headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0.1 Safari/605.1.15",
            "Accept" to "application/json",
            "Referer" to mainUrl,
        )).text

        val videoRegex = Regex("""\{"id":"[^"]+","title":"([^"]+)","slug":"([^"]+)"[^}]+?"thumbnail_url":"([^"]+)"""")

        val aramaCevap = videoRegex.findAll(jsonResponse).map { match ->
            val title = match.groupValues[1]
            val slug = match.groupValues[2]
            val posterUrl = match.groupValues[3].replace("\\/", "/")
            val videoHref = "$mainUrl/shorts/$slug"

            newMovieSearchResponse(title, videoHref, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }.toList()

        return newSearchResponseList(aramaCevap, hasNext = false)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(url)
        val htmlText = response.text

        val document = response.document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content") ?: return null
        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")

        val videoRegex = Regex("""\\"title\\":\\"([^\\"]+)\\",\\"slug\\":\\"([^\\"]+)\\"(?:,\\"description\\":\\"([^\\"]*)\\")?.*?\\"thumbnail_url\\":\\"([^\\"]+)\\"""")

        val recommendations = videoRegex.findAll(htmlText).mapNotNull { match ->
            val recTitle = match.groupValues[1]
            val recSlug  = match.groupValues[2]
            val recThumb = match.groupValues[4].replace("\\u0026", "&")

            if (url.contains(recSlug)) return@mapNotNull null

            val videoUrl = "$mainUrl/shorts/$recSlug"
            newMovieSearchResponse(recTitle, videoUrl, TvType.NSFW) {
                this.posterUrl = recThumb
            }
        }.toList().distinctBy { it.url }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val video = fixUrlNull(document.selectFirst("meta[property=og:video]")?.attr("content")).toString()

        callback.invoke(newExtractorLink(
            source = this.name,
            name = this.name,
            url = video,
            type = ExtractorLinkType.M3U8,
            initializer = {
                this.referer = mainUrl
            }
        ))

        return true
    }
}