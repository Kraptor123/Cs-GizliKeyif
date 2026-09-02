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
        mainUrl to "All",
        "$mainUrl/tag/anal" to "Anal",
        "$mainUrl/tag/asian" to "Asian",
        "$mainUrl/tag/blonde" to "Blonde",
        "$mainUrl/tag/brunette" to "Brunette",
        "$mainUrl/tag/blowjob" to "Blowjob",
        "$mainUrl/tag/doggystyle" to "DoggyStyle",
        "$mainUrl/tag/ebony" to "Ebony",
        "$mainUrl/tag/latina" to "Latina",
        "$mainUrl/tag/lesbian" to "Lesbian",
        "$mainUrl/tag/milf" to "Milf",
        "$mainUrl/tag/petite" to "Petite",
        "$mainUrl/tag/pov" to "POV",
        "$mainUrl/tag/redhead" to "Red Head",
        "$mainUrl/tag/teen" to "Teen",
        "$mainUrl/tag/threesome" to "Threesome",
        "$mainUrl/tag/hardcore" to "Hardcore",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val cleanUrl = request.data.removeSuffix("/")
        val url      = if (page == 1) cleanUrl else "$cleanUrl/page/$page/"
        val document = app.get(url).document
        val home     = document.select("ul.g1-collection-items > li.g1-collection-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3.entry-title a")?.text()?.trim()?.ifEmpty { return null } ?: return null
        val href      = fixUrl(this.selectFirst("h3.entry-title a")?.attr("href")?.ifEmpty { return null } ?: return null)
        val posterUrl = fixUrlNull(this.selectFirst("div.entry-featured-media img")?.attr("src")?.ifEmpty { null })

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url      = if (page == 1) "$mainUrl/?s=$query" else "$mainUrl/page/$page/?s=$query"
        val document = app.get(url).document
        val results  = document.select("ul.g1-collection-items > li.g1-collection-item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(
            results,
            hasNext = results.isNotEmpty()
        )
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse {
        val document        = app.get(url).document

        val title           = document.selectFirst("h1.entry-title")?.text()?.trim()?.ifEmpty { null } ?: throw ErrorLoadingException("Invalid Title")
        val posterUrl       = fixUrlNull(document.selectFirst("div.single-post-image img")?.attr("src")?.ifEmpty { null })
        val plot            = document.select("div.entry-content p:not(:has(a[data-type=post_tag]))").lastOrNull()?.text()?.trim()?.ifEmpty { null }
        val tags            = document.select("span.tags-links a").mapNotNull { it.text().trim().ifEmpty { null } }
        val actors          = document.select("div.entry-content p:contains(Pornstar:) a, div.entry-content p a[data-type=post_tag]").mapNotNull { it.text().trim().ifEmpty { null } }
        val recommendations = document.select("div.king-related div.king-simple-post").mapNotNull { it.toRecommendationResult() }

        Log.d("kraptor_FamilyPorn", "Loaded: $title")

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = posterUrl
            this.plot            = plot
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val anchor    = this.selectFirst("span.entry-title a") ?: return null
        val title     = anchor.text().trim().ifEmpty { return null }
        val href      = fixUrl(anchor.attr("href").ifEmpty { return null })

        val img       = this.selectFirst("img")
        val posterSrc = img?.attr("data-king-img-src")?.ifEmpty { null } ?: img?.attr("src")?.ifEmpty { null }
        val poster    = fixUrlNull(posterSrc)

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframe   = document.selectFirst("iframe")?.attr("src")?.ifEmpty { null } ?: return false

        Log.d("kraptor_FamilyPorn", "iframe: $iframe")

        return loadExtractor(iframe, subtitleCallback, callback)
    }
}