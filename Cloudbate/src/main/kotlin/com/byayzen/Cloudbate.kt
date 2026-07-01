// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Cloudbate : MainAPI() {
    override var mainUrl = "https://cloudbate.com"
    override var name = "Cloudbate"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/latest-updates/" to "Latest Updates",
        "${mainUrl}/most-popular/" to "Most Popular",
        "${mainUrl}/categories/girls/" to "Girls",
        "${mainUrl}/categories/couples/" to "Couples",
        "${mainUrl}/models/" to "Models"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val baseUrl   = request.data.trimEnd('/')
        val targetUrl = if (page <= 1) "$baseUrl/" else "$baseUrl/$page/"
        val document  = app.get(targetUrl).document

        val home = if (request.data.contains("/models")) {
            document.select("div#list_models_models_list_items div.item").mapNotNull { it.toModelPageResult() }
        } else {
            document.select("div.video-loop div.item").mapNotNull { it.toMainPageResult() }
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val thumb      = this.selectFirst("a.thumb") ?: return null
        val title      = thumb.attr("title").ifEmpty { return null }
        val href       = fixUrlNull(thumb.attr("href")) ?: return null
        val imgElement = this.selectFirst("img.video-img")
        val posterUrl  = fixUrlNull(imgElement?.attr("data-original")?.ifEmpty { null } ?: imgElement?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    private fun Element.toModelPageResult(): SearchResponse? {
        val thumb      = this.selectFirst("a.thumb") ?: return null
        val title      = this.selectFirst("span.title")?.text()?.trim()?.ifEmpty { null } ?: thumb.attr("title").ifEmpty { return null }
        val href       = fixUrlNull(thumb.attr("href")) ?: return null
        val imgElement = this.selectFirst("img.video-img")
        val posterUrl  = fixUrlNull(imgElement?.attr("data-original")?.ifEmpty { null } ?: imgElement?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document    = app.get("${mainUrl}/search/?q=$query").document
        val aramaCevap  = document.select("li.lists a").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(aramaCevap, hasNext = false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.attr("title").ifEmpty { this.text().trim() }.ifEmpty { return null }
        val href  = fixUrlNull(this.attr("href")) ?: return null

        return newTvSeriesSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=http://cloudbate.com&size=128"
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        Log.d(name, "Load aşaması: $url")
        val document = app.get(url).document

        return if (url.contains("/models/")) {
            val showTitle   = document.selectFirst("a#chat-streamer i")?.text()?.trim() ?: return null
            val allEpisodes = mutableListOf<Episode>()
            var showPoster: String? = null
            var pageDoc = document

            while (true) {
                val items = pageDoc.select("div#list_videos_common_videos_list_items div.item")
                if (items.isEmpty()) break

                items.forEach { item ->
                    val thumb    = item.selectFirst("a.thumb") ?: return@forEach
                    val rawTitle = thumb.attr("title")
                    val epUrl    = fixUrlNull(thumb.attr("href")) ?: return@forEach

                    val imgElement = thumb.selectFirst("img.video-img")
                    val epPoster   = fixUrlNull(imgElement?.attr("data-original")?.ifEmpty { null } ?: imgElement?.attr("src"))

                    if (showPoster == null && epPoster != null) {
                        showPoster = epPoster
                    }

                    val epTitle = rawTitle.replace(showTitle, "", ignoreCase = true).trim()
                    val dateText = item.selectFirst("span.views-number")?.text()?.trim()
                    val dateFormatted = dateText?.let { Regex("(\\d{4}/\\d{2}/\\d{2})").find(it)?.value?.replace("/", "-") }

                    allEpisodes += newEpisode(epUrl) {
                        name = epTitle.ifEmpty { rawTitle }
                        posterUrl = epPoster
                        if (dateFormatted != null) addDate(dateFormatted)
                    }
                }

                val nextHref = pageDoc.selectFirst("a.next.page-link")?.attr("href") ?: break
                pageDoc = app.get(fixUrl(nextHref)).document
            }

            newTvSeriesLoadResponse(showTitle, url, TvType.NSFW, allEpisodes) {
                posterUrl = showPoster
                addActors(listOf(Actor(showTitle)))
            }
        } else {
            val title       = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: return null
            val poster      = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
            val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
            val tags        = document.select("div.item a[href*='/tags/']").map { it.text() }
            val actors      = document.select("div.item a[href*='/models/']").map { Actor(it.text()) }
            val recommendations = document.select("div#list_videos_related_videos_items div.item").mapNotNull { it.toRecommendationResult() }

            newMovieLoadResponse(title, url, TvType.NSFW, url) {
                this.posterUrl       = poster
                this.plot            = description
                this.tags            = tags
                this.recommendations = recommendations
                addActors(actors)
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val thumb      = this.selectFirst("a.thumb") ?: return null
        val title      = thumb.attr("title").ifEmpty { return null }
        val href       = fixUrlNull(thumb.attr("href")) ?: return null
        val imgElement = this.selectFirst("img.video-img")
        val posterUrl  = fixUrlNull(imgElement?.attr("data-original")?.ifEmpty { null } ?: imgElement?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "data = $data")
        val html = app.get(data).text

        val rawVideoUrl = Regex("video_url:\\s*'(https?://[^']+)'").find(html)?.groupValues?.get(1) ?: return false
        val finalVideoUrl = "$rawVideoUrl&rnd=${System.currentTimeMillis()}"

        Log.d("kraptor_$name", "Video linki: $finalVideoUrl")

        val locationResponse = app.get(
            url = finalVideoUrl,
            headers = mapOf("Referer" to data),
            allowRedirects = false
        )

        val redirectUrl = locationResponse.headers["location"] ?: return false
        Log.d("kraptor_$name", "Yönlendirme var: $redirectUrl")

        callback(
            newExtractorLink(
                source = name,
                name = name,
                url = redirectUrl,
                type = ExtractorLinkType.VIDEO
            ) {
                headers = mutableMapOf("Referer" to data)
            }
        )

        return true
    }
}