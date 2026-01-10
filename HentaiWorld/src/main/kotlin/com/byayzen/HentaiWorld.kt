// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class HentaiWorld : MainAPI() {
    override var mainUrl = "https://hentaiworld.tv"
    override var name = "HentaiWorld"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/all-episodes/" to "All Episodes",
        "${mainUrl}/hentai-videos/tag/uncensored/" to "Uncensored",
        "${mainUrl}/hentai-videos/tag/vanilla/" to "Vanilla",
        "${mainUrl}/hentai-videos/tag/big-boobs/" to "Big Boobs",
        "${mainUrl}/hentai-videos/tag/milf/" to "MILF",
        "${mainUrl}/hentai-videos/tag/school-girl/" to "School Girl",
        "${mainUrl}/hentai-videos/tag/cosplay/" to "Cosplay",
        "${mainUrl}/hentai-videos/tag/fantasy/" to "Fantasy",
        "${mainUrl}/hentai-videos/tag/nurse/" to "Nurse",
        "${mainUrl}/hentai-videos/tag/threesome/" to "Threesome",
        "${mainUrl}/hentai-videos/tag/public-sex/" to "Public Sex",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + if (page > 1) "page/$page/" else ""
        val document = app.get(url).document
        val content = document.selectFirst("div#primary, main#main") ?: document
        val home = content.select("a.card-container, article a:has(img)")
            .distinctBy { it.attr("href") }
            .mapNotNull { element ->
                val href = fixUrlNull(element.attr("href")) ?: return@mapNotNull null
                if (href.contains("/page/") || element.attr("rel") == "tag") return@mapNotNull null
                val img = element.selectFirst("img") ?: return@mapNotNull null
                val title = element.selectFirst(".video-title-text")?.text()
                    ?: element.attr("title").ifEmpty { img.attr("title") }
                val poster = img.attr("srcset").substringBefore(" ").ifEmpty { img.attr("src") }

                newMovieSearchResponse(title, href, TvType.NSFW) {
                    this.posterUrl = fixUrlNull(poster)
                }
            }

        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}/page/$page").document

        val aramaCevap = document.select("article").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null

        val poster = fixUrlNull(document.selectFirst("div.left-content img")?.attr("src"))
        val plot = document.selectFirst("p.episode-description:not(:has(strong))")
            ?.text()
            ?.filter { it.code < 128 }
            ?.trim()

        val year = document.selectFirst("div.extra span.C a")?.text()?.toIntOrNull()
        val score = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration =
            document.selectFirst("span.runtime")?.text()?.substringBefore(" ")?.toIntOrNull()

        val studioElements = document.select("p.episode-description:has(strong:contains(Studio)) a")
        val castElements = document.select("span.valor a")

        val tags = document.select("div.video-tags a").map { it.text() }.toMutableList()
        studioElements.forEach { tags.add("Studio: ${it.text()}") }

        val actors = (studioElements + castElements).map { Actor(it.text()) }

        val recommendations = document.select("div.crp_related div.swiper-slide").mapNotNull {
            it.toRecommendationResult()
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = plot
            this.year = year
            this.tags = tags
            this.score = Score.from10(score)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val link = selectFirst("a.crp_link") ?: return null
        val href = fixUrlNull(link.attr("href")) ?: return null

        if (!href.contains("hentaiworld.tv")) return null

        val name = selectFirst(".crp_title")?.text()?.trim() ?: link.attr("title").trim()
        val image = fixUrlNull(selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(name, href, TvType.NSFW) {
            this.posterUrl = image
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val iframeUrl = Regex("""https://hentaiworld\.tv/video-player\.html\?videos/[^'"]+""")
            .find(app.get(data).text)?.value
            ?.replace("&lt;", "")?.replace("&gt;", "")?.trim() ?: return false

        val videoPath = iframeUrl.substringAfter("?").replace(" ", "%20")
        val playerHtml = app.get(iframeUrl.replace(" ", "%20")).text
        val prefix = Regex("""let\s+prefix\s*=\s*['"](https?://[^'"]+)['"]""")
            .find(playerHtml)?.groupValues?.get(1) ?: return false

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = "$prefix$videoPath",
                type = null
            )
        )

        return true
    }
}