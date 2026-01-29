// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller

class Pimpbunny : MainAPI() {
    override var mainUrl = "https://pimpbunny.com"
    override var name = "Pimpbunny"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Main Menu",
        "${mainUrl}/categories/4k/" to "4K",
        "${mainUrl}/categories/anal/" to "Anal",
        "${mainUrl}/categories/bbc/" to "BBC",
        "${mainUrl}/categories/bdsm/" to "BDSM",
        "${mainUrl}/categories/big-boobs/" to "Big Boobs",
        "${mainUrl}/categories/bizarre-porn/" to "Bizarre",
        "${mainUrl}/categories/blowjob/" to "Blowjob",
        "${mainUrl}/categories/bunnies/" to "Bunnies",
        "${mainUrl}/categories/deep-throat/" to "Deep Throat",
        "${mainUrl}/categories/double-penetration/" to "Double Penetration",
        "${mainUrl}/categories/exclusive/" to "Exclusive",
        "${mainUrl}/categories/feet/" to "Feet",
        "${mainUrl}/categories/fetish/" to "Fetish",
        "${mainUrl}/categories/gang-bang/" to "Gang Bang",
        "${mainUrl}/categories/lesbian/" to "Lesbian",
        "${mainUrl}/categories/masturbation/" to "Masturbation",
        "${mainUrl}/categories/outdoor/" to "Outdoor",
        "${mainUrl}/categories/pawg/" to "PAWG",
        "${mainUrl}/categories/seduction/" to "Seduction",
        "${mainUrl}/categories/sex/" to "Sex",
        "${mainUrl}/categories/striptease/" to "Striptease",
        "${mainUrl}/categories/threesome/" to "Threesome"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val response = app.get(
            url = request.data,
            interceptor = CloudflareKiller(),
            headers = mapOf("Referer" to "$mainUrl/")
        )
        val document = response.document

        val home = document.select(".ui-card-root__0dWeQJ, div.col").mapNotNull {
            it.toMainPageResult(isSearch = false)
        }.distinctBy { it.url }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(isSearch: Boolean): SearchResponse? {
        val titleElement = this.selectFirst(".ui-card-title__igirYJ")
        val title = titleElement?.text()?.trim() ?: return null

        val anchor = this.selectFirst("a.ui-card-link__KxRw6l")
        val href = fixUrlNull(anchor?.attr("href")) ?: return null

        val img = this.selectFirst("img.ui-card-thumbnail__8dZcLX")
        val posterUrl = fixUrlNull(img?.attr("data-webp") ?: img?.attr("src"))

        return if (isSearch) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".text-truncate, .ui-card-title__igirYJ")?.text()?.trim()
            ?: return null
        val href =
            fixUrlNull(this.selectFirst("a.ui-card-link__KxRw6l")?.attr("href")) ?: return null

        val img = this.selectFirst("img.ui-card-thumbnail__8dZcLX")

        val posterUrl = fixUrlNull(
            img?.attr("data-original")?.ifEmpty { null }
                ?: img?.attr("data-src")?.ifEmpty { null }
                ?: img?.attr("src")?.ifEmpty { null }
        )

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val searchUrl =
            if (page > 1) "${mainUrl}/search/${query}/page/$page/" else "${mainUrl}/search/${query}/"

        val document = app.get(
            url = searchUrl,
            interceptor = CloudflareKiller(),
            headers = mapOf("Referer" to "$mainUrl/")
        ).document

        val results = document.select(".ui-card-root__0dWeQJ, .ui-card-model__HSYU48").mapNotNull {
            it.toSearchResult()
        }.distinctBy { it.url }

        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(
            url,
            headers = mapOf("Referer" to "$mainUrl/")
        )
        val document = response.document

        val title =
            document.selectFirst("h1.ui-heading-h1__0HdXaM, h1.ui-text-root__ZkCuFK")?.text()
                ?.trim()
                ?: document.selectFirst("div.pages-view-video-video-title__9lYVyi")?.text()?.trim()

        if (title == null) return null

