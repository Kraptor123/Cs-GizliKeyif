package com.byayzen

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newSearchResponseList
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.fixUrlNull
import org.jsoup.nodes.Element

class Sextb : MainAPI() {
    override var mainUrl = "https://sextb.net"
    override var name = "SexTB"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    private val commonHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer" to "$mainUrl/",
        "Accept-Language" to "en-US,en;q=0.9"
    )

    override val mainPage = mainPageOf(
        "${mainUrl}/genre/amateur" to "Amatör",
        "${mainUrl}/genre/anal" to "Anal",
        "${mainUrl}/genre/av-idol" to "AV Idol",
        "${mainUrl}/genre/beautiful-girl" to "Güzel Kız",
        "${mainUrl}/genre/beautiful-pussy" to "Güzel Vajina",
        "${mainUrl}/genre/big-asses" to "Büyük Kalçalar",
        "${mainUrl}/genre/big-tits" to "Büyük Göğüsler",
        "${mainUrl}/genre/blowjob" to "Oral Seks",
        "${mainUrl}/genre/bondage" to "Bondaj",
        "${mainUrl}/genre/bukkake" to "Bukkake",
        "${mainUrl}/genre/cheating-wife" to "Aldatan Kadın",
        "${mainUrl}/genre/cosplay" to "Cosplay",
        "${mainUrl}/genre/creampie" to "İç boşalma",
        "${mainUrl}/genre/cumshot" to "Boşalma",
        "${mainUrl}/genre/deep-throat" to "Derin Oral",
        "${mainUrl}/genre/doggy-style" to "Doggy Style",
        "${mainUrl}/genre/drama" to "Drama",
        "${mainUrl}/genre/facials" to "Yüze boşalma",
        "${mainUrl}/genre/featured-actress" to "Öne Çıkan Oyuncu",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page <= 1) {
            request.data
        } else {
            "${request.data.removeSuffix("/")}/pg-$page"
        }

        val document = app.get(url, headers = commonHeaders).document
        val items = document.select("div.tray-item").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(
            listOf(HomePageList(request.name, items)),
            hasNext = items.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search/$query"
        } else {
            "$mainUrl/search/$query/pg-$page"
        }

        val document = app.get(url, headers = commonHeaders).document
        val items = document.select("div.tray-item").mapNotNull {
            it.toSearchResult()
        }

        if (items.isEmpty()) {
            Log.d("STB_Search", "Arama listesi boş döndü! URL: $url")
        }

        return newSearchResponseList(items, hasNext = items.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElement = this.selectFirst("a") ?: return null
        val href = linkElement.attr("href")

        if (href.startsWith("/search") || href.contains("javascript") || href.startsWith("/genre")) return null

        val fullHref = fixUrl(href)
        val title = this.selectFirst("div.tray-item-title")?.text()?.trim()
            ?: this.selectFirst("img")?.attr("alt")?.trim()
            ?: return null

        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(title, fullHref, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val res = app.get(url, headers = commonHeaders)
        val document = res.document

        val title = document.selectFirst("h1.film-info-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
            ?: fixUrlNull(document.selectFirst("#infomation img")?.attr("data-src"))

        val description = document.selectFirst("span.full-text-desc")?.text()?.trim()
        val yearText = document.selectFirst("div.description:has(i.fa-calendar) strong")?.text()
        val year =
            yearText?.let { Regex("""(\d{4})""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val duration = document.selectFirst("div.description:has(i.fa-clock) strong")?.text()
            ?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
        val tags = document.select("div.description:has(i.fa-list) a").map { it.text() }
        val actors = document.select("div.description:has(i.fa-users) a").map { Actor(it.text()) }

        val recommendations = mutableListOf<SearchResponse>()
        val filmId = Regex("""var filmId\s*=\s*(\d+)""").find(res.text)?.groupValues?.get(1)

        if (filmId != null) {
            try {
                val apiRes =
                    app.get("${mainUrl}/ajax/related/$filmId", headers = commonHeaders).text
                val apiDoc = org.jsoup.Jsoup.parse(apiRes)
                apiDoc.select(".tray-item").forEach { el ->
                    val recName =
                        el.selectFirst(".tray-item-title")?.text()?.trim() ?: return@forEach
                    val recHref = fixUrl(el.selectFirst("a")?.attr("href") ?: return@forEach)
                    val recPoster = fixUrlNull(
                        el.selectFirst("img")?.attr("data-src") ?: el.selectFirst("img")
                            ?.attr("src")
                    )
                    recommendations.add(
                        newMovieSearchResponse(
                            recName,
                            recHref,
                            TvType.NSFW
                        ) { this.posterUrl = recPoster })
                }
            } catch (e: Exception) {
            }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.duration = duration
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
        val res = app.get(data, headers = commonHeaders)
        val filmId = Regex("""var filmId\s*=\s*(\d+)""").find(res.text)?.groupValues?.get(1)

        if (filmId == null) {
            return false
        }

        val episodes = res.document.select(".episode-list button.btn-player")
        var foundAnyLink = false

        for (ep in episodes) {
            val episodeId = ep.attr("data-id")

            try {
                val ajaxResponse = app.post(
                    "${mainUrl}/ajax/player",
                    headers = commonHeaders.toMutableMap().apply {
                        put("Referer", data)
                    },
                    data = mapOf("episode" to episodeId, "filmId" to filmId)
                ).text

                val iframeUrl = Regex("""src=\\?["'](https:.*?)(?:\?|\\?["']|["'])""")
                    .find(ajaxResponse)?.groupValues?.get(1)
                    ?.replace("\\/", "/")

                if (iframeUrl != null && !iframeUrl.contains("upgrade")) {
                    val wasExtracted = loadExtractor(iframeUrl, data, subtitleCallback, callback)
                    if (wasExtracted) foundAnyLink = true
                }
            } catch (_: Exception) {
            }
        }
        return foundAnyLink
    }
}