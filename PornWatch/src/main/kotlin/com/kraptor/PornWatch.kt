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

class PornWatch : MainAPI() {
    override var mainUrl              = "https://pornwatch.ws"
    override var name                 = "PornWatch"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/movies/"                            to "Movies",
        "$mainUrl/most-viewed/"                       to "Most Viewed",
        "$mainUrl/most-rating/"                       to "Most Rating",
        "$mainUrl/genre/appearance/"                  to "Appearance",
        "$mainUrl/genre/international/"               to "International",
        "$mainUrl/genre/ethnic/"                      to "Ethnic",
        "$mainUrl/genre/gonzo/"                       to "Gonzo",
        "$mainUrl/genre/big-tits/"                    to "Big Tits",
        "$mainUrl/genre/oral/"                        to "Oral",
        "$mainUrl/genre/age/"                         to "Age",
        "$mainUrl/genre/cumshots/"                    to "Cumshots",
        "$mainUrl/genre/anal/"                        to "Anal",
        "$mainUrl/genre/big-dicks/"                   to "Big Dicks",
        "$mainUrl/genre/amateur/"                     to "Amateur",
        "$mainUrl/genre/blowjobs/"                    to "Blowjobs",
        "$mainUrl/genre/shaved/"                      to "Shaved",
        "$mainUrl/genre/european/"                    to "European",
        "$mainUrl/genre/group-sex/"                   to "Group Sex",
        "$mainUrl/genre/clothing/"                    to "Clothing",
        "$mainUrl/genre/18-teens/"                    to "18 Teens",
        "$mainUrl/genre/interracial/"                 to "Interracial",
        "$mainUrl/genre/plot-oriented/"               to "Plot Oriented",
        "$mainUrl/genre/brunettes/"                   to "Brunettes",
        "$mainUrl/genre/threesomes/"                  to "Threesomes",
        "$mainUrl/genre/character/"                   to "Character",
        "$mainUrl/genre/small-tits/"                  to "Small Tits",
        "$mainUrl/genre/sex-toy-play/"                to "Sex Toy Play",
        "$mainUrl/genre/erotic-vignette/"             to "Erotic Vignette",
        "$mainUrl/genre/facials/"                     to "Facials",
        "$mainUrl/genre/blondes/"                     to "Blondes",
        "$mainUrl/genre/big-butt/"                    to "Big Butt",
        "$mainUrl/genre/fetish/"                      to "Fetish",
        "$mainUrl/genre/naturally-busty/"             to "Naturally Busty",
        "$mainUrl/genre/popular-with-women/"          to "Popular With Women",
        "$mainUrl/genre/milf/"                        to "Milf",
        "$mainUrl/genre/masturbation/"                to "Masturbation",
        "$mainUrl/genre/compilation/"                 to "Compilation",
        "$mainUrl/genre/pov/"                         to "Pov",
        "$mainUrl/genre/tattoos/"                     to "Tattoos",
        "$mainUrl/genre/lesbian/"                     to "Lesbian",
        "$mainUrl/genre/asian/"                       to "Asian",
        "$mainUrl/genre/language/"                    to "Language",
        "$mainUrl/genre/stockings/"                   to "Stockings",
        "$mainUrl/genre/pantyhose/"                   to "Pantyhose",
        "$mainUrl/genre/settings/"                    to "Settings",
        "$mainUrl/genre/bbc/"                         to "Bbc",
        "$mainUrl/genre/interracial-black-men/"       to "Interracial Black Men",
        "$mainUrl/genre/all-sex/"                     to "All Sex",
        "$mainUrl/genre/interracial-caucasian-girls/" to "Interracial Caucasian Girls",
        "$mainUrl/genre/bdsm/"                        to "Bdsm",
        "$mainUrl/genre/niche/"                       to "Niche",
        "$mainUrl/genre/creampie/"                    to "Creampie",
        "$mainUrl/genre/japanese/"                    to "Japanese",
        "$mainUrl/genre/redheads/"                    to "Redheads",
        "$mainUrl/genre/mature/"                      to "Mature",
        "$mainUrl/genre/family-roleplay/"             to "Family Roleplay",
        "$mainUrl/genre/couples/"                     to "Couples",
        "$mainUrl/genre/lingerie/"                    to "Lingerie",
        "$mainUrl/genre/feature/"                     to "Feature",
        "$mainUrl/genre/cunnilingus/"                 to "Cunnilingus",
        "$mainUrl/genre/double-penetration/"          to "Double Penetration",
        "$mainUrl/genre/petite/"                      to "Petite",
        "$mainUrl/genre/black-women/"                 to "Black Women"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data.removeSuffix("/")}/page/$page/"
        }
        val document = app.get(url).document
        val home     = document.select("div.ml-item").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text()?.trim()?.ifEmpty { null } ?: return null
        if (title.contains(igrencRegex)) {
            return null
        }
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")?.ifEmpty { null }) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src")?.ifEmpty { null })

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page == 1) {
            "$mainUrl/?s=$query"
        } else {
            "$mainUrl/page/$page/?s=$query"
        }
        val document = app.get(url).document
        val results  = document.select("div.ml-item").mapNotNull { it.toSearchResult() }

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
                ?: document.selectFirst("div.thumb img, div.poster > img")?.attr("src")?.ifEmpty { null },
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
            addDuration(document.selectFirst("span.mli-meta, span.duration")?.text()?.trim())
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val links    = document.select("div#pettabs div.Rtable1-cell a[href]").mapNotNull { fixUrlNull(it.attr("href").ifEmpty { null }) }
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
    "Trade", "Vers", "Twink", "Otter", "Bear", "Femme", "Masc", "No fats, no fems", "Serving", "Gagged",
    "Receipts", "Kiki", "Kai Kai", "Werk", "Realness", "Hunty", "Snatched", "Beat",
    "Zaddy", "Chosen family", "Closet case", "Out and proud",
    "Henny", "Queening out", "Slay", "Camp", "Fishy", "Cruising", "Bathhouse", "Power bottom",
    "Situationship", "Pegging", "Anal Gape", "Sick", "Gross", "Femdom", "futa", "strap-on", "strapon", "tranny", "tribute", "crossdress",
    "t-girl", "tgirl", "Bisexual", "Intersex", "LGBTQ", "Trans",
)

private val igrencRegex = Regex("\\b(${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b", RegexOption.IGNORE_CASE)
