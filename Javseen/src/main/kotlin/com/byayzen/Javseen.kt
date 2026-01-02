// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.util.Base64
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

class Javseen : MainAPI() {
    override var mainUrl = "https://javseen.tv"
    override var name = "Javseen"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/recent/" to "Recent Videos",
        "${mainUrl}/jav-censored/" to "Jav Censored",
        "${mainUrl}/solowork/" to "Solowork",
        "${mainUrl}/amateur/" to "Amateur",
        "${mainUrl}/asian/" to "Asian",
        "${mainUrl}/beautiful-girl/" to "Beautiful Girl",
        "${mainUrl}/bbw/" to "BBW",
        "${mainUrl}/married-woman/" to "Married Woman",
        "${mainUrl}/mature-woman/" to "Mature Woman",
        "${mainUrl}/mother/" to "Mother",
        "${mainUrl}/sister/" to "Sister",
        "${mainUrl}/young-wife/" to "Young Wife",
        "${mainUrl}/big-tits/" to "Big Tits",
        "${mainUrl}/breasts/" to "Breasts",
        "${mainUrl}/busty-fetish/" to "Busty Fetish",
        "${mainUrl}/huge-butt/" to "Huge Butt",
        "${mainUrl}/slender/" to "Slender",
        "${mainUrl}/glasses/" to "Glasses",
        "${mainUrl}/cosplay/" to "Cosplay",
        "${mainUrl}/school/" to "School",
        "${mainUrl}/teacher/" to "Teacher",
        "${mainUrl}/drama/" to "Drama",
        "${mainUrl}/documentary/" to "Documentary",
        "${mainUrl}/omnibus/" to "Omnibus",
        "${mainUrl}/planning/" to "Planning",
        "${mainUrl}/creampie/" to "Creampie",
        "${mainUrl}/handjob/" to "Handjob",
        "${mainUrl}/hardcore/" to "Hardcore",
        "${mainUrl}/facials/" to "Facials",
        "${mainUrl}/titty-fuck/" to "Titty Fuck",
        "${mainUrl}/toy/" to "Toy",
        "${mainUrl}/pov/" to "POV",
        "${mainUrl}/cowgirl/" to "Cowgirl",
        "${mainUrl}/kiss/" to "Kiss",
        "${mainUrl}/massage/" to "Massage",
        "${mainUrl}/squirting/" to "Squirting",
        "${mainUrl}/shaved/" to "Shaved",
        "${mainUrl}/lesbian/" to "Lesbian",
        "${mainUrl}/bukkake/" to "Bukkake",
        "${mainUrl}/cuckold/" to "Cuckold",
        "${mainUrl}/debut-production/" to "Debut Production",
        "${mainUrl}/deep-throating/" to "Deep Throating",
        "${mainUrl}/dirty-words/" to "Dirty Words",
        "${mainUrl}/humiliation/" to "Humiliation",
        "${mainUrl}/incest/" to "Incest",
        "${mainUrl}/japan-sex/" to "Japan Sex",
        "${mainUrl}/nampa/" to "Nampa",
        "${mainUrl}/nasty/" to "Nasty",
        "${mainUrl}/promiscuity/" to "Promiscuity",
        "${mainUrl}/prostitutes/" to "Prostitutes",
        "${mainUrl}/reducing-mosaic/" to "Reducing Mosaic",
        "${mainUrl}/risky-mosaic/" to "Risky Mosaic"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data.removeSuffix("/")}/$page/"
        val document = app.get(url).document
        val home = document.select("li[id^=video-]").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search/video/?s=$query"
        } else {
            "$mainUrl/search/video/?s=$query&page=$page"
        }

        val document = app.get(url).document
        val results = document.select("li[id^=video-]").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val link = this.selectFirst("a.thumbnail") ?: return null
        val title = link.selectFirst("span.video-title")?.text() ?: link.attr("title")
        val href = fixUrlNull(link.attr("href")) ?: return null
        val posterUrl = fixUrlNull(link.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = document.selectFirst("meta[name=description]")?.attr("content")?.trim()

        val contentContainer = document.select("div.content-container")
        val yearText = contentContainer.text()
        val year =
            Regex("""Release Day: (\d{4})""").find(yearText)?.groupValues?.get(1)?.toIntOrNull()

        val detailBox = document.select("div.col-xs-12.col-sm-6.col-md-8")
        val tags = detailBox.select("a:has(i.fa-th-list)").map { it.text().trim() }.distinct()

        val actors = detailBox.select("a[href*='/pornstar/']").map {
            Actor(it.text().trim())
        }.ifEmpty {
            Regex("""type pornstar (.+) and""").find(description ?: "")?.groupValues?.get(1)?.let {
                listOf(Actor(it))
            }
        } ?: emptyList()

        val duration =
            document.selectFirst("meta[property=og:video:duration]")?.attr("content")?.toIntOrNull()
                ?.let { it / 60 }

        val episodes = document.select("button.button_choice_server").mapNotNull { btn ->
            val encodedEmbed = btn.attr("data-embed") ?: return@mapNotNull null
            val decodedBytes =
                Base64.decode(encodedEmbed, android.util.Base64.DEFAULT)
            val decodedUrl = String(decodedBytes, Charsets.UTF_8)
            decodedUrl
        }

        val recommendations = document.select("ul.videos.related li").mapNotNull { element ->
            val aTag = element.selectFirst("a.thumbnail") ?: return@mapNotNull null
            val recTitle =
                aTag.attr("title").ifEmpty { element.selectFirst("span.video-title")?.text() }
                    ?: return@mapNotNull null
            val recHref = aTag.attr("href") ?: return@mapNotNull null
            val recPoster = element.selectFirst("img")?.attr("src")

            newMovieSearchResponse(recTitle, fixUrl(recHref), TvType.NSFW) {
                this.posterUrl = recPoster
            }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, episodes) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.duration = duration
            addActors(actors)
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parseData = mapper.readValue<List<String>>(data)

        parseData.forEach { server ->
            loadExtractor(server, subtitleCallback, callback)
        }

        return true
    }
}