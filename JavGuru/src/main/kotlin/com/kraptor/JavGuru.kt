// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import kotlin.text.Regex
import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import org.jsoup.Jsoup


class JavGuru : MainAPI() {
    override var mainUrl = "https://jav.guru"
    override var name = "JavGuru"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    private val mainHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "Accept-Language" to "en-US,en;q=0.9",
        "Referer" to "$mainUrl/",
        "Cache-Control" to "max-age=0",
        "Sec-Ch-Ua" to "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
        "Sec-Ch-Ua-Mobile" to "?0",
        "Sec-Ch-Ua-Platform" to "\"Windows\"",
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
        val url = if (page == 1) "${request.data}/" else "${request.data}/page/$page/"
        val document = app.get(url, headers = mainHeaders).document
        val items = document.select("div.inside-article, article, div.tabcontent li, .item-list li")

        val home = items.mapNotNull { it.toSearchResponse() }
        val hasNext = document.select("a.next, a.last, nav.pagination").isNotEmpty()

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
        val linkElement = this.selectFirst("div.imgg a, h2 a, a")
        val href = fixUrlNull(linkElement?.attr("href")) ?: return null

        val imgElement = this.selectFirst("img")
        val title = imgElement?.attr("alt")?.trim()?.ifBlank { null }
            ?: linkElement?.attr("title")?.trim()?.ifBlank { null }
            ?: linkElement?.text()?.trim()?.ifBlank { null }
            ?: this.selectFirst("h2")?.text()?.trim()
            ?: return null

        val posterUrl = fixUrlNull(imgElement?.attr("src") ?: imgElement?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mainHeaders
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page == 1) "$mainUrl/?s=$query" else "$mainUrl/page/$page/?s=$query"
        val document = app.get(url, headers = mainHeaders).document
        val items = document.select("div.inside-article, article")

        val results = items.mapNotNull { it.toSearchResponse() }
        val hasNext = document.select("a.next, a.last").isNotEmpty()

        return newSearchResponseList(results, hasNext = hasNext)
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

        val yearText = document.selectFirst("div.infometa li:contains(Release Date)")?.ownText()
            ?.substringBefore("-")?.toIntOrNull()


        val tags = document.select("li.w1 a[rel=tag]").mapNotNull { it.text().trim() }

        val recommendations = document.select("li").mapNotNull { it.toRecommendationResult() }

        val actors =
            document.select("li.w1 strong:not(:contains(tags)) ~ a").mapNotNull { Actor(it.text()) }

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
        if (title.isNullOrBlank()) return null

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
        val res = app.get(data, headers = mainHeaders)
        val document = res.text

        val regex = Regex(pattern = "\"iframe_url\":\"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))
        val iframeEslesmeler = regex.findAll(document)

        iframeEslesmeler.forEach { eslesme ->
            val iframesifreli = eslesme.groupValues[1]
            val iframeCoz = base64Decode(iframesifreli)

            val iframeRes = app.get(iframeCoz, mainHeaders)
            val iframeAl = iframeRes.text

            val frameBaseRegex = Regex("var frameBase = '([^']*)'")
            val rTypeRegex = Regex("var rType = '([^']*)'")
            val tokenRegex = Regex("data-token=\"([^\"]*)\"")

            val frameBase = frameBaseRegex.find(iframeAl)?.groupValues?.get(1)
            val rType = rTypeRegex.find(iframeAl)?.groupValues?.get(1)
            val token = tokenRegex.find(iframeAl)?.groupValues?.get(1)

            if (frameBase != null && rType != null && token != null) {
                val terstoken = token.reversed()
                val urlOlustur = "$frameBase?$rType" + "r=$terstoken"
                try {
                    val sonUrlRes = app.get(urlOlustur, mainHeaders)
                    val sonUrlAl = sonUrlRes.text
                    val scriptAl = Jsoup.parse(sonUrlAl).selectFirst("script:containsData(eval)")?.data()
                    if (scriptAl != null) {
                        val jsUnpacker = getAndUnpack(scriptAl)
                        val hlsRegex = Regex("[\"'](https?://[^\"']+\\.m3u8[^\"']*)[\"']")

                        hlsRegex.findAll(jsUnpacker).forEach { hls ->
                            val videoLink = hls.groupValues[1]
                            callback.invoke(
                                newExtractorLink(
                                    source = "JavGuru",
                                    name = "JavGuru",
                                    url = videoLink,
                                    type = ExtractorLinkType.M3U8,
                                ) {
                                    this.referer = "$frameBase/"
                                }
                            )
                        }
                    } else {
                        val sonIframeRegex = Regex("iframe src=\"([^\"]*)\"")
                        val sonLink = sonIframeRegex.find(sonUrlAl)?.groupValues?.get(1)

                        if (sonLink != null) {
                            loadExtractor(sonLink, data, subtitleCallback, callback)
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }

        return true
    }
    }