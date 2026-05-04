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
        "${mainUrl}/genre/amateur" to "Amateur",
        "${mainUrl}/genre/anal" to "Anal",
        "${mainUrl}/genre/av-idol" to "AV Idol",
        "${mainUrl}/genre/beautiful-girl" to "Beautiful Girl",
        "${mainUrl}/genre/beautiful-pussy" to "Beautiful Pussy",
        "${mainUrl}/genre/big-asses" to "Big Asses",
        "${mainUrl}/genre/big-tits" to "Big Tits",
        "${mainUrl}/genre/blowjob" to "Blowjob",
        "${mainUrl}/genre/bondage" to "Bondage",
        "${mainUrl}/genre/bukkake" to "Bukkake",
        "${mainUrl}/genre/cheating-wife" to "Cheating Wife",
        "${mainUrl}/genre/cosplay" to "Cosplay",
        "${mainUrl}/genre/creampie" to "Creampie",
        "${mainUrl}/genre/cumshot" to "Cumshot",
        "${mainUrl}/genre/deep-throat" to "Deep Throat",
        "${mainUrl}/genre/doggy-style" to "Doggy Style",
        "${mainUrl}/genre/drama" to "Drama",
        "${mainUrl}/genre/facials" to "Facials",
        "${mainUrl}/genre/featured-actress" to "Featured Actress"
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
        Log.d("sextb", data)
        val res = app.get(data, headers = commonHeaders)

        val filmid = Regex("""var filmId\s*=\s*(\d+)""").find(res.text)?.groupValues?.get(1)
        var currentpt = Regex("""__pt\s*=\s*['"](.*?)['"]""").find(res.text)?.groupValues?.get(1)
        var currentpk = Regex("""__pk\s*=\s*['"](.*?)['"]""").find(res.text)?.groupValues?.get(1)

        Log.d("sextb", "$filmid")
        Log.d("sextb", "$currentpt")

        if (filmid == null || currentpt == null) {
            return false
        }

        val episodes = res.document.select(".episode-list button.btn-player")
        Log.d("sextb", "${episodes.size}")
        var foundanylink = false

        for (ep in episodes) {
            val episodeid = ep.attr("data-id")
            val sourceid = ep.attr("data-source").ifEmpty { filmid }
            Log.d("sextb", episodeid)

            try {
                val safept = currentpt ?: ""
                val postdata = mapOf(
                    "episode" to episodeid,
                    "filmId" to sourceid,
                    "pt" to safept
                )

                val ajaxresponse = app.post(
                    "${mainUrl}/ajax/player",
                    headers = mapOf(
                        "Referer" to data,
                        "X-Requested-With" to "XMLHttpRequest",
                        "Authorization" to "Basic Y1ZGUWNVSnROVlJOTUVWMlZsUldVMjlsWjBGelFUMDk6T0ZaNksxQmhjVTFhTHpCdFlWZDFNbE5CUm01Qlp6MDk=",
                        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                        "Accept" to "*/*",
                        "Origin" to mainUrl
                    ),
                    data = postdata
                )

                Log.d("sextb", ajaxresponse.text)

                val responsedata = ajaxresponse.parsedSafe<PlayerResponse>()
                val encryptedplayer = responsedata?.player_enc
                val key = currentpk ?: ""

                responsedata?.next_pt?.let { currentpt = it }
                responsedata?.next_pk?.let { currentpk = it }

                if (encryptedplayer != null && key.isNotEmpty()) {
                    val decryptedraw = decryptPlayer(encryptedplayer, key)
                    Log.d("sextb", decryptedraw)

                    val iframeurl = Regex("""src=\\?["'](https:.*?)(?:\?|\\?["']|["'])""")
                        .find(decryptedraw)?.groupValues?.get(1)
                        ?.replace("\\/", "/")

                    if (iframeurl != null && !iframeurl.contains("upgrade")) {
                        val wasextracted = loadExtractor(iframeurl, data, subtitleCallback, callback)
                        Log.d("sextb", "$wasextracted")
                        if (wasextracted) foundanylink = true
                    }
                }
            } catch (e: Exception) {
                Log.d("sextb", "${e.message}")
            }
        }
        Log.d("sextb", "$foundanylink")
        return foundanylink
    }

    private fun decryptPlayer(encoded: String, key: String): String {
        return try {
            val decoded = base64Decode(encoded)
            val result = StringBuilder()
            for (i in decoded.indices) {
                result.append((decoded[i].toInt() xor key[i % key.length].toInt()).toChar())
            }
            result.toString()
        } catch (e: Exception) {
            Log.d("sextb", "${e.message}")
            ""
        }
    }

    data class PlayerResponse(
        val player_enc: String? = null,
        val next_pt: String? = null,
        val next_pk: String? = null
    )
}