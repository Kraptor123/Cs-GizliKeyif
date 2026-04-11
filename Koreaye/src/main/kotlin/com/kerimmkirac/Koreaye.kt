// ! Bu araç @kerimmkirac tarafından | @Cs-Gizlikeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Koreaye : MainAPI() {
    override var mainUrl = "https://koreaye.com"
    override var name = "Koreaye"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    private val posterCache = mutableMapOf<String, String>()

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Son Videolar",
        "$mainUrl/kategori/turk/" to "Türk",
        "$mainUrl/kategori/turkce-altyazili/" to "Türkçe Altyazılı",
        "$mainUrl/kategori/amator/" to "Amatör",
        "$mainUrl/kategori/uvey-anne/" to "Üvey Anne",
        "$mainUrl/kategori/milf/" to "Milf",
        "$mainUrl/kategori/olgun/" to "Olgun",
        "$mainUrl/kategori/genç/" to "Genç",
        "$mainUrl/kategori/ensest/" to "Ensest",
        "$mainUrl/kategori/hizmetci/" to "Hizmetçi",
        "$mainUrl/kategori/asyali/" to "Asyalı",
        "$mainUrl/kategori/konulu/" to "Konulu",
        "$mainUrl/kategori/anal/" to "Anal",
        "$mainUrl/kategori/lezbiyen/" to "Lezbiyen",
        "$mainUrl/kategori/hd/" to "HD"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page/").document
        val home = document.select("div.item-video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            listOf(HomePageList(request.name, home, isHorizontalImages = true)), true
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = selectFirst("a.clip-link") ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val title = anchor.attr("title")?.trim() ?: return null

        val posterurl = fixUrlNull(
            selectFirst("source")?.let { source ->
                val srcset = source.attr("srcset").ifBlank { source.attr("data-srcset") }
                if (srcset.isNotBlank()) srcset.split(" ").firstOrNull() else null
            } ?: selectFirst("img")?.let { img ->
                val src = img.attr("src").ifBlank { img.attr("data-src") }
                src
            }
        )
        posterurl?.let { posterCache[href] = it }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterurl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()

        for (page in 1..3) {
            val document = app.get("${mainUrl}/page/$page/?s=${query}").document
            val pageResults = document.select("div.item-video").mapNotNull { it.toSearchResult() }

            if (pageResults.isEmpty()) break

            results.addAll(pageResults)
        }

        return results
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = selectFirst("a.clip-link") ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val title = anchor.attr("title")?.trim() ?: return null

        val posterurl = fixUrlNull(
            selectFirst("source")?.let { source ->
                val srcset = source.attr("srcset").ifBlank { source.attr("data-srcset") }
                if (srcset.isNotBlank()) srcset.split(" ").firstOrNull() else null
            } ?: selectFirst("img")?.let { img ->
                val src = img.attr("src").ifBlank { img.attr("data-src") }
                src
            }
        )
        posterurl?.let { posterCache[href] = it }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterurl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(data: String): LoadResponse? {
        val url = data
        val doc = app.get(url).document

        val title = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val description = doc.selectFirst("div.entry-content")?.text()?.trim()
        val tags = doc.select("div#extras a").map { it.text().trim() }

        val poster =
            posterCache[url] ?: fixUrlNull(doc.selectFirst("img.wp-post-image")?.attr("src"))

        return newMovieLoadResponse(title, data, TvType.NSFW, data) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data).document
        val iframeUrl = response.selectFirst("iframe")?.attr("data-src") ?: return false

        val iframeResponse = app.get(
            iframeUrl,
            referer = mainUrl,
            headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7")
        )
        val iframeText = iframeResponse.text
        val currentIframeUrl = iframeResponse.url

        val encodedData = iframeText.substringAfter("_d = \"").substringBefore("\"")
        val decodedM3u8 = android.util.Base64.decode(encodedData, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)

        val firstUrlLine = decodedM3u8.lines().firstOrNull { it.startsWith("http") } ?: return false
        val playlistId = Regex("/playlists/([^/]+)/").find(firstUrlLine)?.groupValues?.get(1) ?: return false

        val host = currentIframeUrl.substringBefore("/player")
        val finalM3u8Url = "$host/playlists/$playlistId/playlist.m3u8"

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = finalM3u8Url,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = "https://cdnfast.sbs/"
                this.headers = mapOf(
                    "Origin" to "https://cdnfast.sbs",
                    "Accept" to "*/*",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                this.quality = Qualities.Unknown.value
            }
        )

        return true
    }
}