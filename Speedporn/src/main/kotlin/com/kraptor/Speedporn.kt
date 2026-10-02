// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addDuration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.nodes.Element
import kotlin.random.Random

class Speedporn : MainAPI() {
    override var mainUrl              = "https://mangoporn.net"
    override var name                 = "Speedporn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    private val imageHeaders = mapOf("Accept" to "image/avif,image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5")

    override val mainPage
        get() = mainPageOf(
            *(try {
                SpeedAyarlar.getOrderedAndEnabledCategories().map { (path, name) ->
                    val cleanPath = path.trim().removePrefix("/").removeSuffix("/")
                    "$mainUrl/$cleanPath/" to name
                }.toTypedArray()
            } catch (_: Exception) {
                arrayOf(
                    "$mainUrl/movies/" to "Latest Release"
                )
            })
        )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val dataUrl = request.data.removeSuffix("/")
        val url = if (dataUrl.endsWith("random", ignoreCase = true)) {
            val randomPage = Random.nextInt(1, 3801)
            "$mainUrl/movies/page/$randomPage/"
        } else if (page == 1) {
            "$dataUrl/"
        } else {
            "$dataUrl/page/$page/"
        }

        val document = app.get(url).document
        val home     = document.select("div.video-block, article, div.ml-item").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("span.title, a.infos, h2, h3 a, div.details a")?.text()?.trim()
            ?.ifEmpty { null } ?: this.selectFirst("a.infos")?.attr("title")?.trim()?.ifEmpty { null } ?: return null

        if (title.contains(igrencRegex)) return null

        val href = fixUrlNull(
            this.selectFirst("a.thumb, a.infos, a.ml-mask, h3 a, div.image a, a")?.attr("href")?.ifEmpty { null }
        ) ?: return null

        val img = this.selectFirst("img")
        val posterUrl = fixUrlNull(
            img?.attr("data-wpfc-original-src")?.ifEmpty { null }
                ?: img?.attr("src")?.ifEmpty { null }
                ?: img?.attr("data-src")?.ifEmpty { null }
        )

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
        val document = app.get(url).document
        val results  = document.select("div.video-block, article, div.ml-item").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(results, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val jsonLd   = document.select("script[type=application/ld+json]").firstNotNullOfOrNull { el -> el.data().takeIf { it.contains("VideoObject") } }

        val posterFromJson = jsonLd?.let {
            Regex(""""thumbnailUrl"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1)
        }
        val descriptionFromJson = jsonLd?.let {
            Regex(""""description"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1)
        }

        val title    = document.selectFirst("div.mvic-desc h3, div.data > h1, h1")?.text()?.trim()?.ifEmpty { null } ?: return null
        val poster   = fixUrlNull(
            posterFromJson?.ifEmpty { null }
                ?: document.selectFirst("div.thumb img, div.poster > img")?.attr("data-wpfc-original-src")?.ifEmpty { null }
                ?: document.selectFirst("div.thumb img, div.poster > img")?.attr("src")?.ifEmpty { null }
        )
        val plot     = descriptionFromJson?.ifEmpty { null }
            ?: document.selectFirst("div.mvic-desc div.desc, div.wp-content > p")?.text()?.trim()?.ifEmpty { null }
        val year     = document.selectFirst("a[href*=/release-year/], span.textco a[rel=tag]")?.text()?.trim()?.toIntOrNull()
        val tags     = document.select("div.mvici-left a[href*=/genre/], span.valors a[href*=/genre/]").map { it.text().trim() }.filter { it.isNotEmpty() }
        val actors   = document.select("div.mvici-left a[href*=/director/], div.persons a[href*=/pornstar/]").map { Actor(it.text().trim()) }
        val recommendations = document.select("div.mlw-related div.ml-item, div.sbox.srelacionados article, div.video-block").mapNotNull { it.toSearchResult() }

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
        val document = app.get(data).document
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
