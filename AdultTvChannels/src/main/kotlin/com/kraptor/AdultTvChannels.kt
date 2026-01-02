// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class AdultTvChannels : MainAPI() {
    override var mainUrl              = "https://adult-tv-channels.com"
    override var name                 = "AdultTvChannels"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/page/1/"      to "Sayfa 1",
        "${mainUrl}/page/2/"      to "Sayfa 2",
        "${mainUrl}/page/3/"      to "Sayfa 3",
        "${mainUrl}/page/4/"      to "Sayfa 4",
        "${mainUrl}/page/5/"      to "Sayfa 5",
        "${mainUrl}/page/6/"      to "Sayfa 6",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document
        val home     = document.select("div.col-lg-4").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(list = HomePageList(
            name = request.name,
            list = home,
            isHorizontalImages = true
        ), hasNext = false)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h2.entry-title a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("h2.entry-title a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newLiveSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("figure.entry-thumb img")?.attr("src"))
        val description     = "18 Yaş ve Üzeri İçin Uygundur! - $title"
        val iframe          = fixUrlNull(document.selectFirst("iframe")?.attr("src")).toString()

        return newLiveStreamLoadResponse(title, url, iframe, {
            this.posterUrl       = poster
            this.plot            = description
        })
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data, referer = "${mainUrl}/").text
        Log.d("kraptor_$name", "document = ${document}")
        val regex = Regex(
            pattern = """(?:signedUrl =|file:)\s*"([^"]*)"""",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        val videoM3u8 = regex.find(document)?.groupValues[1].toString()

        val duzeltilmis = if (videoM3u8.contains("https")){
            videoM3u8
        } else{
            videoM3u8.replace("http","https")
        }


        Log.d("kraptor_$name", "videoM3u8 = ${videoM3u8}")

        callback.invoke(newExtractorLink(
            name = name,
            source = name,
            url = duzeltilmis,
            type = ExtractorLinkType.M3U8,
            initializer = {
                this.referer = "${mainUrl}/"
                this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0")
            }
        ))

        return true
    }
}