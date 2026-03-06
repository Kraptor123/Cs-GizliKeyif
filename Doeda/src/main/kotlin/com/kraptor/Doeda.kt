// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Doeda : MainAPI() {
    override var mainUrl              = "https://www.doeda.one"
    override var name                 = "Doeda"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        mainUrl to    "Ana Sayfa",
        "${mainUrl}/category/1080p-porno-one/"       to    "1080p",
        "${mainUrl}/category/turbanli-sex/"           to    "Türbanlı",
        "${mainUrl}/category/turk-yerli-porno/"       to    "Türk",
        "${mainUrl}/category/turkce-altyazili-tr/"    to    "Türkçe Altyazılı",
        "${mainUrl}/category/aile-pornos/"            to    "Aile",
        "${mainUrl}/category/amator/"                 to    "Amatör",
        "${mainUrl}/category/anal/"                   to    "Anal",
        "${mainUrl}/category/anne-olgun/"             to    "Anne",
        "${mainUrl}/category/asyali-cekik-gozlu/"     to    "Asyalı",
        "${mainUrl}/category/bakire/"                 to    "Bakire",
        "${mainUrl}/category/balik-etli-dolgun/"      to    "Balık Etli",
        "${mainUrl}/category/brazzers-premium/"       to    "Brazzers",
        "${mainUrl}/category/buyuk-meme-gogus/"       to    "Büyük Meme",
        "${mainUrl}/category/dul-kadin/"              to    "Dul",
        "${mainUrl}/category/ensest-p/"               to    "Ensest",
        "${mainUrl}/category/esmer-guzel/"            to    "Esmer",
        "${mainUrl}/category/evli-cift-p/"            to    "Evli Çift",
        "${mainUrl}/category/fantezi-p/"              to    "Fantezi",
        "${mainUrl}/category/fetis-p/"                to    "Fetiş",
        "${mainUrl}/category/genc/"                   to    "Genç",
        "${mainUrl}/category/gotten/"                 to    "Götten",
        "${mainUrl}/category/grup/"                   to    "Grup",
        "${mainUrl}/category/ifsa-gizli/"             to    "İfşa",
        "${mainUrl}/category/japon/"                  to    "Japon",
        "${mainUrl}/category/konulu/"                 to    "Konulu",
        "${mainUrl}/category/latin-p/"                to    "Latin",
        "${mainUrl}/category/lezbiyen-leziz/"         to    "Lezbiyen",
        "${mainUrl}/category/liseli/"                 to    "Liseli",
        "${mainUrl}/category/milf/"                   to    "Milf",
        "${mainUrl}/category/minyon-p/"               to    "Minyon",
        "${mainUrl}/category/olgun/"                  to    "Olgun",
        "${mainUrl}/category/oral-porno/"             to    "Oral",
        "${mainUrl}/category/pornhub-premium/"        to    "Pornhub",
        "${mainUrl}/category/sakso-p/"                to    "Sakso",
        "${mainUrl}/category/sarisin-p/"              to    "Sarışın",
        "${mainUrl}/category/sert/"                   to    "Sert",
        "${mainUrl}/category/swinger-es-degistirme/"  to    "Swinger",
        "${mainUrl}/category/uvey-anne-azgin/"        to    "Üvey Anne",
        "${mainUrl}/category/yabanci-p/"              to    "Yabancı",
        "${mainUrl}/category/zenci/"                  to    "Zenci",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (request.data == mainUrl || request.data == "$mainUrl/") {
            "$mainUrl/page/$page/"
        } else {
            "${request.data}page/$page/"
        }

        val document = app.get(url).document
        val home = document.select("div.thumb").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        for (page in 1..4) {
            val url = "$mainUrl/page/$page/?s=${query}"
            val document = app.get(url).document
            val pageResults = document
                .select("div.thumb")
                .mapNotNull { it.toSearchResult() }
            results += pageResults
        }
        return results
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun load(url: String): LoadResponse? {
        val documentapp = app.get(url)
        val document    = documentapp.document
        val doctext     = documentapp.text
        val title           = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val postRegex       = Regex(pattern = "\\{\"@type\":\"ImageObject\",\"url\":\"([^\"]*)\",", options = setOf(RegexOption.IGNORE_CASE))
        val poster          = fixUrlNull(postRegex.find(doctext)?.groupValues[1])
        val description     = document.selectFirst("div.entry-content.rich-content p")?.text()?.trim()
        val year            = document.selectFirst("span.time")?.text()?.substringAfterLast(" ")?.trim()?.toIntOrNull()
        val tags            = document.select("#extras a").map { it.text() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_${this.name}", "data = ${data}")
        val document = app.get(data).document

        val iframe   = document.selectFirst("iframe")?.attr("src").toString()

        Log.d("kraptor_${this.name}", "iframe = ${iframe}")

         loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}