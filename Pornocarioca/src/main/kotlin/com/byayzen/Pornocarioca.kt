// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Pornocarioca : MainAPI() {
    override var mainUrl = "https://www.pornocarioca.com"
    override var name = "Pornocarioca"
    override val hasMainPage = true
    override var lang = "pt-BR"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
           mainUrl to "Latest",
        "${mainUrl}/videos/" to "Videos",
        "${mainUrl}/videos/amadoras-porno/" to "Amateurs",
        "${mainUrl}/videos/porno-caiu-na-net/" to "Leaked",
        "${mainUrl}/videos/porno-amador/" to "Amateur Porn",
        "${mainUrl}/videos/brasileiras-porno/" to "Brazilian Girls",
        "${mainUrl}/videos/gostosas-videos-amador/" to "Hotties",
        "${mainUrl}/videos/sexo-anal/" to "Anal",
        "${mainUrl}/videos/amadores/" to "Amateur Couples",
        "${mainUrl}/videos/porno-novinhas/" to "Teens",
        "${mainUrl}/videos/sexo-amador-videos/" to "Amateur Sex",
        "${mainUrl}/videos/caseiros/" to "Homemade",
        "${mainUrl}/videos/porno-caseiro/" to "Homemade Porn",
        "${mainUrl}/videos/videos-de-sexo/" to "Sex Videos",
        "${mainUrl}/videos/boquetes/" to "Blowjobs",
        "${mainUrl}/videos/bucetas/" to "Pussies",
        "${mainUrl}/videos/ninfetas/" to "Nymphets",
        "${mainUrl}/videos/porno-videos/" to "Porn",
        "${mainUrl}/videos/porno-brasileiro-videos/" to "Brazilian Porn",
        "${mainUrl}/videos/sexo-gostoso/" to "Good Sex",
        "${mainUrl}/videos/cavalas/" to "Big Booty"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            "${request.data}page/$page/"
        }

        val document = app.get(url).document
        val home = document.select("div.videos div.video").mapNotNull { it.tomainpageresult() }

        return newHomePageResponse(listOf(HomePageList(request.name, home, isHorizontalImages = true)), hasNext = home.isNotEmpty())
    }

    private fun Element.tomainpageresult(): SearchResponse? {
        val title = this.selectFirst("h2.video-titulo")?.text() ?: this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterurl = fixUrlNull(this.selectFirst("img")?.attr("data-src")) ?: fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterurl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page == 1) {
            "$mainUrl/?s=$query"
        } else {
            "$mainUrl/page/$page/?s=$query"
        }

        val document = app.get(url).document
        val searchresults = document.select("div.videos div.video").mapNotNull { it.tomainpageresult() }

        return newSearchResponseList(searchresults, hasNext = searchresults.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.post-titulo")?.text()?.trim() ?: document.selectFirst("meta[itemprop=name]")?.attr("content")
        ?: return null

        val postr = fixUrlNull(document.selectFirst("link[itemprop=thumbnailUrl]")?.attr("href"))
            ?: fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))

        val description = document.selectFirst("div.pc-comments-tabs__panel[data-panel=description] p")?.text()?.trim()
        ?: document.selectFirst("meta[itemprop=description]")?.attr("content")

        val tags = document.select("div.post-tags-v2-inside a, ul.post-categories li a").map { it.text() }.distinct()

        val score = document.selectFirst("span.pc-info-box__stat-item:contains(%)")?.text()?.replace("%", "")?.trim()

        val tarih = document.selectFirst("meta[itemprop=datePublished]")?.attr("content")
        val year = tarih?.split("-")?.firstOrNull()?.toIntOrNull()

        val recommendations = document.select("div.videos div.video").mapNotNull { it.tomainpageresult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = postr
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from100(score)
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val videosource = document.selectFirst("div.myvideo video source")?.attr("src")
        val links = mutableListOf<Triple<String, String, Int>>()
        if (videosource.isNullOrBlank()) {
            val scriptsource = document.select("script[type=application/ld+json]")
                .map { it.data() }.firstOrNull { it.contains("embedUrl") }
            val embedurl = if (scriptsource != null) {
                Regex("""["']embedUrl["']\s*:\s*["']([^"']+)["']""").find(scriptsource)?.groupValues?.get(1)
            } else {
                document.selectFirst("div.post-embed iframe")?.attr("src") }
            if (!embedurl.isNullOrBlank() && !embedurl.contains("/embed/0")) {
                val iframeresponse = app.get(embedurl, referer = mainUrl).document
                iframeresponse.select("video source, source").forEach { source ->
                    val link = source.attr("src").trim()
                    val res = source.attr("res").toIntOrNull() ?: 480
                    if (link.isNotBlank()) links.add(Triple(link, embedurl, res))
                }
            }
        } else {
            links.add(Triple(videosource, data, 480))
        }
        links.forEach { (url, ref, res) ->
            callback(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = url,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.referer = ref
                    this.quality = res
                }
            )
        }

        return true
    }
}
