// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import kotlinx.coroutines.delay
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Wumaobi : MainAPI() {
    override var mainUrl              = "https://wumaobi.com"
    override var name                 = "Wumaobi"
    override val hasMainPage          = true
    override var lang                 = "zh"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    //cloudflare v2
    override var sequentialMainPage = true
    override var sequentialMainPageDelay       = 50L  // 0.05 saniye
    override var sequentialMainPageScrollDelay = 50L  // 0.05 saniye

    override val mainPage = mainPageOf(
        "hits" to "Popular",
        "id" to "Latest"
    )

    // Site hiz siniri koyuyor: HTTP 429 + "Jump page" (Cloudflare). Bekleyip yeniden dene.
    private suspend fun fetch(
        url: String,
        cookies: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): com.lagradost.nicehttp.NiceResponse {
        var response = app.get(url, cookies = cookies, headers = headers)
        var attempt = 0
        while ((response.code == 429 || response.document.title().startsWith("Jump page")) && attempt < 4) {
            delay(1500L * (attempt + 1))
            response = app.get(url, cookies = cookies, headers = headers)
            attempt++
        }
        return response
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "${mainUrl}/recommend" else "${mainUrl}/recommend/page-$page.html"
        val document = fetch(url, cookies = mapOf("sort" to request.data)).document
        val home = document.select(".card").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.card-title, .card-title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) "${mainUrl}/search/$query/" else "${mainUrl}/search/$query/page-$page.html"

        val response = fetch(
            url,
            cookies = mapOf("sort" to "id"),
            headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "Upgrade-Insecure-Requests" to "1"
            )
        )

        val results = response.document.select(".card").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = fetch(url).document
        if (document.title().startsWith("Jump page")) throw ErrorLoadingException("Rate limited by the site (HTTP 429), try again in a moment")

        // h1 sitenin logosu ("wumaobi.com"); gercek baslik h2.video-title icinde, yedek og:title/<title>
        val title = document.selectFirst(".video-title a")?.text()?.trim()?.ifEmpty { null }
            ?: document.selectFirst("meta[property=og:title]")?.attr("content")?.trim()?.ifEmpty { null }
            ?: document.title().substringBefore(" - ").trim().ifEmpty { null }
            ?: return null
        val poster = fixUrlNull(document.selectFirst("video#playerCnt")?.attr("poster")
            ?: document.selectFirst("meta[property=og:image]")?.attr("content"))
        val tags = document.select(".post-meta a, .video-tag, .card-text")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = fetch(data).document

        // Gercek kaynaklar sayfa JS'inde: QUALITY_SOURCE=[{tier:"..",line:"..",hq:!1,cn:!1,url:"https://..mp4?.."}]
        // (HTML'deki <source> etiketleri ilgili videolarin onizlemeleri, o yuzden kullanilmaz.)
        val sources = Regex("""tier:"([^"]*)",line:"([^"]*)"[^}]*?url:"([^"]+)"""")
            .findAll(document.html()).toList()

        if (sources.isNotEmpty()) {
            sources.forEach { m ->
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name   = "$name ${m.groupValues[1].replace('_', ' ')} (${m.groupValues[2]})",
                        url    = m.groupValues[3],
                        type   = ExtractorLinkType.VIDEO
                    ) {
                        this.quality = Qualities.Unknown.value
                        this.referer = "$mainUrl/"
                    }
                )
            }
            return true
        }

        return false
    }
}