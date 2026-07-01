// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.jsoup.nodes.Document
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId

class Hanime : MainAPI() {
    override var mainUrl              = "https://hanime.tv"
    override var name                 = "HanimeTV"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    companion object {
        private var AramaOnbellegi: List<HvsVideo>? = null
        private var cacheSuresi: Long = 0L
        private const val CACHE_TTL = 3_600_000L
        private const val FAHApi = "https://cached.freeanimehentai.net"
        private const val SEARCH_API = "$FAHApi/api/v10/search_hvs"
    }

    override val mainPage = mainPageOf(
        "3d" to "3D",
        "ahegao" to "Ahegao",
        "anal" to "Anal",
        "bdsm" to "BDSM",
        "big%20boobs" to "Big Boobs",
        "blow%20job" to "Blow Job",
        "bondage" to "Bondage",
        "boob%20job" to "Boob Job",
        "censored" to "Censored",
        "comedy" to "Comedy",
        "cosplay" to "Cosplay",
        "creampie" to "Creampie",
        "dark%20skin" to "Dark Skin",
        "facial" to "Facial",
        "fantasy" to "Fantasy",
        "filmed" to "Filmed",
        "foot%20job" to "Foot Job",
        "futanari" to "Futanari",
        "gangbang" to "Gangbang",
        "glasses" to "Glasses",
        "hand%20job" to "Hand Job",
        "harem" to "Harem",
        "hd" to "HD",
        "horror" to "Horror",
        "incest" to "Incest",
        "inflation" to "Inflation",
        "lactation" to "Lactation",
        "maid" to "Maid",
        "masturbation" to "Masturbation",
        "milf" to "Milf",
        "mind%20break" to "Mind Break",
        "mind%20control" to "Mind Control",
        "monster" to "Monster",
        "nekomimi" to "Nekomimi",
        "ntr" to "NTR",
        "nurse" to "Nurse",
        "orgy" to "Orgy",
        "plot" to "Plot",
        "pov" to "POV",
        "pregnant" to "Pregnant",
        "public%20sex" to "Public Sex",
        "rimjob" to "Rimjob",
        "school%20girl" to "School Girl",
        "softcore" to "Softcore",
        "swimsuit" to "Swimsuit",
        "teacher" to "Teacher",
        "tentacle" to "Tentacle",
        "threesome" to "Threesome",
        "toys" to "Toys",
        "tsundere" to "Tsundere",
        "ugly%20bastard" to "Ugly Bastard",
        "uncensored" to "Uncensored",
        "vanilla" to "Vanilla",
        "virgin" to "Virgin",
        "watersports" to "Watersports",
        "x-ray" to "X-Ray",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url      = "$mainUrl/browse/tags/${request.data}?page=$page"
        val document = app.get(url).document

        val home = document.select("div.grid.grid-cols-2 a[href^=/videos/hentai/]").mapNotNull {
            val href   = it.attr("href")
            val name   = it.selectFirst("h3")?.text() ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("abs:src") ?: return@mapNotNull null
            if (name.isBlank() || href.isBlank()) return@mapNotNull null

            newAnimeSearchResponse(
                name = name,
                url  = href,
                type = TvType.NSFW
            ) {
                this.posterUrl     = poster
                this.posterHeaders = mapOf("Referer" to "$mainUrl/")
            }
        }

        Log.d("HanimeTV", "getMainPage tag=${request.data} page=$page items=${home.size}")

        return newHomePageResponse(
            listOf(
                HomePageList(
                    name               = request.name,
                    list               = home,
                    isHorizontalImages = true
                )
            ),
            hasNext = home.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val videos = AramaveriCek()
        if (videos.isEmpty()) return newSearchResponseList(emptyList(), hasNext = false)

        val q = query.lowercase().trim()
        if (q.isBlank()) return newSearchResponseList(emptyList(), hasNext = false)

        val filtered = videos.filter { v ->
            v.name.contains(q, ignoreCase = true) ||
                    v.searchTitles.contains(q, ignoreCase = true) ||
                    v.tags.any { it.contains(q, ignoreCase = true) } ||
                    v.brand.contains(q, ignoreCase = true)
        }

        val pageSize = 24
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, filtered.size)
        val pageItems = if (startIndex < filtered.size) filtered.subList(startIndex, endIndex) else emptyList()

        Log.d("HanimeTV", "search q=$q page=$page total=${filtered.size} showing=${pageItems.size}")

        val results = pageItems.mapNotNull { v ->
            if (v.slug.isBlank()) return@mapNotNull null
            Log.d("HanimeTV", "poster ${v.name}: ${v.posterUrl}")
            newMovieSearchResponse(
                name = v.name,
                url = "/videos/hentai/${v.slug}",
                type = TvType.NSFW
            ) {
                this.posterUrl = v.posterUrl
                this.posterHeaders = mapOf("Referer" to "$mainUrl/")
            }
        }

        return newSearchResponseList(results, hasNext = endIndex < filtered.size)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    private suspend fun AramaveriCek(): List<HvsVideo> {
        val simdi = System.currentTimeMillis()
        AramaOnbellegi?.takeIf { simdi - cacheSuresi < CACHE_TTL }?.let {
            Log.d("HanimeTV", "Önbellekten ${it.size} video getirildi.")
            return it
        }
        return runCatching {
            val response = app.get(SEARCH_API, headers = mapOf("Origin" to mainUrl)).text
            Log.d("HanimeTV", "HAM JSON (ilk 1000 karakter): ${response.take(1000)}")
            parseJson<List<HvsVideo>>(response).also {
                Log.d("HanimeTV", "API'den ${it.size} video çekildi.")
                it.firstOrNull()?.let { first ->
                    Log.d("HanimeTV", "İlk öğe: name=${first.name}, posterUrl=${first.posterUrl}, coverUrl=${first.coverUrl}")
                }
                AramaOnbellegi = it
                cacheSuresi = simdi
            }
        }.getOrNull() ?: AramaOnbellegi ?: emptyList()
    }



    private fun HeaderAl(): Map<String, String> {
        val time = (System.currentTimeMillis() / 1000).toString()
        val signature = SignatureCek(time)
        return mapOf(
            "Origin" to mainUrl,
            "Referer" to "$mainUrl/",
            "X-Signature-Version" to "web2",
            "X-Time" to time,
            "X-Signature" to signature,
            "Content-Type" to "application/json"
        )
    }

    override suspend fun load(url: String): LoadResponse? {
        val slug = url.substringAfterLast("/")
        val apiPath = "/api/v8/video?id=$slug&"

        val apiResponse = app.get(
            "$FAHApi$apiPath",
            headers = HeaderAl()
        ).parsed<VideoApiResponse>()

        val video = apiResponse.hanimeVideo
        val yil = video.releasedAtUnix?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("UTC")).year }
        val durationMin = video.durationInMs?.div(60000)?.toInt()
        val score = video.rating?.let { Score.from10(it) }

