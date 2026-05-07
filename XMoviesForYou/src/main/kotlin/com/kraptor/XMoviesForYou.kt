// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
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
        val icerik = sayfa.select("a.group.flex.flex-col").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, icerik, hasNext = true)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val baslikelementi = this.selectFirst("h3")
        val asilbaslik = baslikelementi?.text()?.trim() ?: return null
        val baslik = asilbaslik.replace(Regex("""\[.*?\]"""), "").trim()
        val adres = fixUrlNull(this.attr("href")) ?: return null
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
        val sonuclar = sayfa.select("a.group.flex.flex-col").mapNotNull { it.toMainPageResult() }

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

        val yilElementi = sayfa.selectFirst("span.material-symbols-outlined:contains(calendar_month)")?.parent()?.text()
        val yilBulucu = Regex("""\b\d{4}\b""")
        val yil = yilBulucu.find(yilElementi ?: baslik)?.value?.toIntOrNull()

        val etiketler = sayfa.select("a[href*='/category/']").map {
            it.text().replace("category", "", ignoreCase = true).trim()
        }

        val oyuncular = sayfa.select("a[href*='/pornstar/']").map {
            Actor(it.text().trim(), null)
        }

        val postId = cevap.text.substringAfter("const postId = \"").substringBefore("\"")
        val oneriler = if (postId.isNotEmpty() && postId.length > 3) {
            try {
                val apiCevap = app.get("$mainUrl/api/related/$postId", referer = url).text
                val veri = parseJson<RelatedData>(apiCevap)
                veri.data?.mapNotNull { item ->
                    newMovieSearchResponse(
                        item.title ?: return@mapNotNull null,
                        fixUrl(item.slug ?: return@mapNotNull null),
                        TvType.NSFW
                    ) {
                        this.posterUrl = item.thumbnail_url
                    }
                }
            } catch (e: Exception) {
                Log.d("RelatedError", e.message ?: "")
                null
            }
        } else null

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

    data class RelatedData(
        val data: List<RelatedItem>? = null
    )

    data class RelatedItem(
        val title: String? = null,
        val slug: String? = null,
        val thumbnail_url: String? = null
    )

}