// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class AdultDeepFakes : MainAPI() {
    override var mainUrl              = "https://adultdeepfakes.com"
    override var name                 = "AdultDeepFakes"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/top-rated/"                                    to "Top Rated",
        "${mainUrl}/tags/passionate/"                              to "passionate",
        "${mainUrl}/tags/creampie/"                                to "creampie",
        "${mainUrl}/tags/fetish/"                                  to "fetish",
        "${mainUrl}/tags/nsfw/"                                    to "nsfw",
        "${mainUrl}/tags/foot/"                                    to "foot",
        "${mainUrl}/tags/sex/"                                     to "sex",
        "${mainUrl}/tags/missionary/"                              to "missionary",
        "${mainUrl}/tags/2-naked/"                                 to "naked",
        "${mainUrl}/tags/model/"                                   to "model",
        "${mainUrl}/tags/feet/"                                    to "feet",
        "${mainUrl}/tags/deepfake/"                                to "deepfake",
        "${mainUrl}/tags/porn/"                                    to "porn",
        "${mainUrl}/tags/fashion-model/"                           to "fashion",
        "${mainUrl}/tags/asian/"                                   to "asian",
        "${mainUrl}/tags/stockings/"                               to "stockings",
        "${mainUrl}/tags/deepfakekpop/"                            to "deepfakekpop",
        "${mainUrl}/tags/young/"                                   to "young",
        "${mainUrl}/tags/facial/"                                  to "facial",
        "${mainUrl}/tags/kpopdeepfake/"                            to "kpopdeepfake",
        "${mainUrl}/tags/pop-idol/"                                to "pop",
        "${mainUrl}/tags/fc323ac10f06036b367374c409769002/"        to "딥페이크",
        "${mainUrl}/tags/kpop/"                                    to "kpop",
        "${mainUrl}/tags/jidol/"                                   to "jidol",
        "${mainUrl}/tags/solo/"                                    to "solo",
        "${mainUrl}/tags/jakefakes/"                               to "jakefakes",
        "${mainUrl}/tags/hot/"                                     to "hot",
        "${mainUrl}/tags/american/"                                to "american",
        "${mainUrl}/tags/nogizaka/"                                to "nogizaka",
        "${mainUrl}/tags/straight-sex/"                            to "straight",
        "${mainUrl}/tags/nude/"                                    to "nude",
        "${mainUrl}/tags/scene/"                                   to "scene",
        "${mainUrl}/tags/81648136f80da41f036112879cfa5be1/"        to "乃木坂",
        "${mainUrl}/tags/feet-fetish/"                             to "feet",
        "${mainUrl}/tags/censored/"                                to "censored",
        "${mainUrl}/tags/avengers/"                                to "avengers",
        "${mainUrl}/tags/foot-fetish/"                             to "foot",
        "${mainUrl}/tags/fake/"                                    to "fake",
        "${mainUrl}/tags/korean/"                                  to "korean",
        "${mainUrl}/tags/blacked/"                                 to "blacked",
        "${mainUrl}/tags/actress/"                                 to "actress",
        "${mainUrl}/tags/bbc/"                                     to "bbc",
        "${mainUrl}/tags/50e81d7d3ded1e1b770419be096d3adc/"        to "김태연",
        "${mainUrl}/tags/indian/"                                  to "indian",
        "${mainUrl}/tags/46/"                                      to "乃木坂46",
        "${mainUrl}/tags/blowjob/"                                 to "blowjob",
        "${mainUrl}/tags/blonde/"                                  to "blonde",
        "${mainUrl}/tags/pov/"                                     to "pov",
        "${mainUrl}/tags/hardcore/"                                to "hardcore",
        "${mainUrl}/tags/uncensored/"                              to "uncensored",
        "${mainUrl}/tags/cumshot/"                                 to "cumshot",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?mode=async&function=get_block&block_id=list_videos_common_videos_list&sort_by=rating&items_per_page=24&from=$page", headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.5",
            "X-Requested-With" to "XMLHttpRequest",
            "Connection" to "keep-alive",
            "Referer" to "${mainUrl}/",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "same-origin",
            "Priority" to "u=0",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache"
        )).document

        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("strong.title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-webp"))
//        val trailer   = fixUrlNull(this.selectFirst("img")?.attr("data-preview"))
        val private   = this.selectFirst("span.ico-private")?.text() ?: ""

        if (private.contains("private",true)){
            return null
        }

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}/?mode=async&function=get_block&block_id=list_videos_videos_list_search_result&q=$query").document

        val aramaCevap = document.select("div.item").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("link[rel=preload]")?.attr("href"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.info div.item:containsOwn(tags) a , div.info div.item:containsOwn(categories) a").map { it.text() }
        val score          = document.selectFirst("span.voters")?.text()?.split(" ")[0]?.replace("%","")?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.item").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("div.info div.item:containsOwn(celebrities) a").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from100(score)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val pageHtml = app.get(data).text
        return KtPlayerExtractor.getLinks(name, mainUrl, data, pageHtml, callback = callback)
    }
}
