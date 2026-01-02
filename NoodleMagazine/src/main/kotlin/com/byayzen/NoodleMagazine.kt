package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.google.gson.Gson

class NoodleMagazine : MainAPI() {
    override var mainUrl = "https://noodlemagazine.com"
    override var name = "NoodleMagazine"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pages = mutableListOf<HomePageList>()

        if (page <= 1) {
            try {
                val tagsDoc = app.get("$mainUrl/video/a").document
                tagsDoc.select("div.tags-scroll a").take(10).forEach { tag ->
                    val tagTitle = tag.text()
                    val tagHref = fixUrl(tag.attr("href"))
                    val items = app.get(tagHref).document.select("div.item").mapNotNull { it.toRes() }
                    if (items.isNotEmpty()) pages.add(HomePageList(tagTitle, items))
                }
            } catch (e: Exception) { }
        }

        if (pages.isEmpty() || page > 1) {
            val latestUrl = if (page > 1) "$mainUrl/video/?p=${page - 1}" else "$mainUrl/video/"
            val latestItems = app.get(latestUrl).document.select("div.item").mapNotNull { it.toRes() }
            pages.add(HomePageList("Latest Videos", latestItems))
        }

        return newHomePageResponse(pages, true)
    }

    private fun Element.toRes(): SearchResponse? {
        val a = this.selectFirst("a") ?: return null
        val title = this.selectFirst("div.title, div.title a")?.text()
            ?: this.selectFirst("img")?.attr("alt")
            ?: return null

        val img = this.selectFirst("img")
        val poster = fixUrlNull(img?.attr("data-src")?.ifBlank { null } ?: img?.attr("src"))
        val href = fixUrl(a.attr("href"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val q = query.trim().replace(" ", "-")
        val url = if (page > 1) "$mainUrl/video/$q?p=${page - 1}" else "$mainUrl/video/$q"
        val res = app.get(url).document.select("div.item").mapNotNull { it.toRes() }
        return newSearchResponseList(res, res.isNotEmpty())
    }

    override suspend fun load(url: String): LoadResponse? {
        val d = app.get(url).document
        val t = d.selectFirst("h1")?.text()?.trim() ?: return null
        return newMovieLoadResponse(t, url, TvType.NSFW, url) {
            posterUrl = fixUrlNull(d.selectFirst("meta[property=og:image]")?.attr("content"))
            plot = d.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
            recommendations = d.select("div.item").mapNotNull { it.toRes() }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        sub: (SubtitleFile) -> Unit,
        cb: (ExtractorLink) -> Unit
    ): Boolean {
        val text = app.get(data).text
        return Regex("""window\.playlist\s*=\s*(\{.*?\});""").find(text)?.let { m ->
            try {
                Gson().fromJson(m.groupValues[1], Playlist::class.java).sources.forEach { s ->
                    cb(newExtractorLink("Noodle", "Noodle", s.file, INFER_TYPE) {
                        this.referer = "$mainUrl/"
                        this.quality = s.label.filter { it.isDigit() }.toIntOrNull() ?: Qualities.Unknown.value
                    })
                }
                true
            } catch (e: Exception) { false }
        } ?: false
    }

    data class Playlist(val sources: List<VideoSource>)
    data class VideoSource(val file: String, val label: String)
}