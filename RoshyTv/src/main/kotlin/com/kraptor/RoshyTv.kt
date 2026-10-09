// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup

class RoshyTv : MainAPI() {
    override var mainUrl = "https://roshy.tv"
    override var name = "RoshyTv"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/category/english-sub-6/?sort_by=new" to "Subtitles - New",
        "$mainUrl/category/english-sub-6/?sort_by=most_viewed" to "Subtitles - Most Viewed",
        "$mainUrl/category/decensored-5/?sort_by=new" to "Decensored - New",
        "$mainUrl/category/decensored-5/?sort_by=most_viewed" to "Decensored - Most Viewed"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get(request.data).document
        } else {
            app.get("${request.data}page/$page/").document
        }
        val home = document.select("article[id^=post]").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = false
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val titleLink = this.selectFirst("h3.entry-title a, a.blog-img-link, a[title]") ?: this.selectFirst("a") ?: return null
        val title = titleLink.attr("title").ifEmpty { titleLink.text() }.trim()
        if (title.isEmpty()) return null

        val href = fixUrlNull(titleLink.attr("href")) ?: return null

        val img = this.selectFirst("img")
        val posterUrl = fixUrlNull(
            img?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("src")?.takeIf { it.isNotBlank() && !it.startsWith("data:") }
                ?: img?.attr("data-srcset")?.substringBefore(" ")?.takeIf { it.isNotBlank() }
                ?: img?.attr("srcset")?.substringBefore(" ")?.takeIf { it.isNotBlank() }
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1) {
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=$query").document
        }

        val aramaCevap = document.select("article[id^=post]").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document        = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content")?.ifEmpty { null })
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()?.ifEmpty { null }
        val tags            = document.select("div.main-block-wrapper a.category-item").mapNotNull { it.attr("title").ifEmpty { null } }
        val recommendations = document.select("div.site__row article[id*=post]").mapNotNull { it.toMainPageResult() }

        val actors = document.select("div.cast-variant-items-wrapper a.blog-img-link").mapNotNull { el ->
            val actorName  = el.attr("title").trim().ifEmpty { return@mapNotNull null }
            val pictureEl  = el.selectFirst("picture")
            val actorImage = fixUrlNull(
                pictureEl?.selectFirst("source")?.attr("data-srcset")?.ifEmpty { null }
                    ?: el.selectFirst("img")?.attr("src")?.ifEmpty { null }
            )
            Actor(actorName, actorImage)
        }

        val mirrorLinks  = document.select("div.btn-p-groups-items a[href]").mapNotNull { fixUrlNull(it.attr("href").ifEmpty { null }) }
        val allPageLinks = (listOf(url) + mirrorLinks).distinct()

        return newMovieLoadResponse(title, url, TvType.NSFW, allPageLinks) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val pageUrls  = try {
            mapper.readValue<List<String>>(data)
        } catch (e: Exception) {
            listOf(data)
        }

        var linkFound = false

        coroutineScope {
            val deferredIframes = pageUrls.map { pageUrl ->
                async {
                    try {
                        val doc     = app.get(pageUrl, referer = "$mainUrl/").document
                        val html    = doc.html().replace("\\/", "/").replace("\\\"", "\"")

                        Regex("""(?:embedUrl["']\s*:\s*["']|src=["'])(https?://[^"'>\s]+)""")
                            .findAll(html)
                            .mapNotNull { match ->
                                val rawUrl = match.groupValues.getOrNull(1)?.replace("&amp;", "&")
                                fixUrlNull(rawUrl)
                            }
                            .filter { it.contains("/embed/") || it.contains("/e/") }
                            .toList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }

            val uniqueIframes = deferredIframes.awaitAll().flatten().distinct()

            uniqueIframes.forEach { iframe ->
                loadExtractor(iframe, "$mainUrl/", subtitleCallback, callback)
                linkFound = true
            }
        }

        return linkFound
    }
}