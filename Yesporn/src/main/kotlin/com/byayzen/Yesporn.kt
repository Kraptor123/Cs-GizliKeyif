// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Yesporn : MainAPI() {
    override var mainUrl              = "https://yesporn.vip"
    override var name                 = "Yesporn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest",
        "${mainUrl}/channels/brazzers-hl6mji/" to "Brazzers",
        "${mainUrl}/channels/realitykings-hl6mji/" to "RealityKings",
        "${mainUrl}/channels/bangbros-hl6mji/" to "Bangbros",
        "${mainUrl}/channels/porn-world-hl6mji/" to "Porn World",
        "${mainUrl}/channels/fake-hostel-hl6mji/" to "Fake Hostel",
        "${mainUrl}/channels/public-agent-hl6mji/" to "Public Agent",
        "${mainUrl}/channels/digitalplayground-hl6mji/" to "DigitalPlayground",
        "${mainUrl}/channels/missax-hl6mji/" to "MissaX",
        "${mainUrl}/channels/bang-hl6mji/" to "BANG",
        "${mainUrl}/channels/mofos-hl6mji/" to "Mofos",
        "${mainUrl}/channels/bellesa-films-hl6mji/" to "Bellesa Films",
        "${mainUrl}/channels/elegant-angel-hl6mji/" to "Elegant Angel",
        "${mainUrl}/channels/jacquieetmicheltv-hl6mji/" to "JacquieEtMichelTV",
        "${mainUrl}/channels/roccosiffredifilms-hl6mji/" to "Roccosiffredifilms",
        "${mainUrl}/channels/sexart-hl6mji/" to "SEXART",
        "${mainUrl}/channels/hardwerk-hl6mji/" to "Hardwerk"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            val cleanData  = request.data.removeSuffix("/")
            val isMainPage = cleanData == mainUrl || cleanData.isEmpty()
            val blockId    = if (isMainPage) "list_videos_most_recent_videos" else "list_videos_common_videos_list"
            "$cleanData/?mode=async&function=get_block&block_id=$blockId&sort_by=post_date&from=$page"
        }

        val document = app.get(
            url     = url,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        ).document

        val home = document.select("div.thumbs div.thumb.item, div.thumb.item, div.thumb").distinct().mapNotNull { it.toSearchResult() }

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
        if (this.hasClass("item--adv-thumb") || this.hasClass("js-adv-thumb-item") || this.hasClass("is-adv-randomized") || this.selectFirst("div.qualtiy:contains(AD)") != null) return null
        val linkTag   = this.selectFirst("a") ?: return null
        val rawHref   = linkTag.attr("href").trim()
        if (rawHref.isEmpty()) return null
        if (!rawHref.startsWith("/") && !rawHref.startsWith(mainUrl)) return null

        val href      = fixUrlNull(rawHref) ?: return null
        val title     = this.selectFirst("div.title")?.text()?.trim() ?: return null
        val imgTag    = this.selectFirst("img")
        val posterUrl = fixUrlNull(
            imgTag?.attr("data-original")?.ifEmpty { null }
                ?: imgTag?.attr("data-src")?.ifEmpty { null }
                ?: imgTag?.attr("src")?.ifEmpty { null }
        )

        return newMovieSearchResponse(title, "${href}kraptor$posterUrl", TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search/$query/"
        } else {
            "$mainUrl/search/$query/?mode=async&function=get_block&block_id=list_videos_videos_list_search_result&q=$query&category_ids=&sort_by=&from_videos=$page&from_albums=$page"
        }

        val document = app.get(
            url     = url,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        ).document

        val searchAnswer = document.select("div.thumbs div.thumb.item, div.thumb.item, div.thumb").distinct().mapNotNull { it.toSearchResult() }

        return newSearchResponseList(searchAnswer)
    }

    override suspend fun load(url: String): LoadResponse? {
        val split      = url.split("kraptor")
        val currentUrl = split[0].trim()
        val posterUrl  = split.getOrNull(1)?.trim()?.ifEmpty { null }

        Log.d(tag, "Load aşaması: $currentUrl")
        val document = app.get(currentUrl).document

        val title           = document.selectFirst("h1.title")?.text()?.trim() ?: return null
        val description     = document.selectFirst("div.description")?.text()?.trim()
        val duration        = getDurationFromString(document.selectFirst("div.count-item:has(svg.icon-oclock)")?.text()?.trim())
        val tags            = document.select("div.tags-row a, div.video-taxonomy a").map { it.text().trim() }.distinct()
        val actors          = document.select("a.btn.gold[href*=/models/], a[href*=/models/]:not(.nav-link)").mapNotNull {
            val actorName = it.text().trim()
            val href = it.attr("href")
            if (actorName.equals("Models", ignoreCase = true) || actorName.isEmpty() || href.endsWith("/models/") || href.endsWith("/models")) null
            else Actor(actorName)
        }.distinct()
        val recommendations = document.select("div.thumbs div.thumb.item, div.thumb.item, div.thumb").distinct().mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, currentUrl, TvType.NSFW, currentUrl) {
            this.posterUrl       = posterUrl
            this.plot            = description
            this.duration        = duration
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(tag, "loadLinks data = $data")
        val pageHtml = app.get(data).text
        return KtPlayerExtractor.getLinks(name, mainUrl, data, pageHtml, callback = callback)
    }
}