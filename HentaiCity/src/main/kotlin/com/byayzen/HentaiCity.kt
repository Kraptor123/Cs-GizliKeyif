// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HentaiCity : MainAPI() {
    override var mainUrl              = "https://www.hentaicity.com/"
    override var name                 = "HentaiCity"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/straight/all-popular.html"         to "Most Popular",
        "${mainUrl}/videos/straight/all-recent.html"          to "Most Recent",
        "${mainUrl}/videos/straight/all-view.html"            to "Most Viewed",
        "${mainUrl}/videos/straight/all-rate.html"            to "Top Rated",
        "${mainUrl}/videos/straight/all-length.html"          to "Longest",
        "${mainUrl}/videos/straight/anal-popular.html"        to "Anal",
        "${mainUrl}/videos/straight/babe-popular.html"        to "Babe",
        "${mainUrl}/videos/straight/bigdick-popular.html"     to "Big Dick",
        "${mainUrl}/videos/straight/bigtits-popular.html"     to "Big Tits",
        "${mainUrl}/videos/straight/blowjob-popular.html"     to "Blowjob",
        "${mainUrl}/videos/straight/cartoon-popular.html"     to "Cartoon",
        "${mainUrl}/videos/straight/comics-popular.html"      to "Comics",
        "${mainUrl}/videos/straight/cumshot-popular.html"     to "Cumshot",
        "${mainUrl}/videos/straight/fetish-popular.html"      to "Fetish",
        "${mainUrl}/videos/straight/groupsex-popular.html"    to "Groupsex",
        "${mainUrl}/videos/straight/hentai-popular.html"      to "Hentai",
        "${mainUrl}/videos/straight/lesbian-popular.html"     to "Lesbian",
        "${mainUrl}/videos/straight/masturbation-popular.html" to "Masturbation",
        "${mainUrl}/videos/straight/mature-popular.html"      to "Mature",
        "${mainUrl}/videos/straight/monster-popular.html"     to "Monster",
        "${mainUrl}/videos/straight/roughsex-popular.html"    to "Rough Sex",
        "${mainUrl}/videos/straight/teen-popular.html"        to "Teen",
        "${mainUrl}/videos/straight/toys-popular.html"        to "Toys",
        "${mainUrl}/videos/straight/voyeur-popular.html"      to "Voyeur",
        "${mainUrl}/videos/straight/3d-popular.html"          to "3D",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        Log.d(tag, "getMainPage ${request.data} page=$page")
        val url      = if (page <= 1) request.data else request.data.replace(".html", "-$page.html")
        val document = app.get(url).document
        val items    = document.select("section.content div.thumb-list div.outer-item")
        val home     = items.mapNotNull { it.toMainPageResult() }
        Log.d(tag, "items=${items.size} parsed=${home.size}")
        return newHomePageResponse(
            list = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("p a.video-title")?.text()?.trim() ?: return null
        val href      = fixUrlNull(this.selectFirst("a.thumb-img")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.thumb-ratio img")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        Log.d(tag, "search $query page=$page")
        val url          = "${mainUrl}/search/video/${query}/$page/"
        val document     = app.get(url).document
        val searchAnswer = document.select("section.content div.thumb-list div.outer-item").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(searchAnswer)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        Log.d(tag, "Load : $url")
        val document         = app.get(url).document

        val title            = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster           = fixUrlNull(document.selectFirst("video")?.attr("poster"))
        val descElements     = document.select("div.ubox-text > div[style]")
        val description      = if (descElements.size > 1) descElements[1].text()?.trim() else null
        val infoText         = document.selectFirst("div.information")?.text()?.trim() ?: ""
        val year             = Regex("(\\d{4})").find(infoText)?.groupValues?.get(1)?.toIntOrNull()
        val duration         = Regex("(\\d{2}):(\\d{2})").find(infoText)?.groupValues?.let { (it[1].toIntOrNull() ?: 0) * 60 + (it[2].toIntOrNull() ?: 0) }
        val tags             = document.select("div#taglink a[href*=/tags/]").map { it.text().trim() }
        val scoreText        = document.select("div").firstOrNull { it.text().trim().endsWith("%") }?.text()?.trim()?.replace("%", "") ?: ""
        val score            = Regex("\\d+").find(scoreText)?.value?.toIntOrNull()?.let { Score.from10(it.toDouble() / 10.0) }
        val recommendations  = document.select("div.thumb-list div.item").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = score
            this.duration        = duration
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val titleElement = this.selectFirst("a.video-title") ?: return null
        val title        = titleElement.attr("title")
        val href         = fixUrlNull(titleElement.attr("href").ifEmpty { return null }) ?: return null
        val posterUrl    = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d(tag, "data = ${data}")
        val document      = app.get(data).document
        val sourceElement = document.selectFirst("video#video-id source") ?: return false
        val videoUrl      = sourceElement.attr("src").ifEmpty { return false }

        callback(
            newExtractorLink(
                source = this.name,
                name   = this.name,
                url    = videoUrl,
                type   = INFER_TYPE
            ) {
                this.referer = "${mainUrl}/"
                this.quality = Qualities.Unknown.value
            }
        )
        return true
    }
}