// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

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
        "${mainUrl}/kategori/masaj"               to "Masaj",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document
        val home     = document.select("a.video-card-hover").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = false
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3")?.text() ?: return null
        val rawHref   = this.attr("href").ifEmpty { this.selectFirst("a")?.attr("href") }
        val href      = fixUrlNull(rawHref) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val apiResponse = app.get(
            "${mainUrl}/api/search?q=$query&sort=relevant",
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:158.0) Gecko/20100101 Firefox/158.0",
                "Accept" to "application/json",
                "Referer" to mainUrl,
            )
        ).parsedSafe<SearchApiResponse>()

        val directVideos = apiResponse?.videos?.mapNotNull { video ->
            val title     = video.title ?: return@mapNotNull null
            val slug      = video.slug ?: return@mapNotNull null
            val videoHref = "$mainUrl/shorts/$slug"
            val posterUrl = fixUrlNull(video.thumbnailUrl)

            newMovieSearchResponse(title, videoHref, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }.orEmpty()

        val starVideos = apiResponse?.stars?.mapNotNull { star ->
            val slug    = star.slug ?: return@mapNotNull null
            val starUrl = "$mainUrl/star/$slug"
            runCatching {
                val document = app.get(starUrl).document
                document.select("a.video-card-hover").mapNotNull { it.toMainPageResult() }
            }.getOrNull()?.distinctBy { it.url }
        }?.flatten().orEmpty()

        val aramaCevap = (directVideos + starVideos).distinctBy { it.url }

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
        Log.d("kraptor_$name", "data = $data")
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

private data class SearchStar(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    @JsonProperty("avatar_url") val avatarUrl: String? = null,
    @JsonProperty("cover_url") val coverUrl: String? = null,
)

private data class SearchVideo(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    @JsonProperty("thumbnail_url") val thumbnailUrl: String? = null,
)

private data class SearchApiResponse(
    @JsonProperty("videos") val videos: List<SearchVideo>? = null,
    @JsonProperty("stars") val stars: List<SearchStar>? = null,
)
