// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
import kotlinx.serialization.json.*

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class FamilyPorn : MainAPI() {
    override var mainUrl              = "https://familypornhd.com"
    override var name                 = "FamilyPorn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "All Porn Videos",
        "${mainUrl}/tag/redhead"   to "Red Head Porn Videos",
        "${mainUrl}/tag/cowgirl" to "Cowgirl Porn Videos",
        "${mainUrl}/tag/doggystyle"  to "DoggyStyle Porn Videos",
        "${mainUrl}/tag/latina"   to "Latina Porn Videos",
        "${mainUrl}/tag/milf"   to "Milf Porn Videos",
        "${mainUrl}/tag/natural-tits"   to "Natural Tits Porn Videos",
        "${mainUrl}/tag/stepmomporn"   to "Stepmom Porn Videos",
        "${mainUrl}/tag/stepsisterporn"   to "Step Sister Porn Videos",
        "${mainUrl}/tag/athletic"   to "Athletic Porn Videos",
        "${mainUrl}/tag/asian"   to "Asian Porn Videos",
        "${mainUrl}/tag/big-natural-tits"   to "Big Natural Tits Porn Videos",
        "${mainUrl}/tag/big-tits"   to "Big Tits Porn Videos",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}/page/$page"
        val document = app.get(url).document
        val home = document.select("li.g1-collection-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = this.selectFirst("article a") ?: return null
        val title = anchor.attr("title")?.trim() ?: return null
        val href = fixUrl(anchor.attr("href"))
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=${query}").document
        }

        val aramaCevap = document.select("li.g1-collection-item").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("article a") ?: return null
        val title = anchor.attr("title")?.trim() ?: return null
        val href = fixUrl(anchor.attr("href"))
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse {

        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content").toString()
        val description = document.selectFirst("meta[property=og:description]")?.attr("content").toString()
        val tags = document.select("p.entry-tags a").map { it.text().lowercase() }.take(5)
        val posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content").toString()
        val recommendations = document.select("aside.g1-related-entries div.g1-collection li").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, type = TvType.NSFW, data = url,
            initializer = {
                this.posterUrl = posterUrl
                this.plot      = description
                this.tags      = tags
                this.recommendations = recommendations
        })
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val anchor = this.selectFirst("article a") ?: return null
        val title = anchor.attr("title")?.trim() ?: return null
        val href = fixUrl(anchor.attr("href"))
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_FamilyPorn", "loadLinks() başladı - data: $data")
        val document = app.get(data).document

        val iframe   = document.selectFirst("div.embed-container iframe")?.attr("src").toString()

        Log.d("kraptor_FamilyPorn", "iframe: $iframe")

        loadExtractor(iframe, subtitleCallback, callback)

        return true
    }
}