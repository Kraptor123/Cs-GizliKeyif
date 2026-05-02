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



class EU : MainAPI() {
    override var mainUrl = "https://18eu.net"
    override var name = "18EU"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/movies/" to "All Movies",
        "${mainUrl}/tv-series/" to "TV Series",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data.trimEnd('/')}/page/$page/"
        val document = app.get(url).document
        val home = document.select("article.thumb").mapNotNull {
            it.toMainPageResult()
        }
        return newHomePageResponse(request.name, home, hasNext = true  )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search/$query"
        } else {
            "$mainUrl/search/$query/page/$page/"
        }

        val document = app.get(url).document
        val results = document.select("article.thumb").mapNotNull {
            it.toMainPageResult()
        }

        return newSearchResponseList(results, results.isNotEmpty())
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2.entry-title")?.text()?.trim()
            ?: this.selectFirst("a.halim-thumb")?.attr("title")?.trim()
            ?: return null
        val href = fixUrlNull(this.selectFirst("a.halim-thumb")?.attr("href")) ?: return null
        val posterurl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterurl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)


    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: ""
        val poster = fixUrlNull(document.selectFirst("img.movie-thumb")?.attr("src"))
        val description = document.selectFirst("article.item-content p")?.text()?.trim()
        val year = document.selectFirst("span.released a")?.text()?.trim()?.toIntOrNull()

        val actors = document.select("p.actors a").map {
            Actor(it.text().trim(), null)
        }

        val recommendations = document.select("div#halim-ajax-popular-post div.item").mapNotNull {
            val rectitle = it.selectFirst("h3.title")?.text() ?: return@mapNotNull null
            val rechref = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val recposter = fixUrlNull(
                it.selectFirst("img")?.attr("data-src") ?: it.selectFirst("img")?.attr("src")
            )
            newMovieSearchResponse(rectitle, rechref, TvType.NSFW) {
                this.posterUrl = recposter
            }
        }

        val episodes = ArrayList<Episode>()
        document.select("ul.halim-list-eps li.halim-episode-item").forEach {
            val epurl = it.selectFirst("a")?.attr("href") ?: it.attr("data-href") ?: return@forEach
            val epname =
                it.selectFirst("span")?.text()?.trim() ?: it.selectFirst("a")?.attr("title")?.trim()
                ?: ""
            episodes.add(newEpisode(epurl) {
                this.name = epname
                this.episode = epname.filter { it.isDigit() }.toIntOrNull()
            })
        }

        if (episodes.isEmpty()) {
            val script =
                document.select("script").find { it.data().contains("var jsonEpisodes") }?.data()
            if (script != null) {
                val regex = """\"postUrl\":\"(.*?)\".*?\"episodeName\":\"(.*?)\"""".toRegex()
                regex.findAll(script).forEach { match ->
                    val epurl = match.groupValues[1].replace("\\/", "/")
                    val epname = match.groupValues[2]
                    episodes.add(newEpisode(epurl) {
                        this.name = epname
                        this.episode = epname.filter { it.isDigit() }.toIntOrNull()
                    })
                }
            }
        }

        val watchurl = document.selectFirst("a.watch-movie")?.attr("href")

        return if (episodes.size > 1) {
            newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.recommendations = recommendations
                addActors(actors)
            }
        } else {
            val movieurl = episodes.firstOrNull()?.data ?: watchurl ?: url
            newMovieLoadResponse(title, url, TvType.NSFW, movieurl) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.recommendations = recommendations
                addActors(actors)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data)
        val mainpage = response.text
        val nonce = Regex("""data-nonce="([^"]+)"""").find(mainpage)?.groupValues?.get(1)
        val postid = Regex("""post_id":(\d+)""").find(mainpage)?.groupValues?.get(1)
        val serverid = Regex("""server":"(\d+)"""").find(mainpage)?.groupValues?.get(1)

        var episodeslug = data.trimEnd('/').substringAfterLast("/").replace(".html", "")
        if (episodeslug.contains("-sv")) {
            episodeslug = episodeslug.substringBefore("-sv")
        }

        if (nonce == null || postid == null) return false

        val playerresponse = app.get(
            url = "$mainUrl/wp-content/themes/halimmovies/player.php",
            params = mapOf(
                "episode_slug" to episodeslug,
                "server_id" to (serverid ?: "1"),
                "subsv_id" to "",
                "post_id" to postid,
                "nonce" to nonce,
                "custom_var" to ""
            ),
            headers = mapOf(
                "Referer" to data,
                "X-Requested-With" to "XMLHttpRequest",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36"
            )
        ).text

        val m3u8url =
            Regex("""(?i)"file"\s*:\s*"([^"]+)"""").find(playerresponse)?.groupValues?.get(1)
                ?.replace("\\/", "/")

        return if (!m3u8url.isNullOrBlank()) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = m3u8url
                ) {
                    this.referer = "$mainUrl/"
                    this.type = ExtractorLinkType.M3U8
                    this.quality = Qualities.P720.value
                }
            )
            true
        } else {
            false
        }
    }
}