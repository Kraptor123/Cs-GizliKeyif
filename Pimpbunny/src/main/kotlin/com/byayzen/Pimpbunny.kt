// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.network.CloudflareKiller

class Pimpbunny : MainAPI() {
    override var mainUrl = "https://pimpbunny.com"
    override var name = "Pimpbunny"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val tag             = "gizlikeyif_${name}"
    private val cloudflareKiller = CloudflareKiller()

    override val mainPage = mainPageOf(
        "${mainUrl}/videos" to "Newest Videos",
        "${mainUrl}/" to "Featured Videos",
        "${mainUrl}/onlyfans-creators/" to "Newest Models",
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
            "${request.data.removeSuffix("/")}/$page/"
        }

        val document = app.get(
            url = url,
            interceptor = cloudflareKiller,
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

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(isModel: Boolean = true): SearchResponse? {
        if (this.hasClass("item--adv-thumb") || this.hasClass("js-adv-thumb-item") || this.hasClass("is-adv-randomized") || this.selectFirst("div.qualtiy:contains(AD)") != null) return null
        val anchor = this.selectFirst("a.ui-card-link__KxRw6l, a")
        val rawHref = anchor?.attr("href")?.trim() ?: return null
        if (rawHref.isEmpty()) return null
        if (!rawHref.startsWith("/") && !rawHref.startsWith(mainUrl)) return null

        val href = fixUrlNull(rawHref) ?: return null

        val title = this.selectFirst(".ui-card-title__igirYJ, .text-truncate")?.text()?.trim()
            ?: return null
        val img = this.selectFirst("img.ui-card-thumbnail__8dZcLX, img")
        val posterUrl = fixUrlNull(
            img?.attr("data-original") ?: img?.attr("data-webp") ?: img?.attr("data-src")
            ?: img?.attr("src")
        )

        return if (isModel || href.contains("/onlyfans-creators/")) {
            newTvSeriesSearchResponse(title, href, TvType.NSFW) {
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
        val searchUrl =
            "$mainUrl/search/$query/?mode=async&function=get_block&block_id=list_models_models_list_search_result&from_models=$page&sort_by=title&items_per_page=$itemsPerPage&models_per_page=$itemsPerPage&_=$timestamp"
        val response = app.get(
            url = searchUrl,
            interceptor = cloudflareKiller,
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

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        Log.d(tag, "Load url: $url")
        val document = app.get(
            url,
            interceptor = cloudflareKiller,
            headers = mapOf("Referer" to "$mainUrl/")
        ).document

        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
            ?: document.selectFirst("div.pages-view-video-video-text__wO4wIS, div.pages-view-video-video-title__9lYVyi")?.text()?.trim()
            ?: document.selectFirst("h1:not(:contains(This site is for adults only))")?.text()?.trim()
            ?: return null

        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
            ?: document.selectFirst("div.pages-view-video-description__CuSQws, div.blocks-model-view-creator-description__MQ09nz")?.text()?.trim()

        val mainPoster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
            ?: fixUrlNull(document.selectFirst("div.pages-view-video-player-wrapper__8D_N_ img, div.blocks-model-view-thumbnail__z5_Ral img")?.attr("data-original"))
            ?: fixUrlNull(document.selectFirst("div.pages-view-video-player-wrapper__8D_N_ img, div.blocks-model-view-thumbnail__z5_Ral img")?.attr("src"))

        val duration = document.selectFirst("meta[property=video:duration]")?.attr("content")?.toIntOrNull()
            ?: getDurationFromString(document.selectFirst("div.fp-time-duration, span.pages-view-video-views___bmCJD")?.text())

        val year = document.selectFirst("meta[property=video:release_date]")?.attr("content")?.let {
            Regex("\\d{4}").find(it)?.value?.toIntOrNull()
        } ?: document.selectFirst("div.pages-view-video-video-info__re_sY")?.text()?.let {
            Regex("\\d{4}").find(it)?.value?.toIntOrNull()
        }

        val tags = document.select("meta[property=video:tag]").map { it.attr("content").trim() }
            .ifEmpty {
                document.select("ul.pages-view-video-categories__OWVJKQ li a, ul.includes-list-categories-wrapper__NTP3e_ li a, ul.pages-view-video-tags__EjO14g li a")
                    .map { it.text().trim() }
            }
            .filter { it.isNotEmpty() && !it.equals("Categories", ignoreCase = true) && !it.equals("Tags", ignoreCase = true) }
            .distinct()

        val actors = document.select("ul.pages-view-video-models__OeBRr0 li, div.blocks-model-view-title__7xX3ZF").mapNotNull {
            val name = it.selectFirst("div.pages-view-video-model-title__jPOPZM a, h1, a")?.text()?.trim() ?: return@mapNotNull null
            if (name.equals("Models", ignoreCase = true) || name.isEmpty()) return@mapNotNull null
            val imgElement = it.selectFirst("img")
            val image = fixUrlNull(imgElement?.attr("data-original")?.ifEmpty { null } ?: imgElement?.attr("src"))
            Actor(name, image)
        }.ifEmpty {
            Regex("""video_models:\s*['"]([^'"]+)['"]""").find(document.html())?.groupValues?.get(1)?.split(",")?.mapNotNull {
                val n = it.trim()
                if (n.isNotEmpty()) Actor(n) else null
            } ?: emptyList()
        }.distinct()

        val recommendations = document.select("div#list_videos_similar_videos_items div.ui-card-root__0dWeQJ, div.ui-card-root__0dWeQJ")
            .distinct()
            .mapNotNull { it.toSearchResult() }

        val isSeries = url.contains("/onlyfans-creators/") || url.contains("/categories/")

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            val lastPage = document.select("ul.includes-pagination-list__0cyzaJ li a").mapNotNull {
                it.text().trim().toIntOrNull()
            }.maxOrNull() ?: 1

            for (i in 1..lastPage) {
                val pageUrl = if (i == 1) url else "${url.removeSuffix("/")}/$i/"
                val pageDoc = if (i == 1) document else app.get(
                    pageUrl,
                    interceptor = cloudflareKiller,
                    headers = mapOf("Referer" to "$mainUrl/")
                ).document

                pageDoc.select("#list_videos_model_video_list_items .ui-card-video__Iv9u1W")
                    .forEach { card ->
                        val epHref = fixUrlNull(card.selectFirst("a.ui-card-link__KxRw6l")?.attr("href"))
                        if (epHref != null) {
                            episodes.add(newEpisode(epHref) {
                                this.name = card.selectFirst(".ui-card-title__igirYJ")?.text()?.trim()
                                val epImgElement = card.selectFirst("img")
                                this.posterUrl = if (epImgElement != null) {
                                    fixUrlNull(epImgElement.attr("data-original").ifEmpty { epImgElement.attr("src") })
                                } else null
                            })
                        }
                    }
            }

            newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes.distinctBy { it.data }) {
                this.posterUrl = mainPoster ?: actors.firstOrNull()?.image
                this.plot = description
                this.tags = tags
                this.duration = duration
                this.year = year
                this.recommendations = recommendations
                addActors(actors)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.NSFW, url) {
                this.posterUrl = mainPoster ?: actors.firstOrNull()?.image
                this.plot = description
                this.duration = duration
                this.year = year
                this.tags = tags
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
        Log.d(tag, "loadLinks data = $data")
        val response = app.get(data, interceptor = cloudflareKiller).text
        return KtPlayerExtractor.getLinks(
            sourceName         = name,
            mainUrl            = mainUrl,
            pageUrl            = data,
            pageHtml           = response,
            cookieHeaders      = cloudflareKiller.getCookieHeaders(data).associate { (key, value) -> key to value },
            callback           = callback
        )
    }
}