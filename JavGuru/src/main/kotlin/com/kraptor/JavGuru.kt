// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import kotlin.text.Regex
import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
class JavGuru : MainAPI() {
    override var mainUrl = "https://jav.guru"
    override var name = "JavGuru"
    override val hasMainPage = true
    override var lang = "jp"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    private val mainHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.5",
        "Referer" to "$mainUrl/",
        "Upgrade-Insecure-Requests" to "1"
    )

    override val mainPage = mainPageOf(
        mainUrl to "Home",
        "$mainUrl/most-watched-rank" to "Most Watched",
        "$mainUrl/category/jav-uncensored" to "Uncensored",
        "$mainUrl/category/amateur" to "Amateur",
        "$mainUrl/category/idol" to "Idol",
        "$mainUrl/category/english-subbed" to "English Subbed",
        "$mainUrl/tag/married-woman" to "Married",
        "$mainUrl/tag/mature-woman" to "Mature",
        "$mainUrl/tag/big-tits" to "Big Tits",
        "$mainUrl/tag/stepmother" to "Stepmother",
        "$mainUrl/tag/incest" to "Incest",
        "$mainUrl/tag/bukkake" to "Bukkake",
        "$mainUrl/tag/slut" to "Slut",
        "$mainUrl/tag/cowgirl" to "Cowgirl",
        "$mainUrl/tag/nasty" to "Nasty",
        "$mainUrl/tag/hardcore" to "Hardcore",
        "$mainUrl/tag/abuse" to "Abuse",
        "$mainUrl/tag/gal" to "Gal",
        "$mainUrl/tag/black-actor" to "Black",
        "$mainUrl/tag/pantyhose" to "Pantyhose",
        "$mainUrl/tag/prostitutes" to "Prostitutes",
        "$mainUrl/tag/bride" to "Bride",
        "$mainUrl/tag/maid" to "Maid",
        "$mainUrl/tag/gangbang" to "Gangbang",
        "$mainUrl/tag/underwear" to "Underwear"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get("${request.data}/", headers = mainHeaders).document
        } else {
            app.get("${request.data}/page/$page/", headers = mainHeaders).document
        }
        val items = document.select("main#main div.row div.column div.inside-article, div.tabcontent li")

        val hasNext = !request.name.contains("En Çok İzlenenler")

        val home = items.mapNotNull { it.toSearchResponse() }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = hasNext
        )
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val href = fixUrlNull(this.selectFirst("div.imgg a, a")?.attr("href")).toString()
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src")).toString()
        val title = this.selectFirst("img")?.attr("alt")
            ?.ifBlank { this.selectFirst("a")?.attr("title") }.toString()

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mainHeaders
        }
    }


    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1) {
            app.get("${mainUrl}/?s=$query", mainHeaders).document
        } else {
            app.get("${mainUrl}/page/$page/?s=$query", mainHeaders).document
        }
        val aramaCevap = document.select("#main > div").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.select("a img").attr("alt")
        val href = fixUrl(this.select("a").attr("href"))
        val posterUrl = fixUrlNull(this.select("a img").attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mainHeaders
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, headers = mainHeaders).document

        val title = document.selectFirst("h1.tit1")?.text()?.trim()
            ?: document.selectFirst("h1")?.text()?.trim()
            ?: "Unknown"

        val poster = fixUrlNull(document.selectFirst("div.large-screenshot img")?.attr("src"))

        val description = document.select("div.wp-content p:not(:has(img))").joinToString(" ") { it.text() }
            .ifBlank { "Japonları Seviyoruz..." }

        val yearText = document.selectFirst("div.infometa li:contains(Release Date)")?.ownText()?.substringBefore("-")?.toIntOrNull()


        val tags = document.select("li.w1 a[rel=tag]").mapNotNull { it.text().trim() }

        val recommendations = document.select("li").mapNotNull { it.toRecommendationResult() }

        val actors = document.select("li.w1 strong:not(:contains(tags)) ~ a").mapNotNull { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.posterHeaders = mainHeaders
            this.plot = description
            this.year = yearText
            this.tags = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a img")?.attr("alt")?.trim()
        if (title.isNullOrBlank()) return null   // title boşsa listeye eklenmez

        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mainHeaders
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data, headers = mainHeaders).text

        val regex = Regex(pattern = "\"iframe_url\":\"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))
        val iframe = regex.findAll(document)

        iframe.forEach { iframeMatch ->
            val iframeEncoded = iframeMatch.groupValues[1]
            val iframeCoz = base64Decode(iframeEncoded)


            val iframeAl = app.get(iframeCoz, mainHeaders).text

            val frameBaseRegex = Regex(pattern = "frameBase = '([^']*)'", options = setOf(RegexOption.IGNORE_CASE))
            val paramRegex = Regex(pattern = "param = '([^']*)'", options = setOf(RegexOption.IGNORE_CASE))
            val olidRegex = Regex(pattern = "OLID = '([^']*)'", options = setOf(RegexOption.IGNORE_CASE))

            val frameBase = frameBaseRegex.find(iframeAl)?.groupValues?.get(1)
            val param = paramRegex.find(iframeAl)?.groupValues?.get(1)
            val olid = olidRegex.find(iframeAl)?.groupValues?.get(1)

            if (frameBase != null && param != null && olid != null) {
                val olidReverse = olid.reversed()
                val urlOlustur = "$frameBase?$param=$olidReverse"


                try {
                    val sonUrlAl = app.get(urlOlustur, mainHeaders).document
                    val scriptAl = sonUrlAl.selectFirst("script:containsData(eval)")?.data()

                    if (scriptAl != null) {
                        val jsUnpacker = getAndUnpack(scriptAl)

                        val hlsLinkRegex = Regex(pattern = "\"hls[0-9]+\":\"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))
                        val hlsler = hlsLinkRegex.findAll(jsUnpacker)

                        hlsler.forEach { hls ->
                            val m3u8 = hls.groupValues[1]
                            val videoLink = if (!m3u8.contains("http")) {
                                "https://javclan.com$m3u8"
                            } else {
                                m3u8
                            }

                            callback.invoke(
                                newExtractorLink(
                                    this.name,
                                    this.name,
                                    videoLink,
                                    ExtractorLinkType.M3U8
                                ) {
                                    this.referer = "${mainUrl}/"
                                }
                            )
                        }
                    }
                } catch (e: Exception) {
                }
            }
            val directBaseRegex = Regex(pattern = "var directBase = '([^']*)'", options = setOf(RegexOption.IGNORE_CASE))
            val directBase = directBaseRegex.find(iframeAl)?.groupValues?.get(1)

            if (directBase != null) {

                val suffixRegex = Regex(pattern = "var suffix = '([^']*)'", options = setOf(RegexOption.IGNORE_CASE))
                val suffix = suffixRegex.find(iframeAl)?.groupValues?.get(1) ?: ""

                if (olid != null) {
                    val olidReversed = olid.reversed()

                    val decodedId = try {
                        olidReversed.chunked(2)
                            .map { it.toInt(16).toChar() }
                            .joinToString("")
                    } catch (e: Exception) {
                        null
                    }

                    if (decodedId != null) {
                        val vide0Url = "$directBase$decodedId$suffix"

                        try {
                            loadExtractor(vide0Url, data, subtitleCallback, callback)
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }

        return true
    }
}