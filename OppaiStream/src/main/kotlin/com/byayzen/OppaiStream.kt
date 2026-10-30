// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class OppaiStream : MainAPI() {
    override var mainUrl              = "https://oppai.stream"
    override var name                 = "OppaiStream"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "4k" to "4K",
        "3D" to "3D",
        "Ahegao" to "Ahegao",
        "Beach" to "Beach",
        "Blood" to "Blood",
        "Censored" to "Censored",
        "Comedy" to "Comedy",
        "Fantasy" to "Fantasy",
        "Filmed" to "Filmed",
        "Gore" to "Gore",
        "Harem" to "Harem",
        "Horror" to "Horror",
        "Incest" to "Incest",
        "Lactation" to "Lactation",
        "Mind Break" to "Mind Break",
        "Mind Control" to "Mind Control",
        "Monster" to "Monster",
        "POV" to "POV",
        "Plot" to "Plot",
        "Softcore" to "Softcore",
        "Tentacle" to "Tentacle",
        "Uncensored" to "Uncensored",
        "Vanilla" to "Vanilla",
        "Watersports" to "Watersports",
        "X-Ray" to "X-Ray"
    )

    private suspend fun searchApi(query: String = "", tag: String = "", page: Int = 1): SearchResponseList {
        val res = app.post(
            "$mainUrl/api/search.php",
            data = mapOf(
                "text"      to query,
                "tags"      to tag,
                "blacklist" to "",
                "studios"   to "",
                "order"     to "recent",
                "limit"     to "16",
                "page"      to page.toString(),
                "classes"   to "smaller-episode-gap"
            ),
            headers = mapOf(
                "Referer"          to "$mainUrl/search",
                "Origin"           to mainUrl,
                "X-Requested-With" to "XMLHttpRequest",
                "User-Agent"       to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
            )
        )
        val doc     = res.document
        val results = doc.select("a.episode").mapNotNull { it.toSearchResponse() }
        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val searchList = searchApi(tag = request.data, page = page)
        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = searchList.items,
                isHorizontalImages = true
            ),
            hasNext = searchList.hasNext
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        return searchApi(query = query, page = page)
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val rawTitle  = this.selectFirst("p.episode-title")?.text()?.trim()?.ifEmpty { null } ?: return null
        val epNum     = this.selectFirst("p.episode-number")?.text()?.trim() ?: ""
        val title     = if (epNum.isNotBlank()) "$rawTitle Episode $epNum" else rawTitle
        val href      = fixUrlNull(this.attr("href").ifEmpty { return null }) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img.cover")?.attr("data-src")?.ifEmpty { null }
                ?: this.selectFirst("img.cover")?.attr("src")?.ifEmpty { null }
                ?: this.selectFirst("noscript img")?.attr("src")?.ifEmpty { null }
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl     = posterUrl
            this.posterHeaders = mapOf("Referer" to "$mainUrl/")
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val document        = app.get(url).document
        val title           = document.selectFirst("h1#episode-title")?.text()?.trim()?.ifEmpty { null }
            ?: document.selectFirst("meta[property=og:title]")?.attr("content")?.trim()?.ifEmpty { null }
            ?: return null

        val poster          = fixUrlNull(
            document.selectFirst("meta[property=og:image]")?.attr("content")?.ifEmpty { null }
                ?: document.selectFirst("img.cover")?.attr("src")?.ifEmpty { null }
        )
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()?.ifEmpty { null }
            ?: document.selectFirst("article#episode-description p.s5")?.text()?.trim()?.ifEmpty { null }

        val yearText        = document.selectFirst("a#stats-released p")?.text()
        val year            = Regex("(\\d{4})").find(yearText ?: "")?.groupValues?.get(1)?.toIntOrNull()

        val tags            = document.select("div.wrap-genres a").map { it.text().trim() }
        val recommendations = document.select("div#related-wrap a.episode").mapNotNull {
            it.toSearchResponse()
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.posterHeaders   = mapOf("Referer" to "$mainUrl/")
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document    = app.get(data).document
        val scriptText  = document.select("script").map { it.data() }.firstOrNull { it.contains("var folder =") }
        val folder      = Regex("""var\s+folder\s*=\s*["']([^"']+)["']""").find(scriptText ?: "")?.groupValues?.get(1)
            ?: return false
        val storageBase = Regex("""var\s+storageBase\s*=\s*["']([^"']+)["']""").find(scriptText ?: "")?.groupValues?.get(1)
            ?.replace("\\/", "/") ?: "$mainUrl/videos"

        val manifestUrl = fixUrl("$storageBase/$folder/dash/manifest.mpd")

        document.select("video#episode track, video track").forEach { track ->
            val src  = fixUrlNull(track.attr("src").ifEmpty { return@forEach }) ?: return@forEach
            val lang = track.attr("label").ifBlank { "English" }
            subtitleCallback(newSubtitleFile(lang = lang, url = src))
        }

        callback(
            newExtractorLink(
                source = name,
                name   = name,
                url    = manifestUrl,
                type   = ExtractorLinkType.DASH
            ) {
                this.referer = "$mainUrl/"
                this.headers = mapOf(
                    "Referer"    to "$mainUrl/",
                    "Origin"     to mainUrl,
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                )
            }
        )
        return true
    }
}
