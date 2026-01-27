// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Arbada : MainAPI() {
    override var mainUrl              = "https://www.arbada.com"
    override var name                 = "Arbada"
    override val hasMainPage          = true
    override var lang                 = "ar"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/categories/سكس-كبار-في-السن/" to "سكس كبار في السن",
        "${mainUrl}/categories/سكس-شيميل-متحول-جنسي/" to "سكس شيميل متحول جنسي",
        "${mainUrl}/categories/سكس-كرتون/" to "سكس كرتون",
        "${mainUrl}/categories/افلام-سكس-نيك-امهات-محارم/" to "سكس امهات",
        "${mainUrl}/categories/سكس-زنوج/" to "سكس زنوج",
        "${mainUrl}/categories/سكس-سوداء/" to "سكس سوداء",
        "${mainUrl}/categories/سكس-روسي/" to "سكس روسي",
        "${mainUrl}/categories/سكس-الماني/" to "سكس الماني",
        "${mainUrl}/categories/سكس-خادمات/" to "سكس خادمات",
        "${mainUrl}/categories/سكس-كلاسيكي-قديم/" to "سكس كلاسيكي قديم",
        "${mainUrl}/categories/سكس-صيني/" to "سكس صيني",
        "${mainUrl}/categories/افلام-سكس-محارم-عائلي-جنس-اباحي/" to "سكس محارم",
        "${mainUrl}/categories/افلام-سكس-مترجم-عربي/" to "سكس مترجم",
        "${mainUrl}/categories/قذف-الكس/" to "قذف الكس",
        "${mainUrl}/categories/افلام-سكس-نيك-اخوات-محارم/" to "سكس اخوات",
        "${mainUrl}/categories/نيك-بزاز-كبيرة/" to "نيك بزاز كبيرة",
        "${mainUrl}/categories/سكس-سحاق/" to "سكس سحاق",
        "${mainUrl}/categories/سكس-اوروبي/" to "سكس اوروبي",
        "${mainUrl}/categories/european/" to "European",
        "${mainUrl}/categories/bbw/" to "BBW",
        "${mainUrl}/categories/سكس-مغربي/" to "سكس مغربي",
        "${mainUrl}/categories/سكس-افريقي/" to "سكس افريقي",
        "${mainUrl}/categories/anal/" to "Anal - سكس شرجي",
        "${mainUrl}/categories/سكس-مساج/" to "سكس مساج",
        "${mainUrl}/categories/Japanese-Porn-Sex-سكس-ياباني/" to "سكس ياباني",
        "${mainUrl}/categories/سكس-نيك-فخاد/" to "سكس نيك فخاد",
        "${mainUrl}/categories/نيك-طيز-كبيرة/" to "نيك طيز كبيرة",
        "${mainUrl}/categories/قذف-الزب/" to "قذف الزب",
        "${mainUrl}/categories/افلام-سكس-اجنبي/" to "سكس اجنبي",
        "${mainUrl}/categories/سكس-هندي/" to "سكس هندي",
        "${mainUrl}/categories/blonde/" to "Blonde - نيك شقراوات",
        "${mainUrl}/categories/milf/" to "MILF - سكس ميلفات",
        "${mainUrl}/categories/سكس-العادة-السرية/" to "سكس العادة السرية",
        "${mainUrl}/categories/big-tits-نهود-كبيرة/" to "نهود كبيرة - Big Tits",
        "${mainUrl}/categories/سكس-نيك-طيز/" to "سكس نيك طيز",
        "${mainUrl}/categories/وصول-للنشوة/" to "وصول للنشوة",
        "${mainUrl}/categories/random-porn/" to "سكس عشوائي",
        "${mainUrl}/categories/سكس-متزوجات/" to "سكس متزوجات",
        "${mainUrl}/categories/masturbation/" to "Masturbation",
        "${mainUrl}/categories/افلام-سكس-جودة-عالية-HD/" to "افلام سكس جودة عالية HD",
        "${mainUrl}/categories/hardcore/" to "Hardcore - نيك قاسي",
        "${mainUrl}/categories/سكس-جزائري/" to "سكس جزائري",
        "${mainUrl}/categories/hd2/" to "HD",
        "${mainUrl}/categories/mature/" to "Mature - سكس ناضج",
        "${mainUrl}/categories/blowjob/" to "Blowjob",

    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page/").document
        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original") ?: this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}/").document

        val aramaCevap = document.select("div.item").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.wp-content p")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.info div.item a").map { it.text() }
        val score          = document.selectFirst("span.voters")?.text()?.trim()?.substringBefore("%")
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from100(score)
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val source   = document.select("source")

        source.forEach { source ->
            val title = source.attr("title")
            val type  = source.attr("type")
            val video = source.attr("src")
            callback.invoke(newExtractorLink(
                "${this.name}",
                "${this.name}",
                video,
                type = INFER_TYPE,
                {
                    this.referer = "${mainUrl}/"
                    this.quality = getQualityFromName(title)
                }
            ))
        }

        return true
    }
}