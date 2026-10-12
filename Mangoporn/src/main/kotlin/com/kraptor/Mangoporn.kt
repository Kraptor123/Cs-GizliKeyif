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

class Mangoporn : MainAPI() {
    override var mainUrl              = "https://mangoporn.net"
    override var name                 = "Mangoporn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded
    override var getMainPageTimeoutMs = 10_000L

    private val defaultHeaders = mapOf(
        "User-Agent"      to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept"          to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.5"
    )

    private val imageHeaders = mapOf("Accept" to "image/avif,image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5")

    override val mainPage
        get() = mainPageOf(
            *(try {
                MangoAyarlar.getOrderedAndEnabledCategories().map { (path, name) ->
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
                    "$mainUrl/" to "Latest Release"
                )
            })
        )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val dataUrl = request.data
        val url     = if (dataUrl.contains("random", ignoreCase = true)) {
            val randomPage = Random.nextInt(1, 3801)
            "$mainUrl/movies/page/$randomPage/"
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
            Log.d("Mangoporn", "$url: ${e.message}")
            try {
                val fallbackUrl = if (page == 1) "$mainUrl/" else "$mainUrl/page/$page/"
                val document    = app.get(fallbackUrl, headers = defaultHeaders, timeout = 10).document
                document.select("article.card").mapNotNull { it.toSearchResult() }
            } catch (fallbackEx: Exception) {
                Log.d("Mangoporn", "$url: ${fallbackEx.message}")
                emptyList()
            }
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2.card__t")?.text()?.trim()?.ifEmpty { null } ?: return null

        if (title.contains(igrencRegex)) return null

        val href = fixUrlNull(
            this.selectFirst("a.card__th")?.attr("href")?.ifEmpty { null }
        ) ?: return null

        val posterUrl = fixUrlNull(this.selectFirst("img.card__img")?.attr("src")?.ifEmpty { null })

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl     = posterUrl
            this.posterHeaders = imageHeaders
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page == 1) {
            "$mainUrl/?s=$query"
        } else {
            "$mainUrl/page/$page/?s=$query"
        }
        val document = app.get(url, headers = defaultHeaders, timeout = 10).document
        val results  = document.select("article.card, article, div.video-block, div.ml-item").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(results, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, headers = defaultHeaders, timeout = 10).document
        val jsonLd   = document.select("script[type=application/ld+json]").firstNotNullOfOrNull { el -> el.data().takeIf { it.contains("VideoObject") } }

        val posterFromJson = jsonLd?.let {
            Regex(""""thumbnailUrl"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1)
        }
        val descriptionFromJson = jsonLd?.let {
            Regex(""""description"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1)
        }

        val title           = document.selectFirst("div.mvic-desc h3, div.data > h1, h1")?.text()?.trim()?.ifEmpty { null } ?: return null
        val poster          = fixUrlNull(
            posterFromJson?.ifEmpty { null }
                ?: document.selectFirst("div.thumb img, div.poster > img")?.attr("data-wpfc-original-src")?.ifEmpty { null }
                ?: document.selectFirst("div.thumb img, div.poster > img")?.attr("src")?.ifEmpty { null }
        )
        val plot            = descriptionFromJson?.ifEmpty { null }
            ?: document.selectFirst("div.mvic-desc div.desc, div.wp-content > p")?.text()?.trim()?.ifEmpty { null }
        val year            = document.selectFirst("a[href*=/release-year/], span.textco a[rel=tag]")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.mvici-left a[href*=/genre/], span.valors a[href*=/genre/]").map { it.text().trim() }.filter { it.isNotEmpty() }
        val actors          = document.select("div.mvici-left a[href*=/director/], div.persons a[href*=/pornstar/]").map { Actor(it.text().trim()) }
        val recommendations = document.select("article.card, article, div.mlw-related div.ml-item, div.sbox.srelacionados article, div.video-block").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = plot
            this.year            = year
            this.tags            = tags
            this.recommendations = recommendations
            this.posterHeaders   = imageHeaders
            addDuration(document.selectFirst("span.mli-meta, span.duration")?.text()?.trim())
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data, headers = defaultHeaders, timeout = 10).document
        val links    = document.select("div#pettabs div.Rtable1-cell a[href], div#pettabs > ul a[href]").mapNotNull { fixUrlNull(it.attr("href").ifEmpty { null }) }
        if (links.isEmpty()) return false

        return coroutineScope {
            val jobs = links.map { linkUrl ->
                async {
                    try {
                        loadExtractor(linkUrl, "$mainUrl/", subtitleCallback, callback)
                    } catch (_: Exception) {
                        false
                    }
                }
            }
            val results = jobs.awaitAll()
            results.any { it }
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
    "Situationship", "Pegging", "Femdom", "futa", "strap-on", "strapon", "tranny", "tribute", "crossdress",
    "t-girl", "tgirl", "Bisexual", "Intersex", "LGBTQ", "TS", "TGirl", "T-Boy", "Transsexual",
)

private val igrencRegex = Regex("\\b(${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b", RegexOption.IGNORE_CASE)
