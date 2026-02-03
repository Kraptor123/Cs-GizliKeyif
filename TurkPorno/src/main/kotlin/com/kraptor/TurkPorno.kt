// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class TurkPorno : MainAPI() {
    override var mainUrl              = "https://turkporno72.cfd"
    override var name                 = "TurkPorno"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/?filter=popular"        to "Popüler",
        "${mainUrl}/tag/amator-porno/"            to "Amatör Porno",
        "${mainUrl}/tag/genc-porno/"              to "Genç Porno",
        "${mainUrl}/tag/ifsa-alemi/"              to "ifşa Alemi",
        "${mainUrl}/tag/konusmali-porno/"         to "Konuşmalı Porno",
        "${mainUrl}/tag/masturbasyon/"            to "Mastürbasyon",
        "${mainUrl}/tag/milf-porno/"              to "Milf Porno",
        "${mainUrl}/tag/turk-ifsa/"               to "Türk ifşa",
        "${mainUrl}/tag/turk-sakso/"              to "Türk Sakso",
        "${mainUrl}/tag/vip-ifsa/"                to "VIP ifşa",
        "${mainUrl}/tag/youtube/"                 to "Youtube",
        "${mainUrl}/actor/antarism/"              to "Antarism",
        "${mainUrl}/actor/athelina/"              to "Athelina",
        "${mainUrl}/actor/ayse-elbi/"             to "Ayşe Elbi",
        "${mainUrl}/actor/acelya/"                to "Açelya",
        "${mainUrl}/actor/berfe-ece-talin/"       to "Berfe Ece Talin",
        "${mainUrl}/actor/biricik/"               to "Biricik",
        "${mainUrl}/actor/cerhawk/"               to "Cerhawk",
        "${mainUrl}/actor/damla-ozden/"           to "Damla Özden",
        "${mainUrl}/actor/dilamete/"              to "DilaMete",
        "${mainUrl}/actor/emine-coskun/"          to "Emine Coşkun",
        "${mainUrl}/actor/fatma-ile-her-telden/"  to "Fatma ile Her Telden",
        "${mainUrl}/actor/heraandposeidon/"       to "HeraandPoseidon",
        "${mainUrl}/actor/hollyloly/"             to "Hollyloly",
        "${mainUrl}/actor/ilcin-karagoz/"         to "ilçin Karagöz",
        "${mainUrl}/actor/karolin-fisekci/"       to "Karolin Fişekçi",
        "${mainUrl}/actor/kralicenin-hayati/"     to "Kraliçenin Hayatı",
        "${mainUrl}/actor/kylasuzin/"             to "KylaSuzin",
        "${mainUrl}/actor/letsnudeit/"            to "LetsNudeit",
        "${mainUrl}/actor/naz-elbir/"             to "Naz Elbir",
        "${mainUrl}/actor/neslihan-gunes/"        to "Neslihan Güneş",
        "${mainUrl}/actor/nightmeal/"             to "Nightmeal",
        "${mainUrl}/actor/pelinasmr/"             to "PelinAsmr",
        "${mainUrl}/actor/prenses-annesi/"        to "Prenses Annesi",
        "${mainUrl}/actor/ruhsuzkahpe31/"         to "RuhsuzKahpe31",
        "${mainUrl}/actor/serpil-cansiz/"         to "Serpil Cansız",
        "${mainUrl}/actor/tango-poine/"           to "Tango Poine",
        "${mainUrl}/actor/tango-samael/"          to "Tango Samael",
        "${mainUrl}/actor/yagmur-simsek/"         to "Yağmur Şimşek",
        "${mainUrl}/actor/zeynep-tumbek/"         to "Zeynep Tümbek",
        "${mainUrl}/actor/oznur-guven/"           to "Öznur Güven",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document
        val home = document.select("div.videos-list article, article.loop-video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("header span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null

        val posterUrl = this.attr("data-main-thumb").ifBlank {
            this.selectFirst("img")?.attr("data-lazy-src")?.ifBlank {
                this.selectFirst("img")?.attr("src")
            }
        }

        val score     = this.selectFirst("div.rating-bar span")?.text()?.replace("%","")?.toIntOrNull()

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score     = Score.from100(score)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/?s=${query}").document
        } else {
            app.get("${mainUrl}/page/$page/?s=${query}").document
        }

        val aramaCevap = document.select("div > article").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("header span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val score     = this.selectFirst("div.rating-bar span")?.text()?.replace("%","")?.toIntOrNull()

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score     = Score.from100(score)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.substringAfter("HD izle. ")?.trim()
        val year            = document.selectFirst("div#video-date")?.text()?.substringAfter(", ")?.trim()?.toIntOrNull()
        val tags            = document.select("div.tags a").map { it.text() }
        val score          = document.selectFirst("div.percentage")?.text()?.replace("%","")?.trim()?.toIntOrNull()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.under-video-block article").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("div#video-actors a").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score          = Score.from100(score)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("header span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null

        val posterUrl = this.attr("data-main-thumb").ifBlank {
            this.selectFirst("img")?.attr("data-lazy-src")?.ifBlank {
                this.selectFirst("img")?.attr("src")
            }
        }

        val score     = this.selectFirst("div.rating-bar span")?.text()?.replace("%","")?.toIntOrNull()

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score     = Score.from100(score)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val videocuklar = mutableListOf<String>()

        val videolar = document.selectFirst("iframe")?.attr("src").toString()
        Log.d("kraptor_$name", "videolar = ${videolar}")

        if (videolar.contains("player.php")){
           val video1 = videolar.substringAfter("url1=").substringBefore("&")
           val video2 = videolar.substringAfter("url2=")
            videocuklar.add(video1)
            videocuklar.add(video2)
        } else {
            loadExtractor(videolar, subtitleCallback, callback)
        }


        videocuklar.forEach { video ->
            loadExtractor(video, subtitleCallback, callback)
        }

        Log.d("kraptor_$name", "videocuklar = ${videocuklar}")

        return true
    }
}