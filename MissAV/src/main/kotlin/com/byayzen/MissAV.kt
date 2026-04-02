// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class MissAV : MainAPI() {
    override var mainUrl = "https://missav.live"
    override var name = "MissAV"
    override val hasMainPage = true
    override var lang = "jp"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/dm169/en/weekly-hot?sort=weekly_views" to "Weekly Hot",
        "$mainUrl/dm263/en/monthly-hot?sort=views" to "Monthly Hot",
        "$mainUrl/en/new?sort=published_at" to "Newly Added",
        "$mainUrl/en/english-subtitle" to "English Subtitles",
        "$mainUrl/dm628/en/uncensored-leak" to "Uncensored Leak",
        "$mainUrl/dm150/en/fc2" to "FC2",
        "$mainUrl/dm35/en/madou" to "Madou",
        "$mainUrl/en/klive" to "K-Live",
        "$mainUrl/en/clive" to "C-Live",
        "$mainUrl/dm29/en/tokyohot" to "Tokyo Hot",
        "$mainUrl/dm1198483/en/heyzo" to "HEYZO",
        "$mainUrl/dm2469695/en/1pondo" to "1pondo",
        "$mainUrl/dm3959622/en/caribbeancom" to "Caribbeancom",
        "$mainUrl/dm48032/en/caribbeancompr" to "Caribbeancom Premium",
        "$mainUrl/dm3710098/en/10musume" to "10musume",
        "$mainUrl/dm1342558/en/pacopacomama" to "Pacopacomama",
        "$mainUrl/dm136/en/gachinco" to "Gachinco",
        "$mainUrl/dm29/en/xxxav" to "XXX-AV",
        "$mainUrl/dm24/en/marriedslash" to "Married Slash",
        "$mainUrl/dm20/en/naughty4610" to "Naughty 4610",
        "$mainUrl/dm22/en/naughty0930" to "Naughty 0930"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val separator = if (request.data.contains("?")) "&" else "?"
        val url = "${request.data}${separator}page=$page"

        val document = app.get(url).document

        val home = document.select("div.grid.grid-cols-2 > div, div.thumbnail.group")
            .mapNotNull { it.toMainPageResult() }
            .distinctBy { it.url }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val link = selectFirst("a[href*='/en/'], a[href*='/dm']") ?: return null
        val url = fixUrlNull(link.attr("abs:href")) ?: return null

        val baseTitle = selectFirst("div.my-2 a, div.title a, a.text-secondary")?.text()?.trim()
            ?: link.text().trim()

        if (baseTitle.isBlank()) return null

        val blacklist = listOf("Recent update", "Contact", "Support", "DMCA", "Home")
        if (blacklist.any { baseTitle.equals(it, ignoreCase = true) }) return null

        val isUncensored = (link.attr("alt") + link.attr("href") + this.outerHtml())
            .contains(Regex("uncensored[-_ ]?leak", RegexOption.IGNORE_CASE))

        val title = if (isUncensored && !baseTitle.startsWith("Uncensored - ", ignoreCase = true))
            "Uncensored - $baseTitle" else baseTitle

        val posterUrl = fixUrlNull(
            selectFirst("img")?.let {
                it.attr("abs:data-src").ifEmpty { it.attr("abs:src") }
            }
        )

        if (posterUrl == null) return null

        return newMovieSearchResponse(title, url, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page == 1) {
            "${mainUrl}/en/search/${query}"
        } else {
            "${mainUrl}/en/search/${query}?page=$page"
        }

        val document = app.get(url).document

        val aramaCevap =
            document.select("div.grid.grid-cols-2 > div").mapNotNull { it.toMainPageResult() }


        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.text-base")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        val year = document.selectFirst("time")?.text()?.split("-")?.firstOrNull()?.toIntOrNull()

        val tags = document.select("div.text-secondary:contains(genre) a").map {
            it.text().trim() }
        val actresses = document.select("div.text-secondary:contains(actress) a").map {
            Actor(it.text().trim()) }
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.year = year
            this.tags = tags
            addActors(actresses)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data).text
        getAndUnpack(response).let { unpacked ->
            val playlistId = """/([a-f0-9\-]{36})/""".toRegex().find(unpacked)?.groupValues?.get(1)

            if (playlistId != null) {
                callback.invoke(
                    newExtractorLink(
                        source = "MissAV",
                        name = "MissAV",
                        url = "https://surrit.com/$playlistId/playlist.m3u8",
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = "$mainUrl/"
                        this.headers = mapOf("Referer" to "$mainUrl/")
                    }
                )
            }
        }
        return true
    }
}