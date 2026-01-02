// ! Bu araç @kerimmkirac tarafından | @kerimmkirac için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class HdAbla : MainAPI() {
    override var mainUrl = "https://hdabla.net"
    override var name = "HdAbla"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl" to "Tüm Videolar",
        "$mainUrl/porno/anne" to "Üvey Anne",
        "$mainUrl/porno/kardes/" to "Üvey Kardeş",
        "$mainUrl/porno/hizmetci/" to "Hizmetçi",
        "$mainUrl/porno/esmer" to "Esmer",
        "$mainUrl/porno/buyuk-memeli" to "Büyük Meme",
        "$mainUrl/porno/buyuk-gotlu/" to "Büyük Göt",
        "$mainUrl/porno/konulu/" to "Konulu",
        "$mainUrl/porno/olgun-milf/" to "Milf"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get("${request.data}/page/$page").document
        val items = doc.select("div.item-video").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = selectFirst("a.clip-link") ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val title = anchor.attr("title")?.trim() ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, "$href|$poster", TvType.NSFW) {
            posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()

        for (page in 1..3) {
            val document = app.get("${mainUrl}/page/$page/?s=${query}").document
            val pageResults = document.select("div.item-video").mapNotNull { it.toSearchResult() }

            if (pageResults.isEmpty()) break

            results.addAll(pageResults)
        }

        return results
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = selectFirst("a.clip-link") ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val title = anchor.attr("title")?.trim() ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, "$href|$poster", TvType.NSFW) {
            posterUrl = poster
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(data: String): LoadResponse? {
        val (url, poster) = data.split("|").let {
            it[0] to it.getOrNull(1)
        }

        val doc = app.get(url).document

        val title = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val description = doc.selectFirst("div.entry-content")?.text()?.trim()
        val tags = doc.select("div#extras a").map { it.text().trim() }

        val recommendations = doc.select("div.related-posts div.item-video")
            .mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val aTag = selectFirst("a.clip-link") ?: return null
        val href = fixUrlNull(aTag.attr("href")) ?: return null
        val title = aTag.attr("title")?.trim() ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, "$href|$poster", TvType.NSFW) {
            posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("HdAbla", "data » $data")
        val document = app.get(data).document

        val iframeElement = document.selectFirst("div.screen.fluid-width-video-wrapper iframe")
        if (iframeElement != null) {
            val iframeSrc = iframeElement.attr("src")
            if (iframeSrc.isNotEmpty()) {
                val fullIframeUrl = fixUrl(iframeSrc)
                Log.d("HdAbla", "iframe url » $fullIframeUrl")

                val iframeDoc = app.get(fullIframeUrl, referer = mainUrl).document
                val scriptTags = iframeDoc.select("script")


                val videoPatterns = listOf(
                    """file\s*:\s*['"]([^'"]+)['"]""".toRegex(),
                    """["']([^"']*\.m3u8[^"']*)["']""".toRegex(),
                    """["']([^"']*\.mp4[^"']*)["']""".toRegex(),
                    """src:\s*["']([^"']+)["']""".toRegex(),
                    """source:\s*["']([^"']+)["']""".toRegex()
                )

                val foundUrls = mutableSetOf<String>()

                scriptTags.forEach { script ->
                    val scriptContent = script.html()

                    videoPatterns.forEach { pattern ->
                        pattern.findAll(scriptContent).forEach { match ->
                            val videoUrl = match.groupValues[1]


                            if (videoUrl.startsWith("http") &&
                                (videoUrl.contains(".m3u8") || videoUrl.contains(".mp4")) &&
                                !foundUrls.contains(videoUrl)) {

                                foundUrls.add(videoUrl)
                                Log.d("HdAbla", "video url » $videoUrl")


                                val headers = if (videoUrl.contains(".mp4")) {
                                    mapOf(
                                        "Host" to "sv4.memriosa.cloud",
                                        "Connection" to "keep-alive",
                                        "sec-ch-ua-platform" to "\"Windows\"",
                                        "Accept-Encoding" to "identity;q=1, *;q=0",
                                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
                                        "sec-ch-ua" to "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Brave\";v=\"138\"",
                                        "sec-ch-ua-mobile" to "?0",
                                        "Accept" to "*/*",
                                        "Sec-GPC" to "1",
                                        "Accept-Language" to "tr-TR,tr;q=0.8",
                                        "Sec-Fetch-Site" to "cross-site",
                                        "Sec-Fetch-Mode" to "no-cors",
                                        "Sec-Fetch-Dest" to "video",
                                        "Sec-Fetch-Storage-Access" to "none",
                                        "Referer" to "https://wai.moonfast.site/"
                                    )
                                } else {
                                    mapOf("Referer" to mainUrl)
                                }

                                callback.invoke(

                                        newExtractorLink(
                                            name = name,
                                            source = name,
                                            url = videoUrl,


                                            type = if (videoUrl.contains(".mp4")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8

                                        ){
                                            this.headers = headers
                                            this.referer = if (videoUrl.contains(".mp4")) "https://wai.moonfast.site/" else mainUrl
                                            this.quality = Qualities.P720.value
                                        }
                                    )

                            }
                        }
                    }
                }
            }
        }

        return true
    }

    private fun getQualityFromUrl(url: String): Int {
        return when {
            url.contains("1080") -> Qualities.P1080.value
            url.contains("720") -> Qualities.P720.value
            url.contains("480") -> Qualities.P480.value
            url.contains("360") -> Qualities.P360.value
            else -> Qualities.Unknown.value
        }
    }
}