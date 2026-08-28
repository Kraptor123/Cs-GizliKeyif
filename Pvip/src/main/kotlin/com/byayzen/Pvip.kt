package com.byayzen

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Pvip : MainAPI() {
    override var mainUrl               = "https://pvip.se"
    override var name                  = "Pvip"
    override val hasMainPage           = true
    override var lang                  = "es"
    override val hasQuickSearch        = false
    override val supportedTypes        = setOf(TvType.NSFW)
    override val vpnStatus             = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Main Menu",
        "${mainUrl}/en/amateur/" to "Amateur",
        "${mainUrl}/en/anal/" to "Anal",
        "${mainUrl}/en/latinas/argentinian/" to "Argentinian",
        "${mainUrl}/en/bbw/" to "BBW",
        "${mainUrl}/en/big-ass/" to "Big Ass",
        "${mainUrl}/en/big-tits/" to "Big Tits",
        "${mainUrl}/en/blonde/" to "Blonde",
        "${mainUrl}/en/brunette/" to "Brunette",
        "${mainUrl}/en/latinas/colombian/" to "Colombian",
        "${mainUrl}/en/latinas/cuban/" to "Cuban",
        "${mainUrl}/en/european/" to "European",
        "${mainUrl}/en/family-porn/" to "Family Porn",
        "${mainUrl}/en/european/french/" to "French",
        "${mainUrl}/en/hardcore/" to "Hardcore",
        "${mainUrl}/en/latinas/" to "Latinas",
        "${mainUrl}/en/lesbian/" to "Lesbian",
        "${mainUrl}/en/mature/" to "Mature",
        "${mainUrl}/en/latinas/mexican/" to "Mexican",
        "${mainUrl}/en/milf/" to "MILF",
        "${mainUrl}/en/orgy/" to "Orgy",
        "${mainUrl}/en/latinas/paraguayan/" to "Paraguayan",
        "${mainUrl}/en/latinas/peruvian/" to "Peruvian",
        "${mainUrl}/en/porn-casting/" to "Porn Casting",
        "${mainUrl}/en/pov/" to "POV",
        "${mainUrl}/en/putas/" to "Putas",
        "${mainUrl}/en/redhead/" to "Redhead",
        "${mainUrl}/en/european/russian/" to "Russian",
        "${mainUrl}/en/sex-for-money/" to "Sex for Money",
        "${mainUrl}/en/sin-categoria-en/" to "Sin categoría",
        "${mainUrl}/en/skinny/" to "Skinny",
        "${mainUrl}/en/european/spanish/" to "Spanish"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val requestUrl =
            if (page == 1) request.data.trimEnd('/') + "/" else "${request.data.trimEnd('/')}/page/$page/"
        val document = app.get(requestUrl).document
        val home = document.select("div.dgd article.loop-post").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(
            list = HomePageList(request.name, home, isHorizontalImages = true),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title =
            this.selectFirst("h2.ttl")?.text()?.trim()?.ifEmpty { return null } ?: return null
        val href = fixUrlNull(this.selectFirst("a.lka")?.attr("href")?.ifEmpty { return null }
            ?: return null) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")
                ?.ifBlank { this.selectFirst("img")?.attr("src") ?: "" })
        return newMovieSearchResponse(title, "$posterUrl|$href", TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val searchUrl =
            if (page == 1) "$mainUrl/en/search/$query/" else "$mainUrl/en/search/$query/page/$page/"
        val document = app.get(searchUrl).document
        val results =
            document.select("div.dgd article.loop-post").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val split          = url.split("|")
        val posterFromData = if (split.size > 1) split[0] else null
        val cleanUrl       = if (split.size > 1) split[1] else url
        val document       = app.get(cleanUrl).document

        val title       = document.selectFirst("article.vdeo-single header .ttl")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(posterFromData) ?: fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = document.selectFirst("div.entry p")?.text()?.trim()
        val dateText    = document.selectFirst("div.entry p.f12")?.text()?.trim()
        val year        = Regex("(\\d{4})").find(dateText ?: "")?.groupValues?.get(1)?.toIntOrNull()
        val tags        = document.select("div.tagcloud a:not(.tag-chnl):not(.tag-prst)").map { it.text().trim() }
        val durationStr = document.selectFirst("article.vdeo-single header span.text-b")?.text()?.trim()
        val duration    = getDurationFromString(durationStr ?: "")
        val actors      = document.select("a.tag-prst").map { Actor(it.text().trim()) }
        val recoms      = document.select("aside.cnt article.loop-post.vdeo").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, cleanUrl, TvType.NSFW, "$poster|$cleanUrl") {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recoms
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("h2.ttl")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a.lka")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")
                ?.ifBlank { this.selectFirst("img")?.attr("src") ?: "" })
        return newMovieSearchResponse(title, "$posterUrl|$href", TvType.NSFW) {
            this.posterUrl = fixUrlNull(posterUrl)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val splitData = data.split("|")
        val mainUrl = if (splitData.size > 1) splitData[1].trim() else data
        val poster = if (splitData.size > 1) splitData[0].trim() else null
        val html = app.get(mainUrl).document.html()

        val srcsBlock = html.substringAfter("var playerSrcs = [", "")
            .substringBefore("]", "")
        if (srcsBlock.isBlank() || !srcsBlock.contains("mp4.nu"))
            return false

        val mp4Urls = Regex(""""([^"]+)"""")
            .findAll(srcsBlock)
            .map { it.groupValues[1].replace("\\/", "/") }
            .filter { it.startsWith("http") }
            .toList()

        if (mp4Urls.isEmpty())
            return false

        val videoUrls = mp4Urls.map { "$poster|$it" }
        var anyLoaded = false

        videoUrls.forEach { url ->
            val split = url.split("|")
            val target = split[1].trim()

            val hParam = target.substringAfter("?h=", "")
            if (hParam.isBlank()) return@forEach

            val location = app.get("https://mp4.nu/r.php?h=$hParam", allowRedirects = false)
                .headers["location"] ?: return@forEach

            Log.d("Pvip", "Found iframe location: $location")
            loadExtractor(location, "https://mp4.nu/", subtitleCallback, callback)
            anyLoaded = true
        }

        return anyLoaded
    }
}
