// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import okhttp3.MultipartBody
import okhttp3.Request
import org.jsoup.nodes.Element

class Javtiful : MainAPI() {
    override var mainUrl = "https://javtiful.com"
    override var name = "Javtiful"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/videos" to "Newest",
        "${mainUrl}/videos?sort=most_viewed" to "Most Viewed",
        "${mainUrl}/videos?sort=top_rated" to "Top Rated",
        "${mainUrl}/videos?sort=top_favorites" to "Top Favorites",
        "${mainUrl}/videos?sort=being_watched" to "Being Watched",
        "${mainUrl}/censored" to "Censored",
        "${mainUrl}/uncensored" to "Uncensored",
        "${mainUrl}/category/female-investigator" to "Female Investigator",
        "${mainUrl}/category/chinese-av" to "Chinese AV",
        "${mainUrl}/category/female-boss" to "Female Boss",
        "${mainUrl}/category/mature-woman" to "Mature Woman",
        "${mainUrl}/category/cosplay" to "Cosplay",
        "${mainUrl}/category/amateur" to "Amateur",
        "${mainUrl}/category/housekeeper" to "Housekeeper",
        "${mainUrl}/category/nurse" to "Nurse",
        "${mainUrl}/category/female-student" to "Female Student",
        "${mainUrl}/category/school-girls" to "School Girls",
        "${mainUrl}/category/office-lady" to "Office Lady",
        "${mainUrl}/category/sister-in-law" to "Sister-in-law",
        "${mainUrl}/category/hypnosis" to "Hypnosis",
        "${mainUrl}/category/beautiful-girl" to "Beautiful Girl",
        "${mainUrl}/category/bbw" to "BBW",
        "${mainUrl}/category/drama" to "Drama",
        "${mainUrl}/category/married-woman" to "Married Woman",
        "${mainUrl}/category/milf" to "Milf",
        "${mainUrl}/category/female-teacher" to "Female Teacher",
        "${mainUrl}/category/affair" to "Affair",
        "${mainUrl}/category/big-tits" to "Big Tits"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}?page=$page"
        val res = app.get(url).document
        val home = res.select("article.front-video-card:not(.front-ad-card)").mapNotNull {
            it.mainPageResults()
        }
        return newHomePageResponse(request.name, home, true)
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) "$mainUrl/search?q=$query" else "$mainUrl/search?page=$page&q=$query"
        val res = app.get(url).document
        val results = res.select("article.front-video-card:not(.front-ad-card)").mapNotNull {
            it.mainPageResults()
        }
        return newSearchResponseList(results, results.isNotEmpty())
    }

    private fun Element.mainPageResults(): SearchResponse? {
        val link = this.selectFirst("a.front-video-title") ?: return null
        val title = link.text().trim()
        val href = fixUrl(link.attr("href"))
        val img = this.selectFirst("img") ?: return null
        val poster = fixUrl(img.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val res = app.get(url).document
        val title = res.selectFirst("div.front-watch-title h1")?.text()?.trim() ?: return null
        val poster = res.selectFirst("meta[property=\"og:image\"]")?.attr("content")

        val recommendations = res.select("div.front-video-grid-related article.front-video-card:not(.front-ad-card)").mapNotNull {
            val link = it.selectFirst("a.front-video-title") ?: return@mapNotNull null
            val rectitle = link.text().trim()
            val rechref = fixUrl(link.attr("href"))
            val recposter = it.selectFirst("img")?.attr("src")

            newMovieSearchResponse(rectitle, rechref, TvType.NSFW) {
                this.posterUrl = fixUrlNull(recposter)
            }
        }

        val actorslist = res.select("a.front-watch-actor-card").map {
            val name = it.selectFirst("span")?.text()?.trim() ?: ""
            val image = it.selectFirst("img")?.attr("src")?.takeIf { img ->
                img.isNotEmpty() && !img.contains("profile-placeholder.png")
            }
            Actor(name, fixUrlNull(image))
        }

        val datetext = res.selectFirst("div.front-watch-detail:contains(Added on) time")?.attr("datetime")
        val year = datetext?.split("-")?.firstOrNull()?.toIntOrNull()
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = fixUrlNull(poster)
            this.plot = res.selectFirst("meta[property=\"og:description\"]")?.attr("content")?.trim()
            this.year = year
            this.tags = res.select("div.front-watch-detail:contains(Categories) a, div.front-watch-detail:contains(Tags) a").map { it.text().trim() }
            this.recommendations = recommendations
            addActors(actorslist)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = app.get(data).text
        val configraw = res.substringAfter("id=\"frontWatchConfig\" type=\"application/json\">")
            .substringBefore("</script>")

        val configdata = try {
            app.baseClient.newCall(Request.Builder().url("https://invalid.com").build()).let {
                parseJson<WatchConfig>(configraw)
            }
        } catch (e: Exception) {
            null
        }

        configdata?.playerSources?.forEach { source ->
            callback.invoke(
                newExtractorLink(
                    this.name,
                    this.name,
                    source.src
                ) {
                    this.quality = source.size ?: Qualities.Unknown.value
                    this.referer = "$mainUrl/"
                    this.type = if (source.src.contains(".mp4")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                }
            )
        }

        return !configdata?.playerSources.isNullOrEmpty()
    }

    data class WatchConfig(
        val playerSources: List<PlayerSource>? = null
    )

    data class PlayerSource(
        val src: String,
        val type: String? = null,
        val size: Int? = null
    )
}