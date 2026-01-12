// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Hentaila : MainAPI() {
    override var mainUrl = "https://hentaila.com"
    override var name = "Hentaila"
    override val hasMainPage = true
    override var lang = "mx"
    override val hasQuickSearch = false
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)
    //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    private val categoryUrl = "${mainUrl}/catalogo"

    override val mainPage = mainPageOf(
        mainUrl to "Episodios Recientemente Actualizados",
        "?status=emision&order=latest_released" to "Últimos Estrenados",
        "?status=emision&order=latest_added" to "Últimos Añadidos",
        "?genre=Casadas" to "Casadas",
        "?genre=Ahegao" to "Ahegao",
        "?genre=Chikan" to "Chikan",
        "?genre=Anal" to "Anal",
        "?genre=Enfermeras" to "Enfermeras",
        "?genre=Futanari" to "Futanari",
        "?genre=Hardcore" to "Hardcore",
        "?genre=Incesto" to "Incesto",
        "?genre=Milfs" to "Maduras",
        "?genre=Juegos Sexuales" to "Juegos Sexuales",
        "?genre=Maids" to "Sirvientas",
        "?genre=Netorare" to "Netorare",
        "?genre=Ninfomania" to "Ninfomanía",
        "?genre=Ninjas" to "Ninjas",
        "?genre=Shota" to "Shota",
        "?genre=Orgias" to "Orgías",
        "?genre=Succubus" to "Súcubo",
        "?genre=Teacher" to "Profesoras",
        "?genre=Tentaculos" to "Tentáculos",
        "?genre=Tetonas" to "Pechonas",
        "?genre=Vanilla" to "Vainilla",
        "?genre=Virgenes" to "Vírgenes",
        "?genre=Bondage" to "Bondage",
        "?genre=Threesome" to "Trío",
        "?genre=Elfas" to "Elfas",
        "?genre=Petit" to "Petit",
        "?genre=Paizuri" to "Paizuri",
        "?genre=Gal" to "Gal",
        "?genre=Oyakodon" to "Oyakodon"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (request.data.contains("$mainUrl")){
            val document = app.get(request.data).document

            val home = document.select("section:has(h2:contains(episo)) div.grid article").mapNotNull { it.toMainPageResult() }

            return newHomePageResponse(request.name, home, false)
        } else {
            val document = if (page == 1) {
                app.get("$categoryUrl${request.data.lowercase()}").document
            } else {
                app.get("$categoryUrl${request.data.lowercase()}&page=$page").document
            }
            val home = document.select("div.grid.grid-cols-2 article.group\\/item").mapNotNull { it.toMainPageResult() }

            return newHomePageResponse(request.name, home)
        }
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.text() ?: this.selectFirst("span.sr-only")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1) {
            app.get("${mainUrl}/catalogo?search=${query}").document
        } else {
            app.get("${mainUrl}/catalogo?search=${query}&page=$page").document
        }

        val aramaCevap =
            document.select("div.grid.grid-cols-2 article.group\\/item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val afterMedia = url.substringAfter("media/", "")
        val isEpisode = afterMedia.contains("/")
        val requestUrl = if (isEpisode) url.substringBeforeLast("/") else url
        val document = app.get(requestUrl, referer = "$mainUrl/").document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("img.aspect-poster")?.attr("src"))
        val description = document.selectFirst("div.entry.text-lead p")?.text()?.trim()
        val year = document.selectFirst("div.text-sm span:contains(0)")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.flex-wrap.gap-2 a[href*=genre]").map { it.text() }
        val rating = document.selectFirst("div.flex-wrap div.text-lead")?.text()?.trim()?.toIntOrNull()
        val duration = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("article.bg-mute").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("span.valor a").map { Actor(it.text()) }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }
        val episodes = document.select("div.grid.grid-cols-2 article.group\\/item").map { episode ->
            val href = fixUrlNull(episode.selectFirst("a")?.attr("href")) ?: return null
            val name = episode.selectFirst("div.bg-line")?.text()
            val poster = episode.selectFirst("img")?.attr("src")
            val episodeN = episode.selectFirst("div.bg-line span")?.text()?.toIntOrNull()
            val season =
                title.substringAfter("season").substringAfter("part").replace(" ", "").trim().toIntOrNull() ?: 1
            newEpisode(href, {
                this.name = name
                this.posterUrl = poster
                this.episode = episodeN
                this.season = season
            })
        }

        return newAnimeLoadResponse(title, url, TvType.Anime, true) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
            this.episodes = mutableMapOf(DubStatus.None to episodes)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val script = document.selectFirst("script:containsData(__sveltekit)")?.data() ?: return false

        val embedsData = script.substringAfter("embeds:{", "").substringBefore("},downloads", "")

        if (embedsData.isEmpty()) return false

        listOf("DUB", "SUB").forEach { type ->

            val listPattern = Regex("""$type:\[(.*?)\]""")
            val listMatch = listPattern.find(embedsData)?.groupValues?.get(1)

            if (listMatch != null) {
                val itemPattern = Regex("""\{server:"([^"]+)",\s*url:"([^"]+)"""")

                itemPattern.findAll(listMatch).forEach { match ->
                    val server = match.groupValues[1]
                    val url = match.groupValues[2]

                    Log.d("kraptor_${this.name}", "Type: $type, Server: $server, URL: $url")

                    if (server.equals("PDrain", ignoreCase = true)) {
                        PixelDrain().getUrl("$url|$type", subtitleCallback = subtitleCallback, callback = callback)
                    } else if (server.equals("MP4Upload", ignoreCase = true)) {
                        Mp4Upload().getUrl("$url|$type", subtitleCallback = subtitleCallback, callback = callback)
                    } else if (server.equals("HLS", ignoreCase = true)) {
                        PlayerZilla().getUrl("$url|$type", subtitleCallback = subtitleCallback, callback = callback)
                    } else if (url.contains("cdn.hvidserv.com/play/")) {
                        val m3u8Url = url.replace("/play/", "/m3u8/")
                        callback.invoke(
                            newExtractorLink(
                                source = "Hentaila",
                                name = "Hentaila",
                                url = m3u8Url,
                                type = ExtractorLinkType.M3U8
                            ) {
                                this.quality = Qualities.Unknown.value
                                this.referer = ""
                                this.headers = mapOf("sec-fetch-site" to "same-origin")
                            }
                        )
                    } else {
                        loadExtractor(url, subtitleCallback, callback)
                    }
                }
            }
        }

        return true
    }
}