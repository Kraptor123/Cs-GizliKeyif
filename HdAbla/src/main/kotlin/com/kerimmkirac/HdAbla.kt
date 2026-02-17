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
        val iframeUrl = app.get(data).document.selectFirst("div.screen iframe")?.attr("src") ?: return false
        val scriptBody = app.get(fixUrl(iframeUrl), referer = data).text

        """(?:file|source)\s*:\s*["']([^"']+)["']|["'](http[^"']*\.(?:m3u8|mp4)[^"']*)["']""".toRegex()
            .findAll(scriptBody).forEach { match ->
                val videoUrl = (match.groupValues[1].takeIf { it.isNotBlank() } ?: match.groupValues[2]).replace("\\/", "/")
                val isMp4 = videoUrl.contains(".mp4")
                val host = videoUrl.substringAfter("//").substringBefore("/")

                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = name,
                        url = videoUrl,
                        type = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        this.quality = getQualityFromUrl(videoUrl)
                        this.referer = if (isMp4) "https://wai.moonfast.site/" else iframeUrl
                        this.headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:147.0) Gecko/20100101 Firefox/147.0",
                            "Referer" to (this.referer ?: ""),
                            "Host" to host,
                            "Accept" to "*/*",
                            "Connection" to "keep-alive"
                        )
                    }
                )
            }
        return true
    }

    private fun getQualityFromUrl(url: String): Int {
        return when {
            url.contains("1080") -> Qualities.P1080.value
            url.contains("720") -> Qualities.P720.value
            url.contains("480") -> Qualities.P480.value
            else -> Qualities.P720.value
        }
    }
    }