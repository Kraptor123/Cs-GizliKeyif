package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlin.random.Random
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch


class Mangoporn : MainAPI() {
    override var mainUrl = "https://mangoporn.net"
    override var name = "Mangoporn"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val MAX_PAGE = 3676
    override val mainPage
        get() = mainPageOf(
            *(try {
                MangoAyarlar.getOrderedAndEnabledCategories().toTypedArray()
            } catch (e: Exception) {
                arrayOf(
                )
            })
        )


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (request.data.endsWith("/random")) {
            val randomPageNumber = Random.nextInt(1, MAX_PAGE + 1)
            "$mainUrl/movies/page/$randomPageNumber"
        } else {
            fixUrl("${request.data}/page/$page")
        }

        val document = app.get(url).document
        val home = document.select("div.items > article")
            .mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = false
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val tumKelimeler = igrencKelimeler + Anamenudekiboklar
        val desen = "\\b(?:${tumKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b"
        val kirliKelimeRegex = Regex(desen, RegexOption.IGNORE_CASE)
        val title = this.select("div h3").text()

        if (title.contains(kirliKelimeRegex)) return null

        val href = fixUrl(this.select("div h3 a").attr("href"))
        val img = this.select("div.poster > img")
        val posterUrl = img.attr("data-wpfc-original-src").ifEmpty { img.attr("src") }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Accept" to "image/avif,image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5")
        }
    }

    private fun Element.toSearchingResult(): SearchResponse? {
        val tumKelimeler = igrencKelimeler + Anamenudekiboklar
        val desen = "\\b(?:${tumKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b"
        val kirliKelimeRegex = Regex(desen, RegexOption.IGNORE_CASE)
        val title = this.select("div.details a").text()

        if (title.contains(kirliKelimeRegex)) return null

        val href = fixUrl(this.select("div.image a").attr("href"))
        val img = this.select("div.image img")
        val posterUrl = img.attr("data-wpfc-original-src")
            .ifEmpty { img.attr("src") }
            .ifEmpty { img.attr("data-src") }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Accept" to "image/avif,image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5")
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..2) {
            val document = app.get("${mainUrl}/page/$i/?s=$query").document

            val results = document.select("article")
                .mapNotNull { it.toSearchingResult() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("div.data > h1")?.text().toString()
        val poster = document.selectFirst("div.poster > img")?.attr("data-wpfc-original-src")
            ?.ifEmpty { document.selectFirst("div.poster > img")?.attr("src") }
            ?.trim()
            .toString()

        val year = document.selectFirst("span.textco a[rel=tag]")?.text()?.trim()?.toIntOrNull()
        val duration = document.selectFirst("span.duration")?.text()?.let {
            val hours = Regex("""(\d+)\s*hrs""").find(it)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val minutes = Regex("""(\d+)\s*mins""").find(it)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            (hours * 60) + minutes
        }
        val description = document.selectFirst("div.wp-content > p")?.text()
        val actors = document.select("div.persons a[href*=/pornstar/]").map { Actor(it.text()) }

        val desen = "\\b(?:${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b"
        val kirliKelimeRegex = Regex(desen, RegexOption.IGNORE_CASE)
        val tags = document.select("span.valors a[href*=/genre/]").map { it.text() }

        val recommendations = document.select("div.sbox.srelacionados article").map {
            newMovieSearchResponse(
                it.select("img").attr("alt").replace("Watch ", "").replace(" Porn Online Free", ""),
                it.select("a").attr("href"),
                TvType.NSFW
            ) {
                this.posterUrl = it.select("img").attr("data-wpfc-original-src")
            }
        }

        val imageHeaders = mapOf("Accept" to "image/avif,image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5")

        if (tags.any { it.contains(kirliKelimeRegex) }) {
            val blockedTitle = "Disabled content."
            val blockedDescription = "This content disabled due to filters."
            val blockedPoster = "https://i.imgur.com/3eR1JvE.png"
            val urlBos = ""
            return newMovieLoadResponse(blockedTitle, urlBos, TvType.NSFW, urlBos) {
                this.posterUrl = blockedPoster
                this.plot = blockedDescription
                this.tags = tags
                this.posterHeaders = imageHeaders
                addActors(actors)
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.NSFW, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.duration = duration
                this.tags = tags
                this.recommendations = recommendations
                this.posterHeaders = imageHeaders
                addActors(actors)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = coroutineScope {
        val response = app.get(data)
        val document = response.document
        val tabs = document.select("div#pettabs > ul a")
        val jobs = tabs.map { it.attr("href") }.filter { it.isNotEmpty() }.map { link ->
            launch {
                val fullUrl = fixUrl(link)

                loadExtractor(fullUrl, subtitleCallback) { extractorLink ->
                    callback.invoke(extractorLink)
                }
            }
        }

        jobs.joinAll()
        Log.d("Test", "All jobs finished")
        true
    }

    private val igrencKelimeler = listOf(
        "gay",
        "homosexual",
        "queer",
        "homo",
        "androphile",
        "femboy",
        "feminine boy",
        "effeminate",
        "trap",
        "scat",
        "coprophilia",
        "coprophagia",
        "fecal",
        "poo",
        "shit",
        "crap",
        "bm play",
        "trans",
        "Trade",
        "Vers",
        "Twink",
        "Otter",
        "Bear",
        "Femme",
        "Masc",
        "No fats, no fems",
        "Serving",
        "Gagged",
        "G.O.A.T.",
        "Receipts",
        "Kiki",
        "Kai Kai",
        "Werk",
        "Realness",
        "Hunty",
        "Snatched",
        "Beat",
        "Clocked",
        "Shade",
        "Daddy",
        "Zaddy",
        "Chosen family",
        "Closet case",
        "Out and proud",
        "Henny",
        "Queening out",
        "Slay",
        "Camp",
        "Fishy",
        "Cruising",
        "Bathhouse",
        "Power bottom",
        "Situationship",
        "Pegging",
        "Femdom",
        "futa",
        "tranny",
        "crossdress",
        "Bisexual"
    )

    private val Anamenudekiboklar = listOf("TS", "Trans", "TGirl", "gay", "pegging", "bi", "femboy", "T-Boy", "Bisexual", "Transsexual", "Trans")
}
