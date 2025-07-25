// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream

class Domlepen : MainAPI() {
    override var mainUrl              = "https://www.barcf.com"
    override var name                 = "Domlepen"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "Tüm Videolar",
        "${mainUrl}/category/buyuk-got"   to "Büyük Göt",
        "${mainUrl}/category/buyuk-meme" to "Büyük Meme",
        "${mainUrl}/category/esmer-porno"  to "Esmer",
        "${mainUrl}/category/latin-porno"  to "Latin",
        "${mainUrl}/category/kizil-sacli-porno"  to "Kızıl",
        "${mainUrl}/category/milf"  to "Milf",
        "${mainUrl}/category/memeleri-sikma"  to "Meme Sıkma",
        "${mainUrl}/category/konulu-porno"  to "Konulu",
        "${mainUrl}/category/brazzers"  to "Brazzers",
        "${mainUrl}/category/full-hd-porno"  to "Full Hd",
        "${mainUrl}/category/hd-porno"  to "Hd",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home = document.select("article.item-list").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("span.post-box-title a")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val poster = fixUrlNull(
            this.selectFirst("img")?.attr("src")
                ?: this.selectFirst("img")?.attr("data-src")
        )

        return newMovieSearchResponse(title, "$href|$poster", TvType.NSFW) {
            posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        
        for (page in 1..3) { 
            val document = app.get("${mainUrl}/page/$page/?s=${query}").document
            val pageResults = document.select("article.item-list").mapNotNull { it.toSearchResult() }
            
            if (pageResults.isEmpty()) break 

            results.addAll(pageResults)
        }
        
        return results
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span.post-box-title a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val poster = fixUrlNull(
            this.selectFirst("img")?.attr("src")
                ?: this.selectFirst("img")?.attr("data-src")
        )

        return newMovieSearchResponse(title, "$href|$poster", TvType.NSFW) { posterUrl = poster }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(data: String): LoadResponse? {
        val (url, poster) = data.split("|").let {
            it[0] to it.getOrNull(1)
        }

        val doc = app.get(url).document

        
        val title = doc.selectFirst("h1.name.post-title.entry-title span[itemprop=name]")?.text()?.trim()
            ?: return null

        
        

       
        val tags = doc.select("span.post-cats a").take(5).map { it.text().trim() }

        return newMovieLoadResponse(title, data, TvType.NSFW, data) {
            this.posterUrl = poster
            this.tags = tags
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    
    private fun decompressGzip(data: ByteArray): String {
        return try {
            val inputStream = GZIPInputStream(ByteArrayInputStream(data))
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("DOMLEPEN", " gzip hata: ${e.message}")
            String(data, Charsets.UTF_8)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("DOMLEPEN", " loadLinks → data = $data")

        val document = app.get(data).document

       
        val iframeUrl = document.selectFirst("div#video iframe")?.attr("src")

        Log.d("DOMLEPEN", " iframeUrl = $iframeUrl")

        if (iframeUrl == null) {
            Log.e("DOMLEPEN", " iframe bulunamadı / yok")
            return false
        }

        
        val headers1 = mapOf(
            "Referer" to "https://www.domplayer.org/",
            "User-Agent" to "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36",
            "Accept-Language" to "tr-TR,tr;q=0.6",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Encoding" to "identity", 
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Site" to "cross-site",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Dest" to "iframe",
            "Sec-Ch-Ua" to "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Brave\";v=\"138\"",
            "Sec-Ch-Ua-Mobile" to "?1",
            "Sec-Ch-Ua-Platform" to "\"Android\"",
            "Connection" to "Keep-Alive"
        )

        try {
            
            val iframeRes1 = app.get(iframeUrl, headers = headers1)
            
            Log.d("DOMLEPEN", " Response headers: ${iframeRes1.headers}")
            Log.d("DOMLEPEN", " Content-Type: ${iframeRes1.headers["Content-Type"]}")
            Log.d("DOMLEPEN", " Content-Encoding: ${iframeRes1.headers["Content-Encoding"]}")
            
            var responseText = iframeRes1.text
            
            
            if (iframeRes1.headers["Content-Encoding"]?.contains("gzip") == true) {
                Log.d("DOMLEPEN", " gzip bulundu, hallediyom")
                responseText = decompressGzip(iframeRes1.body.bytes())
            }
            
            Log.d("DOMLEPEN", " iframeRes (ilk 500 karakter) = ${responseText.take(500)}")

            
            val patterns = listOf(
                """player\(\[\{"file":"([^"]+)"\}]""",
                """player\(\[\{"file":"(.*?)"\}]""",
                """"file":"([^"]+)"""",
                """file['"]:['"]([^'"]+)['"]""",
                """player\(\[.*?"file":"([^"]+)".*?\]""",
                """\\x[0-9a-fA-F]{2}""" 
            )

            
            var hexMatch: String? = null
            var videoUrl: String? = null

            
            if (responseText.contains("player(")) {
                Log.d("DOMLEPEN", " player( bulundu")
                
                
                val hexPattern = """\\x[0-9a-fA-F]{2}"""
                if (responseText.contains("""\\x""")) {
                    Log.d("DOMLEPEN", " hex bulundu")
                    
                   
                    val playerMatch = Regex("""player\(\[\{"file":"([^"]+)"\}\]""").find(responseText)
                    if (playerMatch != null) {
                        hexMatch = playerMatch.groupValues[1]
                        Log.d("DOMLEPEN", " playerdan hexmatch = $hexMatch")
                    }
                }
                
                
                if (hexMatch == null) {
                    val fileMatch = Regex(""""file":"([^"]+)"""").find(responseText)
                    if (fileMatch != null) {
                        hexMatch = fileMatch.groupValues[1]
                        Log.d("DOMLEPEN", " dosyadan hexmatch = $hexMatch")
                    }
                }
            }

            if (hexMatch == null) {
                Log.e("DOMLEPEN", " player pattern yok")
                
                
                val playerIndex = responseText.indexOf("player(")
                if (playerIndex != -1) {
                    val surroundingText = responseText.substring(
                        maxOf(0, playerIndex - 50),
                        minOf(responseText.length, playerIndex + 200)
                    )
                    Log.d("DOMLEPEN", " : $surroundingText")
                }
                
                return false
            }

            
            if (hexMatch.contains("\\x")) {
                Log.d("DOMLEPEN", " ")
                try {
                    
                    videoUrl = hexMatch.split("\\x")
                        .drop(1) 
                        .map { hexPair ->
                            val hexValue = hexPair.take(2) 
                            val charValue = hexValue.toInt(16).toChar()
                            val remaining = hexPair.drop(2) 
                            charValue.toString() + remaining
                        }
                        .joinToString("")
                    
                    Log.d("DOMLEPEN", " hex başarılı")
                } catch (e: Exception) {
                    Log.e("DOMLEPEN", " hex fail: ${e.message}")
                    
                    try {
                        videoUrl = hexMatch.replace("\\x", "")
                            .chunked(2)
                            .map { it.toInt(16).toChar() }
                            .joinToString("")
                        Log.d("DOMLEPEN", " dex 1 başarılı")
                    } catch (e2: Exception) {
                        Log.e("DOMLEPEN", " hex 2 failledi: ${e2.message}")
                        return false
                    }
                }
            } else {
                
                videoUrl = hexMatch
                Log.d("DOMLEPEN", " evet")
            }

            Log.d("DOMLEPEN", " videoUrl = $videoUrl")

            if (videoUrl.isNullOrEmpty()) {
                Log.e("DOMLEPEN", "")
                return false
            }

            
            if (!videoUrl.startsWith("http")) {
                Log.e("DOMLEPEN", "hatalı video url: $videoUrl")
                return false
            }

            
            callback.invoke(
                newExtractorLink(
                    name = "Domlepen",
                    source = "Domlepen",
                    url = videoUrl,


                ){
                    this.referer = "https://www.domplayer.org/"
                    this.quality = Qualities.P1080.value
                    this.headers = headers1
                }
            )

            return true

        } catch (e: Exception) {
            Log.e("DOMLEPEN", " loadlinks hata: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}