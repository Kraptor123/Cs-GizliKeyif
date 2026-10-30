// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Aki : MainAPI() {
    override var mainUrl              = "https://aki-h.com"
    override var name                 = "Aki"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/genre/3d/"                 to "3D",
        "${mainUrl}/genre/ahegao/"             to "Ahegao",
        "${mainUrl}/genre/anal/"               to "Anal",
        "${mainUrl}/genre/bdsm/"               to "BDSM",
        "${mainUrl}/genre/big-boobs/"          to "Big Boobs",
        "${mainUrl}/genre/blow-job/"           to "Blow Job",
        "${mainUrl}/genre/bondage/"            to "Bondage",
        "${mainUrl}/genre/paizuri/"            to "Paizuri",
        "${mainUrl}/genre/yuri/"               to "Yuri",
        "${mainUrl}/genre/comedy/"             to "Comedy",
        "${mainUrl}/genre/cosplay/"            to "Cosplay",
        "${mainUrl}/genre/creampie/"           to "Creampie",
        "${mainUrl}/genre/big-breast/"         to "Big breast",
        "${mainUrl}/genre/yaoi/"               to "Yaoi",
        "${mainUrl}/genre/fantasy/"            to "Fantasy",
        "${mainUrl}/genre/double-penetration/" to "Double penetration",
        "${mainUrl}/genre/foot-job/"           to "Foot Job",
        "${mainUrl}/genre/futanari/"           to "Futanari",
        "${mainUrl}/genre/gangbang/"           to "Gangbang",
        "${mainUrl}/genre/hospital/"           to "Hospital",
        "${mainUrl}/genre/hand-job/"           to "Hand Job",
        "${mainUrl}/genre/harem/"              to "Harem",
        "${mainUrl}/genre/sex-toys/"           to "Sex Toys",
        "${mainUrl}/genre/family/"             to "Family",
        "${mainUrl}/genre/incest/"             to "Incest",
        "${mainUrl}/genre/romoance/"           to "Romoance",
        "${mainUrl}/genre/school/"             to "School",
        "${mainUrl}/genre/loli/"               to "Loli",
        "${mainUrl}/genre/maid/"               to "Maid",
        "${mainUrl}/genre/masturbation/"       to "Masturbation",
        "${mainUrl}/genre/milf/"               to "Milf",
        "${mainUrl}/genre/mind-break/"         to "Mind Break",
        "${mainUrl}/genre/mind-control/"       to "Mind Control",
        "${mainUrl}/genre/monster/"            to "Monster",
        "${mainUrl}/genre/bitch/"              to "Bitch",
        "${mainUrl}/genre/ntr/"                to "NTR",
        "${mainUrl}/genre/nurse/"              to "Nurse",
        "${mainUrl}/genre/drama/"              to "Drama",
        "${mainUrl}/genre/blackmail/"          to "Blackmail",
        "${mainUrl}/genre/pov/"                to "POV",
        "${mainUrl}/genre/virgin/"             to "Virgin",
        "${mainUrl}/genre/public-sex/"         to "Public Sex",
        "${mainUrl}/genre/rape/"               to "Rape",
        "${mainUrl}/genre/reverse-rape/"       to "Reverse Rape",
        "${mainUrl}/genre/demon/"              to "Demon",
        "${mainUrl}/genre/remove-censored/"    to "Remove Censored",
        "${mainUrl}/genre/bukkake/"            to "Bukkake",
        "${mainUrl}/genre/shota/"              to "Shota",
        "${mainUrl}/genre/softcore/"           to "Softcore",
        "${mainUrl}/genre/swimsuit/"           to "Swimsuit",
        "${mainUrl}/genre/teacher/"            to "Teacher",
        "${mainUrl}/genre/tentacles/"          to "Tentacles",
        "${mainUrl}/genre/threesome/"          to "Threesome",
        "${mainUrl}/genre/vanilla/"            to "Vanilla",
        "${mainUrl}/genre/trap/"               to "Trap",
        "${mainUrl}/genre/hardCore/"           to "HardCore",
        "${mainUrl}/genre/2d/"                 to "2D",
        "${mainUrl}/genre/furry/"              to "Furry"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            "${request.data.removeSuffix("/")}/page/$page/"
        }

        val res     = app.get(url)
        val doc     = res.document
        val home    = doc.select("div.ak-grid div.ak-card, div.ak-card, div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }
        val hasNext = doc.selectFirst("nav.ak-pager a[title*='Next'], nav.ak-pager li a:contains(›), nav.ak-pager li.active + li a") != null || home.isNotEmpty()

        return newHomePageResponse(request.name, home, hasNext = hasNext)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title  = this.selectFirst("h3.ak-card__title a, h3.ak-card__title, h3.film-name a")?.text()?.trim()
            ?: this.selectFirst("a.ak-card__poster")?.attr("title")?.trim()
            ?: this.selectFirst("a")?.attr("title")?.trim()
            ?: return null
        val href   = fixUrlNull(
            this.selectFirst("a.ak-card__poster, h3.ak-card__title a, a.film-poster-ahref, a")?.attr("href")
        ) ?: return null
        val imgEl  = this.selectFirst("img")
        val poster = fixUrlNull(
            imgEl?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: imgEl?.attr("src")?.takeIf { it.isNotBlank() }
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/?s=$query"
        } else {
            "$mainUrl/page/$page/?s=$query"
        }
        val res = app.get(
            url,
            headers = mapOf(
                "Referer" to "$mainUrl/"
            )
        )
        val doc    = res.document
        val search = doc.select("div.ak-grid div.ak-card, div.ak-card, div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }
        val hasNext = doc.selectFirst("nav.ak-pager a[title*='Next'], nav.ak-pager li a:contains(›)") != null || search.isNotEmpty()

        return newSearchResponseList(search, hasNext = hasNext)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val res   = app.get(url)
        val doc   = res.document
        val title = doc.selectFirst("h1.ak-detail__title")?.text()?.trim()
            ?: doc.selectFirst("h2.film-name.dynamic-name a, h2.film-name, h1")?.text()?.trim()
            ?: return null

        val poster = fixUrlNull(
            doc.selectFirst("div.ak-detail__poster img")?.attr("src")
                ?: doc.selectFirst("div.ak-detail__poster img")?.attr("data-src")
                ?: doc.selectFirst("div.anis-cover")?.attr("style")?.let {
                    Regex("""url\((.*)\)""").find(it)?.groupValues?.get(1)
                }
                ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
        )

        val plot = doc.selectFirst("div.ak-detail__desc")?.text()?.trim()
            ?: doc.selectFirst("div.film-description div.text")?.text()?.trim()
            ?: doc.selectFirst("div.item.item-title.w-hide div.text")?.text()?.trim()
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        val yil  = doc.selectFirst("div.ak-info__row:has(b:contains(Premiered))")?.text()
            ?: doc.selectFirst("div.ak-info__row:has(b:contains(Aired))")?.text()
            ?: doc.selectFirst("div.item:contains(Premiered:) .name")?.text()
            ?: doc.selectFirst("div.item:contains(Released:) .name")?.text()
        val year = Regex("""(\d{4})""").find(yil ?: "")?.groupValues?.get(1)?.toIntOrNull()

        val tags = doc.select("span.ak-chips a, div.ak-info__row:has(b:contains(Genres)) a, div.item.item-list:contains(Genres:) a, div.genres a").map {
            it.text().trim()
        }.distinct()

        val scoreval = doc.selectFirst("span.item:contains(Score:) .name")?.text()?.trim()?.toDoubleOrNull()
        val score    = scoreval?.let { Score.from(it, 10) }

        val actors = doc.select("div.cast-item").mapNotNull {
            val actorName = it.selectFirst(".name")?.text()?.trim()?.ifEmpty { null } ?: return@mapNotNull null
            Actor(actorName)
        }

        val epElements = doc.select("div.ak-eps div.ak-ep, div.ak-ep, div.ss-list a.ssl-item, div.live_content div.item, div.live__-wrap div.item")
        val parsedEpisodes = epElements.mapIndexedNotNull { index, element ->
            val rawName = element.selectFirst("h3.ak-ep__title a, h3.ak-ep__title, div.ep-name, h3.live-name a")?.text()?.trim()
                ?: element.selectFirst("a")?.attr("title")?.trim()
                ?: element.attr("title").trim()
            val href = fixUrlNull(
                element.selectFirst("a.ak-ep__thumb, h3.ak-ep__title a, h3.live-name a, a.live-thumbnail, a.ssl-item")?.attr("href")
                    ?: element.attr("href")
            ) ?: return@mapIndexedNotNull null

            val thumb = fixUrlNull(
                element.selectFirst("img")?.attr("data-src")?.ifEmpty { null }
                    ?: element.selectFirst("img")?.attr("src")?.ifEmpty { null }
            )
            val epNum = Regex("""(?:Vol|ตอนที่|Ep|Episode)\s*(\d+)""", RegexOption.IGNORE_CASE).find(rawName)?.groupValues?.get(1)?.toIntOrNull()
                ?: (index + 1)

            var cleanName = rawName.replace(title, "", ignoreCase = true)
                .trim()
                .removePrefix("-")
                .removePrefix(":")
                .trim()

            if (cleanName.isBlank()) {
                cleanName = "Episode $epNum"
            }

            newEpisode(href) {
                this.name      = cleanName
                this.episode   = epNum
                this.posterUrl = thumb
            }
        }.distinctBy { it.data }

        val recommendations = doc.select("div.ak-grid div.ak-card, div.ak-card, section.block_area_category div.flw-item").mapNotNull {
            it.toSearchResult()
        }

        val movieDataUrl = parsedEpisodes.firstOrNull()?.data ?: url

        return if (parsedEpisodes.size <= 1) {
            newMovieLoadResponse(title, url, TvType.NSFW, movieDataUrl) {
                this.posterUrl       = poster
                this.plot            = plot
                this.year            = year
                this.tags            = tags
                this.score           = score
                this.recommendations = recommendations
                addActors(actors)
            }
        } else {
            newTvSeriesLoadResponse(title, url, TvType.NSFW, parsedEpisodes) {
                this.posterUrl       = poster
                this.plot            = plot
                this.year            = year
                this.tags            = tags
                this.score           = score
                this.recommendations = recommendations
                addActors(actors)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(name, "data = $data")
        var doc = app.get(data).document
        if (!data.contains("/watch/")) {
            val watchLink = fixUrlNull(doc.selectFirst("div.ak-eps a.ak-ep__thumb, h3.ak-ep__title a, a[href*='/watch/']")?.attr("href"))
            if (watchLink != null) {
                doc = app.get(watchLink).document
            }
        }
        var count = 0

        doc.select("div.server-item[data-type=dl] a[href], div.ps__-list a[href]").forEach { el ->
            val link = fixUrlNull(el.attr("href").ifEmpty { return@forEach }) ?: return@forEach
            if (link.contains("gofile.io") || link.contains("rapidgator.net")) {
                Log.d(name, "dl link = $link")
                loadExtractor(link, data, subtitleCallback, callback)
                count++
            }
        }

        val html    = doc.html().replace("\\/", "/")
        val videoId = Regex("""displayvideo\s*\(\s*\d+\s*,\s*(\d+)\s*\)""").find(html)?.groupValues?.get(1)
            ?: doc.selectFirst(".server-item[data-server-id='1'], .server-item[data-id]")?.attr("data-id")?.ifEmpty { null }

        Log.d(name, "videoId = $videoId")
        if (videoId.isNullOrEmpty()) return count > 0

        val videoPageUrl = fixUrl("/video/$videoId/")
        Log.d(name, "videoPageUrl = $videoPageUrl")

        val videoDoc  = app.get(videoPageUrl, referer = data).document
        val videoHtml = videoDoc.html().replace("\\/", "/")

        val vUrlRaw = Regex(""""source"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"""").find(videoHtml)?.groupValues?.get(1)?.replace("\\/", "/")
            ?: videoDoc.selectFirst("iframe[src*='v.aki-h.com']")?.attr("src")?.ifEmpty { null }

        Log.d(name, "vUrlRaw = $vUrlRaw")
        if (vUrlRaw.isNullOrEmpty()) return count > 0

        val vUrl  = fixUrl(vUrlRaw)
        val vDoc  = app.get(vUrl, referer = videoPageUrl).document
        val vHtml = vDoc.html().replace("\\/", "/")

        val fPath = vDoc.selectFirst("a#play, a.cover__link")?.attr("href")?.ifEmpty { null }
            ?: Regex("""/f/([a-zA-Z0-9]+)""").find(vHtml)?.groupValues?.get(0)

        Log.d(name, "fPath = $fPath")
        if (fPath.isNullOrEmpty()) return count > 0

        val fUrl     = fixUrl(if (fPath.startsWith("http")) fPath else "https://v.aki-h.com${if (fPath.startsWith("/")) "" else "/"}$fPath")
        val fDoc     = app.get(fUrl, referer = vUrl).document
        val fHtml    = fDoc.html().replace("\\/", "/")
        val bootHtml = fDoc.selectFirst("script#boot")?.html()?.replace("\\/", "/") ?: fHtml

        val serverUrls = Regex("""https?://streaming\.aki\.today/playback/[^"'\s\\]+""").findAll(bootHtml)
            .map { it.value }
            .distinct()
            .toList()

        Log.d(name, "serverUrls count = ${serverUrls.size}")

        serverUrls.forEach { pUrl ->
            try {
                Log.d(name, "pUrl = $pUrl")
                val pDoc  = app.get(pUrl, headers = mapOf("Referer" to "https://v.aki-h.com/")).document
                val pHtml = pDoc.html().replace("\\/", "/")

                val streamUrls = pDoc.select("iframe[src*='aki-h.stream']").mapNotNull { it.attr("src").ifEmpty { null } } +
                        pDoc.select("div.noshow").map { it.text().trim() } +
                        Regex("""https?://aki-h\.stream/v2?/[a-zA-Z0-9]+""").findAll(pHtml).map { it.value }.toList()

                streamUrls.distinct().forEach { sUrl ->
                    val cleanSurl = fixUrl(sUrl.replace("\\/", "/"))
                    val sid       = cleanSurl.split("/").lastOrNull { it.isNotBlank() }
                    if (!sid.isNullOrBlank()) {
                        val m3u8Url = "https://aki-h.stream/file/$sid/"
                        Log.d(name, "m3u8Url = $m3u8Url")

                        callback(
                            newExtractorLink(
                                source = name,
                                name   = name,
                                url    = m3u8Url,
                                type   = ExtractorLinkType.M3U8
                            ) {
                                this.referer = "https://aki-h.stream/v/$sid"
                                this.headers = mutableMapOf(
                                    "Referer" to "https://aki-h.stream/v/$sid",
                                    "Accept"  to "*/*"
                                )
                            }
                        )
                        count++
                    }
                }
            } catch (e: Exception) {
                Log.d(name, "pUrl error = $e")
            }
        }

        return count > 0
    }
}
