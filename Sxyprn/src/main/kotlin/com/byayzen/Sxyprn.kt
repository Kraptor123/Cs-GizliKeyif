// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.google.ai.client.generativeai.type.content
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject
import org.jsoup.nodes.Element
import java.net.URI
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import com.google.gson.JsonElement
import com.lagradost.cloudstream3.amap
import kotlinx.serialization.json.jsonObject


class Sxyprn : MainAPI() {
    override var mainUrl = "https://sxyprn.com/"
    override var name = "Sxyprn"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        mainUrl to "Main Menu",
        "${mainUrl}/Hardcore.html?sm=latest" to "Latest",
        "${mainUrl}/Hardcore.html?sm=trending" to "Trending",
        "${mainUrl}/Hardcore.html?sm=views" to "Views",
        "${mainUrl}/Hardcore.html?sm=orgasmic" to "Orgazmic"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageValue = (page - 1) * 30

        val url = if (page == 1) {
            request.data
        } else {
            val separator = if (request.data.contains("?")) "&" else "?"
            "${request.data}${separator}page=$pageValue"
        }

        val document = app.get(url).document
        val home = document.select("div.post_el_small").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val pageValue = (page - 1) * 30
        val formattedQuery = query.trim().replace(" ", "-")

        val url = if (page == 1) {
            "${mainUrl}/$formattedQuery.html"
        } else {
            "${mainUrl}/$formattedQuery.html?page=$pageValue"
        }

        val document = app.get(url).document
        val aramaCevap = document.select("div.post_el_small").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(aramaCevap, hasNext = aramaCevap.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val rawTitle = this.selectFirst("div.post_text")?.text() ?: return null
        val title = rawTitle
            .replace(Regex("(?i)NEW|1080p|720p|Full HD.*"), "")
            .substringBefore("#")
            .trim()

        val href = this.selectFirst("a[href*='/post/']")?.attr("href") ?: return null

        val posterUrl = this.selectFirst("div.post_vid_thumb img")?.let { img ->
            val url = img.attr("data-src").ifBlank { img.attr("src") }
            if (url.startsWith("//")) "https:$url" else url
        }

        return newMovieSearchResponse(title, fixUrl(href), TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)


    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title =
            document.selectFirst("title")?.text()?.trim()?.substringBefore(" on the SexyPorn")
                ?: document.selectFirst("h1")?.text()?.trim()
                ?: return null

        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description =
            document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        val tags = document.select("a.hash_link").map { it.text().trim().substring(1) }

        val recommendations = document.select("div.post_el_small").mapNotNull { element ->
            val recTitle =
                element.selectFirst("div.post_text")?.text()?.trim()?.substringBefore("FULL HD")
                    ?.trim()
                    ?: element.selectFirst("h1")?.text()?.trim()
                    ?: return@mapNotNull null
            val recHref = fixUrlNull(element.selectFirst("a.js-pop")?.attr("href"))
                ?: return@mapNotNull null
            val recPoster =
                fixUrlNull(element.selectFirst("img.mini_post_vid_thumb")?.attr("data-src"))

            newMovieSearchResponse(recTitle, recHref, TvType.NSFW) {
                this.posterUrl = recPoster
            }
        }.distinctBy { it.url }
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val url = fixUrl(data)
        var videoFound = false

        val targetRegex = """https?://.*(?:/cdn8/|\.vid).*""".toRegex()

        val resolver = WebViewResolver(
            interceptUrl = targetRegex,
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            useOkhttp = true
        )

        val capturedResponse = app.get(url, interceptor = resolver)
        val capturedUrl = capturedResponse.url

        val finalUrl = if (capturedUrl.contains("trafficdeposit.com")) {
            capturedUrl
        } else {
            val redirectResp = app.get(
                capturedUrl,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer" to url
                ),
                allowRedirects = false
            )
            redirectResp.headers["Location"] ?: redirectResp.headers["location"] ?: capturedUrl
        }

        if (finalUrl.contains(".vid") || finalUrl.contains("/cdn8/")) {
            callback.invoke(
                newExtractorLink(
                    name = "SxyPrn",
                    source = name,
                    url = finalUrl,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.referer = url
                    this.quality = Qualities.P1080.value
                }
            )
            videoFound = true
        }

        val doc = app.get(url).document
        val mainContainer = doc.selectFirst("div.post_text")

        if (mainContainer != null) {
            val externalLinks = mainContainer.select("a.extlink_icon.extlink")
                .mapNotNull { it.attr("href") }
                .distinctBy {
                    URI(it).host.replace("www.", "")
                }

            externalLinks.forEach { link ->
                loadExtractor(link, url, subtitleCallback, callback)
                videoFound = true
            }
        }

        return videoFound
    }
}