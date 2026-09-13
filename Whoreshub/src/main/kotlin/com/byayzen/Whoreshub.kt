// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Whoreshub : MainAPI() {
    override var mainUrl = "https://www.whoreshub.com"
    override var name = "Whoreshub"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/latest-updates/" to "Latest Updates",
        "${mainUrl}/most-popular/" to "Most Popular",
        "${mainUrl}/top-rated/" to "Top Rated",
        "${mainUrl}/categories/big-tits/" to "Big Tits",
        "${mainUrl}/categories/big-ass/" to "Big Ass",
        "${mainUrl}/categories/blowjob/" to "Blowjob",
        "${mainUrl}/categories/anal/" to "Anal",
        "${mainUrl}/categories/webcam/" to "Webcam",
        "${mainUrl}/categories/babe/" to "Babe",
        "${mainUrl}/categories/amateur/" to "Amateur",
        "${mainUrl}/categories/hardcore/" to "Hardcore",
        "${mainUrl}/categories/milf/" to "Milf",
        "${mainUrl}/categories/teen/" to "Teen",
        "${mainUrl}/categories/asian/" to "Asian",
        "${mainUrl}/categories/blonde/" to "Blonde",
        "${mainUrl}/categories/brunette/" to "Brunette",
        "${mainUrl}/categories/interracial/" to "Interracial",
        "${mainUrl}/categories/cumshot/" to "Cumshot",
        "${mainUrl}/categories/pov/" to "POV",
        "${mainUrl}/categories/shemale/" to "Shemale",
        "${mainUrl}/categories/latina/" to "Latina",
        "${mainUrl}/categories/lesbian/" to "Lesbian",
        "${mainUrl}/categories/threesome/" to "Threesome",
        "${mainUrl}/categories/masturbation/" to "Masturbation",
        "${mainUrl}/categories/solo/" to "Solo",
        "${mainUrl}/categories/creampie/" to "Creampie",
        "${mainUrl}/categories/small-tits/" to "Small Tits",
        "${mainUrl}/categories/behind-the-scene/" to "Behind The Scene (BTS)"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        Log.d(tag, "getMainPage başladı")
        val blockId   = if (request.data.contains("latest-updates")) "list_videos_latest_videos_list" else "list_videos_common_videos_list"
        val sortBy    = when {
            request.data.contains("top-rated")    -> "rating"
            request.data.contains("most-popular") -> "video_viewed"
            else                                  -> "post_date"
        }
        val separator = if (request.data.contains("?")) "&" else "?"
        val url       = "${request.data}${separator}mode=async&function=get_block&block_id=$blockId&sort_by=$sortBy&from=$page"

        val document = app.get(
            url     = url,
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Cookie"           to "confirmed=true; kt_tcookie=1"
            )
        ).document

        val home = document.select("#${blockId}_items div.thumb").ifEmpty {
            document.select("div.block-thumbs div.thumb")
        }.mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkTag   = this.selectFirst("a.item") ?: return null
        val title     = linkTag.attr("title").ifEmpty { linkTag.selectFirst("span.description")?.text()?.trim() } ?: return null
        val href      = fixUrlNull(linkTag.attr("href").ifEmpty { return null }) ?: return null
        val imgTag    = this.selectFirst("img")
        val posterUrl = fixUrlNull(imgTag?.attr("data-src")?.ifEmpty { null } ?: imgTag?.attr("src")?.ifEmpty { null })
        val passUrl   = if (posterUrl != null) "$href kraptor $posterUrl" else href

        return newMovieSearchResponse(title, passUrl, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        Log.d(tag, "search başladı")
        val url = "$mainUrl/search/$query/?mode=async&function=get_block&block_id=list_videos_videos_list_search_result&q=$query&category_ids=&sort_by=&from_videos=$page&from_albums=$page"

        val document = app.get(
            url     = url,
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Cookie"           to "confirmed=true; kt_tcookie=1"
            )
        ).document

        val searchResults = document.select("#list_videos_videos_list_search_result_items div.thumb").ifEmpty {
            document.select("div.block-thumbs div.thumb")
        }.mapNotNull { it.toSearchResult() }

        return newSearchResponseList(searchResults)
    }

    override suspend fun load(url: String): LoadResponse? {
        Log.d(tag, "load başladı")
        val split      = url.split("kraptor")
        val currentUrl = split[0].trim()
        val posterUrl  = split.getOrNull(1)?.trim()?.ifEmpty { null }

        val document = app.get(currentUrl).document

        val title           = document.selectFirst("h1.title")?.text()?.trim() ?: return null
        val description     = document.selectFirst("div.description")?.text()?.trim()
        val duration        = getDurationFromString(document.selectFirst("div.count-item:has(svg.icon-oclock)")?.text()?.trim())
        val tags            = document.select("div.video-taxonomy a").map { it.text().trim() }.distinct()
        val recommendations = document.select("div.thumbs div.thumb").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, currentUrl, TvType.NSFW, currentUrl) {
            this.posterUrl       = posterUrl
            this.plot            = description
            this.duration        = duration
            this.tags            = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(tag, "loadLinks başladı")
        val response = app.get(data).text

        val links = listOfNotNull(
            Regex("""video_alt_url:\s*'(https?:[^']+)'""").find(response)?.groupValues?.get(1)?.let { it to Qualities.P1080.value },
            Regex("""video_url:\s*'(https?:[^']+)'""").find(response)?.groupValues?.get(1)?.let { it to Qualities.P720.value }
        )

        if (links.isEmpty()) return false

        links.forEach { (url, quality) ->
            callback.invoke(
                newExtractorLink(
                    source      = this.name,
                    name        = this.name,
                    url         = url,
                    type        = INFER_TYPE,
                    initializer = {
                        this.quality = quality
                        this.referer = "$mainUrl/"
                    }
                )
            )
        }

        return true
    }
}