        Log.d("HanimeTV", "load slug=$slug title=${video.name} year=$yil duration=$durationMin")

        return newMovieLoadResponse(video.name, url, TvType.NSFW, video.id.toString()) {
            this.posterUrl = video.posterUrl
            this.posterHeaders = mapOf("Referer" to "$mainUrl/")
            this.plot = video.description
            this.year = yil
            this.tags = video.hanimeTags?.map { it.text } ?: emptyList()
            this.duration = durationMin
            this.score = score
            this.recommendations = Onerilenler(apiResponse.hanimeFranchiseHanimeVideos)
        }
    }

    private fun Onerilenler(videos: List<HanimeVideo>?): List<SearchResponse> {
        return videos?.mapNotNull { v ->
            if (v.posterUrl.isNullOrBlank())
                return@mapNotNull null
            newMovieSearchResponse(v.name, "/videos/hentai/${v.slug}", TvType.NSFW) {
                this.posterUrl = v.posterUrl
            }
        } ?: emptyList()
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.isBlank()) return false

        val path = "/api/v8/guest/videos/$data/manifest"

        val manifestResponse = runCatching {
            app.get(
                "$FAHApi$path",
                headers = HeaderAl()
            ).parsed<VideoManifestResponse>()
        }.getOrNull() ?: return false

        val servers = manifestResponse.videosManifest.servers
        Log.d("HanimeTV", "loadLinks id=$data servers=${servers.size}")

        for (server in servers) {
            for (stream in server.streams) {
                if (stream.isGuestAllowed == false) continue
                val qualityInt = stream.height.toIntOrNull() ?: 0
                Log.d("HanimeTV", "stream server=${server.name} height=${stream.height} url=${stream.url}")
                callback(
                    newExtractorLink(
                        source = "HanimeTV - ${server.name}",
                        name = "Hanime",
                        url = stream.url,
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = "$mainUrl/"
                        this.quality = qualityInt
                    }
                )
            }
        }
        return true
    }



    private fun SignatureCek(time: String): String {
        val message = "$time,Xkdi29,$mainUrl,mn2,$time"
        val bytes = MessageDigest.getInstance("SHA-256").digest(message.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Serializable
    data class VideoApiResponse(
        @JsonProperty("hentai_video")
        @SerialName("hentai_video")
        val hanimeVideo: HanimeVideo,
        @JsonProperty("hentai_franchise_hentai_videos")
        @SerialName("hentai_franchise_hentai_videos")
        val hanimeFranchiseHanimeVideos: List<HanimeVideo>? = null,
        @JsonProperty("hentai_franchise")
        @SerialName("hentai_franchise")
        val hanimeFranchise: HanimeFranchise? = null,
        @JsonProperty("videos_manifest")
        @SerialName("videos_manifest")
        val videosManifest: VideosManifest? = null
    )

    @Serializable
    data class HanimeVideo(
        val id: Int,
        val name: String,
        val slug: String,
        val description: String? = null,
        @JsonProperty("poster_url")
        @SerialName("poster_url")
        val posterUrl: String? = null,
        val brand: String? = null,
        val likes: Int? = null,
        val dislikes: Int? = null,
        val views: Int? = null,
        @JsonProperty("duration_in_ms")
        @SerialName("duration_in_ms")
        val durationInMs: Long? = null,
        @JsonProperty("is_censored")
        @SerialName("is_censored")
        val isCensored: Boolean? = null,
        val rating: Double? = null,
        @JsonProperty("hentai_tags")
        @SerialName("hentai_tags")
        val hanimeTags: List<HanimeTag>? = null,
        @JsonProperty("released_at_unix")
        @SerialName("released_at_unix")
        val releasedAtUnix: Long? = null,
        @JsonProperty("created_at_unix")
        @SerialName("created_at_unix")
        val createdAtUnix: Long? = null
    )

    @Serializable
    data class HanimeTag(val text: String)

    @Serializable
    data class HanimeFranchise(
        val id: Int,
        val name: String,
        val slug: String,
        val title: String?
    )

    @Serializable
    data class HvsVideo(
        val id: Int = 0,
        val name: String = "",
        @JsonProperty("search_titles")
        @SerialName("search_titles")
        val searchTitles: String = "",
        val slug: String = "",
        val description: String = "",
        val views: Int = 0,
        @JsonProperty("cover_url")
        @SerialName("cover_url")
        val coverUrl: String = "",
        @JsonProperty("poster_url")
        @SerialName("poster_url")
        val posterUrl: String = "",
        val brand: String = "",
        @JsonProperty("brand_id")
        @SerialName("brand_id")
        val brandId: Int = 0,
        val likes: Int = 0,
        val dislikes: Int = 0,
        val downloads: Int = 0,
        val tags: List<String> = emptyList(),
        @JsonProperty("created_at_unix")
        @SerialName("created_at_unix")
        val createdAtUnix: Long = 0,
        @JsonProperty("released_at_unix")
        @SerialName("released_at_unix")
        val releasedAtUnix: Long = 0,
        @JsonProperty("created_at")
        @SerialName("created_at")
        val createdAt: String = "",
        @JsonProperty("released_at")
        @SerialName("released_at")
        val releasedAt: String = "",
    )

    data class VideoManifestResponse(
        @param:JsonProperty("videos_manifest") val videosManifest: VideosManifest
    )

    data class VideosManifest(
        val servers: List<VideoServer>
    )

    data class VideoServer(
        val id: Int,
        val name: String,
        val streams: List<VideoStream>
    )

    data class VideoStream(
        val id: Int,
        val url: String,
        val kind: String,
        val height: String,
        @param:JsonProperty("is_guest_allowed") val isGuestAllowed: Boolean? = null
    )
}