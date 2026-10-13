// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addDuration
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.nodes.Element

class PornWatch : MainAPI() {
    override var mainUrl              = "https://pornwatch.ws"
    override var name                 = "PornWatch"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override var getMainPageTimeoutMs = 10_000L

    private val defaultHeaders = mapOf(
        "User-Agent"      to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept"          to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.5"
    )

    override val mainPage
        get() = mainPageOf(
            *(try {
                PornWatchAyarlar.getOrderedAndEnabledCategories().map { (path, name) ->
                    val cleanPath = path.trim().removePrefix("/")
                    val url       = when {
                        cleanPath.isEmpty() -> "$mainUrl/"
                        cleanPath.startsWith("?") -> "$mainUrl/$cleanPath"
                        else -> "$mainUrl/${cleanPath.removeSuffix("/")}/"
                    }
                    url to name
                }.toTypedArray()
            } catch (_: Exception) {
                arrayOf(
                    "$mainUrl/" to "Latest"
                )
            })
        )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val dataUrl = request.data
        val url     = if (dataUrl.contains("random", ignoreCase = true)) {
            val randomPage = Random.nextInt(1, 3163)
            "$mainUrl/page/$randomPage/"
        } else if (page == 1) {
            dataUrl
        } else {
            if (dataUrl.contains("?")) {
                val parts = dataUrl.split("?", limit = 2)
                val base  = parts[0].removeSuffix("/")
                val query = parts[1]
                if (base.isEmpty() || base == mainUrl) {
                    "$mainUrl/page/$page/?$query"
                } else {
                    "$base/page/$page/?$query"
                }
            } else {
                val base = dataUrl.removeSuffix("/")
                if (base.isEmpty() || base == mainUrl) {
                    "$mainUrl/page/$page/"
                } else {
                    "$base/page/$page/"
                }
            }
        }

        val home = try {
            val document = app.get(url, headers = defaultHeaders, timeout = 10).document
            document.select("article.card").mapNotNull { it.toSearchResult() }
        } catch (e: Exception) {
            Log.d("PornWatch",  "$url: ${e.message}")
            try {
                val fallbackUrl = if (page == 1) "$mainUrl/" else "$mainUrl/page/$page/"
                val document    = app.get(fallbackUrl, headers = defaultHeaders, timeout = 10).document
                document.select("article.card").mapNotNull { it.toSearchResult() }
            } catch (fallbackEx: Exception) {
                Log.d("PornWatch", "$url: ${fallbackEx.message}")
                emptyList()
            }
        }

