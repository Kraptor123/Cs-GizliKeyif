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
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/genre/3d/" to "3D",
        "${mainUrl}/genre/ahegao/" to "Ahegao",
        "${mainUrl}/genre/anal/" to "Anal",
        "${mainUrl}/genre/bdsm/" to "BDSM",
        "${mainUrl}/genre/big-boobs/" to "Big Boobs",
        "${mainUrl}/genre/blow-job/" to "Blow Job",
        "${mainUrl}/genre/bondage/" to "Bondage",
        "${mainUrl}/genre/paizuri/" to "Paizuri",
        "${mainUrl}/genre/yuri/" to "Yuri",
        "${mainUrl}/genre/comedy/" to "Comedy",
        "${mainUrl}/genre/cosplay/" to "Cosplay",
        "${mainUrl}/genre/creampie/" to "Creampie",
        "${mainUrl}/genre/big-breast/" to "Big breast",
        "${mainUrl}/genre/yaoi/" to "Yaoi",
        "${mainUrl}/genre/fantasy/" to "Fantasy",
        "${mainUrl}/genre/double-penetration/" to "Double penetration",
        "${mainUrl}/genre/foot-job/" to "Foot Job",
        "${mainUrl}/genre/futanari/" to "Futanari",
        "${mainUrl}/genre/gangbang/" to "Gangbang",
        "${mainUrl}/genre/hospital/" to "Hospital",
        "${mainUrl}/genre/hand-job/" to "Hand Job",
        "${mainUrl}/genre/harem/" to "Harem",
        "${mainUrl}/genre/sex-toys/" to "Sex Toys",
        "${mainUrl}/genre/family/" to "Family",
        "${mainUrl}/genre/incest/" to "Incest",
        "${mainUrl}/genre/romoance/" to "Romoance",
        "${mainUrl}/genre/school/" to "School",
        "${mainUrl}/genre/loli/" to "Loli",
        "${mainUrl}/genre/maid/" to "Maid",
        "${mainUrl}/genre/masturbation/" to "Masturbation",
        "${mainUrl}/genre/milf/" to "Milf",
        "${mainUrl}/genre/mind-break/" to "Mind Break",
        "${mainUrl}/genre/mind-control/" to "Mind Control",
        "${mainUrl}/genre/monster/" to "Monster",
        "${mainUrl}/genre/bitch/" to "Bitch",
        "${mainUrl}/genre/ntr/" to "NTR",
        "${mainUrl}/genre/nurse/" to "Nurse",
        "${mainUrl}/genre/drama/" to "Drama",
        "${mainUrl}/genre/blackmail/" to "Blackmail",
        "${mainUrl}/genre/pov/" to "POV",
        "${mainUrl}/genre/virgin/" to "Virgin",
        "${mainUrl}/genre/public-sex/" to "Public Sex",
        "${mainUrl}/genre/rape/" to "Rape",
        "${mainUrl}/genre/reverse-rape/" to "Reverse Rape",
        "${mainUrl}/genre/demon/" to "Demon",
        "${mainUrl}/genre/remove-censored/" to "Remove Censored",
        "${mainUrl}/genre/bukkake/" to "Bukkake",
        "${mainUrl}/genre/shota/" to "Shota",
        "${mainUrl}/genre/softcore/" to "Softcore",
        "${mainUrl}/genre/swimsuit/" to "Swimsuit",
        "${mainUrl}/genre/teacher/" to "Teacher",
        "${mainUrl}/genre/tentacles/" to "Tentacles",
        "${mainUrl}/genre/threesome/" to "Threesome",
        "${mainUrl}/genre/vanilla/" to "Vanilla",
        "${mainUrl}/genre/trap/" to "Trap",
        "${mainUrl}/genre/hardCore/" to "HardCore",
        "${mainUrl}/genre/2d/" to "2D",
        "${mainUrl}/genre/furry/" to "Furry"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            "${request.data.removeSuffix("/")}/page/$page/"
        }

        val res = app.get(url)
        val doc = res.document
        val home = doc.select("div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.film-name a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a.film-poster-ahref")?.attr("href")) ?: return null
        val poster = fixUrlNull(this.selectFirst("img.film-poster-img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = "$mainUrl/search/?q=$query&page=$page"
        val res = app.get(
            url,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        )
        val doc = res.document

        val search = doc.select("div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }

        return newSearchResponseList(search, hasNext = search.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val res = app.get(url)
        val doc = res.document
        val title = doc.selectFirst("h2.film-name.dynamic-name")?.text()?.trim()
            ?: doc.selectFirst("h1")?.text()?.trim() ?: return null

        val poster = fixUrlNull(doc.selectFirst("div.anis-cover")?.attr("style")?.let {
            Regex("""url\((.*)\)""").find(it)?.groupValues?.get(1)
        } ?: doc.selectFirst("meta[property=og:image]")?.attr("content"))

        val plot = doc.selectFirst("div.item.item-title.w-hide div.text")?.text()?.trim()
            ?: doc.selectFirst("div.film-description.readmore.js-readmore")?.text()?.trim()
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        val yil = doc.selectFirst("div.item:contains(Premiered:) .name")?.text() ?:
        doc.selectFirst("div.item:contains(Released:) .name")?.text()
        val year = Regex("""(\d{4})""").find(yil ?: "")?.groupValues?.get(1)?.toIntOrNull()

        val tags = doc.select("div.item.item-list:contains(Genres:) a").map { it.text() }.ifEmpty {
            doc.select("div.genres a").map { it.text() }
        }

        val scoreval = doc.selectFirst("span.item:contains(Score:) .name")?.text()?.trim()?.toDoubleOrNull()
        val score = scoreval?.let { Score.from(it, 10) }

        val actors = doc.select("div.cast-item").map {
            Actor(it.selectFirst(".name")?.text() ?: "")
        }

        val episodes = doc.select("div.live_content div.item, div.live__-wrap div.item").mapNotNull {
            val name = it.selectFirst("h3.live-name a")?.text()?.trim() ?: ""
            val href = fixUrlNull(it.selectFirst("a.live-thumbnail")?.attr("href")) ?: return@mapNotNull null
            val thumb = it.selectFirst("img.live-thumbnail-img")?.attr("data-src")
            val epnum = Regex("""(?:Vol|ตอนที่)\s*(\d+)""").find(name)?.groupValues?.get(1)?.toIntOrNull()

            newEpisode(href) {
                this.name = name
                this.episode = epnum
                this.posterUrl = thumb
            }
        }

        val recommendations = doc.select("section.block_area_category div.flw-item").mapNotNull {
            it.toSearchResult()
        }

        return if (episodes.size <= 1) {
            newMovieLoadResponse(title, url, TvType.NSFW, episodes.firstOrNull()?.data ?: url) {
                this.posterUrl = poster
                this.plot = plot
                this.year = year
                this.tags = tags
                this.score = score
                this.recommendations = recommendations
                addActors(actors)
            }
        } else {
            newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes) {
                this.posterUrl = poster
                this.plot = plot
                this.year = year
                this.tags = tags
                this.score = score
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
        Log.d("Aki", data)
        val res = app.get(data)
        val doc = res.document

        doc.select("div.item.server-item[data-type=dl] a.btn").forEach { el ->
            val link = el.attr("href")
            Log.d("Aki", link)
            if (link.contains("gofile.io")) {
                loadExtractor(link, this.mainUrl, subtitleCallback, callback)
            }
        }

        val script = doc.select("script").find { it.html().contains("window.displayvideo") }?.html() ?: ""
        val id = Regex("""displayvideo\(\d+,\s*(\d+)\)""").find(script)?.groupValues?.get(1)
        Log.d("Aki", id ?: "")

        id?.let { vid ->
            val vurl = "https://v.aki-h.com/v/$vid"
            Log.d("Aki", vurl)

            val vdoc = app.get(vurl, referer = this.mainUrl).document
            val embedid = Regex("""var vid = '(.*?)'""").find(vdoc.html())?.groupValues?.get(1)
            Log.d("Aki", embedid ?: "")

            embedid?.let { eid ->
                val furl = "https://v.aki-h.com/f/$eid"
                val fdoc = app.get(furl, referer = vurl).document

                val playurl = fdoc.select("script").mapNotNull { s ->
                    val h = s.html()
                    when {
                        h.contains("streaming.aki.today/playback/") -> {
                            Regex("""src\s*=\s*"(https://streaming\.aki\.today/playback/[^"]+)"""").find(h)?.groupValues?.get(1)
                                ?: Regex("""iframe src="(https://streaming\.aki\.today/playback/[^"]+)"""").find(h)?.groupValues?.get(1)
                        }
                        else -> null
                    }
                }.firstOrNull()

                playurl?.let { purl ->
                    Log.d("Aki", purl)
                    val pdoc = app.get(purl, referer = furl).document
                    val source = pdoc.selectFirst("iframe.embed-responsive-item")?.attr("src")
                    Log.d("Aki", source ?: "")

                    source?.let { surl ->
                        val sid = surl.split("/").lastOrNull { it.isNotEmpty() }
                        if (sid != null) {
                            val m3u8 = "https://aki-h.stream/file/$sid/"
                            Log.d("Aki", m3u8)

                            val content = app.get(
                                m3u8,
                                referer = surl,
                                headers = mapOf("Accept" to "*/*")
                            ).text

                            val master = Regex("""RESOLUTION=\d+x(\d+).*?\n(.*)""")
                            master.findAll(content).forEach { m ->
                                val resnum = m.groupValues[1]
                                val stream = m.groupValues[2].trim()
                                Log.d("Aki", stream)

                                val q = when {
                                    resnum.contains("1080") -> Qualities.P1080.value
                                    resnum.contains("720") -> Qualities.P720.value
                                    resnum.contains("480") -> Qualities.P480.value
                                    resnum.contains("360") -> Qualities.P360.value
                                    else -> Qualities.Unknown.value
                                }

                                callback.invoke(
                                    newExtractorLink(
                                        this.name,
                                        this.name,
                                        stream,
                                        ExtractorLinkType.M3U8
                                    ) {
                                        this.quality = q
                                        this.referer = surl
                                        this.headers = mapOf("Accept" to "*/*")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        return true
    }
}