        val description =
            document.selectFirst("div.blocks-model-view-creator-description__MQ09nz, .ui-text-muted__v_mC_E")
                ?.text()?.trim()
                ?: document.selectFirst("div.ui-text-md__xx4iLH")?.text()?.trim()

        val tags =
            document.select("ul.includes-list-categories-wrapper__NTP3e_ li a, ul.pages-view-video-tags__EjO14g li a")
                .map {
                    it.text().trim()
                }

        val actors =
            document.select("div.blocks-model-view-title__7xX3ZF h1, ul.pages-view-video-models__OeBRr0 li")
                .map {
                    val name = it.select("div.pages-view-video-model-title__jPOPZM a").text().trim()
                        .ifEmpty { it.text().trim() }
                    val image = fixUrlNull(
                        it.select("img").attr("data-original")
                            .ifEmpty { it.select("img").attr("src") })
                    Actor(name, image)
                }

        val mainPosterElement =
            document.selectFirst("div.blocks-model-view-thumbnail__z5_Ral img, div.pages-view-video-player-wrapper__8D_N_ img")
        val mainPoster = fixUrlNull(
            mainPosterElement?.attr("data-original")?.ifEmpty { null }
                ?: mainPosterElement?.attr("src")?.ifEmpty { null }
                ?: actors.firstOrNull()?.image
        )

        val isModelPage = url.contains("/onlyfans-models/") || url.contains("/categories/")
        val videoCards = document.select(".ui-card-root__0dWeQJ, .ui-card-video__Iv9u1W")

        return if (isModelPage && videoCards.isNotEmpty()) {
            val episodes = videoCards.mapNotNull {
                val epTitle = it.selectFirst(".ui-card-title__igirYJ")?.text()?.trim()
                    ?: return@mapNotNull null
                val epHref = fixUrlNull(it.selectFirst("a.ui-card-link__KxRw6l")?.attr("href"))
                    ?: return@mapNotNull null
                val epImg = it.selectFirst("img")

                newEpisode(epHref) {
                    this.name = epTitle
                    this.posterUrl = fixUrlNull(
                        epImg?.attr("data-original")?.ifEmpty { null }
                            ?: epImg?.attr("src")?.ifEmpty { null }
                    )
                }
            }.distinctBy { it.data }

            newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes) {
                this.posterUrl = mainPoster
                this.plot = description
                this.tags = tags
            }
        } else {
            newMovieLoadResponse(title, url, TvType.NSFW, url) {
                this.posterUrl = mainPoster
                this.plot = description
                this.tags = tags
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(
            url = data,
            headers = mapOf("Referer" to "$mainUrl/")
        )
        val document = response.document

        val downloadLinks =
            document.select("div[data-popover-name='download-actions'] ul li a").mapNotNull {
                val link = it.attr("href")
                val qualityText = it.text().trim()
                if (link.isBlank()) null else Pair(link, qualityText)
            }

        if (downloadLinks.isNotEmpty()) {
            downloadLinks.forEach { (link, qText) ->
                callback(
                    newExtractorLink(
                        source = name,
                        name = "PimpBunny",
                        url = link
                    ) {
                        this.referer = data
                        this.quality = when {
                            qText.contains("1440") || qText.contains("2K") -> Qualities.P2160.value
                            qText.contains("1080") -> Qualities.P1080.value
                            qText.contains("720") -> Qualities.P720.value
                            qText.contains("480") -> Qualities.P480.value
                            qText.contains("360") -> Qualities.P360.value
                            else -> getQualityFromName(link)
                        }
                    }
                )
            }
        } else {
            val html = response.text
            val videoRegex =
                Regex("""(?:video_url|video_alt_url\d*)\s*:\s*['"](?:function/\d+/)?(https?://[^'"]+)""")
            videoRegex.findAll(html).forEach { match ->
                val link = match.groupValues[1]
                if (link.isNotBlank() && !link.contains("_preview.mp4")) {
                    callback(
                        newExtractorLink(
                            source = name,
                            name = "PimpBunny Player",
                            url = link
                        ) {
                            this.referer = data
                            this.quality = getQualityFromName(link)
                        }
                    )
                }
            }
        }
        return true
    }
}