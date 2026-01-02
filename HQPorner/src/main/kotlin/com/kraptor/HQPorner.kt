// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class HQPorner : MainAPI() {
    override var mainUrl              = "https://hqporner.com"
    override var name                 = "HQPorner"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/top"                           to "All time best porn",
        "${mainUrl}/top/month"                     to "Month top porn",
        "${mainUrl}/top/week"                      to "Week top porn",
        "${mainUrl}/category/1080p-porn"           to "1080p porn",
        "${mainUrl}/category/4k-porn"              to "4k porn",
        "${mainUrl}/category/60fps-porn"           to "60fps",
        "${mainUrl}/category/amateur"              to "Amateur",
        "${mainUrl}/category/anal-sex-hd"          to "Anal",
        "${mainUrl}/category/asian"                to "Asian",
        "${mainUrl}/category/babe"                 to "Babe",
        "${mainUrl}/category/bdsm"                 to "Bdsm",
//        "${mainUrl}/category/beach-porn"           to "beach",
        "${mainUrl}/category/big-ass"              to "Big Ass",
//        "${mainUrl}/category/big-dick"             to "big dick",
        "${mainUrl}/category/big-tits"             to "Big Tits",
//        "${mainUrl}/category/bisexual"             to "bisexual",
        "${mainUrl}/category/blonde"               to "Blonde",
        "${mainUrl}/category/blowjob"              to "Blowjob",
        "${mainUrl}/category/bondage"              to "Bondage",
        "${mainUrl}/category/brunette"             to "Brunette",
//        "${mainUrl}/category/casting"              to "casting",
        "${mainUrl}/category/creampie"             to "Creampie",
//        "${mainUrl}/category/cumshot"              to "cumshot",
//        "${mainUrl}/category/deepthroat"           to "deepthroat",
        "${mainUrl}/category/ebony"                to "Ebony",
//        "${mainUrl}/category/fetish"               to "fetish",
//        "${mainUrl}/category/fingering"            to "fingering",
//        "${mainUrl}/category/fisting"              to "fisting",
        "${mainUrl}/category/gangbang"             to "GangBang",
//        "${mainUrl}/category/group-sex"            to "group sex",
//        "${mainUrl}/category/hairy-pussy"          to "hairy pussy",
        "${mainUrl}/category/handjob"              to "HandJob",
//        "${mainUrl}/category/hentai"               to "hentai",
//        "${mainUrl}/category/interracial"          to "interracial",
        "${mainUrl}/category/japanese-girls-porn"  to "Japanese",
//        "${mainUrl}/category/latina"               to "latina",
        "${mainUrl}/category/lesbian"              to "Lesbian",
//        "${mainUrl}/category/long-hair"            to "long hair",
//        "${mainUrl}/category/masturbation"         to "masturbation",
        "${mainUrl}/category/mature"               to "Mature",
        "${mainUrl}/category/milf"                 to "Milf",
//        "${mainUrl}/category/moaning"              to "moaning",
        "${mainUrl}/category/old-and-young"        to "Old and Young",
//        "${mainUrl}/category/orgasm"               to "orgasm",
//        "${mainUrl}/category/orgy"                 to "orgy",
        "${mainUrl}/category/outdoor"              to "Outdoor",
//        "${mainUrl}/category/pickup"               to "pickup",
        "${mainUrl}/category/pov"                  to "Pov",
        "${mainUrl}/category/public"               to "Public",
//        "${mainUrl}/category/pussy-licking"        to "pussy licking",
        "${mainUrl}/category/redhead"              to "Redhead",
        "${mainUrl}/category/russian"              to "Russian",
//        "${mainUrl}/category/porn-massage"         to "sex massage",
//        "${mainUrl}/category/sex-parties"          to "sex party",
        "${mainUrl}/category/shaved-pussy"         to "Shaved Pussy",
//        "${mainUrl}/category/shemale"              to "shemale",
        "${mainUrl}/category/small-tits"           to "Small Tits",
//        "${mainUrl}/category/squeezing-tits"       to "squeezing tits",
//        "${mainUrl}/category/squirt"               to "squirt",
        "${mainUrl}/category/stockings"            to "Stockings",
        "${mainUrl}/category/tattooed"             to "Tattooed",
        "${mainUrl}/category/teen-porn"            to "Teen porn",
//        "${mainUrl}/category/threesome"            to "threesome",
//        "${mainUrl}/category/undressing"           to "undressing",
        "${mainUrl}/category/uniforms"             to "Uniforms",
//        "${mainUrl}/category/vibrator"             to "vibrator",
//        "${mainUrl}/category/vintage"              to "vintage",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page", referer = "${mainUrl}/").document
        val home     = document.select("div.row section.box.feature:has(span.icon)").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, "${href}kraptor${posterUrl?.substringAfter("//")}", TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/?q=${query}&p=$page", referer = "${mainUrl}/").document

        val aramaCevap = document.select("div.row section.box.feature:has(span.icon)").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val split = url.split("kraptor")
        val url = split[0].trim()
        val poster = "https://${split[1]}"
        val document = app.get(url, referer = "${mainUrl}/").document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val description     = document.selectFirst("meta[name=description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("section h3 + p a").map { it.text() }
        val score           = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("li.icon.fa-clock-o")
            ?.text()
            ?.let { text ->
                val parts = text.split(" ")
                var totalMinutes = 0
                parts.forEach { part ->
                    when {
                        part.endsWith("h") -> totalMinutes += part.removeSuffix("h").toIntOrNull()?.times(60) ?: 0
                        part.endsWith("m") -> totalMinutes += part.removeSuffix("m").toIntOrNull() ?: 0
                    }
                }
                totalMinutes
            }

        val recommendations = document.select("div.\\34 u section").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("li.icon.fa-star-o a").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(score)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data, referer = "${data}/", headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Referer" to "${mainUrl}/",
        )).document

        val iframe =  fixUrlNull(document.selectFirst("iframe[src*=mydaddy]")?.attr("src")) ?: ""

//        Log.d("kraptor_$name", "iframe = ${iframe}")

        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}