// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonProperty
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

class XMoviesForYou : MainAPI() {
    override var mainUrl = "https://xmoviesforyou.com"
    override var name = "XMoviesForYou"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/category/21sextury" to "21Sextury",
        "${mainUrl}/category/adulttime-69907a252fb37" to "AdultTime",
        "${mainUrl}/category/anal" to "Anal",
        "${mainUrl}/category/asian" to "Asian",
        "${mainUrl}/category/bdsm" to "BDSM",
        "${mainUrl}/category/bangbros" to "BangBros",
        "${mainUrl}/category/blonde" to "Blonde",
        "${mainUrl}/category/brazzers" to "Brazzers",
        "${mainUrl}/category/brunette" to "Brunette",
        "${mainUrl}/category/cumlouder" to "Cumlouder",
        "${mainUrl}/category/ddfnetwork" to "DDFNetwork",
        "${mainUrl}/category/dvd" to "DVD",
        "${mainUrl}/category/ebony" to "Ebony",
        "${mainUrl}/category/fakehub" to "FakeHub",
        "${mainUrl}/category/gangbang" to "Gangbang",
        "${mainUrl}/category/hardcore" to "Hardcore",
        "${mainUrl}/category/interracial" to "Interracial",
        "${mainUrl}/category/killergram" to "Killergram",
        "${mainUrl}/category/kinky" to "Kinky",
        "${mainUrl}/category/latina" to "Latina",
        "${mainUrl}/category/lesbian" to "Lesbian",
        "${mainUrl}/category/milf" to "MILF",
        "${mainUrl}/category/masturbation" to "Masturbation",
        "${mainUrl}/category/mofos" to "Mofos",
        "${mainUrl}/category/naughtyamerica" to "NaughtyAmerica",
        "${mainUrl}/category/orgy" to "Orgy",
        "${mainUrl}/category/pornpros" to "PornPros",
        "${mainUrl}/category/realitykings" to "RealityKings",
        "${mainUrl}/category/redhead" to "Redhead",
        "${mainUrl}/category/spizoo" to "Spizoo",
        "${mainUrl}/category/squirt" to "Squirt",
        "${mainUrl}/category/tattoo" to "Tattoo",
        "${mainUrl}/category/teamskeet" to "TeamSkeet",
        "${mainUrl}/category/teen" to "Teen",
        "${mainUrl}/category/threesome" to "Threesome",
        "${mainUrl}/category/uncategorized" to "Uncategorized"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val link = if (page <= 1) {
            request.data
        } else {
            "${request.data}?page=$page"
        }

        val sayfa = app.get(link).document
        val icerik = sayfa.select("article:has(img)").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, icerik, hasNext = true)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val baslik_elementi = this.selectFirst("h3 a") ?: this.selectFirst("a")
        val baslik = baslik_elementi?.text()?.trim() ?: return null
        val adres = fixUrlNull(baslik_elementi?.attr("href")) ?: return null
        val afis = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(baslik, adres, TvType.NSFW) {
            this.posterUrl = afis
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val link = if (page == 1) {
            "$mainUrl/search?q=$query"
        } else {
            "$mainUrl/search?q=$query&page=$page"
        }

        val sayfa = app.get(link).document
        val sonuclar = sayfa.select("article:has(img)").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(sonuclar, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val cevap = app.get(url)
        val sayfa = cevap.document

        val baslik = sayfa.selectFirst("h1")?.text()?.trim() ?: return null

        val afis = fixUrlNull(
            sayfa.selectFirst("#player img")?.attr("src")
                ?: sayfa.selectFirst("meta[property='og:image']")?.attr("content")
        )

        val ozet = sayfa.selectFirst("div.prose p")?.text()?.trim()

        val yil_bulucu = Regex("""\b(19|20)\d{2}\b""")
        val yil = yil_bulucu.find(baslik)?.value?.toIntOrNull()

        val etiketler = sayfa.select("a[href*='/category/']").map {
            it.text().replace("category", "", ignoreCase = true).trim()
        }

        val oyuncular = sayfa.select("a[href*='/pornstar/']").map {
            Actor(it.text().trim(), null)
        }

        val oneriler = try {
            val post_id = cevap.text.substringAfter("const postId = \"").substringBefore("\"")

            if (post_id.isNotEmpty() && post_id.length > 3) {
                val api_cevap = app.get("$mainUrl/api/related/$post_id", referer = url).text
                val ayristirilmis = parseJson<List<RelatedResponse>>(api_cevap)

                ayristirilmis.map {
                    newMovieSearchResponse(it.title ?: "", fixUrl(it.slug ?: ""), TvType.NSFW) {
                        this.posterUrl = it.thumbnail_url
                    }
                }
            } else null
        } catch (e: Exception) {
            null
        }

        return newMovieLoadResponse(baslik, url, TvType.NSFW, url) {
            this.posterUrl = afis
            this.plot = ozet
            this.year = yil
            this.tags = etiketler
            this.recommendations = oneriler
            addActors(oyuncular)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val sayfa = app.get(data).document

        sayfa.select("a[href*='streamtape.com'], a[href*='mixdrop'], a[href*='dood'], a[href*='bigwarp']")
            .forEach { baglanti ->
                val link = baglanti.attr("href")
                if (link.isNotEmpty()) {
                    loadExtractor(link, data, subtitleCallback, callback)
                }
            }
        return true
    }

    data class RelatedResponse(
        val title: String? = null,
        val slug: String? = null,
        val thumbnail_url: String? = null
    )
}