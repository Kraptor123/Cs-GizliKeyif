// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Girlsswallowed : MainAPI() {
    override var mainUrl              = "https://girlswallowed.com/"
    override var name                 = "Girlswallowed"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Home",
        "${mainUrl}/anal/" to "Anal",
        "${mainUrl}/anal-duos/" to "Anal Duos",
        "${mainUrl}/anal-fluffers/" to "Anal Fluffers",
        "${mainUrl}/anal-gapes/" to "Anal Gapes",
        "${mainUrl}/anal-threesome/" to "Anal Threesome",
        "${mainUrl}/asian/" to "Asian",
        "${mainUrl}/atm-scenes/" to "ATM Scenes",
        "${mainUrl}/babes/" to "Babes",
        "${mainUrl}/ball-sucking/" to "Ball Sucking",
        "${mainUrl}/big-ass/" to "Big Ass",
        "${mainUrl}/big-load-swallow/" to "Big Load Swallow",
        "${mainUrl}/big-tits/" to "Big Tits",
        "${mainUrl}/blonde/" to "Blonde",
        "${mainUrl}/blowbang/" to "Blowbang",
        "${mainUrl}/blowjob/" to "Blowjob",
        "${mainUrl}/brunette/" to "Brunette",
        "${mainUrl}/busty/" to "Busty",
        "${mainUrl}/chocolatebjs/" to "ChocolateBJs",
        "${mainUrl}/cowgirl/" to "Cowgirl",
        "${mainUrl}/creampie/" to "Creampie",
        "${mainUrl}/cum/" to "Cum",
        "${mainUrl}/cum-in-mouth/" to "Cum in Mouth",
        "${mainUrl}/cum-swap/" to "Cum Swap",
        "${mainUrl}/cumshot/" to "Cumshot",
        "${mainUrl}/deepthroat/" to "Deepthroat",
        "${mainUrl}/doggy-style/" to "Doggy Style",
        "${mainUrl}/double-blowjob/" to "Double Blowjob",
        "${mainUrl}/drool-swallow/" to "Drool Swallow",
        "${mainUrl}/eating-ass/" to "Eating Ass",
        "${mainUrl}/ebony/" to "Ebony",
        "${mainUrl}/extreme-throat-fuck/" to "Extreme Throat Fuck",
        "${mainUrl}/facefuck/" to "Face Fuck",
        "${mainUrl}/facial/" to "Facial",
        "${mainUrl}/gagging/" to "Gagging",
        "${mainUrl}/kisses/" to "Kisses",
        "${mainUrl}/latina/" to "Latina",
        "${mainUrl}/milf-deep-throat/" to "MILF Deep Throat",
        "${mainUrl}/pair-of-deepthroats/" to "Pair of Deepthroats",
        "${mainUrl}/petite-tits/" to "Petite Tits",
        "${mainUrl}/prolapse-play/" to "Prolapse Play",
        "${mainUrl}/pussy-fuck/" to "Pussy Fuck",
        "${mainUrl}/pussy-licking/" to "Pussy Licking",
        "${mainUrl}/redhead/" to "Redhead",
        "${mainUrl}/rimming/" to "Rimming",
        "${mainUrl}/slim-girls/" to "Slim Girls",
        "${mainUrl}/sloppy-blowjob/" to "Sloppy Blowjob",
        "${mainUrl}/small-butts/" to "Small Butts",
        "${mainUrl}/swallow-load/" to "Swallow Load",
        "${mainUrl}/tattoo/" to "Tattoo",
        "${mainUrl}/threesome/" to "Threesome",
        "${mainUrl}/throatsluts/" to "Throatsluts",
        "${mainUrl}/tits-fucking/" to "Tits Fucking",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            "${request.data.removeSuffix("/")}/page/$page/"
        }
        val document = app.get(url).document
        val home = document.select("#primary article, #main article").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(name = request.name, list = home, isHorizontalImages = true
            ), hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val a = this.selectFirst("a") ?: return null
        val title = a.attr("title").ifEmpty { this.selectFirst("h2, h3")?.text() } ?: return null
        val href = fixUrlNull(a.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        val description = document.selectFirst("meta[property='og:description']")?.attr("content")?.trim()

        val tags = document.select("span.cat-links a, span.tags-links a").map { it.text() }
        val recommendations = document.select("div.under-video-block article").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("span.actors-links a").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val a = this.selectFirst("a") ?: return null
        val title = a.attr("title").ifEmpty { this.selectFirst("header.entry-header span")?.text() } ?: return null
        val href = fixUrlNull(a.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "data = $data")
        val document = app.get(data).document

        document.select(".responsive-player iframe, h2 a").forEach {
            val link = fixUrlNull(it.attr("src").ifEmpty { it.attr("href") })
            if (link != null) {
                Log.d("kraptor_$name", "Wow!: $link")
                loadExtractor(link, "$mainUrl/", subtitleCallback, callback)
            }
        }

        return true
    }
}