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

        val description =
            document.select("div.wp-content p:not(:has(img))").joinToString(" ") { it.text() }
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

        val buttonNames = mutableListOf<String>()
        val doc = Jsoup.parse(document)
        for (btn in doc.select("a.wp-btn-iframe__shortcode[data-localize]")) {
            buttonNames.add(btn.text().trim())
        }

        val iframeRegex = Regex("\"iframe_url\":\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        val iframeMatches = iframeRegex.findAll(document).toList()

        val processedUrls = mutableSetOf<String>()

        for ((index, match) in iframeMatches.withIndex()) {
            try {
                val sourceName =
                    if (index < buttonNames.size) buttonNames[index] else "Source ${index + 1}"

                val encodedUrl = match.groupValues[1]
                val decodedUrl = base64Decode(encodedUrl)

                val iframeRes = app.get(decodedUrl, mainHeaders)
                val iframeHtml = iframeRes.text

                val cfgBase =
                    Regex("base:\\s*['\"]([^'\"]+)['\"]").find(iframeHtml)?.groupValues?.get(1)
                val cfgRtype =
                    Regex("rtype:\\s*['\"]([^'\"]+)['\"]").find(iframeHtml)?.groupValues?.get(1)
                val cfgCid =
                    Regex("cid:\\s*['\"]([^'\"]+)['\"]").find(iframeHtml)?.groupValues?.get(1)
                val cfgKeysRaw =
                    Regex("keys:\\s*\\[([^\\]]+)\\]").find(iframeHtml)?.groupValues?.get(1)

                if (cfgBase == null || cfgRtype == null || cfgCid == null || cfgKeysRaw == null) continue

                val keysList = Regex("['\"]([^'\"]+)['\"]").findAll(cfgKeysRaw)
                    .map { it.groupValues[1] }.toList()

                val iframeDoc = Jsoup.parse(iframeHtml)
                val targetDiv = iframeDoc.getElementById(cfgCid) ?: continue

                val tokenParts = keysList.mapNotNull { key ->
                    targetDiv.attr(key).takeIf { it.isNotEmpty() }
                }

                if (tokenParts.isEmpty()) continue

                val combined = tokenParts.joinToString("")
                val reversed = combined.reversed()
                val cleanBase = cfgBase.trimEnd('/')
                val finalUrl = "$cleanBase/?${cfgRtype}r=$reversed"

                val redirectRes = app.get(finalUrl, mainHeaders, allowRedirects = false)
                val location = redirectRes.headers["location"]
                    ?: redirectRes.headers["Location"]
                    ?: redirectRes.headers["LOCATION"]

                if (location != null) {
                    Log.d("kraptor_$name", "[$sourceName] Embed URL: $location")

                    if (location.contains("/searcho/")) {
                        loadExtractor(location, data, subtitleCallback, callback)
                        continue
                    }

                    val playerRes = app.get(location, mainHeaders)
                    val playerHtml = playerRes.text

                    val hlsRegex = Regex("[\"'](https?://[^\"']+\\.m3u8[^\"']*)[\"']")
                    val hlsFound = hlsRegex.find(playerHtml)?.groupValues?.get(1)

                    if (hlsFound != null && !processedUrls.contains(hlsFound)) {
                        processedUrls.add(hlsFound)
                        callback.invoke(
                            newExtractorLink(
                                source = "$name $sourceName",
                                name = sourceName,
                                url = hlsFound,
                                type = ExtractorLinkType.M3U8
                            ) {
                                this.referer = "$cleanBase/"
                            }
                        )
                    } else {
                        loadExtractor(location, data, subtitleCallback, callback)
                    }
                } else {
                    loadExtractor(finalUrl, data, subtitleCallback, callback)
                }

            } catch (e: Exception) {
                Log.d("kraptor_$name", "[$index] Hata: ${e.message}")
                continue
            }
        }

        return true
    }
}