// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class EroThots : MainAPI() {
    override var mainUrl              = "https://erothots.is"
    override var name                 = "EroThots"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    private val allCategories = listOf(
        "${mainUrl}/videos/Porn" to "Porn",
        "${mainUrl}/videos/Onlyfans" to "Onlyfans",
        "${mainUrl}/videos/Tiktok" to "Tiktok",
        "${mainUrl}/videos/Youtube" to "Youtube",
        "${mainUrl}/videos/Twitch" to "Twitch",
        "${mainUrl}/videos/Instagram" to "Instagram",
        "${mainUrl}/videos/Anal" to "Anal",
        "${mainUrl}/videos/Blowjob" to "Blowjob",
        "${mainUrl}/videos/Lingerie" to "Lingerie",
        "${mainUrl}/videos/Small%20Tits" to "Small Tits",
        "${mainUrl}/videos/Asian" to "Asian",
        "${mainUrl}/videos/Brunette" to "Brunette",
        "${mainUrl}/videos/Masturbation" to "Masturbation",
        "${mainUrl}/videos/Tattoos" to "Tattoos",
        "${mainUrl}/videos/Asmr" to "Asmr",
        "${mainUrl}/videos/Cosplay" to "Cosplay",
        "${mainUrl}/videos/Milf" to "Milf",
        "${mainUrl}/videos/Teasing" to "Teasing",
        "${mainUrl}/videos/Bbw" to "Bbw",
        "${mainUrl}/videos/Cute" to "Cute",
        "${mainUrl}/videos/Moaning" to "Moaning",
        "${mainUrl}/videos/Teen" to "Teen",
        "${mainUrl}/videos/Big%20Ass" to "Big Ass",
        "${mainUrl}/videos/Dildo" to "Dildo",
        "${mainUrl}/videos/Thicc" to "Thicc",
        "${mainUrl}/videos/Big%20Naturals" to "Big Naturals",
        "${mainUrl}/videos/Fake%20Tits" to "Fake Tits",
        "${mainUrl}/videos/Pov" to "Pov",
        "${mainUrl}/videos/Big%20Tits" to "Big Tits",
        "${mainUrl}/videos/Feet" to "Feet",
        "${mainUrl}/videos/Redhead" to "Redhead",
        "${mainUrl}/videos/Bikini" to "Bikini",
        "${mainUrl}/videos/Fit" to "Fit",
        "${mainUrl}/videos/Shaved" to "Shaved",
        "${mainUrl}/videos/Blonde" to "Blonde",
        "${mainUrl}/videos/Fucking" to "Fucking",
        "${mainUrl}/videos/Shower" to "Shower",
    )

    override val mainPage get() = mainPageOf(
        *allCategories.shuffled().take(10).toTypedArray()
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1){
            app.get("${request.data}", referer = "${mainUrl}/").document
        } else {
            app.get("${request.data}?p=${page - 1}", referer = "${mainUrl}/").document
        }
        val home     = document.select("div.videos a").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/videos/$query", referer = "${mainUrl}/").document
        } else {
            app.get("${mainUrl}/videos/$query?p=1", referer = "${mainUrl}/").document
        }

        val aramaCevap = document.select("div.videos a").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, referer = "${mainUrl}/").document

        val video = document.selectFirst("source")?.attr("src") ?: ""
        val uyari = if (video.isEmpty()){
            "Video is not available | Video erişilebilir değil |"
        } else {
            ""
        }

        val title           = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val uyariDesc       = "$uyari $description"
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.scroll-box div.scroll a").map { it.text() }
        val score           = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.videos a").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = uyariDesc
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
        val document = app.get(data, referer = "${mainUrl}/").document

        val video = document.selectFirst("source")?.attr("src").toString()

        callback.invoke(newExtractorLink(
            this.name,
            this.name,
            video,
            INFER_TYPE
        ) {
            this.referer = "${mainUrl}/"
        })

        return true
    }
}