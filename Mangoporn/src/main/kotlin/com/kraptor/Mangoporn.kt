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
                val categories = MangoAyarlar.getOrderedAndEnabledCategories().map { (path, name) ->
                    val cleanPath = path.trim().removePrefix("/")
                    val url       = when {
                        cleanPath.isEmpty() -> "$mainUrl/"
                        cleanPath.startsWith("?") -> "$mainUrl/$cleanPath"
                        else -> "$mainUrl/${cleanPath.removeSuffix("/")}/"
                    }
                    url to name
                }.toTypedArray()
                Log.d("Mangoporn", "categoriesCount=${categories.size}")
                categories
            } catch (e: Exception) {
                Log.d("Mangoporn", "categoriesError=${e.message}")
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

        Log.d("Mangoporn", "page=$page requestName=${request.name} dataUrl=$dataUrl url=$url")

        val home = try {
            val response = app.get(url, headers = defaultHeaders, timeout = 10)
            Log.d("Mangoporn", "code=${response.code} url=${response.url}")
            val cards    = response.document.select("article.card")
            Log.d("Mangoporn", "cardsCount=${cards.size}")
            val items    = cards.mapNotNull { it.toSearchResult() }
            Log.d("Mangoporn", "itemsCount=${items.size}")
            items
        } catch (e: Exception) {
            Log.d("Mangoporn", "url=$url error=${e.message}")
            try {
                val fallbackUrl = if (page == 1) "$mainUrl/" else "$mainUrl/page/$page/"
                val response    = app.get(fallbackUrl, headers = defaultHeaders, timeout = 10)
                Log.d("Mangoporn", "fallbackCode=${response.code} fallbackUrl=${response.url}")
                val cards       = response.document.select("article.card")
                Log.d("Mangoporn", "fallbackCardsCount=${cards.size}")
                val items       = cards.mapNotNull { it.toSearchResult() }
                Log.d("Mangoporn", "fallbackItemsCount=${items.size}")
                items
            } catch (fallbackEx: Exception) {
                Log.d("Mangoporn", "fallbackUrl=$url fallbackError=${fallbackEx.message}")
                emptyList()
            }
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2.card__t, h3.card__t")?.text()?.trim()?.ifEmpty { null } ?: return null

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
        Log.d("Mangoporn", "query=$query page=$page url=$url")
        val response = app.get(url, headers = defaultHeaders, timeout = 10)
        Log.d("Mangoporn", "code=${response.code} url=${response.url}")
        val results  = response.document.select("article.card, article, div.video-block, div.ml-item").mapNotNull { it.toSearchResult() }
        Log.d("Mangoporn", "resultsCount=${results.size}")

        return newSearchResponseList(results, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        Log.d("Mangoporn", "url=$url")
        val response        = app.get(url, headers = defaultHeaders, timeout = 10)
        Log.d("Mangoporn", "code=${response.code} url=${response.url}")
        val document        = response.document
        val title           = document.selectFirst("span.bc__c")?.text()?.trim()?.ifEmpty { null } ?: return null
        val poster          = fixUrlNull(document.selectFirst("img.hlm-screen__poster")?.attr("src")?.ifEmpty { null })
        val plot            = document.selectFirst("div.desc > p")?.text()?.trim()?.ifEmpty { null }
        val year            = document.selectFirst("span.st--year")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.row--chips a[href*=/movies/genre/]").map { it.text().trim() }.filter { it.isNotEmpty() }
        val actors          = document.select("div.row--stars a[href*=/movies/pornstar/]").map { Actor(it.text().trim()) }
        val duration        = document.selectFirst("span.st--duration")?.text()?.trim()
        val recommendations = document.select("section.sec article.card").mapNotNull { it.toSearchResult() }
        val sources         = Regex(""""u":"([^"]+)"""").findAll(document.selectFirst("section.hlm[data-servers]")?.attr("data-servers").orEmpty()).toList()
        if (sources.isEmpty()) throw ErrorLoadingException("NO sources found!")
        Log.d("Mangoporn", "title=$title recommendationsCount=${recommendations.size}")

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = plot
            this.year            = year
            this.tags            = tags
            this.recommendations = recommendations
            this.posterHeaders   = imageHeaders
            addDuration(duration)
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("Mangoporn", "data=$data")
        val response = app.get(data, headers = defaultHeaders, timeout = 10)
        Log.d("Mangoporn", "code=${response.code} url=${response.url}")
        val document = response.document
        val links    = Regex(""""u":"([^"]+)"""").findAll(document.selectFirst("section.hlm[data-servers]")?.attr("data-servers").orEmpty()).map { fixUrl(it.groupValues[1].replace("\\/", "/")) }.toList()
        Log.d("Mangoporn", "linksCount=${links.size}")
        if (links.isEmpty()) return false

        return coroutineScope {
            val jobs = links.map { linkUrl ->
                async {
                    try {
                        loadExtractor(linkUrl, "$mainUrl/", subtitleCallback, callback)
                    } catch (e: Exception) {
                        Log.d("Mangoporn", "linkUrl=$linkUrl error=${e.message}")
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
    "t-girl", "tgirl", "Bisexual", "Intersex", "LGBTQ", "TS", "TGirl", "T-Boy", "Transsexual", "shemale",
)

private val igrencRegex = Regex("\\b(${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b", RegexOption.IGNORE_CASE)
