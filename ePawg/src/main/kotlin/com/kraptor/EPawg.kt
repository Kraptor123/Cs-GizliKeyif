// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class EPawg : MainAPI() {
    override var mainUrl = "https://epawg.com"
    override var name = "EPawg"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/top-rated/"         to "Top Rated",
        "${mainUrl}/most-popular/"                  to "Most Popular",
        "${mainUrl}/categories/manyvids/"           to "ManyVids",
        "${mainUrl}/categories/double-penetration/" to "Double Penetration",
        "${mainUrl}/categories/anal/"               to "Anal",
        "${mainUrl}/categories/squirt/"             to "Squirt",
        "${mainUrl}/categories/anal-play/"          to "Anal Play",
        "${mainUrl}/categories/tattooed/"           to "Tattooed",
//        "${mainUrl}/categories/dirty-talk/"         to "Dirty Talk",
        "${mainUrl}/categories/asian/"              to "Asian",
        "${mainUrl}/categories/oil/"                to "Oil",
        "${mainUrl}/categories/hardcore/"           to "Hardcore",
        "${mainUrl}/categories/hairy/"              to "Hairy",
//        "${mainUrl}/categories/asmr/"               to "ASMR",
//        "${mainUrl}/categories/youtube/"            to "YouTube",
        "${mainUrl}/categories/twerk/"              to "Twerk",
        "${mainUrl}/categories/strip-tease/"        to "Strip Tease",
        "${mainUrl}/categories/oral/"               to "Oral",
//        "${mainUrl}/categories/cowgirl/"            to "Cowgirl",
//        "${mainUrl}/categories/facial/"             to "Facial",
//        "${mainUrl}/categories/kink/"               to "Kink",
//        "${mainUrl}/categories/lesbian/"            to "Lesbian",
        "${mainUrl}/categories/onlyfans/"           to "OnlyFans",
//        "${mainUrl}/categories/interracial/"        to "Interracial",
//        "${mainUrl}/categories/deepthroat/"         to "Deepthroat",
//        "${mainUrl}/categories/boy-girl/"           to "Boy Girl",
        "${mainUrl}/categories/small-breast/"       to "Small Breast",
//        "${mainUrl}/categories/finger-fucking/"     to "Finger Fucking",
//        "${mainUrl}/categories/solo-female/"        to "Solo Female",
//        "${mainUrl}/categories/doggystyle/"         to "Doggystyle",
        "${mainUrl}/categories/public/"             to "Public",
//        "${mainUrl}/categories/bbc/"                to "BBC",
//        "${mainUrl}/categories/gangbang/"           to "Gangbang",
//        "${mainUrl}/categories/blowjob/"            to "Blowjob",
//        "${mainUrl}/categories/softcore/"           to "Softcore",
//        "${mainUrl}/categories/hitachi/"            to "Hitachi",
//        "${mainUrl}/categories/69/"                 to "69",
//        "${mainUrl}/categories/threesome/"          to "Threesome",
//        "${mainUrl}/categories/group-sex/"          to "Group sex",
//        "${mainUrl}/categories/girl-girl/"          to "Girl Girl",
//        "${mainUrl}/categories/snapchat/"           to "Snapchat",
//        "${mainUrl}/categories/bukkake/"            to "Bukkake",
//        "${mainUrl}/categories/pole-dance/"         to "Pole Dance",
        "${mainUrl}/categories/big-tits/"           to "Big Tits",
//        "${mainUrl}/categories/instagram/"          to "Instagram",
        "${mainUrl}/categories/cum-shot/"           to "Cum Shot",
//        "${mainUrl}/categories/rough-sex/"          to "Rough Sex",
//        "${mainUrl}/categories/lingerie/"           to "Lingerie",
//        "${mainUrl}/categories/handjob/"            to "Handjob",
//        "${mainUrl}/categories/twitch/"             to "Twitch",
//        "${mainUrl}/categories/joi/"                to "JOI",
//        "${mainUrl}/categories/water-sports/"       to "Water Sports",
        "${mainUrl}/categories/cosplay/"            to "Cosplay",
//        "${mainUrl}/categories/fetish/"             to "Fetish",
//        "${mainUrl}/categories/patreon/"            to "Patreon",
//        "${mainUrl}/categories/swallow/"            to "Swallow",
//        "${mainUrl}/categories/role-play/"          to "Role Play",
//        "${mainUrl}/categories/feet/"               to "Feet",
//        "${mainUrl}/categories/masturbation/"       to "Masturbation",
//        "${mainUrl}/categories/creampie/"           to "Creampie",
//        "${mainUrl}/categories/web-cam/"            to "Web Cam",
//        "${mainUrl}/categories/dildo-sex/"          to "Dildo Sex",
//        "${mainUrl}/categories/creamy/"             to "Creamy",
//        "${mainUrl}/categories/panty-fetish/" to "Panty Fetish",
    )


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page/").document
        val home = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.thumb.lazy-load")?.attr("data-original"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}/?from_videos=$page", headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Referer" to "https://epawg.com/"
        )).document

//        Log.d("kraptor_EPawg", "document = $document")

        val aramaCevap = document.select("div.item").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }
    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.thumb.lazy-load")?.attr("data-original"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val getDoc = app.get(url)
        val document = getDoc.document
        val textDoc = getDoc.text

        val title = document.selectFirst("div.headline h1")?.text()?.trim() ?: return null
        val posterUrl = Regex(
            pattern = "preview_url: '([^']*)',",
            options = setOf(RegexOption.IGNORE_CASE)
        ).find(textDoc)?.groupValues?.getOrNull(1).toString()
        val poster = fixUrlNull(posterUrl)
        val description = document.selectFirst("div.info > div:nth-child(2) > em:nth-child(1)")?.text()?.trim()
        val tags = document.select("div.info > div:nth-child(3) a").map { it.text() }
        val score = document.selectFirst("span.scale")?.attr("data-rating")?.trim()

        val recommendations = document.select("div#list_videos_related_videos_items div.item").mapNotNull {
            it.toRecommendationResult()
        }

        val actors = document.select("div.info > div:nth-child(5) a").map { Actor(it.text()) }
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.score = Score.from10(score)
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("strong.title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null

        val img = this.selectFirst("img")
        val posterUrl = fixUrlNull(
            if (img?.hasAttr("data-original") == true) img.attr("data-original")
            else img?.attr("src")
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
        val pageHtml = app.get(data).text
        return KtPlayerExtractor.getLinks(name, mainUrl, data, pageHtml, callback = callback)
    }
}
