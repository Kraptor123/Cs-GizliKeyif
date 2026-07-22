// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.fasterxml.jackson.annotation.JsonProperty

class HentaiWorld : MainAPI() {
    override var mainUrl = "https://hentaiworld.tv"
    override var name = "HentaiWorld"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/videos?sort=uploaded" to "Uploaded",
        "${mainUrl}/videos?sort=newest"   to "Newest",
        "${mainUrl}/videos?sort=trending" to "Trending",
        "${mainUrl}/videos?sort=views"    to "Views",
        "${mainUrl}/videos?sort=random"   to "Random"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page > 1) "${request.data}&page=$page" else request.data
        val home = app.get(url).document
            .select("div.grid > div.group > a[href^=\"/videos/\"]")
            .mapNotNull {
                val href = fixUrlNull(it.attr("href")) ?: return@mapNotNull null
                val title = it.selectFirst("h3")?.text() ?: return@mapNotNull null
                val poster = it.selectFirst("img")?.attr("src")

                newMovieSearchResponse(title, href, TvType.NSFW) {
                    this.posterUrl = fixUrlNull(poster)
                }
            }

        return newHomePageResponse(request.name, home)
    }



    override suspend fun search(query: String, page: Int): SearchResponseList {
        val json = app.get("${mainUrl}/api/search?q=$query").parsedSafe<SearchResponseWrapper>()
        val aramaCevap = json?.results?.map {
            newMovieSearchResponse(it.title, "$mainUrl/videos/${it.slug}", TvType.NSFW) {
                this.posterUrl = it.coverUrl
            }
        } ?: emptyList()
        return newSearchResponseList(aramaCevap, hasNext = false)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? {
        val json = app.get("${mainUrl}/api/search?q=$query").parsedSafe<SearchResponseWrapper>()
        return json?.results?.map {
            newMovieSearchResponse(it.title, "$mainUrl/videos/${it.slug}", TvType.NSFW) {
                this.posterUrl = it.coverUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = document.selectFirst("img[alt*=\"cover\"]")?.attr("src")
        val plot = document.selectFirst("div.relative .space-y-3 p")?.text()?.trim()
        val tags = document.select("a[href^=\"/search?tag=\"]").map { it.text() }

        val recommendations = document.select("aside a[href^=\"/videos/\"]").mapNotNull {
            val href = fixUrlNull(it.attr("href")) ?: return@mapNotNull null
            val name = it.selectFirst("h3")?.text()?.trim() ?: return@mapNotNull null
            val image = it.selectFirst("img")?.attr("src")

            newMovieSearchResponse(name, fixUrl(href), TvType.NSFW) {
                this.posterUrl = image
            }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = plot
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
        val html = app.get(data).text


        val videoUrl = Regex("""videoUrl\s*:\s*['"](https?://[^'"]+)['"]""").find(html)?.groupValues?.get(1)
        if (!videoUrl.isNullOrEmpty()) {
             callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = videoUrl,
                    type = null
                )
            )
        }

        return true
    }
}


data class SearchResult(
    @JsonProperty("slug") val slug: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("coverUrl") val coverUrl: String?
)

data class SearchResponseWrapper(
    @JsonProperty("results") val results: List<SearchResult>
)