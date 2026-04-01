// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Erome : MainAPI() {
    override var mainUrl = "https://www.erome.com"
    override var name = "Erome"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/explore" to "Hot",
        "${mainUrl}/explore/new" to "New"
    )

    private val headers: Map<String, String>
        get() = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "tr-TR,tr;q=0.9",
            "Referer" to "$mainUrl/"
        )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            "${request.data}?page=$page"
        }

        val response = app.get(url, headers = headers)
        val elements = response.document.select("div.album")

        val home = elements.mapNotNull { element ->
            val videoCount =
                element.selectFirst("span.album-videos")?.text()?.filter { it.isDigit() }
                    ?.toIntOrNull() ?: 0
            if (videoCount == 0) return@mapNotNull null
            element.toMainPageResult()
        }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val titleElement = selectFirst("a.album-title") ?: return null
        val title = titleElement.text().trim()
        val href = fixUrlNull(titleElement.attr("href")) ?: return null

        val posterUrl = fixUrlNull(
            selectFirst("img.album-thumbnail.active")?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("$mainUrl/search?q=$query&page=$page").document
        val results = document.select("div#albums > div").mapNotNull { element ->
            val rawCount = element.selectFirst("span.album-videos")?.text()?.trim().orEmpty()
            val numeric = rawCount.replace(Regex("[^0-9KMkm]"), "")
            val videoCount = when {
                numeric.isBlank() -> 0
                numeric.contains(Regex("[Kk]")) -> (numeric.replace(Regex("[^0-9]"), "")
                    .toIntOrNull() ?: 0) * 1000

                numeric.contains(Regex("[Mm]")) -> (numeric.replace(Regex("[^0-9]"), "")
                    .toIntOrNull() ?: 0) * 1_000_000

                else -> numeric.toIntOrNull() ?: 0
            }
            if (videoCount == 0) return@mapNotNull null
            element.toMainPageResult()
        }
        val hasNext = document.selectFirst(".pagination a.next") != null || results.isNotEmpty()
        return newSearchResponseList(results, hasNext = hasNext)
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim().orEmpty()
        val poster =
            fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content")).orEmpty()

        val tags = document.select("p.mt-10 a")
            .map { it.text().trim().replace(Regex("^#+\\s*"), "") }
            .toMutableList()
            .apply { if (!contains("+18")) add("+18") }

        val recommendations =
            document.select("div#albums div.album").mapNotNull { it.toRecommendationResult() }
        val actors = document.selectFirst("a#user_name")?.text()?.trim()?.let { listOf(Actor(it)) }
            ?: emptyList()

        val episodes = document.select("div.video video").mapIndexedNotNull { idx, videoTag ->
            val sourceTag = videoTag.selectFirst("source") ?: return@mapIndexedNotNull null
            val src = sourceTag.attr("src").trim()
            if (src.isBlank()) return@mapIndexedNotNull null

            val label = sourceTag.attr("label").orEmpty()
            val name =
                if (label.isNotBlank()) label else inferQualityFromUrl(src) ?: "Bölüm ${idx + 1}"

            newEpisode(src) {
                this.name = name
                this.episode = idx + 1
            }
        }.ifEmpty { listOf(newEpisode("") { episode = 1 }) }

        return if (episodes.size <= 1) {
            newMovieLoadResponse(title, url, TvType.NSFW, episodes.first().data) {
                this.posterUrl = poster
                this.tags = tags
                this.recommendations = recommendations
                addActors(actors)
            }
        } else {
            newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes) {
                this.posterUrl = poster
                this.tags = tags
                this.recommendations = recommendations
                addActors(actors)
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val titleElement = selectFirst("a.album-title") ?: return null
        val title = titleElement.text().trim()
        val href = fixUrlNull(titleElement.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            selectFirst("a.album-link img.active")?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    private fun inferQualityFromUrl(url: String): String? {
        return Regex("_(\\d{3,4}p)\\.(mp4|m3u8)$", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.uppercase()
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val playHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Referer" to mainUrl
        )
        if (data.contains(".mp4") || data.contains(".m3u8")) {
            val type =
                if (data.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            callback(
                newExtractorLink(
                    name,
                    name,
                    data,
                    type = ExtractorLinkType.VIDEO,
                )
                {
                    this.headers = playHeaders
                }
            )
            return true
        }
        val document = app.get(data, headers = headers).document
        val videoContainers = document.select("div.video video")

        if (videoContainers.isEmpty()) return false

        videoContainers.forEach { videoTag ->
            val source = videoTag.selectFirst("source") ?: return@forEach
            val url = source.attr("src").takeIf { it.isNotBlank() } ?: return@forEach

            val quality = source.attr("label").ifEmpty { inferQualityFromUrl(url) ?: "HD" }
            val type =
                if (url.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO

            callback(
                newExtractorLink(name, "$name | $quality", url, type) {
                    this.headers = playHeaders
                }
            )
        }
        return true
    }
}