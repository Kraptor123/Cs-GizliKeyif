// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URLEncoder

class Rusporn : MainAPI() {
    override var mainUrl = "https://en.rusporn.center"
    override var name = "Rusporn"
    override val hasMainPage = true
    override var lang = "ru"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val mainPage = mainPageOf(
        "${mainUrl}/domashneye/" to "Amateur",
        "${mainUrl}/anal/" to "Anal",
        "${mainUrl}/aziatki/" to "Asians",
        "${mainUrl}/bolshiye-popki/" to "Big Ass",
        "${mainUrl}/bolshiye-chleny/" to "Big Dick",
        "${mainUrl}/bolshiye-doyki/" to "Big Tits",
        "${mainUrl}/blondinki/" to "Blondes",
        "${mainUrl}/lesbiyanki/" to "Lesbians",
        "${mainUrl}/massazh/" to "Massage",
        "${mainUrl}/masturbatsiya/" to "Masturbation",
        "${mainUrl}/zrelye/" to "Mature",
        "${mainUrl}/mamki/" to "MILF",
        "${mainUrl}/negry/" to "Blacked",
        "${mainUrl}/molodyye/" to "Teen"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data}page-${page}/"
        }
        Log.i("RuspornMainPage", "İstek yapılıyor -> Sayfa: $page, Oluşturulan URL: $url")

        val response = app.get(url)
        val document = response.document

        Log.i("RuspornMainPage", "Yanıt Alındı -> Gerçek URL: ${response.url}, Durum Kodu: ${response.code}")
        if (response.code == 404) {
            Log.w("RuspornMainPage", "Sayfa bulunamadı (404). Boş liste döndürülüyor.")
            return newHomePageResponse(
                list = HomePageList(
                    name = request.name,
                    list = emptyList(),
                    isHorizontalImages = true
                ),
                hasNext = false
            )
        }

        val home = document.select("div#preview").mapNotNull { it.toSearchResult() }
        Log.i("RuspornMainPage", "Sayfa $page'de ${home.size} adet video bulundu.")
        val nextPageLink = document.select("a[href*='page-${page + 1}']").firstOrNull()
        val hasNext = nextPageLink != null

        Log.i("RuspornMainPage", "Sonraki sayfa kontrolü (Sayfa ${page + 1}) -> Eleman bulundu: $hasNext, hasNext: $hasNext")

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = hasNext
        )
    }


    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.preview-name a, div.title a")?.text()?.trim()
            ?: this.selectFirst("h1")?.text()?.trim()
            ?: return null
        val href = fixUrlNull(
            this.selectFirst("div.preview-images a, div.title a")?.attr("href")
                ?: this.selectFirst("a")?.attr("href")
        ) ?: return null

        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("src")
                ?: this.selectFirst("div.preview-images img")?.attr("src")
        ) ?: return null

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun search(query: String, page: Int): SearchResponseList {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = if (page == 1) {
            "${mainUrl}/search/?text=${encodedQuery}"
        } else {
            "${mainUrl}/search/?text=${encodedQuery}&page=$page"
        }

        val document = app.get(searchUrl).document

        val results = document.select("div#preview").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(results, hasNext = true)
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim()
            ?: document.selectFirst("title")?.text()?.trim()?.substringBefore(" - HD porn online")
            ?: return null

        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
            ?: fixUrlNull(document.selectFirst("div.story-description img")?.attr("src"))
            ?: fixUrlNull(document.selectFirst("div.preview-images img")?.attr("src"))

        val description = document.selectFirst("div.story-description#ivideo_info")?.text()?.trim()
            ?: document.selectFirst("meta[name=description]")?.attr("content")?.trim()
        val tags = document.select("div.video-categories a").map { it.text().trim() }


        val recommendations = document.select("div#preview").mapNotNull { element ->
            val recTitle = element.selectFirst("div.preview-name a")?.text()?.trim()
                ?: return@mapNotNull null
            val recHref = fixUrlNull(element.selectFirst("div.preview-images a")?.attr("href"))
                ?: return@mapNotNull null
            val recPoster = fixUrlNull(element.selectFirst("div.preview-images img")?.attr("src"))
            newMovieSearchResponse(recTitle, recHref, TvType.NSFW) { this.posterUrl = recPoster }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.duration = duration
            this.recommendations = recommendations
        }
    }




        override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val scriptTag = app.get(data).document.select("script").find { it.data().contains("var player") }?.data()
            ?: return false

        Regex("""\[([\d+p]+)\]\s*([^,\]]+)""").findAll(scriptTag)
            .map { match -> Pair(match.groupValues[1], match.groupValues[2].trim()) }
            .sortedByDescending { (quality, _) -> quality.replace(Regex("\\D"), "").toIntOrNull() ?: 0 }
            .forEach { (quality, url) ->
                callback(
                    newExtractorLink(
                        source = name,
                        name = "$name - $quality",
                        url = url,
                        type = ExtractorLinkType.VIDEO
                    )
                )
            }
        return true
    }
}