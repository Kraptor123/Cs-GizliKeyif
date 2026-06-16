package com.byayzen

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
                    val items =
                        app.get(tagHref).document.select("div.item").mapNotNull { it.toRes() }
                    if (items.isNotEmpty()) pages.add(HomePageList(tagTitle, items, isHorizontalImages = true))
                }
            } catch (e: Exception) {
            }
        }

        if (pages.isEmpty() || page > 1) {
            val latestUrl = if (page > 1) "$mainUrl/video/?p=${page - 1}" else "$mainUrl/video/"
            val latestItems =
                app.get(latestUrl).document.select("div.item").mapNotNull { it.toRes() }
            pages.add(HomePageList("Latest Videos", latestItems, isHorizontalImages = true))
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
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: return null
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            posterUrl = fixUrlNull(doc.selectFirst("meta[property=og:image]")?.attr("content"))
            plot = doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
            recommendations = doc.select("div.item").mapNotNull { it.toRes() }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        sub: (SubtitleFile) -> Unit,
        cb: (ExtractorLink) -> Unit
    ): Boolean {
        val text = app.get(data).text

        val jsonString = Regex(
            """window\.playlist\s*=\s*(\{.*?\});""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        ).find(text)?.groupValues?.get(1) ?: run {
            Log.d(name, "playlist bulunamadı")
            return false
        }

        val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val sources = mapper.readValue(jsonString, PlaylistData::class.java)?.sources ?: run {
            Log.d(name, "parse başarısız")
            return false
        }

        sources.forEach { s ->
            val quality = s.label.filter { it.isDigit() }.toIntOrNull() ?: Qualities.Unknown.value
            cb(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = s.file,
                    type = INFER_TYPE
                ) {
                    this.referer = mainUrl
                    this.quality = quality
                }
            )
        }
        return true
    }

    data class PlaylistData(val sources: List<VideoSource> = emptyList())
    data class VideoSource(val file: String = "", val label: String = "")
    }