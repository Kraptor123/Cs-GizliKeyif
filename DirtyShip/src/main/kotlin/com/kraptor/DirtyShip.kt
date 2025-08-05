// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import android.widget.ImageView
import com.fasterxml.jackson.annotation.JsonProperty
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.json.JSONObject

class DirtyShip(val plugin: DirtyShipPlugin) : MainAPI() {
    override var mainUrl              = "https://dirtyship.com"
    override var name                 = "DirtyShip"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/category/amateur-d/"      to "Amateur",
        "${mainUrl}/category/anal-d/"         to "Anal",
        "${mainUrl}/category/anime/"          to "Anime",
        "${mainUrl}/category/asian-d/"        to "Asian",
        "${mainUrl}/category/asmr-h/"         to "ASMR",
        "${mainUrl}/category/big-dick/"       to "Big Dick",
        "${mainUrl}/category/blowjob-h/"      to "Blowjob",
        "${mainUrl}/category/couple-b/"       to "Couple",
        "${mainUrl}/category/fansly-f/"       to "Fansly",
        "${mainUrl}/category/indian/"         to "Indian",
        "${mainUrl}/category/loyalfans/"      to "LoyalFans",
        "${mainUrl}/category/onlyfans-i/"     to "OnlyFans",
        "${mainUrl}/category/patreon-de/"     to "Patreon",
        "${mainUrl}/category/snapchat-c/"     to "Snapchat",
        "${mainUrl}/category/teen-def/"       to "Teen",
        "${mainUrl}/category/tiktok-a/"       to "TikTok",
        "${mainUrl}/category/twitch-b/"       to "Twitch",
        "${mainUrl}/category/twitter/"        to "Twitter",
        "${mainUrl}/category/youtube-b/"      to "YouTube",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get(request.data).document
        }else{
            app.get("${request.data}page/$page/").document
        }
        val home     = document.select("li.thumi").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?search_param=all&s=${query}").document

        return document.select("li.thumi").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val galeriResimleri = document.select("div.col-md-7 div#album img").mapNotNull { it.attr("src") }
        Log.d("kraptor_$name", "galeriResimleri = ${galeriResimleri}")
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val topluResimler = galeriResimleri.joinToString(",") + "|$title"
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = "Sadece 18 Yaş ve Üzeri İçin Uygundur!"
        val tags = document.select("p.data-row a").map { it.text() }
        val recommendations = document.select("li.thumi").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.content-data.clearfix ul.post_performers").map { aktor ->
            val actorIsim = aktor.selectFirst("a")?.attr("title").toString()
            val actorPoster = aktor.selectFirst("img")?.attr("src")
            Actor(name = actorIsim, actorPoster)
        }

        if (galeriResimleri.isNotEmpty()) {
            plugin.loadChapter(title, galeriResimleri)
            return newMovieLoadResponse(title, topluResimler, TvType.NSFW, topluResimler) {
                this.posterUrl = poster
                this.plot = "Bu bir resim galerisi!"
                this.tags = tags
                this.recommendations = recommendations
                addActors(actors)
            }
        } else {

            return newMovieLoadResponse(title, url, TvType.NSFW, url) {
                this.posterUrl = poster
                this.plot = description
                this.tags = tags
                this.recommendations = recommendations
                addActors(actors)
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        if (data.contains(",")){
            val galeriResimleri = data.substringBefore("|").split(",").toList()
            val title = data.substringAfter("|")
            plugin.loadChapter(title, galeriResimleri)
        }
        val document = app.get(data).document
        val videoSource = document
            .selectFirst("div.embed-responsive source")
            ?.attr("src")
            .orEmpty()

        val finalVideoUrl = videoSource.ifBlank {
            val div = document.selectFirst("div[id^=wpfp_]")
            val dataItem = div
                ?.attr("data-item")
                ?.replace("&quot;", "\"")
                ?: ""
            val cleanJsonStr = dataItem
                .replace("\\/", "/")
            val json = JSONObject(cleanJsonStr)
            json
                .getJSONArray("sources")
                .getJSONObject(0)
                .getString("src")
        }

        Log.d("kraptor_$name", "finalVideoUrl = ${finalVideoUrl}")

        callback.invoke(newExtractorLink(
            source = "DirtyShip",
            name   = "DirtyShip",
            url    = finalVideoUrl,
            type   = INFER_TYPE,
            {
                quality = Qualities.Unknown.value
                referer = "${mainUrl}/"
            }
        ))
        return true
    }
}