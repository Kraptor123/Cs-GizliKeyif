// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class EFukt : MainAPI() {
    override var mainUrl              = "https://efukt.com"
    override var name                 = "EFukt"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    private var sessionCookies: Map<String, String>? = null
    private val initMutex = Mutex()

    private suspend fun initSession() {
        if (sessionCookies != null) return
        initMutex.withLock {
            if (sessionCookies != null ) return@withLock
            Log.d("kraptor_EFukt", "🔄 Oturum başlatılıyor: cookie aliniyor")
            val headers = mapOf(
                "Accept-Language" to "en-US,en;q=0.5",
                "Connection" to "keep-alive",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:139.0) Gecko/20100101 Firefox/139.0"
            )
            val resp = app.get("${mainUrl}/", headers = headers )
            sessionCookies = resp.cookies + mapOf("volume_level" to "0.5", "volume_muted" to "no", "urzohsd_ef_pop2" to "1|1751668797|n")
            Log.d("kraptor_EFukt", "🔄 cookie alindi = $sessionCookies")
        }
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/"                       to "Videos",
        "${mainUrl}/category/amateur/"            to "Amateur",
        "${mainUrl}/category/anal/"               to "Anal",
        "${mainUrl}/category/asians/"             to "Asians",
        "${mainUrl}/category/beef-curtains/"      to "Beef Curtains",
        "${mainUrl}/category/big-dicks/"          to "Big Dicks",
        "${mainUrl}/category/bizarre/"            to "Bizarre",
        "${mainUrl}/category/busted/"             to "Busted!",
        "${mainUrl}/category/camwhores/"          to "Camwhores",
        "${mainUrl}/category/crazy/"              to "Crazy",
        "${mainUrl}/category/cuckold/"            to "Cuckold",
        "${mainUrl}/category/documentary/"        to "Documentary",
        "${mainUrl}/category/exhibition/"         to "Exhibition",
        "${mainUrl}/category/extreme/"            to "Extreme",
        "${mainUrl}/category/fail/"               to "Fail",
        "${mainUrl}/category/fail-old/"           to "Fail-OLD",
        "${mainUrl}/category/famous/"             to "Famous",
        "${mainUrl}/category/fights/"             to "Fights",
        "${mainUrl}/category/fisting/"            to "Fisting",
        "${mainUrl}/category/gallery/"            to "Gallery",
        "${mainUrl}/category/gape/"               to "Gape",
        "${mainUrl}/category/gloryhole/"          to "Gloryhole",
        "${mainUrl}/category/groped/"             to "Groped",
        "${mainUrl}/category/hookers/"            to "Hookers",
        "${mainUrl}/category/insertions/"         to "Insertions",
        "${mainUrl}/category/loose/"              to "Loose",
        "${mainUrl}/category/lulz/"               to "LULZ",
        "${mainUrl}/category/mildly-retarded/"    to "Mildly Retarded",
        "${mainUrl}/category/modifications/"      to "Modifications",
        "${mainUrl}/category/nasty/"              to "Nasty",
        "${mainUrl}/category/orgasms/"            to "Orgasms",
        "${mainUrl}/category/painal/"             to "Painal",
        "${mainUrl}/category/parody/"             to "Parody",
        "${mainUrl}/category/premature/"          to "Premature",
        "${mainUrl}/category/prolapse/"           to "Prolapse",
        "${mainUrl}/category/public/"             to "Public",
        "${mainUrl}/category/strippers/"          to "Strippers",
        "${mainUrl}/category/teen/"               to "Teen",
        "${mainUrl}/category/vintage/"            to "Vintage",
        "${mainUrl}/category/virgins/"            to "Virgins",
        "${mainUrl}/category/voyeur/"             to "Voyeur",
        "${mainUrl}/category/wrong-hole/"         to "Wrong Hole",
        "${mainUrl}/category/wtf/"                to "WTF",
        "${mainUrl}/category/amputee/"            to "Amputee",
    )


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:139.0) Gecko/20100101 Firefox/139.0"
        )
        initSession()
        val document =  if (page == 1) {
            app.get("${request.data}", headers, cookies = sessionCookies!!).document
        }else{
            app.get("${request.data}$page/", headers, cookies = sessionCookies!!).document
        }
        val home     = document.select("div.col div.tile").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a")?.attr("style")?.substringAfter("'")?.substringBefore("'"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.result-item article").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.title a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.title a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("h2")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("h3 a").map { it.text() }
        val rating          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()?.toRatingInt()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("div.meta:nth-child(4) > div:nth-child(3) > span:nth-child(2) a").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.rating          = rating
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
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

        val iframe   = document.selectFirst("source")?.attr("src").toString()

        Log.d("kraptor_$name", "iframe = ${iframe}")

        callback.invoke(newExtractorLink(
            source = this.name,
            name   = this.name,
            url    = iframe,
            type   = INFER_TYPE
        ))

        return true
    }
}