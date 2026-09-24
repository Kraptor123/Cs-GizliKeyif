// ! This Extension Made By @kraptor for GizliKeyif

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

class EffedUpMovies : MainAPI() {
    override var mainUrl              = "https://www.effedupmovies.com"
    override var name                 = "EffedUpMovies"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Main Page",
        "${mainUrl}/category/hentai/"                               to "Anime-Animation",
        "${mainUrl}/category/movies-based-on-a-true-story/"         to "Based on a True Story",
        "${mainUrl}/category/bdsm/"                                 to "BDSM-Roleplay",
        "${mainUrl}/category/bizarre-surreal/"                      to "Bizarre-Surreal",
        "${mainUrl}/category/abduction/"                            to "Captivity-Kidnapping",
        "${mainUrl}/category/mystery/"                              to "Detective-Mystery-Cops",
        "${mainUrl}/category/mannequin/"                            to "Dolls-Toys",
        "${mainUrl}/category/junkie/"                               to "Drugs",
        "${mainUrl}/category/home-invasion/"                        to "Home Invasion",
        "${mainUrl}/category/horror/"                               to "Horror",
        "${mainUrl}/category/incest/"                               to "Incest",
        "${mainUrl}/category/pornography-in-world-cinema/"          to "Porno-Themed",
        "${mainUrl}/category/revenge/"                              to "Revenge",
        "${mainUrl}/category/science-fiction/"                      to "Sci-Fi",
        "${mainUrl}/category/brothers-sisters-family/"              to "Siblings-Family",
        "${mainUrl}/category/torture/"                              to "Torture",
        "${mainUrl}/category/human-trafficking/"                    to "Trafficking-Prostitution",
        "${mainUrl}/category/real-sex/"                             to "Unsimulated Sex",
        "${mainUrl}/category/vampires-witches/"                     to "Vampires-Witches-Werewolves",
        "${mainUrl}/category/voyeur/"                               to "Voyeur-Stalking",
        "${mainUrl}/category/zombies/"                              to "Zombies-Humanlike Creatures",

    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1){
            app.get("${request.data}").document
        } else {
            app.get("${request.data}page/$page/?0").document
        }
        val home     = document.select("div.row article").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(list = HomePageList(request.name, home, false))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/?s=$query").document
        } else {
            app.get("${mainUrl}/page/$page/?s=$query").document
        }
        val searchAnswer = document.select("div.row article").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(searchAnswer, hasNext = true)
    }



    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)


    override suspend fun load(url: String): LoadResponse? {
        Log.d(tag, "Load aşaması: $url")
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val posterAl        = document.selectFirst("link[rel=alternate][title*=xml]")?.attr("href") ?: ""
        val rawText         = app.get(posterAl).text
        val posterDoc       = Jsoup.parse(rawText, "", Parser.xmlParser())
        val poster          = fixUrlNull(posterDoc.selectFirst("thumbnail_url")?.text())
        val description     = document.selectFirst("meta[name=description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.entry-content p strong a").map { it.text() }
        val scoreText       = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration = document.selectFirst("div.entry-content p strong:contains(Runtime)")
            ?.nextSibling()
            ?.toString()
            ?.removePrefix(":")
            ?.trim()
            ?.substringBefore(" ")
            ?.split(":")
            ?.let { parts ->
                when (parts.size) {
                    3 -> {
                        val saat = parts[0].toIntOrNull() ?: 0
                        val dakika = parts[1].toIntOrNull() ?: 0
                        (saat * 60) + dakika
                    }
                    2 -> {
                        val saat = parts[0].toIntOrNull() ?: 0
                        val dakika = parts[1].toIntOrNull() ?: 0
                        (saat * 60) + dakika
                    }
                    else -> parts.firstOrNull()?.toIntOrNull()
                }
            }
        val recommendations = document.select("a.yarpp-thumbnail").mapNotNull { it.toRecommendationResult() }
        val actors = document.selectFirst("div.entry-content p strong:contains(Starring)")
            ?.nextSibling()
            ?.toString()
            ?.removePrefix(":")
            ?.trim()
            ?.split(",")
            ?.map {
                Actor(it.trim())
            }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(scoreText)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("src"))
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
        Log.d(tag, "data = $data")
        val document = app.get(data).document

        val source = document.selectFirst("source")?.attr("src") ?: ""
        val track = document.selectFirst("track")?.attr("src") ?: ""
        val label = document.selectFirst("track")?.attr("label") ?: ""

        Log.d(tag, "source = $source track = $track label = $label")

        subtitleCallback.invoke(
            newSubtitleFile(
                lang = label,
                url = track
            )
        )

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = source,
                type = INFER_TYPE,
                initializer = {
                    this.referer = "$mainUrl/"
                }
            ))
        return true
    }
}