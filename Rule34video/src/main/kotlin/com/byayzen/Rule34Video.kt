// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Rule34Video : MainAPI() {
    override var mainUrl              = "https://rule34video.com/"
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
        "$mainUrl/?mode=async&function=get_block&block_id=custom_list_videos_most_recent_videos&tag_ids&sort_by=pseudo_rand" to "Random"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            val formattedpage = page.toString().padStart(2, '0')
            "${request.data}&from=$formattedpage"
        }

        val document = app.get(url).document
        val home = document.select("div.item.thumb").mapNotNull { it.toSearchResponse() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        if (this.selectFirst("header")?.text()?.contains("AD", ignoreCase = true) == true) return null

        val title = this.selectFirst("div.thumb_title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a.th")?.attr("href")) ?: return null

        if (!href.startsWith(mainUrl) && !href.startsWith("/")) return null

        val posterurl = fixUrlNull(this.selectFirst("img")?.attr("data-original") ?: this.selectFirst("img")?.attr("src"))
        val data = "$href|$posterurl"

        return newMovieSearchResponse(title, data, TvType.NSFW) {
            this.posterUrl = posterurl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search/$query/?temp_skip_items=tag:8754"
        } else {
            val formattedpage = page.toString().padStart(2, '0')
            "$mainUrl/search/$query/?temp_skip_items=tag:8754&mode=async&function=get_block&block_id=custom_list_videos_videos_list_search&q=$query&sort_by=&from_videos=$formattedpage&from_albums=$formattedpage"
        }

        val document = app.get(url).document
        val searchresults = document.select("div.item.thumb").mapNotNull { it.toSearchResponse() }

        return newSearchResponseList(searchresults, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val (resurl, poster) = url.split("|").let {
            it[0] to it.getOrNull(1)
        }

        val document = app.get(resurl).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null

        val description = document.selectFirst("div.row em")?.text()?.trim()
            ?: document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        val tags = document.select("a.tag_item").map { it.text() }

        val scoreraw = document.selectFirst("span.voters.count")?.text()?.trim()
        val scorevalue = scoreraw?.split("%")?.firstOrNull()?.toDoubleOrNull()
        val finalscore = scorevalue?.let { Score.from(it / 10.0, 10) }

        val durationtext = document.selectFirst("div.item_info:has(svg.custom-time) span")?.text()
            ?: document.select("div.item_info").lastOrNull()?.selectFirst("span")?.text()
        val duration = durationtext?.split(":")?.firstOrNull()?.toIntOrNull()

        val actorlist = document.select("div.col:has(.label:contains(Artist)) a.item").mapNotNull { el ->
            val name = el.selectFirst(".name")?.text() ?: return@mapNotNull null
            val image = el.selectFirst("img")?.attr("src")

            ActorData(Actor(name, image), roleString = "Artist")
        }

        return newMovieLoadResponse(title, resurl, TvType.NSFW, resurl) {
            this.posterUrl = poster ?: fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
            this.plot = description
            this.tags = tags
            this.score = finalscore
            this.duration = duration
            this.actors = actorlist
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

        Log.d(name, "url = $url")
        val response = app.get(url).text

        val videoregex = Regex("""video_alt_url\d*:\s*'(https?://[^']+)'""")
        val primaryregex = Regex("""video_url:\s*'(https?://[^']+)'""")

        val links = videoregex.findAll(response).map { it.groupValues[1] }.toList() +
                primaryregex.findAll(response).map { it.groupValues[1] }.toList()

        if (links.isEmpty()) {
            Log.d(name, "No links found")
            return false
        }

        links.distinct().forEach { link ->
            val quality = when {
                link.contains("2160p") || link.contains("4k") -> 2160
                link.contains("1080p") -> 1080
                link.contains("720p") -> 720
                link.contains("480p") -> 480
                link.contains("360") -> 360
                else -> Qualities.Unknown.value
            }

            Log.d(name, "link = $link | quality = $quality")

            callback(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = link,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.referer = "$mainUrl/"
                    this.quality = quality
                }
            )
        }

        return true
    }
}