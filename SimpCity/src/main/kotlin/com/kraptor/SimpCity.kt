package com.kraptor

import com.lagradost.api.Log
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

    private val username = "ksbkb51102"
    private val password = "ksbkb51102"

    private val PAGES_TO_LOAD = 5

    override val mainPage = mainPageOf(
        "${mainUrl}/forums/onlyfans.8" to "OnlyFans",
        "${mainUrl}/forums/patreon.9" to "Patreon",
        "${mainUrl}/forums/instagram.12" to "Instagram",
        "${mainUrl}/forums/tiktok.10" to "Tiktok",
        "${mainUrl}/forums/youtube.13" to "Youtube",
    )

    // ── Auth helpers ────────────────────────────────────────────

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
            Log.d("kraptor_$name", "İstek başarısız, login yenileniyor: ${e.message}")
        }

        val newCookies = simpLogin(username, password, forceRefresh = true)
        if (newCookies.isEmpty()) throw ErrorLoadingException("Giriş yapılamadı!")

        return app.get(url, headers = mapOf("Cookie" to newCookies)).document
    }

    private fun isLoginPage(doc: org.jsoup.nodes.Document): Boolean {
        return doc.selectFirst("[data-template=login]") != null
                || doc.selectFirst("[data-logged-in=false]") != null
    }

    // ── Main page ───────────────────────────────────────────────

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

    // ── Search ──────────────────────────────────────────────────

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val doc = if (page == 1){
            authedGetDoc("${mainUrl}/search/25489314/?q=$query&o=date")
        } else {
            authedGetDoc("${mainUrl}/search/25489314/?page=$page&q=$query&o=date")
        }
        return newSearchResponseList(doc.select("div.contentRow").mapNotNull { it.toSearchResult() }, hasNext = true)
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

    // ── Image extraction helpers ────────────────────────────────

    private val imageExtRegex = Regex("""\.(jpe?g|png|gif|webp)(\?[^"\s<>]*)?$""", RegexOption.IGNORE_CASE)
    private val videoExtRegex = Regex("""\.(mp4|m4v|m3u8)(\?[^"\s<>]*)?$""", RegexOption.IGNORE_CASE)
    private val imageCdnRegex = Regex("""/images\d*/""")

    /** Extract direct image URLs from bbImage <img> tags inside message bodies. */
    private fun extractImages(doc: org.jsoup.nodes.Document): List<String> {
        return doc.select("img.bbImage")
            .mapNotNull { el ->
                // data-url is the canonical full-size URL; fallback to src
                val url = el.attr("data-url").ifBlank { el.attr("src") }
                if (url.isNotBlank() && imageExtRegex.containsMatchIn(url)) url else null
            }
            .distinctBy { it.substringBefore("?") }
    }

    /** Extract video URLs from both direct links and saint-iframe embeds. */
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

    // ── Load ────────────────────────────────────────────────────

    override suspend fun load(url: String): LoadResponse? {
        val firstDoc = authedGetDoc(url)
        val totalPages = firstDoc.selectFirst(
            "div.block-outer-main li.pageNav-page:last-child"
        )?.text()?.toIntOrNull() ?: 1

        val title       = firstDoc.selectFirst("h1")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(firstDoc.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = firstDoc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags        = firstDoc.select("a.labelLink span").map { it.text() }

        val pagesToLoad = minOf(PAGES_TO_LOAD, totalPages)
        val startPage   = maxOf(1, totalPages - pagesToLoad + 1)

        Log.d("kraptor_$name", "Toplam: $totalPages sayfa, yüklenen: $startPage → $totalPages")

        val allEpisodes   = mutableListOf<Episode>()
        val seasonNamesList = mutableListOf<SeasonData>()
        val allImages     = mutableListOf<String>()

        for (page in totalPages downTo startPage) {
            val seasonNumber = totalPages - page + 1
            seasonNamesList.add(SeasonData(season = seasonNumber, name = "Sayfa $page"))

            val doc = if (page == totalPages) firstDoc else authedGetDoc("${url}page-$page")

            // Collect images from this page
            allImages.addAll(extractImages(doc))

            // Collect videos from this page
            val pageVideos = extractVideos(doc)
            allEpisodes.addAll(pageVideos.map { videoUrl ->
                newEpisode(videoUrl) { season = seasonNumber }
            })
        }

        // ── Add image gallery as first episode ──
        if (allImages.isNotEmpty()) {
            val galleryEpisode = newEpisode(
                "IMAGES::" + allImages.joinToString("||")
            ) {
                this.name = "Galeri (${allImages.size} fotoğraf)"
                episode = 1
                season = 1
            }
            allEpisodes.add(0, galleryEpisode)

            // Shift other episode numbers
            allEpisodes.forEachIndexed { idx, ep ->
                if (idx > 0) ep.episode = idx + 1
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.NSFW, allEpisodes) {
            this.posterUrl   = poster
            this.plot        = description
            this.tags        = tags
            this.seasonNames = seasonNamesList
        }
    }

    // ── Load links ──────────────────────────────────────────────

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "loadLinks called, data starts with: ${data.take(30)}")

        if (data.contains("IMAGES::")) {
            val imagesPart = data.substringAfter("IMAGES::")
            val images = imagesPart.split("||")
            Log.d("kraptor_$name", "IMAGES detected! ${images.size} images, calling loadGallery")
            try {
                plugin.loadGallery(images)
            } catch (e: Exception) {
                Log.e("kraptor_$name", "loadGallery FAILED: ${e.message}")
                e.printStackTrace()
            }
        } else {
            loadExtractor(data, subtitleCallback, callback)
        }
        return true
    }
}
