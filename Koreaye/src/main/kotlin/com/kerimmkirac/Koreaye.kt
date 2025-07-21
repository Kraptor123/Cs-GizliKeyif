// ! Bu araç @kerimmkirac tarafından | @Cs-Gizlikeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Koreaye : MainAPI() {
    override var mainUrl              = "https://koreaye.com"
    override var name                 = "Koreaye"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    private val posterCache = mutableMapOf<String, String>()

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "Tüm Videolar",
        "${mainUrl}/kategori/uvey-anne-porno"  to "Üvey Anne",
        "${mainUrl}/kategori/milf-porno"  to "Milf",
        "${mainUrl}/kategori/buyuk-got-porno-izle"   to "Büyük Göt",
        "${mainUrl}/kategori/buyuk-memeli-porno-izle" to "Büyük Meme",
        "${mainUrl}/kategori/hizmetci-porno" to "Hizmetçi",
        "${mainUrl}/kategori/asyali-porno"   to "Asyalı",
        "${mainUrl}/kategori/taytli-porno-izle"  to "Tayt",
        "${mainUrl}/kategori/jartiyerli-porno-izle"  to "Jartiyer"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page/").document
        val home     = document.select("div.item-video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = selectFirst("a.clip-link") ?: return null
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        val title = anchor.attr("title")?.trim() ?: return null

        val posterUrl = fixUrlNull(
            selectFirst("source")?.attr("data-srcset")
                ?: selectFirst("img")?.attr("src")
                ?: selectFirst("img")?.attr("data-src")
        )
        posterUrl?.let { posterCache[href] = it }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
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
        
        val posterUrl = fixUrlNull(
            selectFirst("source")?.attr("data-srcset")
                ?: selectFirst("img")?.attr("src")
                ?: selectFirst("img")?.attr("data-src")
        )
        posterUrl?.let { posterCache[href] = it }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(data: String): LoadResponse? {
        val url = data
        val doc = app.get(url).document

        val title = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val description = doc.selectFirst("div.entry-content")?.text()?.trim()
        val tags = doc.select("div#extras a").map { it.text().trim() }

        val poster = posterCache[url] ?: fixUrlNull(doc.selectFirst("img.wp-post-image")?.attr("src"))

        return newMovieLoadResponse(title, data, TvType.NSFW, data) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("Koreaye", "data » ${data}")
        
        val url = data
        Log.d("Koreaye", "extracted url » ${url}")
        
        val document = app.get(url).document

        val iframe = document.selectFirst("iframe")?.attr("data-src") ?: return false
        Log.d("Koreaye", "iframe » ${iframe}")

        val iframeDocument = app.get(iframe, referer = mainUrl).document
        
        val scriptText = iframeDocument.select("script").joinToString("\n") { it.html() }
        val fileRegex = """file:\s*["']([^"']+)["']""".toRegex()
        val fileMatch = fileRegex.find(scriptText)
        val m3u8Url = fileMatch?.groupValues?.get(1) ?: return false
        
        val fullM3u8Url = if (m3u8Url.startsWith("http")) {
            m3u8Url
        } else {
            "${iframe.substringBefore("/player.php")}$m3u8Url"
        }
        
        Log.d("Koreaye", "m3u8Url » ${fullM3u8Url}")

        val playlistResponse = app.get(fullM3u8Url, referer = iframe)
        val playlistContent = playlistResponse.text
        
        Log.d("Koreaye", "playlist content » ${playlistContent.take(200)}")
        
        callback.invoke(
            newExtractorLink(
                name,
                name,
                fullM3u8Url,
                type = if (fullM3u8Url.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            ){
                this.referer = iframe
                this.quality = Qualities.Unknown.value
            }
        )
        return true
    }
}