// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class YouJizz : MainAPI() {
    override var mainUrl              = "https://www.youjizz.com"
    override var name                 = "YouJizz"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/most-popular" to "Most Popular",
        "${mainUrl}/newest-clips" to "Newest",
        "${mainUrl}/top-rated-month" to "Top Rated Month",
        "${mainUrl}/trending" to "Trending",
        "${mainUrl}/random" to "Random"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page.html").document
        val home     = document.select("div.video-thumb").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.video-title a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/$query-$page.html?").document

        val aramaCevap = document.select("div.video-thumb").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        Log.d(name, "Load aşaması: $url")
        val document = app.get(url).document

        val title           = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: return null
        Log.d(name, "title: $title")
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.tag-list li a:not(a[href=/tags])").map { it.text() }
        val scoreText       = document.selectFirst("strong.rating-value")?.text()?.trim()
        val duration        = document.selectFirst("span.video-length")
            ?.text()
            ?.split(":")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.let { parts ->
                val size = parts.size
                val seconds = if (size >= 1) parts[size - 1] else 0
                val minutes = if (size >= 2) parts[size - 2] else 0
                val hours   = if (size >= 3) parts[size - 3] else 0

                hours * 60 + minutes + if (seconds >= 30) 1 else 0
            }
        val recommendations = document.select("div.video-thumb").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from5(scoreText)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }


    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).text

        val video = Regex(pattern = "\"quality\":\"([^\"]*)\",\"filename\":\"((?:[^\")]|\"\")*)\"", options = setOf(RegexOption.IGNORE_CASE))

        video.findAll(document).forEach {
            val quality = it.groupValues[1].toIntOrNull() ?: 0
            val video = "https://" + it.groupValues[2].replace("\\","")

            callback.invoke(newExtractorLink(
                "YouJizz",
                "YouJizz",
                video,
                INFER_TYPE,
                {
                    this.referer = "${mainUrl}/"
                    this.quality = quality
                }
            ))
        }

        return true
    }
}