// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class TurkHub : MainAPI() {
    override var mainUrl              = "https://trkhub.fun"
    override var name                 = "TurkHub"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/"                  to "TurkHub En Son",
        "${mainUrl}/tag/altyazili/"               to "Altyazılı",
        "${mainUrl}/tag/amator-porno/"            to "Amatör Porno",
        "${mainUrl}/tag/amcik/"                   to "Amcık",
        "${mainUrl}/tag/anal/"                    to "anal",
        "${mainUrl}/tag/anime/"                   to "Anime",
        "${mainUrl}/tag/anne/"                    to "anne",
        "${mainUrl}/tag/arap/"                    to "Arap",
        "${mainUrl}/tag/bakire/"                  to "Bakire",
        "${mainUrl}/tag/banyo-dus/"               to "Banyo Duş",
        "${mainUrl}/tag/blacked/"                 to "Blacked",
        "${mainUrl}/tag/buyuk-meme/"              to "Büyük Meme",
        "${mainUrl}/tag/degisik/"                 to "Değişik",
        "${mainUrl}/tag/doktor/"                  to "Doktor",
        "${mainUrl}/tag/dul/"                     to "dul",
        "${mainUrl}/tag/ensest/"                  to "ensest",
        "${mainUrl}/tag/erotik/"                  to "Erotik",
        "${mainUrl}/tag/esmer/"                   to "Esmer",
        "${mainUrl}/tag/fantezi/"                 to "fantezi",
        "${mainUrl}/tag/fetis/"                   to "Fetiş",
        "${mainUrl}/tag/gangbang/"                to "Gangbang",
        "${mainUrl}/tag/genel/"                   to "Genel",
        "${mainUrl}/tag/genc/"                    to "genç",
        "${mainUrl}/tag/gizli-cekim/"             to "gizli cekim",
        "${mainUrl}/tag/grup/"                    to "grup",
        "${mainUrl}/tag/gotten/"                  to "götten",
        "${mainUrl}/tag/hastane/"                 to "Hastane",
        "${mainUrl}/tag/hdabla/"                  to "Hdabla",
        "${mainUrl}/tag/hdturk/"                  to "hdturk",
        "${mainUrl}/tag/ifsa/"                    to "ifsa",
        "${mainUrl}/tag/konulu/"                  to "konulu",
        "${mainUrl}/tag/liseli/"                  to "liseli",
        "${mainUrl}/tag/mobil/"                   to "mobil",
        "${mainUrl}/tag/olgun/"                   to "olgun",
        "${mainUrl}/tag/onlyfans/"                to "onlyfans",
        "${mainUrl}/tag/onlyfans-porno/"          to "onlyfans porno",
        "${mainUrl}/tag/oral/"                    to "Oral",
        "${mainUrl}/tag/periscope-turk-ifsa/"     to "periscope türk ifşa",
        "${mainUrl}/tag/porno-turk/"              to "porno türk",
        "${mainUrl}/tag/sakso/"                   to "sakso",
        "${mainUrl}/tag/sarisin/"                 to "sarışın",
        "${mainUrl}/tag/tombul/"                  to "Tombul",
        "${mainUrl}/tag/tr-lez-porno/"            to "tr lez porno",
        "${mainUrl}/tag/turk/"                    to "turk",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1){
            app.get("${request.data}").document
        } else {
            app.get("${request.data}page/$page/").document
        }
        val home     = document.select("div.mag-box li").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=${query}").document
        }

        val aramaCevap = document.select("div.mag-box li").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags            = document.select("span.tagcloud a").map { it.text() }
        val recommendations = document.select("div.related-posts-list div.related-item").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val iframe = document.selectFirst("iframe")?.attr("src").toString()

         loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}