// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class MilfNut : MainAPI() {
    override var mainUrl              = "https://milfnut.com"
    override var name                 = "MilfNut"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/category/trending/"                   to "Trending",
        "${mainUrl}/category/anal-porn-videos/"           to "Anal Porn",
        "${mainUrl}/category/asian-porn-videos/"          to "Asian Porn",
        "${mainUrl}/category/aunt-nephew-porn/"           to "Aunt Nephew Porn",
        "${mainUrl}/category/big-ass-porn/"               to "Big Ass",
        "${mainUrl}/category/big-tits-porn/"              to "Big Tits",
        "${mainUrl}/category/brother-sister-porn-videos/" to "Brother Sister Porn",
        "${mainUrl}/category/dad-daughter-porn-videos/"   to "Dad Daughter Porn",
        "${mainUrl}/category/ebony-porn/"                 to "Ebony Porn",
        "${mainUrl}/category/milf-porn/"                  to "MILF Porn",
        "${mainUrl}/category/mom-son-porn/"               to "Mom Son Porn",
        "${mainUrl}/category/pov-porn-videos/"            to "POV Porn",
        "${mainUrl}/category/rough-porn/"                 to "Rough Porn",
        "${mainUrl}/category/small-tits/"                 to "Small Tits",
        "${mainUrl}/category/taboo-porn/"                 to "Taboo Porn",
        "${mainUrl}/category/teen-porn-videos/"           to "Teen Porn",
        "${mainUrl}/category/trending/"                   to "Trending",
        "${mainUrl}/category/uncategorized/"              to "Uncategorized",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1){
            app.get("${request.data}").document
        } else {
            app.get("${request.data}page/$page/").document
        }
        val home     = document.select("article.loop-video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("header span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf(
                "Referer" to "${mainUrl}/",
                "User-Agent" to USER_AGENT
            )
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1) {
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=${query}").document
        }

        val aramaCevap = document.select("article.loop-video").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.video-description")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.tags-list a").map { it.text() }
        val score          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("article.loop-video").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("div#video-actors a").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(score)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val video = fixUrlNull(document.selectFirst("meta[itemprop=contentURL]")?.attr("content")).toString()

        callback.invoke(newExtractorLink(
            this.name,
            this.name,
            video,
            INFER_TYPE,
            {
                this.referer = "${mainUrl}/"
            }
        ))

        return true
    }
}