// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class SpankBang : MainAPI() {
    override var mainUrl              = "https://spankbang.com"
    override var name                 = "SpankBang"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    // Site Cloudflare arkasinda; challenge cikarsa WebView ile cozulur
    private val cloudflareKiller by lazy { CloudflareKiller() }

    override val mainPage = mainPageOf(
        "${mainUrl}/"               to "Recommended",
        "${mainUrl}/new_videos/"    to "New Videos",
        "${mainUrl}/most_popular/"  to "Most Popular",
        "${mainUrl}/s/blowjob/"     to "Blowjob",
        "${mainUrl}/s/cowgirl/"     to "Cowgirl",
        "${mainUrl}/s/doggy/"       to "Doggy",
        "${mainUrl}/s/missonary/"   to "Missionary",
        "${mainUrl}/s/licking/"     to "Licking",
        "${mainUrl}/s/japanese/"    to "Japanese",
        "${mainUrl}/s/big+tits/"    to "Big Tits",
        "${mainUrl}/s/asian/"       to "Asian",
        "${mainUrl}/s/hardcore/"    to "Hardcore",
        "${mainUrl}/s/jav/"         to "JAV",
        "${mainUrl}/s/spooning/"    to "Spooning",
        "${mainUrl}/s/asian+amateur/" to "Asian Amateur",
        "${mainUrl}/s/big+dick/"    to "Big Dick",
        "${mainUrl}/s/babe/"        to "Babe",
        "${mainUrl}/s/brunette/"    to "Brunette",
        "${mainUrl}/s/amateur/"     to "Amateur",
        "${mainUrl}/s/pov/"         to "POV",
        "${mainUrl}/s/milf/"        to "MILF"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val cleanUrl = request.data.trimEnd('/')
        val url      = if (cleanUrl == mainUrl) {
            if (page <= 1) "${mainUrl}/" else "${mainUrl}/${page}/"
        } else {
            if (page <= 1) "${cleanUrl}/" else "${cleanUrl}/${page}/"
        }

        val document = app.get(url, interceptor = cloudflareKiller).document
        val allFiltre = document.select("div[data-testid=video-list] > div[data-testid=video-item]:not(.hidden)")
        val tamFiltre = if (cleanUrl != mainUrl && page <= 1 && allFiltre.size > 8) allFiltre.drop(8) else allFiltre

        val items    = tamFiltre
            .distinctBy { it.attr("data-id").ifEmpty { it.selectFirst("a")?.attr("href").orEmpty() } }
            .mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = items,
                isHorizontalImages = true
            ),
            hasNext = items.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val cleanQuery = query.trim().replace(" ", "+")
        val url        = if (page <= 1) "${mainUrl}/s/${cleanQuery}/" else "${mainUrl}/s/${cleanQuery}/${page}/"
        val document   = app.get(url, interceptor = cloudflareKiller).document
        val allFiltre   = document.select("div[data-testid=video-list] > div[data-testid=video-item]:not(.hidden)")
        val tamFiltre   = if (page <= 1 && allFiltre.size > 8) allFiltre.drop(8) else allFiltre
        val items      = tamFiltre
            .distinctBy { it.attr("data-id").ifEmpty { it.selectFirst("a")?.attr("href").orEmpty() } }
            .mapNotNull { it.toSearchResult() }

        return items.toNewSearchResponseList()
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElement = this.selectFirst("a[href*=/video/]") ?: return null
        val href        = fixUrlNull(linkElement.attr("href").ifEmpty { return null }) ?: return null
        val title       = this.selectFirst("p a[title]")?.text()?.ifEmpty { return null } ?: return null
        val posterUrl   = fixUrlNull(this.selectFirst("picture img")?.attr("src")?.ifEmpty { return null })

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)


    override suspend fun load(url: String): LoadResponse? {
        val document        = app.get(url, interceptor = cloudflareKiller).document

        val title           = document.selectFirst("h1")?.text()?.trim()?.ifEmpty { null } ?: return null
        val posterUrl       = fixUrlNull(document.selectFirst("#player_cover_img")?.attr("src")?.ifEmpty { null })
        val year            = document.selectFirst("time[datetime]")?.attr("datetime")?.let { Regex("""\b(\d{4})\b""").find(it)?.groupValues?.get(1)?.toIntOrNull() }

        val actorList       = mutableListOf<Actor>()
        val tagList         = mutableListOf<String>()

        document.select("div[data-testid=video-tags] a").forEach { element ->
            val href = element.attr("href")
            val text = element.text().trim()
            if (text.isNotEmpty() && element.attr("data-testid") != "exclusive-tag") {
                if (href.contains("/pornstar/") || element.selectFirst("svg.i_icon-star") != null) {
                    actorList.add(Actor(text))
                } else if (!href.contains("/channel/")) {
                    tagList.add(text)
                }
            }
        }

        val recommendations = document.select("div.js-related-videos-bottom div[data-testid=video-item]")
            .distinctBy { it.attr("data-id").ifEmpty { it.selectFirst("a")?.attr("href").orEmpty() } }
            .mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = posterUrl
            this.year            = year
            this.tags            = tagList
            this.recommendations = recommendations
            addActors(actorList)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val linkElement = this.selectFirst("a[href*=/video/]") ?: return null
        val href        = fixUrlNull(linkElement.attr("href").ifEmpty { return null }) ?: return null
        val title       = this.selectFirst("p a[title]")?.text()?.ifEmpty { null }
            ?: linkElement.attr("title").ifEmpty { null }
            ?: return null

        val posterUrl   = fixUrlNull(this.selectFirst("picture img")?.attr("src")?.ifEmpty { null })

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data             : String,
        isCasting        : Boolean,
        subtitleCallback : (SubtitleFile) -> Unit,
        callback         : (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(name, "loadLinks data: $data")
        val document     = app.get(data, interceptor = cloudflareKiller).document
        val html         = document.html()
        var found        = false

        // stream_data icindeki dogrudan MP4 kaliteleri
        listOf("4k", "2160p", "1080p", "720p", "480p", "360p", "240p").forEach { q ->
            val qUrl = (Regex("""'$q'\s*:\s*\[\s*'([^']+)'""").find(html) ?: Regex(""""$q"\s*:\s*\[\s*"([^"]+)"""").find(html))
                ?.groupValues?.get(1)?.takeIf { it.isNotEmpty() && !it.contains(".m3u8") } ?: return@forEach
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name   = "${this.name} ${q.uppercase()}",
                    url    = qUrl,
                    type   = ExtractorLinkType.VIDEO
                ) {
                    this.referer = "${mainUrl}/"
                    this.quality = if (q == "4k") Qualities.P2160.value else getQualityFromName(q)
                }
            )
            found = true
        }

        val m3u8Url      = (Regex("""'m3u8'\s*:\s*\[\s*'([^']+)'""").find(html) ?: Regex(""""m3u8"\s*:\s*\[\s*"([^"]+)"""").find(html))
            ?.groupValues?.get(1)

        if (m3u8Url != null) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name   = this.name,
                    url    = m3u8Url,
                    type   = ExtractorLinkType.M3U8
                ) {
                    this.referer = "${mainUrl}/"
                }
            )
            found = true
        }

        return found
    }
}