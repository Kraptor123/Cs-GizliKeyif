// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class XChina : MainAPI() {
    override var mainUrl              = "https://en.xchina.co"
    override var name                 = "XChina"
    override val hasMainPage          = true
    override var lang                 = "zh"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "$mainUrl/video-series/4"  to "Censored JAV",
        "$mainUrl/video-series/5"  to "Uncensored JAV",
        "$mainUrl/video-series/21" to "Madou Media",
        "$mainUrl/video-series/8"  to "Banana Video",
        "$mainUrl/video-series/20" to "Tangxin Vlog",
        "$mainUrl/video-series/62" to "Model Photoshoot",
        "$mainUrl/video-series/44" to "Hot Dance",
        "$mainUrl/video-series/15" to "Faphouse",
        "$mainUrl/video-series/42" to "Jingdong Pictures",
        "$mainUrl/video-series/55" to "PANS"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url      = if (page <= 1) request.data else "${request.data.trimEnd('/')}/$page"
        val document = app.get(url).document
        val home     = document.select("div.grid a[href^='/video/'].group.block").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val href   = fixUrl(attr("href").ifEmpty { return null })
        val title  = selectFirst("span.line-clamp-2")?.text()?.trim()?.ifEmpty { null }
            ?: selectFirst("img")?.attr("alt")?.trim()?.ifEmpty { null }
            ?: return null
        val poster = fixUrlNull(selectFirst("img")?.attr("src")?.ifEmpty { null }
            ?: selectFirst("img")?.attr("data-src")?.ifEmpty { null })

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl     = poster
            this.posterHeaders = mapOf("Referer" to "$mainUrl/")
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url      = if (page <= 1) "$mainUrl/search?type=video&q=$query" else "$mainUrl/search?type=video&q=$query&page=$page"
        val document = app.get(url).document
        val results  = document.select("div.grid a[href^='/video/'].group.block").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(results, results.isNotEmpty())
    }

    override suspend fun load(url: String): LoadResponse? {
        val document        = app.get(url).document
        val title           = document.selectFirst("h1")?.ownText()?.trim()?.ifEmpty { null }
            ?: document.selectFirst("h1")?.text()?.trim()?.ifEmpty { null }
            ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content")?.ifEmpty { null }
            ?: document.selectFirst("div#video-player")?.attr("data-poster")?.ifEmpty { null })
        val tags            = document.select("a[href*='category'], a[href*='series']").map { it.text().trim() }.filter { it.isNotEmpty() }
        val recommendations = document.select("ul.space-y-3 a[href^='/video/']").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.tags            = tags
            this.recommendations = recommendations
            this.posterHeaders   = mapOf("Referer" to "$mainUrl/")
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document  = app.get(data).document
        val videoDiv  = document.selectFirst("div#video-player") ?: return false
        val dataSrc   = videoDiv.attr("data-src").ifEmpty { return false }
        val streamUrl = fixUrl(dataSrc)

        callback.invoke(
            newExtractorLink(
                source  = name,
                name    = name,
                url     = streamUrl,
                type    = ExtractorLinkType.M3U8
            ) {
                this.referer = "$mainUrl/"
                this.headers = mapOf(
                    "Origin"  to mainUrl,
                    "Referer" to "$mainUrl/"
                )
            }
        )
        return true
    }
}
