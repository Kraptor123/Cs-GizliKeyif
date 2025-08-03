// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import kotlinx.coroutines.coroutineScope
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup

class EscobarVip : MainAPI() {
    override var mainUrl              = "https://turkifsa.co"
    override var name                 = "EscobarVip"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override var sequentialMainPage            = true
    override var sequentialMainPageDelay       = 250L // ? 0.25 saniye
    override var sequentialMainPageScrollDelay = 250L // ? 0.25 saniye

    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

            if (doc.html().contains("Just a moment...")) {
                Log.d("kraptor_Escobar", "!!cloudflare geldi!!")
                return cloudflareKiller.intercept(chain)
            }

            return response
        }
    }


    override val mainPage = mainPageOf(
        "${mainUrl}"      to "Ana Sayfa",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page/", interceptor = interceptor).document
        val home     = document.select("article.item-list").mapNotNull { it.toMainPageResult() }

//        Log.d("kraptor_Escobar", "document = $document")

        return newHomePageResponse(request.name, home)
    }

    private suspend fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a")?.text() ?: return null
        Log.d("kraptor_Escobar", "title = $title")

        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        Log.d("kraptor_Escobar", "href = $href")

        val posterAl  = PosterAl(href)

        val posterUrl = posterAl.substringBefore("|")

        val reklamliLink = posterAl.substringAfter("|")

        return newMovieSearchResponse(title, "$href|$reklamliLink", TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    private suspend fun PosterAl(poster : String): String {
        val posterAl  = app.get(poster, interceptor = interceptor).document
        val posterUrl = fixUrlNull(posterAl.selectFirst("figure.wp-block-image:nth-child(4) img")?.attr("src")).toString()
        val reklamliLink = posterAl.selectFirst("figure.wp-block-image.aligncenter.size-large.is-resized a")?.attr("href").toString()
        Log.d("kraptor_$name", "reklamliLink = ${reklamliLink}")
        val reklamDomain = reklamliLink.substringAfter("//").substringBefore("/")
        Log.d("kraptor_$name", "reklamDomain = ${reklamDomain}")
        val reklamPath   = reklamliLink.substringAfter("//").substringAfter("/")
        Log.d("kraptor_$name", "reklamPath = ${reklamPath}")
        val data = "domain=$reklamDomain&path=$reklamPath"
        Log.d("kraptor_$name", "data = ${data}")
        val reklamGecer  = app.post("https://crowd.fastforward.team/crowd/query_v1", headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"), data = mapOf("domain" to "$reklamDomain", "path" to "$reklamPath")).text

        return "$posterUrl|$reklamGecer"
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
        val temizUrl = url.substringBefore("|")
        val reklamliLink = url.substringAfter("|")
        val document = app.get(temizUrl).document

        val title           = document.selectFirst("h1.name > span")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("figure.wp-block-image:nth-child(4) img")?.attr("src")).toString()
        val description     = document.selectFirst("p.has-text-align-center")?.text()?.trim()
        val tags            = document.select("p.post-tag a").map { it.text() }
        Log.d("kraptor_$name", "reklamliLink = ${reklamliLink}")
        val reklamDomain = reklamliLink.substringAfter("//").substringBefore("/")
        Log.d("kraptor_$name", "reklamDomain = ${reklamDomain}")
        val reklamPath   = reklamliLink.substringAfter("//").substringAfter("/")
        Log.d("kraptor_$name", "reklamPath = ${reklamPath}")

        val data = "domain=$reklamDomain&path=$reklamPath"

        Log.d("kraptor_$name", "data = ${data}")

        val reklamGecer  = app.post("https://crowd.fastforward.team/crowd/query_v1", headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"), data = mapOf("domain" to "$reklamDomain", "path" to "$reklamPath")).text
        Log.d("kraptor_$name", "reklamGecer = ${reklamGecer}")
       val sonlink = if (reklamGecer.contains(reklamDomain)) {
            val reklamDomain = reklamGecer.substringAfter("//").substringBefore("/")
            val reklamPath   = reklamGecer.substringAfter("//").substringAfter("/")
            val reklamGecer  = app.post("https://crowd.fastforward.team/crowd/query_v1", headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"), data = mapOf("domain" to "$reklamDomain", "path" to "$reklamPath")).text
           reklamGecer
        } else {
           reklamGecer
        }



        Log.d("kraptor_$name", "sonLink = ${sonlink}")

        return newMovieLoadResponse(title, url, TvType.NSFW, sonlink) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        // TODO:
         loadExtractor(data, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}