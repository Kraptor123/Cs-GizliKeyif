// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class TurkifsaBlog : MainAPI() {
    override var mainUrl        = "https://turkifsa.blog"
    override var name           = "TurkifsaBlog"
    override val hasMainPage    = true
    override var lang           = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus      = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "$mainUrl/"                    to "Yeni Eklenenler",
        "$mainUrl/category/turk-ifsa" to "Türk İfşa",
        "$mainUrl/category/amator"    to "Amatör",
        "$mainUrl/category/onlyfans"  to "OnlyFans",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url      = if (page == 1) request.data else "${request.data.trimEnd('/')}/$page"
        Log.d("ayzen_$name", "getMainPage url = $url")
        val document = app.get(url).document
        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }
        Log.d("ayzen_$name", "getMainPage itemCount = ${home.size}")

        return newHomePageResponse(
            list    = HomePageList(request.name, home, true),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.item_title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a.item-link")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.image")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url         = "$mainUrl/search/${query.replace(" ", "-")}/page/$page/"
        val document    = app.get(url).document
        val searchItems = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(searchItems, hasNext = searchItems.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val document    = app.get(url).document
        val titleEl     = document.selectFirst("h1.ish1") ?: return null
        val title       = titleEl.ownText().trim().ifEmpty { return null }
        val posterUrl   = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")

        val recommendations = document.select("aside.video-sidebar div.item").mapNotNull { it.toMainPageResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = posterUrl
            this.plot            = description
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("ayzen_$name", "data = $data")

        val document   = app.get(data).document
        val iframeSrc  = document.selectFirst("div.responsive-iframe iframe")?.attr("src")
            ?.ifEmpty { return false } ?: return false

        val iframeUrl  = fixUrl(iframeSrc)
        Log.d("ayzen_$name", "iframe = $iframeUrl")

        val iframeHtml = app.get(iframeUrl).text
        val mp4Url     = Regex("""providedUrl\s*=\s*'([^']+)'""").find(iframeHtml)?.groupValues?.get(1)
            ?: return false

        Log.d("ayzen_$name", "mp4 = $mp4Url")

        callback.invoke(
            newExtractorLink(
                source = name,
                name   = name,
                url    = mp4Url,
                type   = ExtractorLinkType.VIDEO,
            )
        )
        return true
    }
}