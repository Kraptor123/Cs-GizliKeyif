// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

class YTBoob : MainAPI() {
    override var mainUrl              = "https://ytboob.com"
    override var name                 = "YTBoob"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "/?filter=popular"    to "Best",
        "/?filter=most-viewed"            to "Most viewed",
        "/?filter=longest"                to "Longest",
        "${mainUrl}/category/slingshot/"            to "Slingshot",
        "${mainUrl}/category/try-on-haul/"          to "Try On Haul",
        "${mainUrl}/category/bikini/"               to "Bikini",
        "${mainUrl}/category/japanese/"             to "Japanese",
        "${mainUrl}/category/asian/"                to "Asian",
        "${mainUrl}/category/tiktok/"               to "TikTok",
        "${mainUrl}/category/massage/"              to "Massage",
        "${mainUrl}/category/yoga/"                 to "Yoga",
        "${mainUrl}/category/fashion/"              to "Fashion",
        "${mainUrl}/category/cameltoe/"             to "Cameltoe",
        "${mainUrl}/category/slips/"                to "Slips",
        "${mainUrl}/category/milk/"                 to "Milk",
        "${mainUrl}/category/big/"                  to "Big",
        "${mainUrl}/category/milf/"                 to "MILF",
        "${mainUrl}/category/foot-fetish/"          to "Foot Fetish",
        "${mainUrl}/category/anal/"                 to "Anal",
        "${mainUrl}/category/blowjob/"              to "Blowjob",
        "${mainUrl}/category/nature/"               to "Nature",
        "${mainUrl}/category/public/"               to "Public",
        "${mainUrl}/category/movie/"                to "Erotic Movies",
        "${mainUrl}/category/indian/"               to "Indian",
        "${mainUrl}/category/hentai/"               to "Hentai",
        "${mainUrl}/category/ebony/"                to "Ebony",
        "${mainUrl}/category/twerking/"             to "Twerking",
        "${mainUrl}/category/flashing/"             to "Flashing",
        "${mainUrl}/category/transparent/"          to "Transparent",
        "${mainUrl}/category/lesbian/"              to "Lesbian",
        "${mainUrl}/category/ass/"                  to "Ass",
        "${mainUrl}/category/bathroom/"             to "Bathroom",
        "${mainUrl}/category/tight/"                to "Tight",
        "${mainUrl}/category/dancing/"              to "Dancing",
        "${mainUrl}/category/paint/"                to "Paint",
        "${mainUrl}/category/swimsuits/"            to "Swimsuits",
        "${mainUrl}/category/boobs/"                to "Boobs",
        "${mainUrl}/category/pussy/"                to "Pussy",
        "${mainUrl}/category/waxing/"               to "Waxing",
        "${mainUrl}/category/rubbing/"              to "Rubbing",
        "${mainUrl}/category/fap-tribute/"          to "Fap Tribute",
        "${mainUrl}/category/nude/"                 to "Nude",
        "${mainUrl}/category/no-panties/"           to "No Panties",
        "${mainUrl}/category/photoshoot/"           to "Photoshoot",
        "${mainUrl}/category/mature/"               to "Mature",
        "${mainUrl}/category/lingerie/"             to "Lingerie",
        "${mainUrl}/category/mirror/"               to "Mirror",
        "${mainUrl}/category/pregnant/"             to "Pregnant",
        "${mainUrl}/category/beach/"                to "Beach",
        "${mainUrl}/category/comedy/"               to "Comedy",
        "${mainUrl}/category/music/"                to "Music",
        "${mainUrl}/category/mxr-clips/"            to "MxR Clips",
        "${mainUrl}/category/entertainment/"        to "Entertainment",
        "${mainUrl}/category/film-animation/"       to "Film & Animation",
        "${mainUrl}/category/education/"            to "Education",
        "${mainUrl}/category/people-blogs/"         to "People & Blogs",
        "${mainUrl}/category/howto-style/"          to "Howto & Style",
        "${mainUrl}/category/travel-events/"        to "Travel & Events",
        "${mainUrl}/category/news-politics/"        to "News & Politics",
        "${mainUrl}/category/gaming/"               to "Gaming",
        "${mainUrl}/category/autos-vehicles/"       to "Autos & Vehicles",
        "${mainUrl}/category/pets-animals/"         to "Pets & Animals",
        "${mainUrl}/category/sports/"               to "Sports",
        "${mainUrl}/category/science-technology/"   to "Science",
        "${mainUrl}/category/featured/"             to "Featured",
        "${mainUrl}/category/nonprofits-activism/"  to "Nonprofits",
        "${mainUrl}/category/uncategorized/"        to "Uncategorized",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (request.data.contains("?filter")) {
            if (page == 1) {
                app.get("${mainUrl}${request.data}").document
            } else {
                app.get("${mainUrl}/page/$page${request.data}").document
            }
        } else {
            if (page == 1) {
                app.get("${request.data}").document
            } else {
                app.get("${request.data}page/$page/").document
            }
        }

        val home     = document.select("article.thumb-block").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val sure      = this.selectFirst("span.duration")?.text()
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val baslik    = "$title |$sure"
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val score     = this.selectFirst("span.rating")?.text()?.replace("%","")?.toIntOrNull()

        return newMovieSearchResponse(baslik, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score     = Score.from100(score)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.post("https://ts-api.ytboob.com/multi_search?x-typesense-api-key=2mFxuIpLuESx5X1aPGkDOx4ZAtM5jG46", json = """{
          "searches": [
            {
              "collection": "post",
              "highlight_full_fields": "post_title,post_content",
              "page": $page,
              "per_page": 12,
              "q": "$query",
              "query_by": "post_title,post_content"
            }
          ]
        }""").text

        val mapper = jacksonObjectMapper().registerKotlinModule()
        val response: Result = mapper.readValue(document)

        val items = response.results?.flatMap { result ->
            result.hits?.mapNotNull { hit ->
                hit.document?.toSearchResult()
            } ?: emptyList()
        } ?: emptyList()
        return newSearchResponseList(items, hasNext = true)
    }

    private fun Icerik.toSearchResult(): SearchResponse? {
        val title     = this.post_title
        val href      = fixUrlNull(this.permalink) ?: return null
        val posterUrl = fixUrlNull(this.post_thumbnail)

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.video-description p:contains(uploaded) + p + p")?.text()?.trim()
        val year            = document.selectFirst("div.video-description p:contains(uploaded)")?.text()?.substringAfter("on ")?.substringBefore("-")?.trim()?.toIntOrNull()
        val tags            = document.select("div.tags-list a").map { it.text() }
        val score          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("article.thumb-block").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(score)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val source   = document.selectFirst("source")?.attr("src").toString()

        callback.invoke(newExtractorLink(
            this.name,
            this.name,
            source,
            ExtractorLinkType.VIDEO,
            {
                this.referer = "${mainUrl}/"
            }
        ))
        return true
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Result(
    val results: List<Cevap>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Cevap(
    val hits: List<Gelen>?
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Gelen(
    val document: Icerik?
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Icerik(
    val permalink: String,
    val post_title: String,
    val post_thumbnail: String,
)