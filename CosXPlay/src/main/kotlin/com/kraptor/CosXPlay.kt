// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class CosXPlay : MainAPI() {
    override var mainUrl              = "https://cosxplay.com"
    override var name                 = "CosXPlay"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/"                              to "Home",
        "${mainUrl}/?filter=most-viewed"                       to "Most Viewed",
        "${mainUrl}/?filter=latest"                            to "Latest",
        "${mainUrl}/?filter=popular"                           to "Popular",
        "${mainUrl}/?filter=longest"                           to "Longest",
        "${mainUrl}/17220-halloween/"                          to "Halloween",
        "${mainUrl}/70946-genshin-impact/"                     to "Genshin",
        "${mainUrl}/11119-pov/"                                to "POV",
        "${mainUrl}/11121-joi/"                                to "JOI",
        "${mainUrl}/11104-anal/"                               to "Anal",
        "${mainUrl}/20911-rem-ram/"                            to "Rem",
        "${mainUrl}/7841-nier-automata/"                       to "2B",
        "${mainUrl}/73136-makima/"                             to "Makima",
        "${mainUrl}/7828-naruto/"                              to "Naruto",
        "${mainUrl}/18231-18321-asmr/"                         to "ASMR",
        "${mainUrl}/7828-naruto/12508-hinata/"                 to "Hinata",
        "${mainUrl}/71061-femboy/"                             to "Femboy",
        "${mainUrl}/5230-latex/"                               to "Latex",
        "${mainUrl}/12814-anime/70920-one-piece/"              to "One Piece",
        "${mainUrl}/4063-ahegao/"                              to "Ahegao",
        "${mainUrl}/19621-uniform/13908-maid/"                 to "Maid",
        "${mainUrl}/11115-asian/12547-japanese/"               to "Japanese",
        "${mainUrl}/16982-solo/"                               to "Solo",
        "${mainUrl}/11118-dildo/"                              to "Dildo",
        "${mainUrl}/12814-anime/"                              to "Anime",
        "${mainUrl}/17809-feet/"                               to "Feet",
        "${mainUrl}/11114-lesbian/"                            to "Lesbian",
        "${mainUrl}/16652-furry/"                              to "Furry",
        "${mainUrl}/12814-anime/59215-nezuko/"                 to "Nezuko",
        "${mainUrl}/11113-creampie/"                           to "Creampie",
        "${mainUrl}/7837-creatures/7833-succubus/"             to "Succubus",
        "${mainUrl}/10570-bondage/"                            to "Bondage",
        "${mainUrl}/17328-superheroines/"                      to "Superheroines",
        "${mainUrl}/17862-nun/"                                to "Nun",
        "${mainUrl}/7776-harley-quinn/"                        to "Harley Quinn",
        "${mainUrl}/19621-uniform/13154-nurse/"                to "Nurse",
        "${mainUrl}/7832-films/7835-scooby-doo/11125-velma/"   to "Velma",
        "${mainUrl}/61046-tsunade/"                            to "Tsunade",
        "${mainUrl}/2101-supergirl/"                           to "Supergirl",
        "${mainUrl}/11120-teen/"                               to "Teen",
        "${mainUrl}/19001-bbw/"                                to "BBW",
        "${mainUrl}/61118-jinx/"                               to "Jinx",
        "${mainUrl}/5231-kigurumi/"                            to "Kigurumi",
        "${mainUrl}/7828-naruto/21040-sakura-haruno/"          to "Sakura",
        "${mainUrl}/2166-pokemon/"                             to "Pokemon",
        "${mainUrl}/17216-public/"                             to "Public",
        "${mainUrl}/922-wonderwoman/"                          to "Wonder Woman",
        "${mainUrl}/95-overwatch/293-dva/"                     to "D.va",
        "${mainUrl}/7881-poison-ivy/"                          to "Poison Ivy",
        "${mainUrl}/11101-neko-porn/"                          to "Neko",
        "${mainUrl}/17807-masturbation/"                       to "Masturbation",
        "${mainUrl}/11117-big-boobs/"                          to "Big boobs",
        "${mainUrl}/8103-bunnies/"                             to "Bunny",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get("${request.data}").document
        } else if (request.data.contains("?filter")){
            app.get("$mainUrl/page/$page/${request.data.substringAfter(".com/")}").document
        } else {
            app.get("${request.data}/page/$page/").document
        }
        val home     = document.select("div.col-xl-2").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("span.title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val img = this.selectFirst("img.video-img")
        val posterUrl = fixUrlNull(
            img?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("src")?.takeIf { it.isNotBlank() }
        )
        val score     = this.selectFirst("span.rating-nolike")?.text()?.replace("%","")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score     = Score.from100(score)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=${query}").document
        }

        val aramaCevap = document.select("div.col-xl-2").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = document.selectFirst("script:containsData(\"thumbnail\")")?.data()?.substringAfter("\"thumbnail\":\"")?.substringBefore("\"")
        val score          = document.selectFirst("script:containsData(\"thumbnail\")")?.data()?.substringAfter("\"likes\":\"")?.substringBefore("\"")?.replace("%","")
        val duration        = document.selectFirst("script:containsData(\"thumbnail\")")?.data()?.substringAfter("\"length\":\"")?.substringBefore("\"")?.split(" ")?.first()?.trim()?.toIntOrNull()
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.tags-list a").map { it.text() }
        val recommendations = document.select("div.col-xl-2").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }

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
        val document = app.get(data).document

        val videolar = document.select("div.responsive-player.video-player video source")

        videolar.map { video ->
            val link = video?.attr("src").toString()
            val title = video?.attr("title").toString()
            callback.invoke(newExtractorLink(
                "${this.name} $title",
                "${this.name} $title",
                link,
                ExtractorLinkType.VIDEO,
                {
                    this.referer = "${mainUrl}/"
                }
            )
            )
        }

        return true
    }
}