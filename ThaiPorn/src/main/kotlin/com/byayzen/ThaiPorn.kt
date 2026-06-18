// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.net.Uri
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.json.JSONObject

class ThaiPorn : MainAPI() {
    override var mainUrl = "https://xn--72c9aha0f8ad1l6bi.com"
    override var name = "ThaiPorn"
    override val hasMainPage = true
    override var lang = "th"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Home",
        "${mainUrl}/video_category/%e0%b8%ab%e0%b8%99%e0%b8%b1%e0%b8%87%e0%b9%82%e0%b8%9b%e0%b9%8a%e0%b9%84%e0%b8%97%e0%b8%a2" to "Thai",
        "${mainUrl}/%e0%b8%ab%e0%b8%99%e0%b8%b1%e0%b8%87%e0%b9%82%e0%b8%9b%e0%b9%8a%e0%b8%ae%e0%b8%b4%e0%b8%95" to "Popular",
        "${mainUrl}/video_category/%e0%b8%84%e0%b8%a5%e0%b8%b4%e0%b8%9b%e0%b8%ab%e0%b8%a5%e0%b8%b8%e0%b8%94" to "Leaked",
        "${mainUrl}/video_category/%e0%b8%84%e0%b8%a5%e0%b8%b4%e0%b8%9b%e0%b9%82%e0%b8%9b%e0%b9%8a" to "Clips",
        "${mainUrl}/video_category/%e0%b8%ab%e0%b8%99%e0%b8%b1%e0%b8%87%e0%b8%ad%e0%b8%b2%e0%b8%a3%e0%b9%8c" to "Erotic",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data.trimEnd('/')
        } else {
            "${request.data.trimEnd('/')}/page/$page"
        }
        val document = app.get(url).document
        val home = document.select("div.box-video").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(
            HomePageList(request.name, home, isHorizontalImages = true),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2.shockx-title a")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("figure.image a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("figure.image img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "${mainUrl}/?s=$query"
        } else {
            "${mainUrl}/page/$page?s=$query"
        }
        val document = app.get(url).document

        val aramaCevap = document.select("div.box-video").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.title")?.text()?.trim()?.take(30) ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags = document.select("div#colophon-ets a.tag.has-text-grey-dark").map { it.text().trim() }.filter { it.isNotBlank() }
        val actors = document.select("span.vcard.author a").map { Actor(it.text().trim()) }
        val recommendations = document.select("div.shockx-message-body div.box-video").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("h2.shockx-title a")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("figure.image a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("figure.image img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("Ayzen", "data = $data")
        val document = app.get(data).document
        val duPhpSrc = document.selectFirst("iframe[src*=/lib/du.php]")?.attr("src")
        if (duPhpSrc != null) {
            val duPhpUrl = fixUrl(duPhpSrc)
            Log.d("Ayzen", "duPhpUrl = $duPhpUrl")

            val duDoc = app.get(duPhpUrl, referer = data).document
            val hlsPlayerSrc = duDoc.selectFirst("iframe[src*=player.hlsbroadcast.com]")?.attr("src")
            if (hlsPlayerSrc != null) {
                Log.d("Ayzen", "hlsPlayerSrc = $hlsPlayerSrc")

                val sParam = Uri.parse(hlsPlayerSrc).getQueryParameter("s")
                if (sParam != null) {
                    val jsonUrl = "https://codeview.hlsbroadcast.com/$sParam.json"
                    Log.d("Ayzen", "jsonUrl = $jsonUrl")

                    val jsonRes = app.get(jsonUrl, referer = hlsPlayerSrc).text
                    val jsonObj = JSONObject(jsonRes)
                    val m3u8Url = jsonObj.getString("r2_url")
                    Log.d("Ayzen", "m3u8Url = $m3u8Url")

                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "ThaiPorn",
                            url = m3u8Url,
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.referer = hlsPlayerSrc
                            this.quality = Qualities.Unknown.value
                        }
                    )
                    return true
                }
            }

            duDoc.select("iframe").map { it.attr("src") }.filter { it.isNotBlank() }.forEach { iframeSrc ->
                val iframeUrl = fixUrl(iframeSrc)
                Log.d("Ayzen", "duDoc loadExtractor = $iframeUrl")
                loadExtractor(iframeUrl, duPhpUrl, subtitleCallback, callback)
            }
        }

        document.select("iframe").map { it.attr("src") }.filter { it.isNotBlank() && !it.contains("/lib/du.php") }.forEach { iframeSrc ->
            val iframeUrl = fixUrl(iframeSrc)
            Log.d("Ayzen", "mainDoc loadExtractor = $iframeUrl")
            loadExtractor(iframeUrl, data, subtitleCallback, callback)
        }

        return true
    }
}