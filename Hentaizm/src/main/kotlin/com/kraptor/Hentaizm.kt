// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlin.collections.mapOf

class Hentaizm : MainAPI() {
    override var mainUrl = "https://www.hentaizm6.online"
    override var name = "Hentaizm"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "Live Action" to "Live Action",
        "JAV" to "JAV",
        "Sansürsüz" to "Sansürsüz",
        "Sansürlü" to "Sansürlü",
        "Ahegao" to "Ahegao",
        "Aldatma" to "Aldatma",
        "Anal" to "Anal",
        "Ayak Fetişi" to "Ayak Fetişi",
        "Asansör" to "Asansör",
        "Bakire" to "Bakire",
        "BDSM" to "BDSM",
        "Bondage" to "Bondage",
        "Büyük Memeler" to "Büyük Memeler",
        "Canavar" to "Canavar",
        "Cosplay" to "Cosplay",
        "Creampie" to "Creampie",
        "Dark Skin" to "Dark Skin",
        "Ecchi" to "Ecchi",
        "Elf" to "Elf",
        "Ensest" to "Ensest",
        "Ev Hanımı" to "Ev Hanımı",
        "Fantastik" to "Fantastik",
        "Fetiş" to "Fetiş",
        "Futanari" to "Futanari",
        "Gang Bang" to "Gang Bang",
        "Genç" to "Genç",
        "Gerilim" to "Gerilim",
        "Hamile" to "Hamile",
        "Harem" to "Harem",
        "Hastane" to "Hastane",
        "Hemşire" to "Hemşire",
        "Hentai" to "Hentai",
        "Hizmetçi" to "Hizmetçi",
        "İsekai" to "İsekai",
        "Jartiyer" to "Jartiyer",
        "Korku" to "Korku",
        "Köle" to "Köle",
        "Küçük Memeler" to "Küçük Memeler",
        "Loli" to "Loli",
        "Mastürbasyon" to "Mastürbasyon",
        "Manipülasyon" to "Manipülasyon",
        "Milf" to "Milf",
        "Netorare" to "Netorare",
        "Psikolojik" to "Psikolojik",
        "Ofis" to "Ofis",
        "Okul" to "Okul",
        "Oral" to "Oral",
        "Oyun" to "Oyun",
        "Öğrenci" to "Öğrenci",
        "Öğretmen" to "Öğretmen",
        "Romantizm" to "Romantizm",
        "Sekreter" to "Sekreter",
        "Shota" to "Shota",
        "Succubus" to "Succubus",
        "Süper Güç" to "Süper Güç",
        "Şantaj" to "Şantaj",
        "Şeytanlar" to "Şeytanlar",
        "Tecavüz" to "Tecavüz",
        "Tentacle" to "Tentacle",
        "Tren" to "Tren",
        "Vahşet" to "Vahşet",
        "Vampir" to "Vampir",
        "Yaoi" to "Yaoi",
        "Yuri" to "Yuri",
        "Yüze Oturma" to "Yüze Oturma"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(
            "${mainUrl}/anime-ara?t=tur&q=$page&tur=${request.data}",
            referer = "${mainUrl}/kategoriler-2",
            cookies = mapOf("wordpress_logged_in_1d71d407c5a965a3f58396033421c9f5" to "igtbyprzkxtigpoqbj@enotj.com|1750201309|ruoecMLpexQK9lbznQrrEkTBgPjKPGyBZgFeG4CLyIZ|35e85012a0e702c4a2aca5e5253e4b4649beb3885d4365ea674481969ddc6818")
        ).document
        val home = document.select("div.moviefilm").mapNotNull { it.toMainPageResult() }
//        Log.d("kraptor_${this.name}", "document » ${document}")
//        Log.d("kraptor_${this.name}", "home » ${home}")

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.movief")?.text() ?: return null
//        Log.d("kraptor_${name}", "title » ${title}")
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")?.replace("../..", "")) ?: return null
//        Log.d("kraptor_${name}", "href » ${href}")
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.moviefilm").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.movief")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")?.replace("../..", "")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(
            url,
            cookies = mapOf("wordpress_logged_in_1d71d407c5a965a3f58396033421c9f5" to "igtbyprzkxtigpoqbj@enotj.com|1750201309|ruoecMLpexQK9lbznQrrEkTBgPjKPGyBZgFeG4CLyIZ|35e85012a0e702c4a2aca5e5253e4b4649beb3885d4365ea674481969ddc6818")
        ).document

        val title = document.selectFirst("div.filmcontent h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.filmcontent img")?.attr("src"))
        val description =
            document.selectFirst("table.anime-detay > tbody:nth-child(1) > tr:nth-child(5) > td:nth-child(1)")?.text()
                ?.trim()
        val year =
            document.selectFirst("dıv.anime-detay > tbody:nth-child(1) > tr:nth-child(3) > td:nth-child(1) > b:nth-child(1)")
                ?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.filmborder td a").map { it.text() }
        val episodes = document.select("div.overview li").map {
            val linkElement = it.selectFirst("a")
            val name = linkElement?.text() ?: ""
            val url = linkElement?.absUrl("href") ?: ""
            newEpisode(url, {
                this.name = name.substringAfter(".")
                val match = Regex("(\\d+)(?=\\.)").find(name)
                this.episode = match?.value?.toInt() ?: 1
            }
            )
        }

        return newAnimeLoadResponse(title, url, TvType.Anime, true) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.episodes = mutableMapOf(
                DubStatus.Subbed to episodes
            )
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "data » ${data}")
        val document = app.get(
            data,
            cookies = mapOf("wordpress_logged_in_1d71d407c5a965a3f58396033421c9f5" to "igtbyprzkxtigpoqbj@enotj.com|1750201309|ruoecMLpexQK9lbznQrrEkTBgPjKPGyBZgFeG4CLyIZ|35e85012a0e702c4a2aca5e5253e4b4649beb3885d4365ea674481969ddc6818")
        ).document

        val anchors = document.select("div.filmicerik a")

        anchors.forEachIndexed { index, element ->
            val url = if (index == 0) {
                // İlk link: href kullan
                fixUrlNull(element.attr("href").substringAfter("url="))
            } else {
                // Diğerleri: onclick içinden URL çek
                val onclick = element.attr("onclick")
                val rawUrl = onclick.substringAfter("ajxget('").substringBefore("'").replace("../../","")
                val fixRaw = fixUrlNull(rawUrl).toString()
                val rawGet = app.get(fixRaw, referer = "${mainUrl}/", cookies = mapOf("wordpress_logged_in_1d71d407c5a965a3f58396033421c9f5" to "igtbyprzkxtigpoqbj@enotj.com|1750201309|ruoecMLpexQK9lbznQrrEkTBgPjKPGyBZgFeG4CLyIZ|35e85012a0e702c4a2aca5e5253e4b4649beb3885d4365ea674481969ddc6818")
                ).document
//                Log.d("kraptor_$name", "rawGet » $rawGet")
                val vidUrl = rawGet.selectFirst("a")?.attr("href")?.replace("../../","")
               if (vidUrl.toString().contains("ay.live")) {
                    vidUrl?.substringAfter("url=")
                }
               else
                fixUrlNull(rawGet.selectFirst("iframe")?.attr("src"))
            }

            url?.let { iframe ->
                if (iframe.contains("https://short.icu")) {
                  val iframe =  app.get(iframe, allowRedirects = true).url
                    loadExtractor(iframe, "$mainUrl/", subtitleCallback, callback)
                } else
                Log.d("kraptor_$name", "iframe » $iframe")
                loadExtractor(iframe, "$mainUrl/", subtitleCallback, callback)
            }
        }
        return true
    }
}