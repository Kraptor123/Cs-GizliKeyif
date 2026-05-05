// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.network.WebViewResolver

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
        "${mainUrl}/onlyfans-creators/?models_per_page=30" to "Newest Models",
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

        val title = this.selectFirst(".ui-card-title__igirYJ, .text-truncate")?.text()?.trim()
            ?: return null
        val img = this.selectFirst("img.ui-card-thumbnail__8dZcLX, img")
        val posterUrl = fixUrlNull(
            img?.attr("data-original") ?: img?.attr("data-webp") ?: img?.attr("data-src")
            ?: img?.attr("src")
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
        val searchUrl =
            "$mainUrl/search/$query/?mode=async&function=get_block&block_id=list_models_models_list_search_result&from_models=$page&sort_by=title&items_per_page=$itemsPerPage&models_per_page=$itemsPerPage&_=$timestamp"
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
        val document = app.get(
            url,
            interceptor = CloudflareKiller(),
            headers = mapOf("Referer" to "$mainUrl/")
        ).document

        val title =
            document.selectFirst("h1.ui-heading-h1__0HdXaM, h1.ui-text-root__ZkCuFK, div.pages-view-video-video-title__9lYVyi")
                ?.text()?.trim() ?: return null
        val description =
            document.selectFirst("div.blocks-model-view-creator-description__MQ09nz, .ui-text-muted__v_mC_E, div.ui-text-md__xx4iLH")
                ?.text()?.trim()
        val tags =
            document.select("ul.includes-list-categories-wrapper__NTP3e_ li a, ul.pages-view-video-tags__EjO14g li a")
                .map { it.text().trim() }

        val actors =
            document.select("div.blocks-model-view-title__7xX3ZF h1, ul.pages-view-video-models__OeBRr0 li")
                .map {
                    val name = it.select("div.pages-view-video-model-title__jPOPZM a").text().trim()
                        .ifEmpty { it.text().trim() }
                    val imgelement = it.selectFirst("img")
                    val image = if (imgelement != null) {
                        fixUrlNull(
                            imgelement.attr("data-original").ifEmpty { imgelement.attr("src") })
                    } else null
                    Actor(name, image)
                }

        val mainPosterElement =
            document.selectFirst("div.blocks-model-view-thumbnail__z5_Ral img, div.pages-view-video-player-wrapper__8D_N_ img")
        val mainPoster = if (mainPosterElement != null) {
            fixUrlNull(
                mainPosterElement.attr("data-original").ifEmpty { mainPosterElement.attr("src") })
        } else {
            actors.firstOrNull()?.image
        }

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
                    interceptor = CloudflareKiller(),
                    headers = mapOf("Referer" to "$mainUrl/")
                ).document

                pageDoc.select("#list_videos_model_video_list_items .ui-card-video__Iv9u1W")
                    .forEach { card ->
                        val epHref =
                            fixUrlNull(card.selectFirst("a.ui-card-link__KxRw6l")?.attr("href"))
                        if (epHref != null) {
                            episodes.add(newEpisode(epHref) {
                                this.name =
                                    card.selectFirst(".ui-card-title__igirYJ")?.text()?.trim()
                                val epImgElement = card.selectFirst("img")
                                this.posterUrl = if (epImgElement != null) {
                                    fixUrlNull(
                                        epImgElement.attr("data-original")
                                            .ifEmpty { epImgElement.attr("src") })
                                } else null
                            })
                        }
                    }
            }

            newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes.distinctBy { it.data }) {
                this.posterUrl = mainPoster
                this.plot = description
                this.tags = tags
                addActors(actors)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.NSFW, url) {
                this.posterUrl = mainPoster
                this.plot = description
                this.year = document.selectFirst("div.pages-view-video-video-info__re_sY")?.text()
                    ?.let { Regex("\\d{4}").find(it)?.value?.toIntOrNull() }
                this.tags = tags
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
        val webview = WebViewResolver(
            interceptUrl = Regex(".*pimpbunny\\.com/get_file/.*?\\.mp4.*"),
            additionalUrls = emptyList(),
            userAgent = null,
            useOkhttp = false,
            script = """
                (function() {
                    var attempt = 0;
                    var timer = setInterval(function() {
                        var btn = document.querySelector('.vjs-big-play-button') || document.querySelector('.fp-play');
                        if (btn) {
                            btn.click();
                            clearInterval(timer);
                        }
                        if (window.player_obj && typeof window.player_obj.play === 'function') {
                            window.player_obj.play();
                            clearInterval(timer);
                        }
                        if (attempt++ > 20) clearInterval(timer);
                    }, 500);
                })();
            """.trimIndent()
        )
        Log.d("PimpBunny", data)
        val sonuc = webview.resolveUsingWebView(
            url = data,
            referer = "$mainUrl/"
        )
        val istek = sonuc.first

        if (istek != null) {
            val url = istek.url.toString()
            val headers = istek.headers.toMap()
            Log.d("PimpBunny", url)
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = url,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.referer = data
                    this.headers = headers
                }
            )
            return true
        }

        return false
    }
}