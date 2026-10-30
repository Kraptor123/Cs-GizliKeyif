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
import java.net.URI

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

    override var sequentialMainPage = true
    override var sequentialMainPageDelay       = 1400L
    override var sequentialMainPageScrollDelay = 1400L

    override val mainPage = mainPageOf(
        "${mainUrl}/genre/amateur" to "Amateur",
        "${mainUrl}/genre/anal" to "Anal",
        "${mainUrl}/genre/av-idol" to "AV Idol",
        "${mainUrl}/genre/beautiful-girl" to "Beautiful Girl",
        "${mainUrl}/genre/beautiful-pussy" to "Beautiful Pussy",
        "${mainUrl}/genre/big-asses" to "Big Asses",
        //"${mainUrl}/genre/big-tits" to "Big Tits",
      //"${mainUrl}/genre/blowjob" to "Blowjob",
      //"${mainUrl}/genre/bondage" to "Bondage",
      //"${mainUrl}/genre/bukkake" to "Bukkake",
      //"${mainUrl}/genre/cheating-wife" to "Cheating Wife",
      //"${mainUrl}/genre/cosplay" to "Cosplay",
      //"${mainUrl}/genre/creampie" to "Creampie",
      //"${mainUrl}/genre/cumshot" to "Cumshot",
      //"${mainUrl}/genre/deep-throat" to "Deep Throat",
      //"${mainUrl}/genre/doggy-style" to "Doggy Style",
      //"${mainUrl}/genre/drama" to "Drama",
      //"${mainUrl}/genre/facials" to "Facials",
      //"${mainUrl}/genre/featured-actress" to "Featured Actress"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page <= 1) {
            request.data
        } else {
            "${request.data.removeSuffix("/")}/pg-$page"
        }

        val document = app.get(url, headers = commonHeaders).document
        val items    = document.select("div.tray-item:not(.ad-card)").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(
            listOf(HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = true
            )),
            hasNext = items.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = "$mainUrl/ajax/search"
        val response = app.post(
            url,
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Content-Type" to "application/x-www-form-urlencoded;charset=UTF-8",
                "Referer" to "$mainUrl/"
            ),
            data = mapOf(
                "q" to query,
                "page" to page.toString()
            )
        )

        val json = response.text
        val hitsRegex = """\{"code":"([^"]+)","code_compact":"[^"]*","content_id":"[^"]*","name":"([^"]+)"[^}]+,"_formatted":\{[^}]+"poster":"([^"]+)"""".toRegex()
        val matches = hitsRegex.findAll(json)

        val items = matches.mapNotNull { match ->
            val code      = match.groupValues[1]
            val title     = match.groupValues[2].replace("<mark>", "").replace("</mark>", "")
            val posterUrl = match.groupValues[3].replace("\\/", "/")
            val fullHref  = "$mainUrl/$code"

            newMovieSearchResponse(title, fullHref, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }.toList()

        val totalPagesRegex = """"totalPages":(\d+)""".toRegex()
        val totalPages      = totalPagesRegex.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val hasNext         = page < totalPages

        return newSearchResponseList(items, hasNext = hasNext)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        if (this.hasClass("ad-card") || this.selectFirst(".ad-card, a[href*='/advertise']") != null) return null
        val linkElement = this.selectFirst("a") ?: return null
        val href        = linkElement.attr("href")
        if (href.startsWith("/search") || href.contains("javascript") || href.startsWith("/genre") || href.startsWith("/advertise")) return null
        val fullHref    = fixUrl(href)
        val title       = this.selectFirst("div.tray-item-title")?.text()?.trim()
            ?: this.selectFirst("img")?.attr("alt")?.trim()
            ?: return null

        val posterUrl   = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(title, fullHref, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val res      = app.get(url, headers = commonHeaders)
        val document = res.document

        val title    = document.selectFirst("h1.film-info-title")?.text()?.trim() ?: return null
        val poster   = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
            ?: fixUrlNull(document.selectFirst("#infomation img")?.attr("data-src"))

        val description = document.selectFirst("span.full-text-desc")?.text()?.trim()
        val yearText    = document.selectFirst("div.description:has(i.fa-calendar) strong")?.text()
        val year        = yearText?.let { Regex("""(\d{4})""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val duration    = document.selectFirst("div.description:has(i.fa-clock) strong")?.text()
            ?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
        val tags        = document.select("div.description:has(i.fa-list) a").map { it.text() }
        val actors      = document.select("div.description:has(i.fa-users) a").map { Actor(it.text()) }

        val recommendations = mutableListOf<SearchResponse>()
        val filmId          = Regex("""var filmId\s*=\s*(\d+)""").find(res.text)?.groupValues?.get(1)

        if (filmId != null) {
            try {
                val apiRes = app.get("${mainUrl}/ajax/related/$filmId", headers = commonHeaders).text
                val apiDoc = org.jsoup.Jsoup.parse(apiRes)
                apiDoc.select(".tray-item:not(.ad-card)").forEach { el ->
                    if (el.hasClass("ad-card") || el.selectFirst("a[href*='/advertise']") != null) return@forEach
                    val recName   = el.selectFirst(".tray-item-title")?.text()?.trim() ?: return@forEach
                    val recHref   = fixUrl(el.selectFirst("a")?.attr("href") ?: return@forEach)
                    val recPoster = fixUrlNull(
                        el.selectFirst("img")?.attr("data-src") ?: el.selectFirst("img")?.attr("src")
                    )
                    recommendations.add(
                        newMovieSearchResponse(
                            recName,
                            recHref,
                            TvType.NSFW
                        ) { this.posterUrl = recPoster }
                    )
                }
            } catch (e: Exception) {
            }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.duration        = duration
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

        val token  = res.document.selectFirst("meta#token, meta[name='_token']")?.attr("value")
        val socket = res.document.selectFirst("meta#socket, meta[name='_socket']")?.attr("value")

        val filmid    = Regex("""var filmId\s*=\s*(\d+)""").find(res.text)?.groupValues?.get(1)
        var currentpt = Regex("""__pt\s*=\s*['"](.*?)['"]""").find(res.text)?.groupValues?.get(1)
        var currentpk = Regex("""__pk\s*=\s*['"](.*?)['"]""").find(res.text)?.groupValues?.get(1)

        if (filmid == null || currentpt == null || token.isNullOrEmpty() || socket.isNullOrEmpty()) {
            return false
        }

        val auth = "Basic " + base64Encode("$token:$socket".toByteArray())

        val episodeButtons = res.document.select(".episode-list button.btn-player")
        val episodeIds     = mutableListOf("0")
        episodeButtons.forEach { btn ->
            val id = btn.attr("data-id")
            if (id.isNotEmpty() && id != "0" && !episodeIds.contains(id)) {
                episodeIds.add(id)
            }
        }

        var foundanylink = false

        for (episodeid in episodeIds) {
            try {
                val safept   = currentpt ?: ""
                val postdata = mapOf(
                    "episode" to episodeid,
                    "filmId" to filmid,
                    "pt" to safept
                )

                val ajaxresponse = app.post(
                    "${mainUrl}/ajax/player",
                    headers = mapOf(
                        "Referer" to data,
                        "X-Requested-With" to "XMLHttpRequest",
                        "Authorization" to auth,
                        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                        "Accept" to "*/*",
                        "Origin" to mainUrl
                    ),
                    cookies = res.cookies,
                    data = postdata
                )

                val responsedata    = ajaxresponse.parsedSafe<PlayerResponse>()
                val encryptedplayer = responsedata?.player_enc
                val key             = currentpk ?: ""

                responsedata?.next_pt?.let { currentpt = it }
                responsedata?.next_pk?.let { currentpk = it }

                if (encryptedplayer != null && key.isNotEmpty()) {
                    val decryptedraw = decryptPlayer(encryptedplayer, key)

                    val iframeurl = Regex("""src=\\?["'](https:[^"'\\>\s]+)""")
                        .find(decryptedraw)?.groupValues?.get(1)
                        ?.replace("\\/", "/")

                    if (iframeurl != null && !iframeurl.contains("upgrade")) {
                        if (iframeurl.contains("f4s")) {
                            try {
                                val embedDoc = app.get(iframeurl, referer = data).document
                                val dataApi  = embedDoc.selectFirst("div.player-wrap")?.attr("data-api")
                                    ?: Regex("""data-api=["']([^"']+)["']""").find(embedDoc.html())?.groupValues?.get(1)

                                if (dataApi != null) {
                                    val baseEmbed  = URI(iframeurl).let { "${it.scheme}://${it.host}" }
                                    val playApiUrl = "$baseEmbed$dataApi?ab=0&ad=0&r=https://sextb.net/&w=1"
                                    val playRes    = app.get(playApiUrl, referer = iframeurl).text
                                    val src        = Regex(""""src"\s*:\s*"([^"]+)"""").find(playRes)?.groupValues?.get(1)?.replace("\\/", "/")

                                    if (src != null) {
                                        callback.invoke(
                                            newExtractorLink(
                                                name   = "F4Stream",
                                                source = "F4Stream",
                                                url    = src,
                                                type   = ExtractorLinkType.M3U8
                                            ) {
                                                this.referer = "$baseEmbed/"
                                                this.headers = mapOf(
                                                    "Origin" to baseEmbed,
                                                    "Referer" to "$baseEmbed/"
                                                )
                                            }
                                        )
                                        foundanylink = true
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d("sextb", "${e.message}")
                            }
                        } else {
                            val wasextracted = loadExtractor(iframeurl, data, subtitleCallback, callback)
                            if (wasextracted) foundanylink = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("sextb", "${e.message}")
            }
        }
        return foundanylink
    }

    private fun decryptPlayer(encoded: String, key: String): String {
        return try {
            val decoded = base64Decode(encoded)
            val result  = StringBuilder()
            for (i in decoded.indices) {
                result.append(((decoded[i].toInt() and 0xFF) xor key[i % key.length].code).toChar())
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
