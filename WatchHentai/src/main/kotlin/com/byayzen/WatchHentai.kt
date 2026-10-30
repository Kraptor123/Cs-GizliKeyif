// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import android.net.Uri
import com.lagradost.api.Log
import org.json.JSONObject
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
    override val supportedTypes       = setOf(TvType.NSFW)
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

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = false
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.data h3 a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.data h3 a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newTvSeriesSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    private var nonce: String? = null

    private suspend fun getNonce(forceRefresh: Boolean = false): String? {
        if (!forceRefresh) {
            val current = nonce
            if (!current.isNullOrBlank()) return current
        }
        val homeHtml  = app.get(mainUrl).text
        val extracted = Regex(""""nonce"\s*:\s*"([a-zA-Z0-9]+)"""").find(homeHtml)?.groupValues?.get(1)
        nonce = extracted
        return extracted
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        var currentNonce = getNonce() ?: return newSearchResponseList(emptyList(), hasNext = false)
        var response     = app.get(
            url     = "$mainUrl/wp-json/dooplay/search/?keyword=$query&nonce=$currentNonce",
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Referer"          to "$mainUrl/",
                "User-Agent"       to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
            )
        )
        var jsonText = response.text
        if (jsonText.contains(""""error"""") && !jsonText.contains("no_posts")) {
            currentNonce = getNonce(forceRefresh = true) ?: return newSearchResponseList(emptyList(), hasNext = false)
            response     = app.get(
                url     = "$mainUrl/wp-json/dooplay/search/?keyword=$query&nonce=$currentNonce",
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Referer"          to "$mainUrl/",
                    "User-Agent"       to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                )
            )
            jsonText = response.text
        }

        if (jsonText.isBlank() || jsonText.contains("no_posts")) {
            return newSearchResponseList(emptyList(), hasNext = false)
        }

        val jsonObject = JSONObject(jsonText)
        val results    = jsonObject.keys().asSequence().mapNotNull { key ->
            val item      = jsonObject.optJSONObject(key) ?: return@mapNotNull null
            val title     = item.optString("title").trim().ifEmpty { return@mapNotNull null }
            val href      = fixUrlNull(item.optString("url").ifEmpty { return@mapNotNull null }) ?: return@mapNotNull null
            val posterUrl = fixUrlNull(item.optString("img").ifEmpty { null })

            newTvSeriesSearchResponse(title, href, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }.toList()

        return newSearchResponseList(results, hasNext = false)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        Log.d(name, "Load aşaması: $url")
        val document        = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.sgeneros a").map { it.text() }
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("article.item.tvshows").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }

        val episodes        = document.select("ul.episodios li").mapNotNull { li ->
            val epA         = li.selectFirst("a.tv-ep-card-link") ?: return@mapNotNull null
            val epUrl       = fixUrlNull(epA.attr("href")) ?: return@mapNotNull null
            val epTitle     = li.selectFirst("span.eptitle")?.text()?.trim()
            val epNum       = Regex("(\\d+)").find(epTitle ?: "")?.groupValues?.get(1)?.toIntOrNull()
            newEpisode(epUrl) {
                this.name    = epTitle
                this.episode = epNum
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
        val title     = this.selectFirst("div.data h3 a")?.text()?.trim() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.data h3 a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.poster img")?.let { img -> img.attr("data-src").ifEmpty { img.attr("src") } })
        return newTvSeriesSearchResponse(title, href, TvType.NSFW) {
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
        val metaHtml = app.get(metaUrl).text
        val videoUrl = Regex(""""contentUrl"\s*:\s*"(https?://[^"]+\.mp4)"""").find(metaHtml)?.groupValues?.get(1) ?: return false

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
