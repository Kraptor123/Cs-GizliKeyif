// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Youperv : MainAPI() {
    override var mainUrl = "https://youperv.com"
    override var name = "Youperv"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Home",
        "${mainUrl}/top-50-most-viewed-videos.html" to "Most Viewed 30 Day",
        "${mainUrl}/top-porn-videos.html" to "Top Rated 30 Day",
        "${mainUrl}/tags/" to "TAGS",
        "${mainUrl}/anal/" to "Anal",
        "${mainUrl}/amateur/" to "Amateur",
        "${mainUrl}/anal-creampie/" to "Anal Creampie",
        "${mainUrl}/bathroom/" to "Bathroom",
        "${mainUrl}/big-dick/" to "Big Dick",
        "${mainUrl}/big-tits/" to "Big Tits",
        "${mainUrl}/beautiful-girl/" to "Beautiful Girl",
        "${mainUrl}/beautiful-porn/" to "Beautiful porn",
        "${mainUrl}/brunette/" to "Brunette",
        "${mainUrl}/blonde/" to "Blonde",
        "${mainUrl}/creampie/" to "Creampie",
        "${mainUrl}/cuckold/" to "Cuckold",
        "${mainUrl}/cumshot/" to "Cumshot",
        "${mainUrl}/female-orgasm/" to "Female Orgasm",
        "${mainUrl}/handjob/" to "Handjob",
        "${mainUrl}/high-heels/" to "High Heels",
        "${mainUrl}/interracial/" to "Interracial",
        "${mainUrl}/juicy-ass/" to "Juicy Ass",
        "${mainUrl}/kitchen/" to "Kitchen",
        "${mainUrl}/lesbian/" to "Lesbian",
        "${mainUrl}/masturbation/" to "Masturbation",
        "${mainUrl}/mature-mom/" to "Mom",
        "${mainUrl}/milf/" to "Milf",
        "${mainUrl}/office/" to "Office",
        "${mainUrl}/pov/" to "POV",
        "${mainUrl}/red-head/" to "Red Head",
        "${mainUrl}/russian/" to "Russian",
        "${mainUrl}/small-tits/" to "Small Tits",
        "${mainUrl}/stockings/" to "Stockings",
        "${mainUrl}/story/" to "Story",
        "${mainUrl}/teacher/" to "Teacher",
        "${mainUrl}/threesome/" to "Threesome",
        "${mainUrl}/young-girl/" to "Young Girl"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            "${request.data.removeSuffix("/")}/page/$page/"
        }

        val document = app.get(url).document
        val home = document.select("div.items div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst(".item-title h2")?.text()?.split(" (")?.firstOrNull()?.trim()
            ?: return null
        val href = fixUrlNull(this.selectFirst("a.item-link")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.poster")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/index.php?do=search&subaction=search&story=$query"
        } else {
            "$mainUrl/index.php?do=search&subaction=search&search_start=$page&full_search=0&story=$query"
        }

        val response = app.get(url).document
        val results = response.select("div.item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(results)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.split(" 11.")?.firstOrNull()?.trim()
            ?: document.selectFirst("h1")?.text()?.trim() ?: return null

        val videoSource = document.selectFirst("video source")?.attr("src")
            ?: document.selectFirst("video")?.attr("src")

        val finalVideoUrl = fixUrlNull(videoSource)?.replace(" ", "%20")

        val poster = fixUrlNull(document.selectFirst("video")?.attr("poster"))
            ?: fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = document.selectFirst(".f-desc")?.text()?.trim()
        val tags = document.select(".full-tags a").map { it.text() }
        val duration = document.selectFirst(".fm-item i.fa-clock-o")?.parent()?.text()
            ?.trim()?.split(":")?.firstOrNull()?.toIntOrNull()

        val recommendations =
            document.select("div.items div.item").mapNotNull { it.toRecommendationResult() }
        val actors =
            document.select(".fm-item a[href*='/xfsearch/pornstar/']").map { Actor(it.text()) }

        return newMovieLoadResponse(
            title,
            url,
            TvType.NSFW,
            finalVideoUrl ?: url
        ) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst(".item-title h2")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a.item-link")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.poster")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override val instantLinkLoading: Boolean = true

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("ByAyzen_$name", "Data: $data")

        if (data.contains(".mp4") || data.contains(".m3u8") ) {
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = data,
                    type = INFER_TYPE
                ) {
                    this.referer = mainUrl
                    this.quality = Qualities.Unknown.value
                }
            )
            return true
        }
        return false
    }
}