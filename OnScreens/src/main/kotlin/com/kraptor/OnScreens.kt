// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.collections.mapOf

class OnScreens : MainAPI() {
    override var mainUrl              = "https://www.onscreens.me"
    override var name                 = "OnScreens"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    // Cursor'ları saklamak için map
    private val cursorMap = mutableMapOf<String, String>()

    override val mainPage = mainPageOf(
        "/t/cam4"       to "Cam4",
        "/t/chaturbate" to "Chaturbate",
        "/t/stripchat"  to "StripChat",
        "/t/camsoda"    to "CamSoda",
        "/t/bongacams"  to "BongaCams",
        "/t/latin"      to "Latin",
        "/t/fingering"  to "Fingering",
        "/t/anal"       to "Anal",
        "/t/lesbian"    to "Lesbian",
        "/t/ass"        to "Ass",
        "/t/feet"       to "Feet",
        "/t/teens"      to "Teen (18+)",
        "/t/pussy"      to "Pussy",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val home = if (page == 1) {
            // İlk sayfa - HTML parse
            val document = app.get("$mainUrl${request.data}", headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language" to "en-US,en;q=0.5",
                "Referer" to "${mainUrl}/",
                "Sec-GPC" to "1",
                "Connection" to "keep-alive",
                "Upgrade-Insecure-Requests" to "1",
                "Sec-Fetch-Dest" to "document",
                "Sec-Fetch-Mode" to "navigate",
                "Sec-Fetch-Site" to "cross-site",
                "Sec-Fetch-User" to "?1",
                "Priority" to "u=0, i",
                "Pragma" to "no-cache",
                "Cache-Control" to "no-cache",
                "TE" to "trailers"
            )).document

            val articles = document.select("article.flex")

            // Son article'dan cursor bilgisini al ve kaydet
            articles.lastOrNull()?.let { lastArticle ->
                val href = lastArticle.selectFirst("a")?.attr("href") ?: ""
                val videoId = href.removePrefix("/").substringBefore("/")

                // Cursor oluştur (timestamp + video_id formatında)
                val timestamp = System.currentTimeMillis()
                val adjustedTimestamp = timestamp - (6 * 3600 * 1000)
                val formattedTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date(adjustedTimestamp))

                val cursorData = "$formattedTime|$videoId"
                val encodedCursor = base64Encode(cursorData.toByteArray())
                cursorMap[request.name] = encodedCursor
            }

