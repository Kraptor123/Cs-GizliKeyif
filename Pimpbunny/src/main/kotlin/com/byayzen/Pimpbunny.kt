// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

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
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Featured Videos",
        "${mainUrl}/onlyfans-models/?models_per_page=30&sort_by=added_date" to "Newest Models",
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
        val url = if (page <= 1) {
            request.data
        } else {
            if (request.data.contains("onlyfans-models")) {
                val base = request.data.substringBefore("?")
                val query = request.data.substringAfter("?")
                "${base}${page}/?${query}"
            } else {
                "${request.data.removeSuffix("/")}/$page/"
            }
        }

        val document = app.get(
            url = url,
            interceptor = CloudflareKiller(),
            headers = mapOf("Referer" to "$mainUrl/")
        ).document

        val selector = when (request.name) {
            "Newest Models" -> "#vt_list_models_with_advertising_custom_models_list_items"
            "Featured Videos" -> "#pb_index_featured_videos_list_featured_videos_items"
            else -> ""
        }.let { if (it.isNotEmpty()) "$it .col, $it .ui-card-root__0dWeQJ" else ".col, .ui-card-root__0dWeQJ" }

        val isModel = request.name == "Newest Models"
        val home = document.select(selector).mapNotNull {
            it.toSearchResult(isModel)
        }.distinctBy { it.url }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.toSearchResult(isModel: Boolean = true): SearchResponse? {
        val anchor = this.selectFirst("a.ui-card-link__KxRw6l, a")
        val href = fixUrlNull(anchor?.attr("href")) ?: return null

        if (!href.contains("pimpbunny.com")) return null

        val title = this.selectFirst(".ui-card-title__igirYJ, .text-truncate")?.text()?.trim() ?: return null
        val img = this.selectFirst("img.ui-card-thumbnail__8dZcLX, img")
        val posterUrl = fixUrlNull(
            img?.attr("data-original") ?: img?.attr("data-webp") ?: img?.attr("data-src") ?: img?.attr("src")
        )

        return if (isModel || href.contains("/onlyfans-models/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val itemsPerPage = 30
        val timestamp = System.currentTimeMillis()
        val searchUrl = "$mainUrl/search/$query/?mode=async&function=get_block&block_id=list_models_models_list_search_result&from_models=$page&sort_by=title&items_per_page=$itemsPerPage&models_per_page=$itemsPerPage&_=$timestamp"
        val response = app.get(
            url = searchUrl,
            interceptor = CloudflareKiller(),
            headers = mapOf(
                "Referer" to "$mainUrl/search/$query/",
                "X-Requested-With" to "XMLHttpRequest"
            )
        )
        val document = response.document
        val results = document.select(".ui-card-root__0dWeQJ, .col").mapNotNull {
            it.toSearchResult()
        }.distinctBy { it.url }

        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, interceptor = CloudflareKiller(), headers = mapOf("Referer" to "$mainUrl/")).document

        val title = document.selectFirst("h1.ui-heading-h1__0HdXaM, h1.ui-text-root__ZkCuFK, div.pages-view-video-video-title__9lYVyi")?.text()?.trim() ?: return null
        val description = document.selectFirst("div.blocks-model-view-creator-description__MQ09nz, .ui-text-muted__v_mC_E, div.ui-text-md__xx4iLH")?.text()?.trim()
        val tags = document.select("ul.includes-list-categories-wrapper__NTP3e_ li a, ul.pages-view-video-tags__EjO14g li a").map { it.text().trim() }

        val actors = document.select("div.blocks-model-view-title__7xX3ZF h1, ul.pages-view-video-models__OeBRr0 li").map {
            val name = it.select("div.pages-view-video-model-title__jPOPZM a").text().trim().ifEmpty { it.text().trim() }
            val image = fixUrlNull(it.select("img").let { img -> img.attr("data-original").ifEmpty { img.attr("src") } })
            Actor(name, image)
        }

        val mainPoster = document.selectFirst("div.blocks-model-view-thumbnail__z5_Ral img, div.pages-view-video-player-wrapper__8D_N_ img")?.let {
            fixUrlNull(it.attr("data-original").ifEmpty { it.attr("src") })
        } ?: actors.firstOrNull()?.image

        val videoCards = document.select(".ui-card-root__0dWeQJ, .ui-card-video__Iv9u1W")
        return if ((url.contains("/onlyfans-models/") || url.contains("/categories/")) && videoCards.isNotEmpty()) {
            val episodes = videoCards.mapNotNull {
                val epHref = fixUrlNull(it.selectFirst("a.ui-card-link__KxRw6l, a")?.attr("href")) ?: return@mapNotNull null
                if (!epHref.contains("pimpbunny.com")) return@mapNotNull null

                newEpisode(epHref) {
                    name = it.selectFirst(".ui-card-title__igirYJ, .text-truncate")?.text()?.trim()
                    posterUrl = fixUrlNull(it.selectFirst("img")?.let { img -> img.attr("data-original").ifEmpty { img.attr("src") } })
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