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
        val response = app.get(request.data, headers = headers)
        val elements = response.document.select("div#albums > div")

        val home = elements.mapNotNull { element ->
            val videoCount = element.selectFirst("span.album-videos")?.text()?.toIntOrNull() ?: 0
            if (videoCount == 0) return@mapNotNull null
            element.toMainPageResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null

        val title = selectFirst("div.flbaslik")?.text()?.trim()
            ?: selectFirst(".album-title")?.text()?.trim()
            ?: selectFirst("img")?.attr("alt")?.trim()

        if (title.isNullOrEmpty()) return null

        val posterUrl = fixUrlNull(
            selectFirst("img")?.attr("data-src") ?: selectFirst("img")?.attr("src")
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
                numeric.contains(Regex("[Kk]")) -> (numeric.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0) * 1000
                numeric.contains(Regex("[Mm]")) -> (numeric.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0) * 1_000_000
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
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content")).orEmpty()

        val tags = document.select("p.mt-10 a")
            .map { it.text().trim().replace(Regex("^#+\\s*"), "") }
            .toMutableList()
            .apply { if (!contains("+18")) add("+18") }

        val recommendations = document.select("div#albums div.col-lg-2").mapNotNull { it.toRecommendationResult() }
        val actors = document.selectFirst("a#user_name")?.text()?.trim()?.let { listOf(Actor(it)) } ?: emptyList()

        val episodes = document.select("div.video video").mapIndexedNotNull { idx, videoTag ->
            val src = videoTag.selectFirst("> source[src]")?.attr("src")?.trim() ?: return@mapIndexedNotNull null
            val label = videoTag.selectFirst("> source[label]")?.attr("label").orEmpty()
            val name = if (label.isNotBlank()) label else inferQualityFromUrl(src) ?: "Bölüm ${idx + 1}"

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

    private fun inferQualityFromUrl(url: String): String? {
        return Regex("_(\\d{3,4}p)\\.(mp4|m3u8)$", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.uppercase()
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = selectFirst("div.album-infos a.album-title")?.text()?.trim() ?: return null
        val href = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.endsWith(".mp4", true) || data.endsWith(".m3u8", true)) {
            val type = if (data.endsWith(".m3u8", true)) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            val playHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Referer" to data
            )
            callback(
                newExtractorLink(name, "$name | Direct", data, type) {
                    this.headers = playHeaders
                }
            )
            return true
        }

        val document = app.get(data, headers = headers).document
        val videoContainers = document.select("div.video video")

        if (videoContainers.isEmpty()) return false

        val playHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Referer" to data
        )

        videoContainers.forEachIndexed { _, videoTag ->
            val source = videoTag.select("> source[src]").firstOrNull {
                val type = it.attr("type").lowercase()
                val src = it.attr("src").lowercase()
                type.contains("video/mp4") || type.contains("mpegurl") || src.endsWith(".mp4") || src.endsWith(".m3u8")
            } ?: return@forEachIndexed

            val url = source.attr("src")
            val quality = source.attr("label").ifEmpty { inferQualityFromUrl(url) ?: "HD" }
            val type = if (url.endsWith(".m3u8", true)) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO

            callback(
                newExtractorLink(name, "$name | $quality", url, type) {
                    this.referer = data
                    this.headers = playHeaders
                }
            )
        }

        return true
    }
}