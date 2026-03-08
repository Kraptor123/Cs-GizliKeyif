// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.coroutineScope
import java.net.URI

class Vsex : MainAPI() {
    override var mainUrl = "https://vsex.in"
    override var name = "VSex"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Home",
        "$mainUrl/4k/" to "4K",
        "$mainUrl/allinternal/" to "Allinternal",
        "$mainUrl/21sextury/" to "21sextury",
        "$mainUrl/archaagelvideo/" to "ArchaAngelVideo",
        "$mainUrl/babes/" to "BABES",
        "$mainUrl/bang/" to "Bang",
        "$mainUrl/bangbros/" to "Bangbros",
        "$mainUrl/bellesa/" to "Bellesa",
        "$mainUrl/brazzers/" to "Brazzers",
        "$mainUrl/burningangel/" to "BurningAngel",
        "$mainUrl/cherrypimps/" to "CherryPimps",
        "$mainUrl/colette/" to "Colette",
        "$mainUrl/culioneros/" to "Culioneros",
        "$mainUrl/deeplush/" to "Deeplush",
        "$mainUrl/ddfnetwork/" to "DDFNetwork",
        "$mainUrl/digitalplayground/" to "Digitalplayground",
        "$mainUrl/erito/" to "Erito",
        "$mainUrl/evilangel/" to "EvilAngel",
        "$mainUrl/evolvedfights/" to "EvolvedFights",
        "$mainUrl/excogi/" to "ExploitedCollegeGirls",
        "$mainUrl/fakehub/" to "Fakehub",
        "$mainUrl/fetish-network/" to "Fetish Network",
        "$mainUrl/hentaipros/" to "HentaiPROS",
        "$mainUrl/hussie-pass/" to "Hussie Pass",
        "$mainUrl/immoral-live/" to "Immoral Live",
        "$mainUrl/julesjordan/" to "Jules Jordan",
        "$mainUrl/joymii/" to "Joymii",
        "$mainUrl/lesbea/" to "Lesbea",
        "$mainUrl/letsdoeit/" to "Letsdoeit",
        "$mainUrl/milehigh/" to "Mile High",
        "$mainUrl/mikeadriano/" to "MikeAdriano",
        "$mainUrl/mofos/" to "Mofos",
        "$mainUrl/mylf/" to "Mylf",
        "$mainUrl/naughtyamerica/" to "Naughty America",
        "$mainUrl/newsensations/" to "NewSensations",
        "$mainUrl/nubiles/" to "Nubiles",
        "$mainUrl/porn-pros/" to "Porn Pros Sitios",
        "$mainUrl/private/" to "Private",
        "$mainUrl/producersfun/" to "ProducersFun",
        "$mainUrl/property-sex/" to "Property Sex",
        "$mainUrl/puretaboo/" to "Puretaboo",
        "$mainUrl/kellymadisonmedia/" to "kellymadisonmedia",
        "$mainUrl/realitykings/" to "Reality Kings",
        "$mainUrl/roccosiffredi/" to "RoccoSiffredi",
        "$mainUrl/sexart/" to "SexArt",
        "$mainUrl/spizoo/" to "Spizoo",
        "$mainUrl/sinslife/" to "Sinslife",
        "$mainUrl/teamskeet/" to "Teamskeet",
        "$mainUrl/teenmegaworld-network/" to "TeenMegaWorld Network",
        "$mainUrl/twistys/" to "Twistys",
        "$mainUrl/vixen-group/" to "Vixen Group",
        "$mainUrl/vixen-group/deeper/" to "Deeper",
        "$mainUrl/xempire/" to "Xempire",
        "$mainUrl/wankz-lethalpass/" to "Wankz-Lethalpass",
        "$mainUrl/wicked/" to "Wicked",
        "$mainUrl/generos/" to "Generos",
        "$mainUrl/emule/" to "Emule"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val sayfaadresi = if (page <= 1) request.data else "${request.data}page/$page/"
        val belge = app.get(sayfaadresi).document
        val anasayfaicerikleri = belge.select("div.thumb").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, anasayfaicerikleri)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val baslik = this.selectFirst("h2.th-title")?.text() ?: return null
        val adres = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val gorsel = fixUrlNull(
            this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(baslik, adres, TvType.NSFW) {
            this.posterUrl = gorsel
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val sayfalamaVerisi = (page - 1) * 28
        val aramaAdresi =
            "$mainUrl/index.php?do=search&subaction=search&search_start=$sayfalamaVerisi&full_search=0&story=$query"

        val belge = app.get(aramaAdresi).document
        val aramaSonuclari = belge.select("div.thumb").mapNotNull {
            it.toSearchResult()
        }

        return newSearchResponseList(aramaSonuclari, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val baslik = this.selectFirst("h2.th-title")?.text() ?: return null
        val adres = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val gorsel = fixUrlNull(
            this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(baslik, adres, TvType.NSFW) {
            this.posterUrl = gorsel
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val belge = app.get(url).document

        val baslik = belge.selectFirst("h1")?.text()?.trim() ?: return null
        val gorsel = fixUrlNull(belge.selectFirst("meta[property='og:image']")?.attr("content"))
        val aciklama = belge.selectFirst("meta[property='og:description']")?.attr("content")?.trim()
        val etiketler = belge.select("div.full-tags:contains(Categorias) a").map {
            it.text().replace("Filemoon", "", ignoreCase = true).trim()
        }.filter { it.isNotEmpty() }

        val oyuncular = belge.select("div.full-tags:contains(Reparto) a").map { Actor(it.text()) }

        val onerilenicerikler = belge.select("div.floats.clearfix div.thumb").mapNotNull {
            it.toRecommendationResult()
        }

        return newMovieLoadResponse(baslik, url, TvType.NSFW, url) {
            this.posterUrl = gorsel
            this.plot = aciklama
            this.tags = etiketler
            this.recommendations = onerilenicerikler
            addActors(oyuncular)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val icerikbasligi = this.selectFirst("a.th-title")?.text() ?: return null
        val icerikadresi = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val icerikgorseli = fixUrlNull(
            this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(icerikbasligi, icerikadresi, TvType.NSFW) {
            this.posterUrl = icerikgorseli
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = coroutineScope {
        val iframeUrl = app.get(data).document.selectFirst("iframe")?.attr("src") ?: return@coroutineScope false
        val html = app.get(iframeUrl, referer = data).text

        val redirectUrl = Regex("""url=(https?://[^"'>]+)""").find(html)?.groupValues?.get(1) ?: iframeUrl
        loadExtractor(redirectUrl, iframeUrl, subtitleCallback, callback)
        true
    }
    }