        return newHomePageResponse(request.name, home)
    }


    private fun Element.toSearchResult(): SearchResponse? {
        val titleA = this.selectFirst("h2.card__t a, h3.card__t a")
        val title  = titleA?.text()?.trim()?.ifEmpty { null } ?: return null
        if (title.contains(igrencRegex)) {
            return null
        }
        val href = fixUrlNull(titleA.attr("href").ifEmpty { null })
            ?: fixUrlNull(this.selectFirst("a.card__th")?.attr("href")?.ifEmpty { null })
            ?: return null
        if (href.contains(igrencRegex)) {
            return null
        }
        val posterUrl = fixUrlNull(
            this.selectFirst("img.card__img")?.attr("src")?.ifEmpty { null }
                ?: this.selectFirst("img.card__img")?.attr("data-src")?.ifEmpty { null },
        )
        val year = this.selectFirst("span.card__q")?.text()?.trim()?.toIntOrNull()

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.year      = year
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page == 1) {
            "$mainUrl/?s=$query"
        } else {
            "$mainUrl/page/$page/?s=$query"
        }
        val document = app.get(url, headers = defaultHeaders, timeout = 10).document
        val results  = document.select("article.card").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(results, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, headers = defaultHeaders, timeout = 10).document
        val jsonLd   = document.select("script[type=application/ld+json]").firstNotNullOfOrNull { el -> el.data().takeIf { it.contains("VideoObject") } }

        val voBlock = jsonLd?.let { Regex("\"@type\"\\s*:\\s*\"VideoObject\".*?(?=\"keywords\")", RegexOption.DOT_MATCHES_ALL).find(it)?.value }
        fun j(field: String): String? = voBlock?.let { Regex("\"$field\"\\s*:\\s*\\[?\"([^\"]+)\"").find(it)?.groupValues?.get(1) }
        fun jArr(field: String): List<String>? = voBlock?.let {
            Regex("\"$field\"\\s*:\\s*\\[([^\\]]*)\\]").find(it)?.groupValues?.get(1)
                ?.split(",")?.map { it.trim().removeSurrounding("\"") }?.filter { it.isNotEmpty() }
        }

        val posterFromJson     = j("thumbnailUrl")
        val descriptionFromJson = j("description")
        val datePublished       = j("datePublished")
        val durIso              = j("duration")
        val tagsFromJson        = jArr("genre").orEmpty()

        val rawTitle = j("name")
            ?: document.selectFirst("h1.vid__t span.fit, h1 span.fit, h1")?.text()?.trim()?.ifEmpty { null }
            ?: return null
        val title = rawTitle
            .removePrefix("Watch ")
            .removeSuffix(" Porn Movie Online Free")
            .removeSuffix(" Porn Full Movie Online Free")
            .replace(Regex(" \\d{4} by .*$"), "")
            .trim()
            .ifEmpty { rawTitle }

        if (title.contains(igrencRegex) || url.contains(igrencRegex)) return null

        val poster = fixUrlNull(
            posterFromJson?.ifEmpty { null }
                ?: document.selectFirst("meta[property=og:image]")?.attr("content")?.ifEmpty { null },
        )

        // Plot: prefer the clean .desc paragraph, fall back to jsonLd description
        val plot = document.selectFirst("div.desc p")?.text()?.trim()?.ifEmpty { null }
            ?: descriptionFromJson?.ifEmpty { null }

        val year = document.selectFirst("a[href*=/release-year/]")?.text()?.trim()?.toIntOrNull()
            ?: datePublished?.take(4)?.toIntOrNull()

        val durationStr = durIso?.let { Regex("PT(?:(\\d+)H)?(\\d+)M").find(it)?.let { m ->
            val h  = m.groupValues[1]
            val mn = m.groupValues[2]
            if (h.isBlank()) "$mn min" else "$h:$mn"
        } }

        // Actors from the Pornstars chips
        val actors = document.select("a.chip--star, a[href*=/cast/]")
            .map { Actor(it.text().trim()) }
            .filter { it.name.isNotBlank() }

        // Tags: genres + studio + category + released date from the info block, jsonLd genres as fallback
        val genreTags   = document.select("div.info a.chip[href*=/genre/]").map { it.text().trim() }.filter { it.isNotEmpty() }
        val studioTag   = document.selectFirst("div.info a.chip[href*=/director/]")?.text()?.trim()?.ifEmpty { null }
        val categoryTag = document.selectFirst("div.info a.chip[href*=/category/]")?.text()?.trim()?.ifEmpty { null }
        val releasedTag = document.selectFirst("div.row--text time")?.text()?.trim()?.ifEmpty { null }
        val tags = (genreTags + listOfNotNull(studioTag, categoryTag, releasedTag)).ifEmpty { tagsFromJson }

        // Recommendations from the related grid (article.card; ad cards are excluded automatically)
        val recommendations = document.select("article.card").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = plot
            this.year            = year
            this.tags            = tags
            this.recommendations = recommendations
            addDuration(durationStr)
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val hostLinks = mutableSetOf<String>()

        // 1) Real streaming embeds from the player's data-servers JSON (LuluStream, DoodStream, MixDrop, ...)
        document.select("section.hlm[data-servers]").forEach { sec ->
            val raw = sec.attr("data-servers").ifEmpty { null } ?: return@forEach
            runCatching {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val u = arr.getJSONObject(i).optString("u").ifEmpty { null }
                    if (!u.isNullOrBlank()) hostLinks.add(u)
                }
            }
        }

        // 2) Fallback: download / host buttons on the detail page
        document.select("a.hlm-btn").mapNotNull { fixUrlNull(it.attr("href").ifEmpty { null }) }.forEach { hostLinks.add(it) }

        // 3) Fallback: /video-embed/ page host links
        val embedUrl = document.selectFirst("script[type=application/ld+json]")?.data()
            ?.let { Regex("\"embedUrl\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.get(1) }
            ?: document.selectFirst("a[href*=/video-embed/]")?.attr("href")?.ifEmpty { null }

        embedUrl?.let { eu ->
            runCatching { app.get(fixUrl(eu)).document }
                .getOrNull()
                ?.select("a.hlm-btn")
                ?.mapNotNull { fixUrlNull(it.attr("href").ifEmpty { null }) }
                ?.forEach { hostLinks.add(it) }
        }

        if (hostLinks.isEmpty()) return false

        return coroutineScope {
            val jobs = hostLinks.map { linkUrl ->
                async {
                    try {
                        loadExtractor(linkUrl, "$mainUrl/", subtitleCallback, callback)
                    } catch (e: Exception) {
                        Log.d("PornWatch", "linkUrl=$linkUrl error=${e.message}")
                        false
                    }
                }
            }
            jobs.awaitAll().any { it }
        }
    }
}

private val igrencKelimeler = listOf(
    "gay", "homosexual", "queer", "homo", "androphile", "femboy", "feminine boy", "effeminate", "trap",
    "scat", "coprophilia", "coprophagia", "fecal", "poo", "shit", "crap", "bm play", "trans", "Trade",
    "Vers", "Twink", "Otter", "Bear", "Femme", "Masc", "No fats, no fems", "Serving", "Gagged",
    "Receipts", "Kiki", "Kai Kai", "Werk", "Realness", "Hunty", "Snatched", "Beat",
    "Zaddy", "Chosen family", "Closet case", "Out and proud",
    "Henny", "Queening out", "Slay", "Camp", "Fishy", "Cruising", "Bathhouse", "Power bottom",
    "Situationship", "Pegging", "Anal Gape", "Sick", "Gross", "Femdom", "futa", "strap-on", "strapon", "tranny", "tribute", "crossdress",
    "t-girl", "tgirl", "Bisexual", "Intersex", "LGBTQ", "TS", "TGirl", "T-Boy", "Transsexual", "shemale",
)

private val igrencRegex = Regex("\\b(${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b", RegexOption.IGNORE_CASE)
