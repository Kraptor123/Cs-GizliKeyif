package com.byayzen

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class xVideos : MainAPI() {
    override var mainUrl = "https://www.xvideos.com"
    override var name = "xVideos"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    private fun String.fixUrl(): String {
        return when {
            startsWith("//") -> "https:$this"
            startsWith("/") -> "$mainUrl$this"
            else -> this
        }
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val titleElement = this.selectFirst("p.title a") ?: return null
        val title = titleElement.attr("title").ifBlank { titleElement.text() }
        val originalHref = titleElement.attr("href") ?: return null

        val urlCleanRegex = Regex("^(/video\\.[^/]+)/[^/]+/[^/]+/(.+)$")
        val matchResult = urlCleanRegex.find(originalHref)

        val cleanedHref = if (matchResult != null && matchResult.groupValues.size == 3) {
            "${matchResult.groupValues[1]}/${matchResult.groupValues[2]}"
        } else {
            originalHref
        }

        val fullUrl = cleanedHref.fixUrl()
        val imgTag = this.selectFirst("div.thumb a img")
        val posterUrl = (imgTag?.attr("data-src")?.takeIf { it.isNotBlank() && !it.contains("lightbox-blank") }
            ?: imgTag?.attr("src")?.takeIf { it.isNotBlank() && !it.contains("lightbox-blank") })?.fixUrl()

        return newAnimeSearchResponse(title, fullUrl, TvType.NSFW) {
            this.posterUrl = posterUrl
            if (!posterUrl.isNullOrBlank()) {
                this.posterHeaders = mapOf("Referer" to mainUrl)
            }
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val pageNumber = if (page > 1) "/new/${page - 1}" else ""
        val document = app.get("$mainUrl$pageNumber").document
        val items = document.select("div.mozaique div.thumb-block").mapNotNull { it.toSearchResponse() }

        val homePages = mutableListOf(HomePageList("Videos", items))

        val categories = listOf(
            Pair("Amateur", "/c/Amateur-65"),
            Pair("Anal", "/c/Anal-12"),
            Pair("Big Tits", "/c/Big_Tits-23"),
            Pair("Big Cock", "/c/Big_Cock-34"),
            Pair("Big Ass", "/c/Big_Ass-24"),
            Pair("Creampie", "/c/Creampie-40"),
            Pair("Milf", "/c/Milf-19"),
            Pair("Mature", "/c/Mature-38"),
            Pair("Teen", "/c/Teen-13"),
            Pair("Interracial", "/c/Interracial-27"),
            Pair("Lesbian", "/c/Lesbian-26"),
            Pair("Blowjob", "/c/Blowjob-15"),
            Pair("Cumshot", "/c/Cumshot-18"),
            Pair("Redhead", "/c/Redhead-31"),
            Pair("Brunette", "/c/Brunette-25")
        )

        categories.forEach { (catName, catHref) ->
            val catDocs = app.get("$mainUrl$catHref").document
            val catItems = catDocs.select("div.mozaique div.thumb-block").take(10).mapNotNull { it.toSearchResponse() }
            if (catItems.isNotEmpty()) {
                homePages.add(HomePageList(catName, catItems))
            }
        }

        val hasNextPage = document.selectFirst("div.pagination a.next-page") != null
        return newHomePageResponse(homePages, hasNextPage)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return app.get("$mainUrl/?k=$query").document
            .select("div.mozaique div.thumb-block").mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val d = app.get(url).document
        val title = d.selectFirst("h2.page-title")?.ownText()?.trim()
            ?: d.selectFirst("meta[property=og:title]")?.attr("content") ?: return null

        val poster = d.selectFirst("meta[property=og:image]")?.attr("content")?.fixUrl()
        val description = d.selectFirst("meta[name=description]")?.attr("content")
            ?: d.selectFirst("div.video-description-text")?.text()

        val tags = d.select("div.video-tags-list li a.is-keyword").map { it.text() }.filter { it.isNotBlank() }
        val uploaderName = d.selectFirst("li.main-uploader a.uploader-tag span.name")?.text()

        val recs = mutableListOf<SearchResponse>()
        val scriptContent = d.select("script").find { it.html().contains("var video_related") }?.html()
        if (scriptContent != null) {
            val itemRegex = Regex("""\{\s*"id":\s*\d+.*?,"u"\s*:\s*"(.*?)",\s*"i"\s*:\s*"(.*?)",.*?tf"\s*:\s*"(.*?)".*?\}""")
            itemRegex.findAll(scriptContent).forEach { itemMatch ->
                val recHref = itemMatch.groupValues[1].replace("\\/", "/").fixUrl()
                val recImage = itemMatch.groupValues[2].replace("\\/", "/").fixUrl()
                val recTitle = unescapeUnicode(itemMatch.groupValues[3].replace("\\/", "/"))

                recs.add(newAnimeSearchResponse(recTitle, recHref, TvType.NSFW) {
                    this.posterUrl = recImage
                    if (!this.posterUrl.isNullOrBlank()) this.posterHeaders = mapOf("Referer" to mainUrl)
                })
            }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            if (!this.posterUrl.isNullOrBlank()) this.posterHeaders = mapOf("Referer" to mainUrl)
            this.plot = description
            this.tags = tags
            this.recommendations = recs
            this.duration = d.selectFirst("h2.page-title span.duration")?.text()?.let { parseDuration(it) }
            uploaderName?.let { addActors(listOf(it)) }
        }
    }

    private fun parseDuration(durationString: String): Int? {
        var totalMinutes = 0
        Regex("""(\d+)\s*h""").find(durationString)?.let { totalMinutes += it.groupValues[1].toInt() * 60 }
        Regex("""(\d+)\s*min""").find(durationString)?.let { totalMinutes += it.groupValues[1].toInt() }
        return if (totalMinutes > 0) totalMinutes else null
    }

    private fun unescapeUnicode(str: String): String {
        return Regex("""\\u([0-9a-fA-F]{4})""").replace(str) {
            it.groupValues[1].toInt(16).toChar().toString()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        loadExtractor(data, subtitleCallback, callback)
        return true
    }
}