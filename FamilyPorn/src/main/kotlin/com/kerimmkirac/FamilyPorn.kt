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
        val url      = if (page == 1) request.data else "${request.data}/page/$page"
        val document = app.get(url).document
        val home     = document.select("li.king-post-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = document.selectFirst("div.nav-previous a") != null
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor    = this.selectFirst("h2.entry-title a") ?: return null
        val title     = anchor.text().trim().ifEmpty { return null }
        val href      = fixUrl(anchor.attr("href").ifEmpty { return null })

        val bgElement = this.selectFirst("div.king-box-bg")
        val dataSrc   = bgElement?.attr("data-king-img-src")?.ifEmpty { null }
        val styleSrc  = bgElement?.attr("style")?.let { style ->
            Regex("""url\(["']?(.*?)["']?\)""").find(style)?.groupValues?.get(1)
        }
        val posterUrl = fixUrlNull(dataSrc ?: styleSrc)

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url      = if (page == 1) "$mainUrl/?s=$query" else "$mainUrl/page/$page/?s=$query"
        val document = app.get(url).document
        val results  = document.select("li.king-post-item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(
            results,
            hasNext = document.selectFirst("div.nav-previous a") != null
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