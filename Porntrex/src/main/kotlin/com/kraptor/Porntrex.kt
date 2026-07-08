// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Porntrex : MainAPI() {
    override var mainUrl = "https://www.porntrex.com"
    override var name = "Porntrex"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/categories/4k-porn/" to "4K Porn",
        "${mainUrl}/categories/ai/" to "Ai",
        "${mainUrl}/categories/amateur/" to "Amateur",
        "${mainUrl}/categories/asian/" to "Asian",
        "${mainUrl}/categories/blonde/" to "Blonde",
        "${mainUrl}/categories/blowjob/" to "Blowjob",
        "${mainUrl}/categories/bondage/" to "Bondage",
        "${mainUrl}/categories/brunette/" to "Brunette",
        "${mainUrl}/categories/busty/" to "Busty",
        "${mainUrl}/categories/celebrities/" to "Celebrities",
        "${mainUrl}/categories/college/" to "College",
        "${mainUrl}/categories/cumshots/" to "Cumshots",
        "${mainUrl}/categories/doggystyle/" to "Doggystyle",
        "${mainUrl}/categories/fetish/" to "Fetish",
        "${mainUrl}/categories/fingering/" to "Fingering",
        "${mainUrl}/categories/hairy/" to "Hairy",
        "${mainUrl}/categories/handjob/" to "Handjob",
        "${mainUrl}/categories/hardcore/" to "Hardcore",
        "${mainUrl}/categories/hentai/" to "Hentai",
        "${mainUrl}/categories/homemade/" to "Homemade",
        "${mainUrl}/categories/lesbian/" to "Lesbian",
        "${mainUrl}/categories/masturbation/" to "Masturbation",
        "${mainUrl}/categories/milf/" to "Milf",
        "${mainUrl}/categories/petite/" to "Petite",
        "${mainUrl}/categories/pussy-licking/" to "Pussy licking",
        "${mainUrl}/categories/red-head/" to "Red Head",
        "${mainUrl}/categories/russian/" to "Russian",
        "${mainUrl}/categories/small-tits/" to "Small tits",
        "${mainUrl}/categories/solo/" to "Solo",
        "${mainUrl}/categories/teen/" to "Teen",
        "${mainUrl}/categories/toys/" to "Toys",
        "${mainUrl}/categories/uniform/" to "Uniform",
        "${mainUrl}/categories/webcam/" to "Webcam",
        "${mainUrl}/categories/wife/" to "Wife"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page > 1) {
            "${request.data}?mode=async&function=get_block&block_id=list_videos_common_videos_list_norm&from=$page&_=${System.currentTimeMillis()}"
        } else {
            request.data
        }

        val document = app.get(
            url,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
            referer = request.data
        ).document

        val home = document.select("div.video-preview-screen.video-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            HomePageList(request.name, home, isHorizontalImages = true),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("p.inf a")?.text()?.trim() ?: return null
        val href      = fixUrlNull(this.selectFirst("a.thumb")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.cover")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val cleanQuery = query.trim().replace(" ", "-")
        val url = if (page > 1) {
            "${mainUrl}/search/$cleanQuery/?mode=async&function=get_block&block_id=list_videos_videos&q=$cleanQuery&category_ids=&sort_by=relevance&from=$page&_=${System.currentTimeMillis()}"
        } else {
            "${mainUrl}/search/$cleanQuery/"
        }

        val document = app.get(
            url,
            headers = if (page > 1) mapOf("X-Requested-With" to "XMLHttpRequest") else emptyMap(),
            referer = if (page > 1) "${mainUrl}/search/$cleanQuery/" else mainUrl
        ).document

        val searchAnswer = document.select("div.video-preview-screen.video-item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(searchAnswer, hasNext = searchAnswer.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        Log.d(tag, "Load : $url")
        val document = app.get(url).document

        val title       = document.selectFirst("p.title-video")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(document.selectFirst("#tab_screenshots img.thumb")?.attr("data-src"))
        val description = document.selectFirst("div.videodesc em.des-link")?.text()?.trim()
        val tags        = document.select("div.js-categories a.js-cat").map { it.text() } +
                document.select("div.item:has(span.title-item:contains(Tags)) div.items-holder a").map { it.text() }
        val duration    = document.selectFirst("i.fa-clock-o")?.parent()?.text()?.trim()
            ?.let { Regex("(\\d+)").find(it)?.value }?.toIntOrNull()
        val recommendations = document.select("div.video-list div.video-item").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("img.cover")?.attr("alt")?.ifEmpty { null } ?: return null
        val href      = fixUrlNull(this.selectFirst("a.thumb")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.cover")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(tag, "data = $data")
        val html     = app.get(data).document.html()
        val videoUrl = Regex("video_url:\\s*'([^']+)'").find(html)?.groupValues?.get(1) ?: return false

        callback(
            newExtractorLink(
                source = name,
                name   = name,
                url    = videoUrl,
                type   = ExtractorLinkType.VIDEO
            ) {
                this.referer = mainUrl
            }
        )

        return true
    }
}