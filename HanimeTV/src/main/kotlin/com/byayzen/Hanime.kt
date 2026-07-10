// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
        private const val FAHApi = "https://guest.freeanimehentai.net"
        private const val SEARCH_API = "$FAHApi/api/v11/search_hvs"
        private const val authApi = "https://auth.hanime.tv"
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
            val response = app.get(SEARCH_API, headers = HeaderAl()).text
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
        val signature = Hanimecozucu.signatureCek(time, mainUrl)
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
        val doc   = app.get(url, headers = HeaderAl()).document
        val title = doc.selectFirst("section#VideoDetails h1")?.text() ?: return null
        val slug  = url.substringAfterLast("/")

        val poster = doc.selectFirst("img[src*='/images/covers/']")?.attr("abs:src")

        val durationText = doc.select("section#VideoDetails span.badge").firstOrNull {
            it.text().contains("min", ignoreCase = true)
        }?.text()
        val duration = durationText?.let { Regex("(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }

        val tags = doc.select("a[href^=\"/browse/tags/\"]").map { it.text() }

        val plot = doc.select("h2:contains(Synopsis)").first()?.parent()?.selectFirst("div[data-expand-content]")?.text()

        val yearText = doc.select("button[data-tip]").firstOrNull()?.attr("data-tip")
        val year     = yearText?.let { Regex("(\\d{4})").find(it)?.groupValues?.get(1)?.toIntOrNull() }

        val recommendations = doc.select("section#NextVideoSection a[href]").mapNotNull { a ->
            val recTitle  = a.selectFirst("span.line-clamp-2")?.text() ?: a.selectFirst("span.text-white:not(.bg-base-300\\/55)")?.text() ?: return@mapNotNull null
            val recPoster = fixUrlNull(a.selectFirst("img.aspect-video")?.attr("src")) ?: return@mapNotNull null
            val recUrl    = fixUrl(a.attr("href"))
            newMovieSearchResponse(recTitle, recUrl, TvType.NSFW) {
                this.posterUrl = recPoster
            }
        }

        Log.d("HanimeTV", "title=$title year=$year duration=$duration")

        return newMovieLoadResponse(title, url, TvType.NSFW, slug) {
            this.posterUrl       = poster
            this.posterHeaders   = mapOf("Referer" to "$mainUrl/")
            this.plot            = plot
            this.year            = year
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.isBlank()) return false

        val time = (System.currentTimeMillis() / 1000).toString()
        val signature = Hanimecozucu.signatureCek(time, mainUrl)

        Log.d("HanimeTV", "slug=$data time=$time")

        val payloadJson = buildJsonObject {
            put("timestamp_unix", time.toLong())
            put("directive", "htv_player_handshake")
            put("slug", data)
        }.toString()

        val encryptedToken = runCatching {
            Hanimecozucu.encryptHandshakeToken(payloadJson)
        }.getOrNull() ?: run {
            Log.e("HanimeTV", "token encrypt fail")
            return false
        }

        val handshakeHeaders = mapOf(
            "accept" to "application/json",
            "content-type" to "application/json",
            "x-signature-version" to "web2",
            "x-signature" to signature,
            "x-time" to time,
            "x-csrf-token" to "null",
            "origin" to mainUrl,
            "referer" to "$mainUrl/"
        )

        val jsonBody = buildJsonObject {
            put("token", encryptedToken)
        }

        val response = runCatching {
            app.post(
                "$authApi/api/v11/handshake",
                headers = handshakeHeaders,
                json = jsonBody,
                allowRedirects = true
            )
        }.getOrNull() ?: run {
            Log.e("HanimeTV", "handshake request fail")
            return false
        }

        if (!response.isSuccessful) {
            Log.e("HanimeTV", "handshake fail code=${response.code}")
            return false
        }

        val xToken = response.headers["x-token"] ?: response.headers["X-Token"] ?: run {
            Log.e("HanimeTV", "x-token missing")
            return false
        }

        val decryptedJson = runCatching { Hanimecozucu.decryptXToken(xToken) }.getOrNull() ?: run {
            Log.e("HanimeTV", "x-token decrypt fail")
            return false
        }

        val handshakeResponse = try {
            parseJson<Hanimecozucu.HandshakeResponse>(decryptedJson)
        } catch (e: Exception) {
            Log.e("HanimeTV", "parse fail: ${e.message}")
            return false
        }

        Log.d("HanimeTV", "sources=${handshakeResponse.sources.size}")

        for (source in handshakeResponse.sources) {
            if (source.kind != "normal" || source.src.isBlank()) {
                Log.d("HanimeTV", "skip kind=${source.kind} label=${source.label}")
                continue
            }

            val fullUrl = if (source.src.startsWith("http")) source.src else "$mainUrl${source.src}"

            Log.d("HanimeTV", "label=${source.label} height=${source.height} url=$fullUrl")

            callback(
                newExtractorLink(
                    source = "HanimeTV",
                    name = "Hanime - ${source.label.ifBlank { "${source.height}p" }}",
                    url = fullUrl,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$mainUrl/"
                    this.quality = source.height
                }
            )
        }
        return true
    }





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
}

