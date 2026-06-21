package com.byayzen

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.math.BigInteger

class EPorner : MainAPI() {
    override var mainUrl = "https://www.eporner.com"
    override var name = "EPorner"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Most recent",
        "$mainUrl/most-viewed/" to "Most viewed",
        "$mainUrl/top-rated/" to "Top rated",
        "$mainUrl/longest/" to "Longest",
        "$mainUrl/tag/cowgirl/" to "Cowgirl",
        "$mainUrl/tag/riding/" to "Riding",
        "$mainUrl/tag/turkish/" to "Turkish",
        "$mainUrl/cat/housewives/" to "Housewives"

    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data.removeSuffix("/")}/$page/"
        val home = app.get(url).document.select("div#vidresults div.mb").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, home, true), true)
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val formattedQuery = query.replace(" ", "-")
        val url = if (page <= 1) "$mainUrl/search/$formattedQuery/" else "$mainUrl/search/$formattedQuery/$page/"
        val results = app.get(url).document.select("div#vidresults div.mb").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(results, true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst("p.mbtit a") ?: return null
        val img = this.selectFirst("div.mbimg img")

        val poster = fixUrlNull(
            img?.attr("data-src")?.takeIf { it.isNotEmpty() && !it.startsWith("data:") }
                ?: img?.attr("src")
        )

        return newMovieSearchResponse(
            titleElement.text(),
            fixUrl(titleElement.attr("href")),
            TvType.NSFW
        ) {
            this.posterUrl = poster
        }
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(
            document.selectFirst("meta[property=og:image]")?.attr("content")
                ?: document.selectFirst("video#EPvideo")?.attr("poster")
        )
        val tags = document.select("div#video-info-tags ul li.vit-category a").map { it.text() }
        val year = document.selectFirst("span.C a")?.text()?.trim()?.toIntOrNull()
        val duration = document.selectFirst("span.vid-length")?.text()?.replace("min", "")?.trim()
            ?.toIntOrNull()
        val description =
            document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val recommendations =
            document.select("div#relateddiv div.mb").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("span.valor a").map { Actor(it.text()) }

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

    private fun Element.toRecommendationResult(): SearchResponse? {
        val titleElement = this.selectFirst("p.mbtit a") ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("div.mbimg img")?.attr("data-src") ?: this.selectFirst("div.mbimg img")
                ?.attr("src")
        )
        return newMovieSearchResponse(
            titleElement.text(),
            fixUrl(titleElement.attr("href")),
            TvType.NSFW
        ) {
            this.posterUrl = posterUrl
        }
    }



    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val url = fixUrl(data)
        var videolink = false

        try {
            val vidmatch = """/(?:embed|video)-([a-zA-Z0-9]+)""".toRegex().find(url)
            val vid = vidmatch?.groupValues?.get(1) ?: return false

            val embedurl = "$mainUrl/embed/$vid/"
            val embedhtml = app.get(embedurl).text

            val md5hash = """EP\.video\.player\.hash\s*=\s*'([^']+)'""".toRegex().find(embedhtml)?.groupValues?.get(1)
            if (md5hash == null) {
                Log.d(name, "hash bulunamadi")
                return false
            }
            val convertedhash = md5hash.chunked(8).map { chunk ->
                BigInteger(chunk, 16).toString(36)
            }.joinToString("")
            Log.d(name, "vid: $vid - hash: $convertedhash")
            val xhrurl = "$mainUrl/xhr/video/$vid?hash=$convertedhash&domain=www.eporner.com&pixelRatio=1&playerWidth=0&playerHeight=0&fallback=false&embed=true&supportedFormats=hls,dash,h265,vp9,av1,mp4&_=${System.currentTimeMillis()}"

            val responsetext = app.get(
                xhrurl,
                headers = mapOf(
                    "Referer" to embedurl,
                    "X-Requested-With" to "XMLHttpRequest"
                )
            ).text
            Log.d(name, "xhr yanit uzunlugu: ${responsetext.length}")

            """labelShort"\s*:\s*"(\d{3,4}p)[^"]*"\s*,\s*"src"\s*:\s*"([^"]+)"""".toRegex()
                .findAll(responsetext).forEach { match ->
                    val quality = match.groupValues[1]
                    val videourl = match.groupValues[2]
                    if (!videourl.contains("/dload/")) {
                        Log.d(name, "kalite: $quality - url: $videourl")
                        callback.invoke(
                            newExtractorLink(
                                name = name,
                                source = name,
                                url = videourl,
                                type = ExtractorLinkType.VIDEO
                            ) {
                                this.referer = mainUrl
                                this.quality = getQualityFromName(quality)
                            }
                        )
                        videolink = true
                    }
                }

            val hlsmatch = """"srcFallback"\s*:\s*"(https?://[^"]+\.m3u8[^"]*)"""".toRegex().find(responsetext)
            if (hlsmatch != null) {
                Log.d(name, "hls: ${hlsmatch.groupValues[1]}")
                callback.invoke(
                    newExtractorLink(
                        name = name,
                        source = "${name}:HLS",
                        url = hlsmatch.groupValues[1],
                        type = ExtractorLinkType.VIDEO
                    ) {
                        this.referer = mainUrl
                        this.quality = Qualities.P1080.value
                    }
                )
                videolink = true
            }

            if (!videolink) {
                Log.d(name, "xhr icerisinde video linki bulunmadı")
            }

        } catch (e: Exception) {
            Log.d(name, "hata: ${e.message}")
        }

        return videolink
    }
    }