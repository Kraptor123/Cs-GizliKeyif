// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import java.util.Base64

class JavGuru : MainAPI() {
    override var mainUrl = "https://jav.guru"
    override var name = "JavGuru"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/page/"              to "Ana Sayfa",
        "${mainUrl}/most-watched-rank/page/"        to "En Çok İzlenenler",
        "${mainUrl}/category/jav-uncensored/page/"  to "Sansürsüz",
        "${mainUrl}/category/amateur/page/"         to "Amatör",
        "${mainUrl}/category/idol/page/"            to "İdol",
        "${mainUrl}/category/english-subbed/page/"  to "İngilizce Altyazılı",
        "${mainUrl}/tag/married-woman/page/"        to "Evli",
        "${mainUrl}/tag/mature-woman/page/"         to "Olgun",
        "${mainUrl}/tag/big-tits/page/"             to "Büyük Memiktolar",
        "${mainUrl}/tag/stepmother/page/"           to "Üvey Anne",
        "${mainUrl}/tag/incest/page/"               to "Biz Bir Aileyiz",
        "${mainUrl}/tag/bukkake/page/"              to "Bukkake",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page/", headers =
            mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Referer" to "${mainUrl}/"
            )).document
        Log.d("kraptor_${this.name}","document = $document")
        val home = document.select("ul.wpp-list li, div.imgg a").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val titleFirst = this.selectFirst("a")?.attr("title")
        val titleAlt = this.selectFirst("img")?.attr("alt")
        val title = when {
            !titleFirst.isNullOrBlank() && !titleAlt.isNullOrBlank() -> "$titleFirst - $titleAlt"
            !titleFirst.isNullOrBlank() -> titleFirst
            !titleAlt.isNullOrBlank() -> titleAlt
            else -> return null
        }
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("${mainUrl}/page/$i/?s=$query").document
            val results = document.select("#main > div").mapNotNull { it.toSearchResult() }
            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }
            if (results.isEmpty()) break
        }
        return searchResponse
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title     = this.select("div > div > div > a > img").attr("alt")
        val href      = fixUrl(this.select("div > div > div > a").attr("href"))
        val posterUrl = fixUrlNull(this.select("div > div > div > a > img").attr("src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.large-screenimg img")?.attr("src"))
        val description = "+18 yaş ve üzeri için uygundur!"
        val year = document.selectFirst("div.infoleft > ul:nth-child(2) > li:nth-child(2)")?.text()
            ?.substringAfter("Release Date:")
            ?.trim()
            ?.substringBefore("-")
            ?.toIntOrNull()
        val tags = document.select("li.w1 a").map { it.text() }
        val rating = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()?.toRatingInt()
        val duration = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("li.w1:nth-child(9) a").map { Actor(it.text()) }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score           = Score.from10(rating)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a img")?.attr("alt") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit ): Boolean {
        Log.d("kraptor_${this.name}", data)
        val document = app.get(data).document
        val script = document.select("script:containsData(iframe_url)").html()
        val iframeb64 = Regex(""""iframe_url":"([^"]+)"""")
        val iframeUrler = iframeb64.findAll(script)
            .map { it.groupValues[1] }
            .map { base64Decode(it) }
            .toList()
        iframeUrler.forEach {
            Log.d("kraptor_${this.name}", it)
            val iframedocument = app.get(it, referer = it).document
            val olid = iframedocument.toString().substringAfter("var OLID = '").substringBefore("'")
            val yeniIstek = iframedocument.toString().substringAfter("iframe").substringAfter("src=\"").substringBefore("'+OLID")
            val reverseolid = olid.reversed()
            val videoyaGit = app.get("$yeniIstek$reverseolid", referer = it, allowRedirects = false)
            val link = videoyaGit.headers["location"].toString()
            if (link.contains(".m3u")) {
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = name,
                        url = link,
                        INFER_TYPE
                    ) {
                        this.referer = ""
                        this.quality = getQualityFromName("")
                    }
                )
            } else {
                loadExtractor(link, referer = it, subtitleCallback, callback)
            }
        }
        return true
    }
}
