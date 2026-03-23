// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class XChina : MainAPI() {
    override var mainUrl = "https://en.xchina.co"
    override var name = "XChina"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded
    companion object {
        private var loadengel = 0
    }

    override val mainPage = mainPageOf(
        "$mainUrl/videos/series-6395aba3deb74.html" to "Censored AV (6853)",
        "$mainUrl/videos/series-5f904550b8fcc.html" to "Model Media (3558)",
        "$mainUrl/videos/series-6395ab7fee104.html" to "Uncensored AV (2356)",
        "$mainUrl/videos/series-61bf6e439fed6.html" to "Independent Creators (1933)",
        "$mainUrl/videos/series-63963186ae145.html" to "Pans Videos (1885)",
        "$mainUrl/videos/series-61014080dbfde.html" to "TXVLOG (1328)",
        "$mainUrl/videos/series-5fe8403919165.html" to "Peach Media (1107)",
        "$mainUrl/videos/series-6054e93356ded.html" to "Star Media (1006)",
        "$mainUrl/videos/series-60153c49058ce.html" to "Timi Media (780)",
        "$mainUrl/videos/series-5fe840718d665.html" to "91mv (627)"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else request.data.replace(".html", "/$page.html")
        val document = app.get(url).document
        val home = document.select("div.item.video").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home, true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("div.title a")?.text() ?: return null
        val href = fixUrl(selectFirst("div.title a")?.attr("href") ?: return null)
        val style = selectFirst("div.img")?.attr("style") ?: ""
        val poster = Regex("""url\(['"]?(.*?)['"]?\)""").find(style)?.groupValues?.get(1) ?: selectFirst("img")?.attr("src") ?: selectFirst("img")?.attr("data-src")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
            this.posterHeaders = mapOf("Referer" to mainUrl)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) "$mainUrl/videos/keyword-$query.html" else "$mainUrl/videos/keyword-$query/$page.html"
        val document = app.get(url).document
        val results = document.select("div.item.video").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(results, true)
    }

    override suspend fun load(url: String): LoadResponse? {
        loadengel++
        if (loadengel <= 3) {
            return null
        }
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")
        val volume = document.selectFirst("div.item:has(i.fa-hashtag) div.text")?.text()?.trim()
        val plot = document.selectFirst(".info-card.video-detail .item .text")?.text()?.trim()
        val tags = document.select(".info-card .item:has(i.fa-tags) .text, .info-card.video-detail .item .text a").map { it.text() }
        val actors = document.select(".model-item").map { Actor(it.text()) }
        val recommendations = document.select("div.item.video").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = if (volume != null) "Code: $volume\n\n$plot" else plot
            this.tags = tags
            this.recommendations = recommendations
            this.posterHeaders = mapOf("Referer" to mainUrl)
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val pageSource = app.get(data).text
        val m3u8 = Regex("""src:\s*['"](https?://video\.xchina\.download/m3u8/.*?\.m3u8.*?)['"]""").find(pageSource)?.groupValues?.get(1)

        m3u8?.let { link ->
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = link,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = mainUrl
                    this.quality = Qualities.Unknown.value
                    this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                }
            )
        }
        return true
    }
}