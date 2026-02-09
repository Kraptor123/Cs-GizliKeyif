// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import okhttp3.MultipartBody
import okhttp3.Request
import org.jsoup.nodes.Element

class Javtiful : MainAPI() {
    override var mainUrl = "https://javtiful.com"
    override var name = "Javtiful"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/videos" to "Newest",
        "${mainUrl}/videos?sort=most_viewed" to "Most Viewed",
        "${mainUrl}/videos?sort=top_rated" to "Top Rated",
        "${mainUrl}/videos?sort=top_favorites" to "Top Favorites",
        "${mainUrl}/videos?sort=being_watched" to "Being Watched",
        "${mainUrl}/censored" to "Censored",
        "${mainUrl}/uncensored" to "Uncensored",
        "${mainUrl}/videos/chinese-av" to "Chinese AV",
        "${mainUrl}/videos/affair" to "Affair",
        "${mainUrl}/videos/amateur" to "Amateur",
        "${mainUrl}/videos/bbw" to "BBW",
        "${mainUrl}/videos/beautiful-girl" to "Beautiful Girl",
        "${mainUrl}/videos/big-tits" to "Big Tits",
        "${mainUrl}/videos/cosplay" to "Cosplay",
        "${mainUrl}/videos/drama" to "Drama",
        "${mainUrl}/videos/female-boss" to "Female Boss",
        "${mainUrl}/videos/female-investigator" to "Female Investigator",
        "${mainUrl}/videos/female-student" to "Female Student",
        "${mainUrl}/videos/female-teacher" to "Female Teacher",
        "${mainUrl}/videos/housekeeper" to "Housekeeper",
        "${mainUrl}/videos/hypnosis" to "Hypnosis",
        "${mainUrl}/videos/married-woman" to "Married Woman",
        "${mainUrl}/videos/mature-woman" to "Mature Woman",
        "${mainUrl}/videos/milf" to "Milf",
        "${mainUrl}/videos/nurse" to "Nurse",
        "${mainUrl}/videos/office-lady" to "Office Lady",
        "${mainUrl}/videos/school-girls" to "School Girls",
        "${mainUrl}/videos/sister-in-law" to "Sister-in-law"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}?page=$page"
        val document = app.get(url).document
        val home = document.select("div.col.pb-3").mapNotNull {
            it.mainpageresults()
        }
        return newHomePageResponse(request.name, home, true)
    }

    private fun Element.mainpageresults(): SearchResponse? {
        val link = this.selectFirst("a.video-link") ?: return null
        val title = link.attr("title").trim()
        val img = this.selectFirst("img")
        val poster = (img?.attr("data-src") ?: img?.attr("src"))?.replace("/tmb/", "/tmb1/")

        return newMovieSearchResponse(title, fixUrl(link.attr("href")), TvType.NSFW) {
            this.posterUrl = fixUrlNull(poster)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) "$mainUrl/search/videos?search_query=$query" else "$mainUrl/search/videos?search_query=$query&page=$page"
        val document = app.get(url).document

        val results = document.select("div.col.pb-3").mapNotNull {
            it.searchresults()
        }

        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    private fun Element.searchresults(): SearchResponse? {
        val link = this.selectFirst("a.video-link") ?: return null
        val title = link.attr("title").trim()

        val img = this.selectFirst("img")
        val poster = img?.attr("data-src")?.takeIf { it.isNotEmpty() } ?: img?.attr("src")

        return newMovieSearchResponse(title, fixUrl(link.attr("href")), TvType.NSFW) {
            this.posterUrl = fixUrlNull(poster)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1.video-title")?.text()?.trim() ?: return null

        val playerStyle = document.selectFirst("div.player-wrapper")?.attr("style") ?: ""
        val playerPoster = Regex("url\\(['\"]?([^'\"]+)['\"]?\\)").find(playerStyle)?.groupValues?.get(1)
        val metaPoster = document.selectFirst("meta[property=\"og:image\"]")?.attr("content")
        val videoPoster = document.selectFirst("video")?.attr("poster")
        val poster = (playerPoster ?: metaPoster ?: videoPoster)?.let { fixUrlNull(it) }
        val recommendations = document.select("ul#related-actress-list li, div.card, div.video-item").mapNotNull {
            val link = it.selectFirst("a.video-link, a[href*='/video/']") ?: return@mapNotNull null
            val href = link.attr("href")
            if (!href.contains("/video/") || url.endsWith(href)) return@mapNotNull null
            val img = it.selectFirst("img")
            val rectitle = link.attr("title").ifEmpty { img?.attr("alt") ?: link.text() }.trim()
            val recposter = (img?.attr("data-src")?.takeIf { s -> s.isNotEmpty() } ?: img?.attr("src"))

            newMovieSearchResponse(rectitle, fixUrl(href), TvType.NSFW) {
                this.posterUrl = fixUrlNull(recposter)
            }
        }

        val year = Regex("\\d{4}").find(
            document.select("div[class*='video-details']:contains(Added On)").text()
        )?.value?.toIntOrNull()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = document.selectFirst("meta[property=\"og:description\"]")?.attr("content")?.trim()
            this.year = year
            this.tags = document.select("div[class*='video-details']:contains(Tags) a").map { it.text().trim() }
            addActors(document.select("div[class*='video-details']:contains(Actress) a").map { it.text().trim() })
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mainpageresponse = app.get(data)
        val document = mainpageresponse.document

        val token = document.selectFirst("#token_full")?.attr("data-csrf-token") ?: ""
        val videoid = data.split("/").find { it.toLongOrNull() != null } ?: ""
        val cookiestring = mainpageresponse.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

        val tamheader = mapOf(
            "authority" to "javtiful.com",
            "cookie" to cookiestring,
            "referer" to data,
            "origin" to mainUrl,
            "x-requested-with" to "XMLHttpRequest",
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
        )

        val multipartbody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("video_id", videoid)
            .addFormDataPart("pid_c", "")
            .addFormDataPart("token", token)
            .build()

        val request = Request.Builder()
            .url("$mainUrl/ajax/get_cdn")
            .post(multipartbody)
            .apply { tamheader.forEach { (name, value) -> header(name, value) } }
            .build()

        val responsetext = app.baseClient.newCall(request).execute().body.string()
        val videourl = Regex("""playlists"\s*:\s*"([^"]+)""").find(responsetext)?.groupValues?.get(1)?.replace("\\/", "/")
            ?: return false

        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                videourl
            ) {
                this.quality = Qualities.Unknown.value
                this.referer = "$mainUrl/"
                this.type = if (videourl.contains(".mp4")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                this.headers = tamheader
            }
        )

        return true
    }
}