            articles.mapNotNull { it.toMainPageResult() }
        } else {
            // İkinci ve sonraki sayfalar - JSON API
            val cursor = cursorMap[request.name] ?: return newHomePageResponse(request.name, emptyList())
            val tag = request.data.substringAfterLast("/")

            val response = app.get(
                "$mainUrl/v1/tag/$tag?limit=24&cursor=$cursor",
                headers = mapOf(
                    "User-Agent" to "OnScreens-Fetcher",
                    "Accept" to "application/json",
                    "Accept-Language" to "en-US,en;q=0.5",
                    "Referer" to "${mainUrl}/",
                    "Sec-GPC" to "1",
                    "Connection" to "keep-alive",
                    "Sec-Fetch-Dest" to "empty",
                    "Sec-Fetch-Mode" to "cors",
                    "Sec-Fetch-Site" to "same-origin",
                    "Priority" to "u=4",
                    "Pragma" to "no-cache",
                    "Cache-Control" to "no-cache",
                    "TE" to "trailers"
                )
            ).parsed<ApiResponse>()

            // Yeni cursor'u güncelle (son video'dan)
            response.videos?.lastOrNull()?.let { lastVideo ->
                val videoId = lastVideo.videoId ?: return@let
                val createdAt = lastVideo.createdAt ?: return@let

                // created_at formatını "2026-01-04T11:46:36.831746Z" formatına çevir
                val timestamp = if (createdAt.contains("+")) {
                    createdAt.substringBefore("+") + "Z"
                } else if (createdAt.lastIndexOf("-") > 10) {
                    createdAt.substring(0, createdAt.lastIndexOf("-")) + "Z"
                } else if (!createdAt.endsWith("Z")) {
                    createdAt + "Z"
                } else {
                    createdAt
                }

                val cursorData = "$timestamp|$videoId"
                val encodedCursor = base64Encode(cursorData.toByteArray())
                cursorMap[request.name] = encodedCursor
            }

            response.videos?.mapNotNull { video ->
                val videoId = video.videoId ?: return@mapNotNull null
                val title = video.title ?: return@mapNotNull null
                val slug = video.slug ?: return@mapNotNull null
                video.toSearchResponse(videoId, title, slug)
            } ?: emptyList()
        }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    private fun VideoData.toSearchResponse(videoId: String, title: String, slug: String): SearchResponse {
        val href = "$mainUrl/$videoId/$slug"
        val posterUrl = imagePoster?.linkMedium ?: imagePoster?.linkDirect ?: imagePoster?.linkThumb

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun search(query: String, page: Int): SearchResponseList {
        val offset = (page - 1) * 12
        val response = app.get(
            "$mainUrl/v1/enhanced-search?q=$query&limit=12&offset=$offset&sort=created_at"
        ).parsed<SearchApiResponse>()

        val results = response.videos?.mapNotNull { video ->
            val videoId = video.videoId ?: return@mapNotNull null
            val title = video.title?.replace(Regex("</?b>"), "") ?: return@mapNotNull null // HTML tag'leri temizle
            val slug = video.slug ?: return@mapNotNull null
            video.toSearchResponse(videoId, title, slug)
        } ?: emptyList()

        return newSearchResponseList(results, hasNext = results.size >= 12)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val videoId = url.substringAfter("//").substringAfter("/").substringBefore("/")

        val iframe = document.selectFirst("iframe")?.attr("src") ?: ""

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = if (iframe.isEmpty()){
            "No Videos Found On This Page"
        } else {
            document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        }
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.max-w-md.sm\\:max-w-6xl a").map { it.text() }
        val score          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration = document.selectFirst("span.flex:contains(:)")?.text()?.split(":")?.let { parts ->
            val saat = parts.getOrNull(0)?.trimStart('0')?.toIntOrNull() ?: 0
            val dakika = parts.getOrNull(1)?.trimStart('0')?.toIntOrNull() ?: 0
            saat * 60 + dakika
        }

        val recommendAl     = app.get("${mainUrl}/v1/s/$videoId", headers = mapOf(
            "User-Agent" to "OnScreens-Fetcher",
            "Accept" to "application/json",
            "Accept-Language" to "en-US,en;q=0.5",
            "Referer" to "${mainUrl}/",
            "Sec-GPC" to "1",
            "Connection" to "keep-alive",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "same-origin",
            "Priority" to "u=4",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache",
            "TE" to "trailers"
        )
        ).parsed<ApiResponse>()

        val recommendations = recommendAl.videos?.mapNotNull { video ->
            val videoId = video.videoId ?: return@mapNotNull null
            val title = video.title ?: return@mapNotNull null
            val slug = video.slug ?: return@mapNotNull null
            video.toSearchResponse(videoId, title, slug)
        } ?: emptyList()

        val actors          = document.select("span.valor a").map { Actor(it.text()) }

        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(score)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val iframe = document.selectFirst("iframe")?.attr("src") ?: ""

         loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiResponse(
    @JsonProperty("number_videos") val numberVideos: Int? = null,
    @JsonProperty("success") val success: Boolean? = null,
    @JsonProperty("tag") val tag: String? = null,
    @JsonProperty("videos") val videos: List<VideoData>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchApiResponse(
    @JsonProperty("number_videos") val numberVideos: Int? = null,
    @JsonProperty("success") val success: Boolean? = null,
    @JsonProperty("query") val query: String? = null,
    @JsonProperty("search_type") val searchType: String? = null,
    @JsonProperty("videos") val videos: List<VideoData>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VideoData(
    @JsonProperty("video_id") val videoId: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("slug") val slug: String? = null,
    @JsonProperty("created_at") val createdAt: String? = null,
    @JsonProperty("image_poster") val imagePoster: ImagePoster? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImagePoster(
    @JsonProperty("link_thumb") val linkThumb: String? = null,
    @JsonProperty("link_direct") val linkDirect: String? = null,
    @JsonProperty("link_medium") val linkMedium: String? = null
)