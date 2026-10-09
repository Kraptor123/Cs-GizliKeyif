// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Rule34Video : MainAPI() {
    override var mainUrl              = "https://rule34video.com"
    override var name                 = "Rule34Video"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "$mainUrl/?mode=async&function=get_block&block_id=custom_list_videos_most_recent_videos&tag_ids&sort_by=post_date" to "Newest",
        "$mainUrl/?mode=async&function=get_block&block_id=custom_list_videos_most_recent_videos&tag_ids&sort_by=video_viewed" to "Most Viewed",
        "$mainUrl/?mode=async&function=get_block&block_id=custom_list_videos_most_recent_videos&tag_ids&sort_by=rating" to "Top Rated",
        "$mainUrl/?mode=async&function=get_block&block_id=custom_list_videos_most_recent_videos&tag_ids&sort_by=duration" to "Longest",
        "$mainUrl/?mode=async&function=get_block&block_id=custom_list_videos_most_recent_videos&tag_ids&sort_by=pseudo_rand" to "Random",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            val formattedPage = page.toString().padStart(2, '0')
            "${request.data}&from=$formattedPage"
        }

        val document = app.get(url).document
        val home = document.select("div.item.thumb").mapNotNull { it.toSearchResponse() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        if (this.selectFirst("header")?.text()?.contains("AD", ignoreCase = true) == true) return null

        val title = this.selectFirst("div.thumb_title")?.text()?.trim()?.ifEmpty { null } ?: return null
        val href = fixUrlNull(this.selectFirst("a.th")?.attr("href")?.ifEmpty { null }) ?: return null

        if (!href.startsWith(mainUrl) && !href.startsWith("/")) return null

        val img = this.selectFirst("img")
        val posterUrl = fixUrlNull(
            img?.attr("data-rd-jpg")?.ifEmpty { null }
                ?: img?.attr("data-original")?.ifEmpty { null }
                ?: img?.attr("data-src")?.ifEmpty { null }
                ?: img?.attr("src")?.ifEmpty { null }
        )
        val data = "$href|$posterUrl"

        return newMovieSearchResponse(title, data, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Referer" to "$mainUrl/", "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search/$query/?temp_skip_items=tag:8754"
        } else {
            val formattedPage = page.toString().padStart(2, '0')
            "$mainUrl/search/$query/?temp_skip_items=tag:8754&mode=async&function=get_block&block_id=custom_list_videos_videos_list_search&q=$query&sort_by=&from_videos=$formattedPage&from_albums=$formattedPage"
        }

        val document = app.get(url).document
        val searchResults = document.select("div.item.thumb").mapNotNull { it.toSearchResponse() }

        return newSearchResponseList(searchResults, hasNext = searchResults.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val (resUrl, poster) = url.split("|").let {
            it[0] to it.getOrNull(1)
        }

        val document = app.get(resUrl).document
        val title = document.selectFirst("h1")?.text()?.trim()?.ifEmpty { null } ?: return null

        val description = document.selectFirst("div.row em")?.text()?.trim()?.ifEmpty { null }
            ?: document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()?.ifEmpty { null }

        val tags = document.select("a.tag_item").map { it.text().trim() }

        val scoreRaw = document.selectFirst("span.voters.count")?.text()?.trim()
        val scoreValue = scoreRaw?.split("%")?.firstOrNull()?.toDoubleOrNull()
        val finalScore = scoreValue?.let { Score.from(it / 10.0, 10) }

        val durationText = document.selectFirst("div.item_info:has(svg.custom-time) span")?.text()
            ?: document.select("div.item_info").lastOrNull()?.selectFirst("span")?.text()
        val duration = durationText?.split(":")?.firstOrNull()?.toIntOrNull()

        val actorList = document.select("div.col:has(.label:contains(Artist)) a.item").mapNotNull { el ->
            val name = el.selectFirst(".name")?.text()?.trim() ?: return@mapNotNull null
            val image = fixUrlNull(el.selectFirst("img")?.attr("src")?.ifEmpty { null })

            ActorData(Actor(name, image), roleString = "Artist")
        }

        val loadPoster = poster ?: fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content")?.ifEmpty { null })

        return newMovieLoadResponse(title, resUrl, TvType.NSFW, resUrl) {
            this.posterUrl = loadPoster
            this.posterHeaders = mapOf("Referer" to "$mainUrl/", "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            this.plot = description
            this.tags = tags
            this.score = finalScore
            this.duration = duration
            this.actors = actorList
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val (url, _) = data.split("|").let {
            it[0] to it.getOrNull(1)
        }

        com.lagradost.api.Log.d(name, "url = $url")
        val response = app.get(url).text

        return KtPlayerExtractor.getLinks(name, mainUrl, url, response, callback = callback)
    }
}
