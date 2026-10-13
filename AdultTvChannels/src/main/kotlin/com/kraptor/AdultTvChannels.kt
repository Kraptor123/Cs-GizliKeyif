// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class AdultTvChannels : MainAPI() {
    override var mainUrl              = "https://adult-tv-channels.com"
    override var name                 = "AdultTvChannels"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override var sequentialMainPage   = true

    override val mainPage = mainPageOf(
        "${mainUrl}/page/1/"      to "Page 1",
        "${mainUrl}/page/2/"      to "Page 2",
        "${mainUrl}/page/3/"      to "Page 3",
        "${mainUrl}/page/4/"      to "Page 4",
        "${mainUrl}/page/5/"      to "Page 5",
        "${mainUrl}/page/6/"      to "Page 6",
        "${mainUrl}/page/7/"      to "Page 7",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = try {
            app.get(request.data).document
        } catch (e: Exception) {
            Log.d(name, "getMainPage hata: url=${request.data} mesaj=${e.message}")
            return newHomePageResponse(list = HomePageList(
                name = request.name,
                list = emptyList(),
                isHorizontalImages = true
            ), hasNext = false)
        }
        val home = document.select("article.entry-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(list = HomePageList(
            name = request.name,
            list = home,
            isHorizontalImages = true
        ), hasNext = false)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val titleElement = this.selectFirst("h2.entry-title a") ?: return null
        val title        = titleElement.text().trim().ifBlank { return null }
        val href         = fixUrlNull(titleElement.attr("href")) ?: return null
        val posterUrl    = fixUrlNull(this.selectFirst("figure.entry-item-thumb img")?.attr("src"))

        return newLiveSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url      = if (page == 1) "$mainUrl/?s=$query" else "$mainUrl/page/$page/?s=$query"
        val document = app.get(url).document
        val results  = document.select("article.entry-item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(document.selectFirst("figure.entry-thumb img")?.attr("src"))
        val description = "18 Yaş ve Üzeri İçin Uygundur! - $title"
        val iframe      = fixUrlNull(document.selectFirst("iframe")?.attr("src")) ?: return null

        return newLiveStreamLoadResponse(title, url, iframe) {
            this.posterUrl = poster
            this.plot      = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d(name, "data=$data")
        val document = app.get(data, referer = "$mainUrl/").text
        val regex    = Regex(
            pattern = """(?:signedUrl =|file:)\s*"([^"]*)"""",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        val videoM3u8 = regex.find(document)?.groupValues?.get(1)?.ifBlank { null } ?: return false

        val duzeltilmis = if (videoM3u8.startsWith("https")) {
            videoM3u8
        } else {
            videoM3u8.replace("http:", "https:")
        }

        Log.d(name, "videoM3u8=$duzeltilmis")

        callback.invoke(newExtractorLink(
            name = name,
            source = name,
            url = duzeltilmis,
            type = ExtractorLinkType.M3U8
        ) {
            this.referer = "$mainUrl/"
            this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0")
        })

        return true
    }
}
