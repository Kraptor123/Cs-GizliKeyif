// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.annotation.SuppressLint
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.Actor
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.content.Context
import com.lagradost.api.Log


class EU : MainAPI() {
    override var mainUrl = "https://18eu.net"
    override var name = "18EU"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/movies/" to "Movies",
        "${mainUrl}/tv-series/" to "TV Series",
    )


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        Log.d(this.name, "getMainPage: page=$page, name=${request.name}")

        val url = if (page <= 1) {
            fixUrl(request.data)
        } else {
            fixUrl("${request.data}?page=$page")
        }

        val document = app.get(url).document
        val home = document.select("a.card").mapNotNull {
            it.toMainPageResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        Log.d("EU", "toMainPageResult")

        val title = selectFirst("div.card-title")?.text()?.trim()
            ?.ifEmpty { return null }
            ?: return null

        val href = fixUrlNull(attr("href"))
            ?.ifEmpty { return null }
            ?: return null

        val posterUrl = fixUrlNull(selectFirst("div.poster-box img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        Log.d(this.name, "search: query=$query, page=$page")

        val url = fixUrl("$mainUrl/_ajax/search?q=$query")
        val response = app.get(url).parsed<SearchApiResponse>()

        val results = response.results.map {
            val href = fixUrl("${mainUrl}/${it.slug}")
            val posterUrl = fixUrlNull("${mainUrl}${it.thumb}")

            newMovieSearchResponse(it.title, href, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }

        return newSearchResponseList(results, results.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? {
        Log.d(this.name, "quickSearch: query=$query")

        return search(query, 1).items
    }

    override suspend fun load(url: String): LoadResponse? {
        Log.d(this.name, "load: url=$url")

        val document = app.get(fixUrl(url)).document

        val title = document.selectFirst("h1.info-title")?.text()?.trim()
            ?.ifEmpty { return null }
            ?: return null

        val posterUrl = fixUrlNull(document.selectFirst("div.poster-lg img")?.attr("src"))
        val description = document.selectFirst("div.ob-pane p")?.text()?.trim()
        val year = document.selectFirst("div.badges a.badge[href*='/year/']")
            ?.text()
            ?.trim()
            ?.toIntOrNull()

        val actors = document.select("div.info-people a[href*='/actor/']").map {
            Actor(it.text().trim(), fixUrlNull(it.attr("href")))
        }

        val recommendations = document.select("section#related a.card").mapNotNull {
            val title = it.selectFirst("div.card-title")?.text()?.trim()
                ?.ifEmpty { return@mapNotNull null }
                ?: return@mapNotNull null

            val href = fixUrlNull(it.attr("href"))
                ?.ifEmpty { return@mapNotNull null }
                ?: return@mapNotNull null

            val posterUrl = fixUrlNull(it.selectFirst("img")?.attr("src"))

            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }

        val watchUrl = fixUrlNull(document.selectFirst("a.btn-watch")?.attr("href"))
            ?.ifEmpty { return null }
            ?: return null

        return newMovieLoadResponse(title, url, TvType.Movie, watchUrl) {
            this.posterUrl = posterUrl
            this.year = year
            this.plot = description
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
        Log.d(this.name, "loadLinks: data=$data")

        val document = app.get(fixUrl(data)).document

        val episodeId = document.selectFirst("div.wserver button.epbtn[data-ep]")
            ?.attr("data-ep")
            ?.ifEmpty { return false }
            ?: return false

        val url = fixUrl("$mainUrl/api/v1/episodes/$episodeId/sources")

        val response = app.get(
            url,
            headers = mapOf(
                "Referer" to fixUrl(data),
                "Accept" to "*/*"
            )
        ).parsed<EpisodeSourcesResponse>()

        val sourceUrl = response.sources
            .firstOrNull { it.type.equals("hls", true) }
            ?.file
            ?.ifEmpty { return false }
            ?: return false

        val streamUrl = fixUrl("$sourceUrl/index.json")

        callback(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = streamUrl
            ) {
                referer = fixUrl("$mainUrl/")
                type = ExtractorLinkType.M3U8
            }
        )

        return true
    }

    data class SearchApiResponse(
        val results: List<SearchResult>
    )

    data class SearchResult(
        val format: String,
        val slug: String,
        val thumb: String,
        val title: String
    )

    data class EpisodeSourcesResponse(
        val sources: List<EpisodeSource> = emptyList(),
        val success: Boolean = false
    )

    data class EpisodeSource(
        val file: String = "",
        val type: String = ""
    )
}
