package com.kraptor

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class SimpCity(private val plugin: SimpCityPlugin) : MainAPI() {
    override var mainUrl              = "https://simpcity.cr"
    override var name                 = "SimpCity"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    private val username get() = Settings.getUsername(SimpCityPlugin.appContext)
    private val password get() = Settings.getPassword(SimpCityPlugin.appContext)

    private val PAGES_TO_LOAD get() = Settings.getPages(SimpCityPlugin.appContext)

    override val mainPage = mainPageOf(
        "${mainUrl}/forums/onlyfans.8" to "OnlyFans",
        "${mainUrl}/forums/patreon.9" to "Patreon",
        "${mainUrl}/forums/instagram.12" to "Instagram",
        "${mainUrl}/forums/tiktok.10" to "Tiktok",
        "${mainUrl}/forums/youtube.13" to "Youtube",
    )

    private suspend fun ensureAuth(): String {
        val saved = getSimpCookie()
        if (saved.isNotEmpty() && saved.contains("_user=")) return saved
        return simpLogin(username, password, forceRefresh = true)
    }

    private suspend fun authedGetDoc(url: String): org.jsoup.nodes.Document {
        val cookies = ensureAuth()
        if (cookies.isEmpty()) throw ErrorLoadingException("Giriş yapılamadı!")

        try {
            val doc = app.get(url, headers = mapOf("Cookie" to cookies)).document
            if (!isLoginPage(doc)) return doc
        } catch (e: Exception) {
        }

        val newCookies = simpLogin(username, password, forceRefresh = true)
        if (newCookies.isEmpty()) throw ErrorLoadingException("Giriş yapılamadı!")

        return app.get(url, headers = mapOf("Cookie" to newCookies)).document
    }

    private fun isLoginPage(doc: org.jsoup.nodes.Document): Boolean {
        return doc.selectFirst("[data-template=login]") != null
                || doc.selectFirst("[data-logged-in=false]") != null
    }

    private suspend fun getSearchId(query: String): String {
        val searchFormDoc = authedGetDoc("$mainUrl/search/")
        val xfToken = searchFormDoc.select("html").attr("data-csrf").ifEmpty {
            searchFormDoc.select("input[name=_xfToken]").attr("value")
        }

        if (xfToken.isEmpty()) {
            throw ErrorLoadingException("Arama için gerekli _xfToken bulunamadı!")
        }

        val postData = mapOf(
            "keywords" to query,
            "c[users]" to "",
            "_xfToken" to xfToken
        )

        val cookies = ensureAuth()
        if (cookies.isEmpty()) throw ErrorLoadingException("Giriş yapılamadı!")

        var response = try {
            app.post("$mainUrl/search/search", headers = mapOf("Cookie" to cookies), data = postData)
        } catch (e: Exception) {
            null
        }

        if (response == null || isLoginPage(response.document)) {
            val newCookies = simpLogin(username, password, forceRefresh = true)
            if (newCookies.isEmpty()) throw ErrorLoadingException("Giriş yapılamadı!")
            response = app.post("$mainUrl/search/search", headers = mapOf("Cookie" to newCookies), data = postData)
        }

        val finalUrl = response.headers["Location"] ?: response.headers["location"] ?: response.url
        val match = ".*/search/(\\d+)".toRegex().find(finalUrl)

        return match?.groupValues?.get(1) ?: throw ErrorLoadingException("Arama ID'si alınamadı!")
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = authedGetDoc(
            if (page == 1) "${request.data}/" else "${request.data}/page-$page"
        )
        val home = doc.select("div.structItemContainer-group div.structItem:not(.is-prefix1)")
            .mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun org.jsoup.nodes.Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a.avatar")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a.avatar")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("style")
                ?.substringAfter("url(")?.substringBefore(")")
        )
        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val searchId = getSearchId(query)
        val searchUrl = if (page == 1) {
            "${mainUrl}/search/$searchId/?q=$query&o=date"
        } else {
            "${mainUrl}/search/$searchId/?page=$page&q=$query&o=date"
        }
        val doc = authedGetDoc(searchUrl)
        return newSearchResponseList(
            doc.select("div.contentRow").mapNotNull { it.toSearchResult() },
            hasNext = doc.select(".pageNav-jump--next").isNotEmpty() 
        )
    }

    private fun org.jsoup.nodes.Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a.avatar")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a.avatar")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("style")
                ?.substringAfter("url(")?.substringBefore(")")
        )
        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    private val imageExtRegex = Regex("""\.(jpe?g|png|gif|webp)(\?[^"\s<>]*)?$""", RegexOption.IGNORE_CASE)
    private val videoExtRegex = Regex("""\.(mp4|m4v|m3u8)(\?[^"\s<>]*)?$""", RegexOption.IGNORE_CASE)
    private val imageCdnRegex = Regex("""/images\d*/""")

    private fun extractImages(doc: org.jsoup.nodes.Document): List<String> {
        val rawUrls = mutableListOf<String>()

        doc.select("img.bbImage").forEach { el ->
            val dataUrl = el.attr("data-url").ifBlank { null }
            val srcUrl = el.attr("src").ifBlank { null }
            val url = dataUrl ?: srcUrl
            if (url != null && url.isNotBlank()) {
                rawUrls.add(ImageUrlFilter.upgradeToFullQuality(url))
            }
        }

        doc.select("a.link--external").forEach { el ->
            val href = el.attr("href")
            if (imageExtRegex.containsMatchIn(href)) {
                rawUrls.add(ImageUrlFilter.upgradeToFullQuality(href))
            }
        }

        return ImageUrlFilter.filterFullQuality(rawUrls.distinct())
    }

    private fun extractVideos(doc: org.jsoup.nodes.Document): List<String> {
        val directVideos = videoExtRegex.findAll(doc.toString())
            .map { it.value }
            .distinctBy { it.substringBefore("?") }
            .filter { vUrl -> !imageCdnRegex.containsMatchIn(vUrl) }
            .toList()

        val iframeVideos = doc.select("iframe.saint-iframe")
            .mapNotNull { it.attr("src").ifBlank { null } }

        return iframeVideos + directVideos
    }

    override suspend fun load(url: String): LoadResponse? {
        val firstDoc = authedGetDoc(url)
        val totalPages = firstDoc.selectFirst(
            "div.block-outer-main li.pageNav-page:last-child"
        )?.text()?.toIntOrNull() ?: 1

        val title       = firstDoc.selectFirst("h1")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(firstDoc.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = firstDoc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags        = firstDoc.selectFirst("a.labelLink span").let { el ->
            firstDoc.select("a.labelLink span").map { it.text() }
        }

        val pagesToLoad = minOf(PAGES_TO_LOAD, totalPages)
        val startPage   = maxOf(1, totalPages - pagesToLoad + 1)

        val allImages = mutableListOf<String>()
        val allVideos = mutableListOf<String>()

        for (page in totalPages downTo startPage) {
            val doc = if (page == totalPages) firstDoc else authedGetDoc("${url}page-$page")
            allImages.addAll(extractImages(doc))
            allVideos.addAll(extractVideos(doc))
        }

        val allEpisodes = mutableListOf<Episode>()
        var globalIndex = 0

        if (allImages.isNotEmpty()) {
            val galleryData = "IMAGES::" + title + "::" + allImages.joinToString("||")
            allEpisodes.add(newEpisode(galleryData) {
                this.name = "Galeri (${allImages.size} fotoğraf)"
                this.season = 1
                this.episode = 1
            })
            globalIndex++
        }

        allVideos.forEach { videoUrl ->
            val seasonNum = (globalIndex / 25) + 1
            val episodeNum = (globalIndex % 25) + 1
            
            allEpisodes.add(newEpisode(videoUrl) {
                this.season = seasonNum
                this.episode = episodeNum
            })
            globalIndex++
        }

        val maxSeason = if (globalIndex == 0) 1 else (globalIndex - 1) / 25 + 1
        val seasonNamesList = (1..maxSeason).map { SeasonData(it, "Sezon $it") }

        return newTvSeriesLoadResponse(title, url, TvType.NSFW, allEpisodes) {
            this.posterUrl   = poster
            this.plot        = description
            this.tags        = tags
            this.seasonNames = seasonNamesList
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.contains("IMAGES::")) {
            val content = data.substringAfter("IMAGES::")
            val threadTitle = content.substringBefore("::")
            val imagesPart = content.substringAfter("::")
            val images = imagesPart.split("||")
            try {
                plugin.loadGallery(threadTitle, images)
            } catch (e: Exception) {
            }
        } else {
            loadExtractor(data, subtitleCallback, callback)
        }
        return true
    }
}
