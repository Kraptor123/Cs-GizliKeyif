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
import org.jsoup.Jsoup

class RoshyTv : MainAPI() {
    override var mainUrl              = "https://roshy.tv"
    override var name                 = "RoshyTv"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/category/decensored/"      to "Sansürsüz",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1){
            app.get(request.data).document
        } else {
            app.get("${request.data}page/$page/").document
        }
        val home     = document.select("article[id^=post]").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=$query").document
        }

        val aramaCevap = document.select("article[id^=post]").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags            = document.select("div.main-block-wrapper a.category-item").map { it.attr("title") }
        val recommendations = document.select("div.site__row article[id*=post]").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("div.cast-variant-items-wrapper a.blog-img-link").map { Actor(it.attr("title"),
            it.selectFirst("img")?.attr("src")
        ) }

        val tumLinkler = document.select("div.btn-p-groups-items a").map { it.attr("href") }

        return newMovieLoadResponse(title, url, TvType.NSFW, tumLinkler) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
       val linkler = mapper.readValue<List<String>>(data)

        linkler.forEach { link ->
            val document = app.get(link, referer = "${mainUrl}/").document
            val script = document.selectFirst("div > script[src*=base64,c]")?.attr("src")?.substringAfter("base64,") ?:""
            val base64Coz = base64Decode(script)
            val jsonCevir = base64Coz.substringAfter("pro_player(").substringBefore(");")
            val mapper = mapper.readValue<RoshyData>(jsonCevir)
            val videoCevap = mapper.video_url ?: ""
            val iframe = Jsoup.parse(videoCevap).selectFirst("iframe")?.attr("src") ?: ""
            loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        }
        return true
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoshyData(
    val video_url: String?
)