// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.Jsoup

class TurkHub : MainAPI() {
    override var mainUrl              = "https://turkhub18.cyou"
    override var name                 = "TurkHub"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/"                                 to "TurkHub En Son",
        "${mainUrl}/category/liseli-porno/"           to "Liseli Porno",
        "${mainUrl}/category/turk-ifsa/"              to "Türk ifşa",
        "${mainUrl}/category/konusmali-porno/"        to "Konuşmalı Porno",
        "${mainUrl}/category/turbanli-porno/"         to "Türbanlı Porno",
        "${mainUrl}/category/cuckold-porno/"          to "Cuckold Porno",
        "${mainUrl}/category/fetis-porno/"            to "Fetiş Porno",
        "${mainUrl}/category/genc-porno/"             to "Genç Porno",
        "${mainUrl}/category/konulu-porno/"           to "Konulu Porno",
        "${mainUrl}/category/sert-porno/"             to "Sert Porno",
        "${mainUrl}/category/canli-porno/"            to "Canlı Porno",
        "${mainUrl}/category/grup-porno/"             to "Grup Porno",
        "${mainUrl}/category/tango-ifsa/"             to "Tango ifşa",
        "${mainUrl}/category/turkce-altyazili/"       to "Türkçe Altyazılı",
        "${mainUrl}/category/sahibe-porno/"           to "Sahibe Porno",
        "${mainUrl}/category/twitter/"                to "Twitter",
        "${mainUrl}/category/zenci/"                  to "Zenci",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document

        val home = document.select("div.grid-item, article[id^=post-]").mapNotNull {
            it.toMainPageResult()
        }.distinctBy { it.url }

        return newHomePageResponse(request.name, home, hasNext = true)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val entrytitle = this.selectFirst("h2.entry-title a")
        val title      = entrytitle?.text()?.trim() ?: return null
        val href       = fixUrlNull(entrytitle.attr("href")) ?: return null
        val posterurl  = fixUrlNull(this.selectFirst("img.post-thumbnail-image")?.attr("src"))

        return newMovieSearchResponse(title, "$href|$posterurl", TvType.NSFW) {
            this.posterUrl = posterurl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page == 1) "$mainUrl/?s=$query" else "$mainUrl/page/$page/?s=$query"
        val document = app.get(url).document

        val aramacevap = document.select("div.grid-item, article[id^=post-]").mapNotNull {
            it.toMainPageResult()
        }.filter {
            !it.name.contains("İçerik kaldırma", ignoreCase = true) &&
                    !it.name.contains("Gizlilik Politikası", ignoreCase = true) &&
                    !it.url.contains("/terms")
        }.distinctBy { it.url }

        val hasnext = document.selectFirst("a.next.page-numbers") != null

        return newSearchResponseList(aramacevap, hasnext)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(data: String): LoadResponse? {
        val (url, storedposter) = data.split("|").let {
            it[0] to it.getOrNull(1)
        }

        val response = app.get(url).text
        val document = Jsoup.parse(response)

        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null

        val plot = document.selectFirst("div.entry-content")?.ownText()?.trim()
            ?: document.selectFirst("div.entry-content p")?.text()?.trim()

        val tags = document.select("span.categories-list a").map { it.text() }

        val recs = mutableListOf<SearchResponse>()

        document.select("nav.post-nav a").mapNotNull {
            it.toRecommendationResult()
        }.forEach { recs.add(it) }

        document.select("div.related-posts li").mapNotNull {
            val rectitle = it.selectFirst(".post-title a")?.text()?.trim() ?: return@mapNotNull null
            val rechref = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recposter = fixUrlNull(it.selectFirst("img")?.attr("src"))

            newMovieSearchResponse(rectitle, "$rechref|$recposter", TvType.NSFW) {
                this.posterUrl = recposter
            }
        }.forEach { recs.add(it) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = storedposter
            this.plot            = plot
            this.tags            = tags
            this.recommendations = recs.distinctBy { it.url }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst(".post-nav-title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterurl = fixUrlNull(this.selectFirst(".post-nav-thumb img")?.attr("src"))

        return newMovieSearchResponse(title, "$href|$posterurl", TvType.NSFW) {
            this.posterUrl = posterurl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val gateiframes = document.select("iframe.wpve-iframe, iframe").mapNotNull { it.attr("src").takeIf { s -> s.isNotBlank() } }

        gateiframes.forEach { gateiframe ->
            val gatehtml = app.get(gateiframe).text
            val links = Regex("""videoUrls\['[^']+'\]\s*=\s*'([^']+)'""").findAll(gatehtml).map { it.groupValues[1] }.toList()

            if (links.isNotEmpty()) {
                links.forEach {
                    loadExtractor(it, gateiframe, subtitleCallback, callback)
                }
            } else {
                Jsoup.parse(gatehtml).select("iframe").forEach {
                    val src = it.attr("data-src").ifEmpty { it.attr("src") }
                    if (src.isNotBlank() && !src.contains("ads")) {
                        loadExtractor(src, gateiframe, subtitleCallback, callback)
                    }
                }
            }
        }
        return true
    }
}