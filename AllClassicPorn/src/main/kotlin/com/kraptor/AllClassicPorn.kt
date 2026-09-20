// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.webkit.WebView
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class AllClassicPorn : MainAPI() {
    override var mainUrl              = "https://allclassic.porn"
    override var name                 = "AllClassicPorn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)


    override val mainPage = mainPageOf(
        "${mainUrl}/categories/amateur/"            to  "Amateur Classic",
        "${mainUrl}/categories/anal/"               to  "Anal",
        "${mainUrl}/categories/antique/"            to  "Antique",
        "${mainUrl}/categories/asian/"              to  "Asian",
        "${mainUrl}/categories/babe/"               to  "Babe",
        "${mainUrl}/categories/bbw/"                to  "BBW And Fat",
        "${mainUrl}/categories/big-ass/"            to  "Big Ass",
        "${mainUrl}/categories/big-dick/"           to  "Big Dick",
        "${mainUrl}/categories/big-tits/"           to  "Big Tits",
        "${mainUrl}/categories/blonde/"             to  "Blondes",
        "${mainUrl}/categories/blowjob/"            to  "Blowjobs",
        "${mainUrl}/categories/bondage/"            to  "Bondage and BDSM",
        "${mainUrl}/categories/brunette/"           to  "Brunettes",
        "${mainUrl}/categories/compilation/"        to  "Compilation",
        "${mainUrl}/categories/cuckold/"            to  "Cuckold",
        "${mainUrl}/categories/cumshot/"            to  "Cumshots",
        "${mainUrl}/categories/cunnilingus/"        to  "Cunnilingus",
        "${mainUrl}/categories/deepthroat/"         to  "Deepthroat",
        "${mainUrl}/categories/double-penetration/" to  "Double Penetration",
        "${mainUrl}/categories/ebony/"              to  "Ebony",
        "${mainUrl}/categories/european/"           to  "European",
        "${mainUrl}/categories/family/"             to  "Family",
        "${mainUrl}/categories/female-orgasm/"      to  "Female orgasm",
        "${mainUrl}/categories/fetish/"             to  "Fetish",
        "${mainUrl}/categories/fisting/"            to  "Fisting",
        "${mainUrl}/categories/full-movie/"         to  "Full Movies",
        "${mainUrl}/categories/gangbang/"           to  "Gangbang",
//        "${mainUrl}/categories/gay/"                to  "Gay",
        "${mainUrl}/categories/hairy/"              to  "Hairy",
        "${mainUrl}/categories/handjob/"            to  "Handjob",
        "${mainUrl}/categories/hardcore/"           to  "Hardcore",
        "${mainUrl}/categories/hd/"                 to  "HD",
        "${mainUrl}/categories/historical/"         to  "Historical",
        "${mainUrl}/categories/interracial/"        to  "Interracial",
        "${mainUrl}/categories/lesbian/"            to  "Lesbians",
        "${mainUrl}/categories/lingerie/"           to  "Lingerie",
        "${mainUrl}/categories/masturbation/"       to  "Masturbation",
        "${mainUrl}/categories/mature/"             to  "Mature",
        "${mainUrl}/categories/milf/"               to  "MILF",
        "${mainUrl}/categories/old-and-young/"      to  "Old and Young",
        "${mainUrl}/categories/orgy/"               to  "Orgy",
        "${mainUrl}/categories/petite/"             to  "Petite",
        "${mainUrl}/categories/pissing/"            to  "Pissing",
        "${mainUrl}/categories/pornstars/"          to  "Pornstars",
        "${mainUrl}/categories/rare/"               to  "Rare",
        "${mainUrl}/categories/redhead/"            to  "Redhead",
        "${mainUrl}/categories/riding/"             to  "Riding",
        "${mainUrl}/categories/school/"             to  "School",
        "${mainUrl}/categories/skinny/"             to  "Skinny",
        "${mainUrl}/categories/small-tits/"         to  "Small Tits",
        "${mainUrl}/categories/softcore/"           to  "Softcore",
        "${mainUrl}/categories/solo/"               to  "Solo",
        "${mainUrl}/categories/stockings/"          to  "Stockings",
        "${mainUrl}/categories/striptease/"         to  "Striptease",
        "${mainUrl}/categories/teen/"               to  "Teens",
        "${mainUrl}/categories/threesome/"          to  "Threesomes",
        "${mainUrl}/categories/toys/"               to  "Toys",
        "${mainUrl}/categories/vintage/"            to  "Vintage",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page/").document
        val home = document.select("a.th.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.th-description")?.text() ?: return null
        if (title.contains("Debbie Does Dallas", ignoreCase = true) || title.contains("Private Teacher", ignoreCase = true)) {
            return null
        }
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val puan = this.selectFirst("span.th-rating")?.text()?.replace("%","")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(puan)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query.replace(" ","-")}/$page/").document

        val aramaCevap = document.select("a.th.item").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.th-description")?.text() ?: return null
        if (title.contains("Debbie Does Dallas", ignoreCase = true) || title.contains("Private Teacher", ignoreCase = true)) {
            return null
        }
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val puan = this.selectFirst("span.th-rating")?.text()?.replace("%","")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(puan)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1.h2")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.description-container")?.text()?.substringAfter(":")?.trim()
        val year            = document.selectFirst("span.move-right:has(strong:matchesOwn(^Released:))")?.text()?.substringAfter(" ")?.trim()?.toIntOrNull()
        val tags            = document.select("div.video-links p:has(strong:matchesOwn(^Tags:)) a").map { it.text().trim() }
        val rating          = document.selectFirst("div.voters strong")?.text()?.replace("%","")?.trim()
        val duration        = document.selectFirst("meta[property=video:duration]")?.attr("content")?.split("M")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("a.th.item").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.video-links p:has(strong:matchesOwn(^Models:))")
            .flatMap { aktorler ->
                aktorler.select("a.btn[itemprop=actor]").map { aTag ->
                    val aktorIsim = aTag.text().trim()
                    val aktorPoster = aTag.selectFirst("img")?.attr("src")
                    Actor(aktorIsim, aktorPoster)
                }
            }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from100(rating)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("div.th-description")?.text() ?: return null
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val puan = this.selectFirst("span.th-rating")?.text()?.replace("%","")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(puan)
        }
    }

    // Minimal WebView temizleme
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
