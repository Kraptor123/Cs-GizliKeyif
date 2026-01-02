package com.byayzen

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

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
        val home =
            app.get(url).document.select("div#vidresults div.mb").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, home, true), true)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return app.get(
            "$mainUrl/search/${
                query.replace(
                    " ",
                    "-"
                )
            }/"
        ).document.select("div#vidresults div.mb").mapNotNull { it.toSearchResult() }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) "$mainUrl/search/${
            query.replace(
                " ",
                "-"
            )
        }/" else "$mainUrl/search/${query.replace(" ", "-")}/$page/"
        val results =
            app.get(url).document.select("div#vidresults div.mb").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(results, true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst("p.mbtit a") ?: return null
        val img = this.selectFirst("div.mbimg img")
        val poster =
            fixUrlNull(img?.attr("data-src")?.takeIf { it.isNotEmpty() && !it.startsWith("data:") }
                ?: img?.attr("src"))
        return newMovieSearchResponse(
            titleElement.text(),
            fixUrl(titleElement.attr("href")),
            TvType.NSFW
        ) { this.posterUrl = poster }
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
        val document = app.get(data, headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/143.0")).document
        var foundAny = false


        document.select("div#downloaddiv a").forEach { element ->
            val link = element.attr("href")

            // Sadece mp4 ve /dload/ içeren linkleri al
            if (link.contains("/dload/") && link.contains(".mp4")) {
                val text = element.text()
                val absoluteUrl = fixUrlNull(link) ?: return@forEach

                val qualityLabel = Regex("""(\d{3,4}p)""").find(text)?.groupValues?.get(1) ?: "Unknown"

                Log.d("EpornerResolver", "Link Bulundu: $qualityLabel -> $absoluteUrl")

                foundAny = true
                callback.invoke(
                    newExtractorLink(
                        name = "$name MP4",
                        source = name,
                        url = absoluteUrl,
                        type = ExtractorLinkType.VIDEO
                    ) {
                        this.referer = data
                        this.quality = getQualityFromName(qualityLabel)
                    }
                )
            }
        }

        if (!foundAny) {
            try {
                val jsonLdScript = document.select("script[type=application/ld+json]").firstOrNull()?.data()
                if (jsonLdScript != null) {
                    val contentUrlMatch = Regex(""""contentUrl"\s*:\s*"([^"]+)"""").find(jsonLdScript)
                    val mainVideoUrl = contentUrlMatch?.groupValues?.get(1)

                    if (mainVideoUrl != null) {
                        Log.d("EpornerResolver", "JSON-LD Linki kullanılıyor: $mainVideoUrl")
                        callback.invoke(
                            newExtractorLink(
                                name = "$name (JSON-LD)",
                                source = name,
                                url = mainVideoUrl,
                                type = ExtractorLinkType.VIDEO
                            ) {
                                this.referer = data
                                this.quality = Qualities.P1080.value
                            }
                        )
                        foundAny = true
                    }
                }
            } catch (e: Exception) {
                Log.e("EpornerResolver", "JSON-LD parse hatası: ${e.message}")
            }
        }

        return foundAny
    }
    }

