// ! SuperErotikGeldi eklentisi @Kraptor123 tarafından | @gizlikeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class SuperErotikGeldi : MainAPI() {
    override var mainUrl              = "https://www.superfilmgeldi4.art"
    override var name                 = "SuperErotikGeldi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    // Sinezy URL'si
    private val sinezyUrl = "https://sinezy.org"

    override val mainPage = mainPageOf(
        "${mainUrl}/hdizle/category/yesilcam-erotik-izle/page/"   to "Yeşilçam Erotik",
        "${mainUrl}/hdizle/category/hd-erotik-filmler-izle/page/" to "Erotik Filmler",
        "${sinezyUrl}/izle/erotik-film-izle/"                     to "Sinezy Erotik",
        "${sinezyUrl}/izle/yetiskin-film/"                        to "Sinezy Yetişkin +18",
        "${sinezyUrl}/izle/turkce-altyazili-promo/"                 to "Sinezy Altyazılı Porno"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        return if (request.data.lowercase().contains("sinezy.org")) {
            // Sinezy için ana sayfa
            val document = app.get("${request.data}page/$page/").document
            val home     = document.select("div.container div.content div.movie_box.move_k").mapNotNull { it.toSinezyMainPageResult() }
            newHomePageResponse(request.name, home)
        } else {
            // SuperFilmGeldi için ana sayfa
            val document = app.get("${request.data}${page}").document
            val home     = document.select("div.movie-preview-content").mapNotNull { it.toSearchResult() }
            newHomePageResponse(request.name, home)
        }
    }

    private fun removeUnnecessarySuffixes(title: String): String {
        val unnecessarySuffixes = listOf(
            " izle",
            " full film",
            " filmini full",
            " full türkçe",
            " alt yazılı",
            " altyazılı",
            " tr dublaj",
            " hd türkçe",
            " türkçe dublaj",
            " yeşilçam ",
            " erotik fil",
            " türkçe",
            " yerli",
        )

        var cleanedTitle = title.trim()

        for (suffix in unnecessarySuffixes) {
            val regex = Regex("${Regex.escape(suffix)}.*$", RegexOption.IGNORE_CASE)
            cleanedTitle = cleanedTitle.replace(regex, "").trim()
        }

        return cleanedTitle
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span.movie-title a")?.text()?.substringBefore(" izle") ?: return null
        val hrefraw   = fixUrlNull(this.selectFirst("span.movie-title a")?.attr("href")) ?: return null
        val href      = if (!hrefraw.contains("erotik")) {
            return null
        }else {
            hrefraw
        }
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(removeUnnecessarySuffixes(title), href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    private fun Element.toSinezyMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}?s=${query}").document

        return document.select("div.movie-preview-content").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        return if (url.lowercase().contains("sinezy.org")) {
            loadSinezy(url)
        } else {
            loadSuperFilmGeldi(url)
        }
    }

    private suspend fun loadSuperFilmGeldi(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.title h1")?.text()?.trim()?.substringBefore(" izle") ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.poster img")?.attr("src"))
        val year            = document.selectFirst("div.release a")?.text()?.toIntOrNull()
        val description     = document.selectFirst("div.excerpt p")?.text()?.trim()
        val tags            = document.select("div.categories a").map { it.text() }
        val rating          = document.selectFirst("span.imdb-rating")?.text()?.trim()?.split(" ")?.first()?.toRatingInt()
        val recommendations = document.select("div.film-content div.existing_item").mapNotNull { it.toSearchResult() }
        val actors          = document.select("div.actor a").map {
            Actor(it.text())
        }

        return newMovieLoadResponse(removeUnnecessarySuffixes(title), url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.year            = year
            this.plot            = description
            this.tags            = tags
            this.score           = Score.from10(rating)
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private suspend fun loadSinezy(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.detail")?.attr("title") ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.move_k img")?.attr("data-src"))
        val description     = document.selectFirst("div.desc.yeniscroll p")?.text()?.trim()
        val year            = document.selectFirst("div.move_k span.year span")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.detail span a").map { it.text() }
        val rating          = document.selectFirst("span.info span.imdb")?.text()?.trim()?.toRatingInt()
        val duration        = document.selectFirst("div.detail > span:nth-child(1) > span:nth-child(2) > p:nth-child(1)")
            ?.text()
            ?.replace(" Dakika","")
            ?.trim()?.toIntOrNull()
        val actors = document.select("span.oyn p")
            .flatMap { it.text().split(",") }
            .map { Actor(it.trim()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(rating)
            this.duration        = duration
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        return if (data.lowercase().contains("sinezy.org")) {
            loadLinksSinezy(data, isCasting, subtitleCallback, callback)
        } else {
            loadLinksSuperFilmGeldi(data, isCasting, subtitleCallback, callback)
        }
    }

    private suspend fun loadLinksSuperFilmGeldi(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("SFG", "data » $data")
        val document = app.get(data).document
        val iframe   = fixUrlNull(document.selectFirst("div#vast iframe")?.attr("src")) ?: return false
        Log.d("SFG", "iframe » $iframe")

        if (iframe.contains("mix") and iframe.contains("index.php?data=")) {
            val iSource  = app.get(iframe, referer="${mainUrl}/").text
            val mixPoint = Regex("""videoUrl":"(.*)","videoServer""").find(iSource)?.groupValues?.get(1)?.replace("\\", "") ?: return false

            var endPoint = "?s=0&d="

            if (iframe.contains("mixlion")) {
                endPoint = "?s=3&d="
            } else if (iframe.contains("mixeagle")) {
                endPoint = "?s=1&d="
            }

            val m3uLink = iframe.substringBefore("/player") + mixPoint + endPoint
            Log.d("SFG", "m3uLink » $m3uLink")

            callback.invoke(
                newExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = m3uLink,
                    type = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to iframe)
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                }
            )
        } else {
            loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }

    private suspend fun loadLinksSinezy(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_${this.name}", "data = ${data}")
        val document = app.get(data).text

        val regex = Regex(pattern = """ilkpartkod = '([^']*)';""", options = setOf(RegexOption.IGNORE_CASE))

        val findreg = regex.find(document)?.groupValues?.get(1).toString()

        val reqCoz  = base64Decode(findreg)

        val iframe  = reqCoz.substringAfter("src=").substringBefore(" ").replace("\"","")

        Log.d("kraptor_${this.name}", "iframe = ${iframe}")

        loadExtractor(iframe, "${sinezyUrl}/", subtitleCallback, callback)

        return true
    }
}