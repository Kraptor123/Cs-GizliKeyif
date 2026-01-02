package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Suppress("ClassName")
class xHamster : MainAPI() {
    override var mainUrl = "https://xhamster.com"
    override var name = "xHamster"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
//        "${mainUrl}/newest/"  to "En Yeniler",
//        "${mainUrl}/most-viewed/weekly/"  to "Haftalık En Çok Görüntülenenler",
//        "${mainUrl}/most-viewed/monthly/" to "Aylık En Çok Görüntülenenler",
//        "${mainUrl}/most-viewed/"  to "Tüm Zamanların En Çok İzlenenleri",
        "${mainUrl}/4k/"                       to "4K",
        "${mainUrl}/hd/2?quality=1080p"       to "1080p",
        "${mainUrl}/categories/teen"          to "Genç",
        "${mainUrl}/categories/mom"           to "Üvey Anne",
        "${mainUrl}/categories/milf"          to "Milf",
        "${mainUrl}/categories/mature"        to "Olgun",
        "${mainUrl}/categories/big-ass"       to "Büyük Göt",
        "${mainUrl}/categories/anal"          to "Anal",
        "${mainUrl}/categories/hardcore"      to "Sert",
        "${mainUrl}/categories/homemade"      to "Ev Yapımı",
        "${mainUrl}/categories/amateur"       to "Amatör Çekim",
        "${mainUrl}/categories/complilation"  to "Derlemeler",
        "${mainUrl}/categories/lesbian"       to "Lezbiyen",
        "${mainUrl}/categories/russian"       to "Rus",
        "${mainUrl}/categories/european"      to "Avrupalı",
        "${mainUrl}/categories/latina"        to "Latin",
        "${mainUrl}/categories/asian"         to "Asyalı",
        "${mainUrl}/categories/jav"           to "Japon",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = app.get("${request.data}/$page?geo=us", cookies = mapOf("video_titles_translation" to "0")).document
    val home = document.select("div.thumb-list div.thumb-list__item")
        .mapNotNull { it.toSearchResult() }

    
    return newHomePageResponse(
        list = HomePageList(
            name = request.name,
            list = home,
            isHorizontalImages = true
        ),
        hasNext = true
    )
}


    


    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a.video-thumb-info__name")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a.video-thumb-info__name")!!.attr("href"))
        val posterUrl = fixUrlNull(this.select("img.thumb-image-container__image").attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document =
        app.get("${mainUrl}/search/${query.replace(" ", "+")}/?page=$page&x_platform_switch=desktop&geo=us", cookies = mapOf("video_titles_translation" to "0")).document

        val aramaCevap = document.select("div.thumb-list div.thumb-list__item").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get("${url}?geo=us", cookies = mapOf("video_titles_translation" to "0")).document

        val title = document.selectFirst("div.with-player-container h1")?.text()?.trim().toString()
        val poster = fixUrlNull(
            document.selectFirst("div.xp-preload-image")?.attr("style")?.substringAfter("https:")
                ?.substringBefore("\');")
        )
        val tags =
            document.select(" nav#video-tags-list-container ul.root-8199e.video-categories-tags.collapsed-8199e li.item-8199e a.video-tag")
                .map { it.text() }
        val recommendations = document.select("div.related-container div.thumb-list div.thumb-list__item")
            .mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    var foundLinks = false
    val sourceName = name

    val document: Document = try {
        app.get("${data}?geo=us", cookies = mapOf("video_titles_translation" to "0")).document
    } catch (e: Exception) {
        Log.e(sourceName, "Failed to fetch document: ${e.message}")
        return false
    }

    
    val preloadLinks = document.select("link[rel=preload][as=fetch]")
    preloadLinks.forEach { link ->
        val href = link.attr("href")
        if (href.isNotEmpty() && href.contains(".m3u8")) {
            val fixed = fixUrl(href)
            Log.d(sourceName, "Found M3U8 URL from preload: $fixed")

            callback(
                newExtractorLink(
                    source = sourceName,
                    name = sourceName,
                    url = fixed,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = data
                    this.quality = Qualities.Unknown.value
                }
            )
            foundLinks = true
        }
    }

    
    val initialData = getInitialsJson(document.html())
    initialData?.xplayerSettings?.subtitles?.tracks?.forEach { track ->
        track.urls?.vtt?.let { url ->
            val fixed = fixUrl(url)
            val cleanLabel =
                track.label?.replace(Regex("\\s*\\(auto-generated\\)"), "") ?: track.lang
                ?: "Unknown"

            Log.d(sourceName, "Subtitle $cleanLabel: $fixed")
            subtitleCallback(newSubtitleFile(lang = cleanLabel, url = fixed))
        } ?: Log.w(sourceName, "Subtitle missing VTT: $track")
    } ?: Log.w(sourceName, "No subtitles in JSON.")

    if (!foundLinks) {
        Log.w(sourceName, "No video links found.")
    }

    return foundLinks
}





data class InitialsJson(
    val xplayerSettings: XPlayerSettings? = null
)

data class XPlayerSettings(
    val sources: VideoSources? = null,
    val subtitles: Subtitles? = null
)

data class VideoSources(
    val hls: HlsSources? = null,
    val standard: StandardSources? = null
)

data class HlsSources(val h264: HlsSource? = null)
data class HlsSource(val url: String? = null)

data class StandardSources(val h264: List<StandardSourceQuality>? = null)
data class StandardSourceQuality(val quality: String? = null, val url: String? = null)

data class Subtitles(val tracks: List<SubtitleTrack>? = null)
data class SubtitleTrack(val label: String? = null, val lang: String? = null, val urls: SubtitleUrls? = null)
data class SubtitleUrls(val vtt: String? = null)



private fun getInitialsJson(html: String): InitialsJson? {
    return try {
        val regex = Regex("window\\.initials\\s*=\\s*(\\{.*?\\});", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(html) ?: return null
        val jsonString = match.groupValues[1]
        
        
        
        val parsedJson = AppUtils.parseJson<InitialsJson>(jsonString)
        
        
        
        parsedJson
    } catch (e: Exception) {
        Log.e("xHamster", "getInitialsJson failed: ${e.message}")
        null
    }
}

}
