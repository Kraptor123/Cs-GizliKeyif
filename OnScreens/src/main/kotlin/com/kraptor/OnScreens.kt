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
        // Sayfa 1 de API'den gelir: imlec (cursor) son videonun created_at degerinden kurulur.
        val cursor = if (page == 1) null else cursorMap[request.name] ?: return newHomePageResponse(request.name, emptyList())
        val tag = request.data.substringAfterLast("/")
        val cursorParam = cursor?.let { "&cursor=" + java.net.URLEncoder.encode(it, "UTF-8") } ?: ""

        val response = app.get(
            "$mainUrl/v1/tag/$tag?limit=24$cursorParam",
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
        response.videos?.lastOrNull()?.let { lastVideo ->
            cursorFor(lastVideo)?.let { cursorMap[request.name] = it }
        }

        val home = response.videos?.mapNotNull { video ->
            val videoId = video.videoId ?: return@mapNotNull null
            val title = video.title ?: return@mapNotNull null
            val slug = video.slug ?: return@mapNotNull null
            video.toSearchResponse(videoId, title, slug)
        } ?: emptyList()

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    // API imleci: base64("<UTC zaman, 6 haneli kesir>Z|<video_id>"). created_at "+01:00" ofsetli gelir, UTC'ye cevrilmeli.
    private fun cursorFor(video: VideoData): String? {
        val videoId = video.videoId ?: return null
        val m = Regex("""^(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d)(?:\.(\d+))?(Z|[+-]\d\d:?\d\d)?$""")
            .find(video.createdAt ?: return null) ?: return null
        val (base, fraction, offset) = m.destructured
        val zone = if (offset.isEmpty() || offset == "Z") "+0000" else offset.replace(":", "")
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US).parse(base + zone) ?: return null
        val utc = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(parsed)
        return base64Encode("$utc.${fraction.take(6).padEnd(6, '0')}Z|$videoId".toByteArray())
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
            "$mainUrl/v1/enhanced-search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=12&offset=$offset&sort=created_at"
        ).parsed<SearchApiResponse>()

        val results = response.videos?.mapNotNull { video ->
            val videoId = video.videoId ?: return@mapNotNull null
            val title = video.title?.replace(Regex("</?b>"), "") ?: return@mapNotNull null
            val slug = video.slug ?: return@mapNotNull null
            video.toSearchResponse(videoId, title, slug)
        } ?: emptyList()

        return newSearchResponseList(results, hasNext = results.size >= 12)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val videoId = url.substringAfter("//").substringAfter("/").substringBefore("/")

        val iframe = document.selectFirst("iframe[src], [data-embed-src]")?.let { it.attr("src").ifEmpty { it.attr("data-embed-src") } } ?: ""

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        if (iframe.isEmpty()) throw ErrorLoadingException("NO VIDEOS FOUND ON THIS PAGE!")
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
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

        val actors          = document.select("span.flex a[href^=/m/]").map { Actor(it.text()) }

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

        // Oynatici artik tikla-oynat: iframe yok, embed adresi button[data-embed-src] icinde
        val iframe = document.selectFirst("iframe[src], [data-embed-src]")?.let { it.attr("src").ifEmpty { it.attr("data-embed-src") } } ?: ""

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