// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import android.net.Uri
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class WatchHentai : MainAPI() {
    override var mainUrl              = "https://watchhentai.net"
    override var name                 = "WatchHentai"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/genre/3d/" to "3D",
        "${mainUrl}/genre/ahegao/" to "Ahegao",
        "${mainUrl}/genre/anal/" to "Anal",
        "${mainUrl}/genre/blackmail/" to "Blackmail",
        "${mainUrl}/genre/blowjob/" to "Blowjob",
        "${mainUrl}/genre/bondage/" to "Bondage",
        "${mainUrl}/genre/creampie/" to "Creampie",
        "${mainUrl}/genre/cosplay/" to "Cosplay",
        "${mainUrl}/genre/dubbed/" to "Dubbed Hentai",
        "${mainUrl}/genre/dark-skin/" to "Dark Skin",
        "${mainUrl}/genre/deepthroat/" to "DeepThroat",
        "${mainUrl}/genre/femdom/" to "Femdom",
        "${mainUrl}/genre/harem/" to "Harem",
        "${mainUrl}/genre/horny-slut/" to "Horny Slut",
        "${mainUrl}/genre/incest/" to "Incest",
        "${mainUrl}/genre/lolicon/" to "Lolicon",
        "${mainUrl}/genre/large-breasts/" to "Large Breasts",
        "${mainUrl}/genre/ntr/" to "NTR",
        "${mainUrl}/genre/public-sex/" to "Public Sex",
        "${mainUrl}/genre/rape/" to "Rape",
        "${mainUrl}/genre/school-girls/" to "School Girls",
        "${mainUrl}/genre/tits-fuck/" to "Tits Fuck",
        "${mainUrl}/genre/uncensored/" to "Uncensored",
        "${mainUrl}/genre/virgins/" to "Virgins",
        "${mainUrl}/genre/x-ray/" to "X-Ray",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url      = if (page <= 1) "${request.data}" else "${request.data}page/$page/"
        val document = app.get(url).document
        val home     = document.select("div.items article").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(list = HomePageList(request.name, home, false))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.data h3 a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.data h3 a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url      = if (page == 1) "${mainUrl}/?s=${query}" else "${mainUrl}/page/$page/?s=${query}"
        val document = app.get(url).document
        val results  = document.select("div.result-item").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(results, hasNext = document.selectFirst("#nextpagination") != null)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.title a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.title a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newTvSeriesSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)


    override suspend fun load(url: String): LoadResponse? {
        Log.d(name, "Load aşaması: $url")
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.sgeneros a").map { it.text() }
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text())
        }

        val episodes = document.select("ul.episodios li").mapNotNull { li ->
            val epiA     = li.selectFirst("div.episodiotitle a") ?: return@mapNotNull null
            val epUrl = fixUrlNull(epiA.attr("href")) ?: return@mapNotNull null
            newEpisode(epUrl) {
            }
        }
        return newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))
        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(tag, "loadLinks data: $data")
        val document = app.get(data).document
        val metaUrl  = document.selectFirst("meta[itemprop=contentUrl]")?.attr("content")?.ifEmpty { null } ?: return false
        Log.d(tag, "metaUrl: $metaUrl")
        val jwHtml   = app.get(metaUrl).text.replace("\\/", "/")
        val videoUrl = Regex("""["']?file["']?\s*:\s*["'](https?://[^"']+\.mp4)["']""").find(jwHtml)?.groupValues?.get(1) ?: return false

        callback.invoke(
            newExtractorLink(
                source      = this.name,
                name        = this.name,
                url         = videoUrl,
                type        = ExtractorLinkType.VIDEO
            ) {
                this.referer = "$mainUrl/"
            }
        )

        return true
    }
}