// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

//import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Porn00 : MainAPI() {
    override var mainUrl              = "https://www.porn00.org"
    override var name                 = "Porn00"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/latest-vids"      to "Latest Porn Videos",
        "${mainUrl}/popular-vids"   to "Most Viewed Porn Videos",
        "${mainUrl}/top-vids" to "Top Rated Porn Videos",
        "${mainUrl}/category-name/4k"  to "4K Porn Videos",
        "${mainUrl}/category-name/amateur"  to "Amateur Porn Videos",
        "${mainUrl}/category-name/asian"  to "Asian Porn Videos",
        "${mainUrl}/category-name/big-ass"  to "Big Ass Porn Videos",
        "${mainUrl}/category-name/big-tits"  to "Big Tits Porn Videos",
        "${mainUrl}/category-name/brazilian"  to "Brazilian Porn Videos",
        "${mainUrl}/category-name/brunette"  to "Brunette Porn videos",
        "${mainUrl}/category-name/gym"  to "Gym Porn Videos",
        "${mainUrl}/category-name/latina"  to "Latina Porn Videos",
        "${mainUrl}/category-name/lingerie"  to "Lingerie Porn Videos",
        "${mainUrl}/category-name/stepmom"  to "Stepmom Porn Videos",
        "${mainUrl}/category-name/stepsister"  to "Stepsister Porn Videos",
        "${mainUrl}/category-name/tittyfuck"  to "Tittyfuck Porn Videos",
        "${mainUrl}/category-name/doggystyle"  to "Doggystyle Porn Videos",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page").document
        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
    val anchor = this.selectFirst("a") ?: return null
    val title = anchor.attr("title")?.trim() ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original"))

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
}


    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/searching/${query}/?from_videos=$page").document

        val aramaCevap = document.select("div.item").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("a") ?: return null
    val title = anchor.attr("title")?.trim() ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original"))

    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl
    }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

   private fun parseFlashvars(html: String): Map<String, String> {
    val flashvarsRegex = Regex("""var flashvars = \{(.*?)\};""", RegexOption.DOT_MATCHES_ALL)
    val flashvarsMatch = flashvarsRegex.find(html) ?: return emptyMap()
    
    val flashvarsContent = flashvarsMatch.groupValues[1]
    val result = mutableMapOf<String, String>()
    
    val keyValueRegex = Regex("""(\w+):\s*'([^']*)'""")
    keyValueRegex.findAll(flashvarsContent).forEach { match ->
        val key = match.groupValues[1]
        val value = match.groupValues[2]
        result[key] = value
    }
    
    return result
}

override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document
    val html = document.html()

    val flashvars = parseFlashvars(html)

    val title = flashvars["video_title"] ?: return null
    val previewUrl = flashvars["preview_url"]

    
    val description = document.selectFirst("div.info div.item")?.let { element ->
        val text = element.text()
        if (text.contains("Description:")) {
            text.substringAfter("Description:").trim()
        } else null
    }
    
    
    
    val recommendations = document.select("div.list-videos div.item").mapNotNull { it.toRecommendationResult() }
    
    val categories = document.select("div.info div.item:contains(Categories:) a").map { 
        it.text().trim() 
    }
    
    
    val tags = document.select("div.info div.item:contains(Tags:) a").map { 
        it.text().trim() 
    }
    
    
    val allTags = (categories + tags).filter { it.isNotEmpty() }
    
    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = previewUrl
        this.plot = description
        this.tags = allTags
        this.recommendations = recommendations
        
    }
}

private fun Element.toRecommendationResult(): SearchResponse? {
    val anchor = this.selectFirst("a") ?: return null
    val title = anchor.attr("title")?.trim() ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original"))

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
        val pageHtml = app.get(data).text
        return KtPlayerExtractor.getLinks(name, mainUrl, data, pageHtml, callback = callback)
    }
}

// Data class ekle
