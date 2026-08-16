// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.serialization.json.Json

class NetFapX : MainAPI() {
    override var mainUrl = "https://netfapx.com"
    override var name = "NetFapX"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}" to "All Porn Videos",
        "${mainUrl}/tag/step-mom" to "Step Mom Porn Videos",
        "${mainUrl}/tag/step-sister" to "Step Sister Porn Videos",
        "${mainUrl}/category/milf" to "Milf Porn Videos",
        "${mainUrl}/category/big-ass" to "Big Ass Porn Videos",
        "${mainUrl}/category/big-tits" to "Big Tits Porn Videos",
        "${mainUrl}/category/asian" to "Asian Porn Videos",
        "${mainUrl}/tag/bikini" to "Bikini Porn Videos",

        )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home = document.select("article").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val href = fixUrlNull(aTag.attr("href")) ?: return null

        val img = aTag.selectFirst("img")
        val title = img?.attr("alt")?.trim() ?: return null
        val posterUrl = fixUrlNull(img?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {

        val url = "$mainUrl/page/$page/?s=$query"
        val document = app.get(url).document
        val aramaCevap = document.select("article").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val href = fixUrlNull(aTag.attr("href")) ?: return null

        val img = aTag.selectFirst("img")
        val title = img?.attr("alt")?.trim() ?: return null
        val posterUrl = fixUrlNull(img?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val jsonData = document.selectFirst("script.aioseo-schema")?.data() ?: return null

        val json = try {
            mapper.readValue<Map<String, Any>>(jsonData)
        } catch (e: Exception) {
            null
        } ?: return null
        val graph = json["@graph"] as? List<*> ?: return null
        val article = graph.mapNotNull { it as? Map<*, *> }
            .firstOrNull { it["@type"] == "Article" } ?: return null

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull((article["image"] as? Map<*, *>)?.get("url")?.toString())

        val description =
            document.selectFirst("div.textbox h2:contains(Description:) + div")?.selectFirst("p")
                ?.text()?.trim()

        val actors = document.select("div.infovideo h2:contains(Pornstars:) + p a").map {
            Actor(it.text().trim())
        }

        val categories =
            document.select("div.infovideo h2:contains(Categories:) + p a").map { it.text().trim() }
        val tagsFromHtml =
            document.select("div.infovideo h2:contains(Tags:) + p a").map { it.text().trim() }
        val tags = (categories + tagsFromHtml).distinct()
        val recommendations = document.select("article").mapNotNull {
            it.toRecommendationResult()
        }
        val year = article["datePublished"]?.toString()?.split("-")?.firstOrNull()?.toIntOrNull()

        val durationString = tags.find { it.contains(":") }
        val duration = durationString?.let {
            val parts = it.split(":").mapNotNull { p -> p.toIntOrNull() }
            when (parts.size) {
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                2 -> parts[0] * 60 + parts[1]
                1 -> parts[0]
                else -> null
            }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val href = fixUrlNull(aTag.attr("href")) ?: return null

        val img = aTag.selectFirst("img")
        val title = img?.attr("alt")?.trim() ?: return null
        val posterUrl = fixUrlNull(img?.attr("src"))

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
        val document = app.get(data).document
        val scriptContent = document.selectFirst("script#wp-postviews-cache-js-extra")?.data()
            ?: return false

        val postId = "\"postId\":\"(\\d+)\"".toRegex().find(scriptContent)?.groupValues?.get(1)
            ?: return false

        val ajaxResponse = app.post(
            url = "$mainUrl/wp-admin/admin-ajax.php",
            data = mapOf(
                "action" to "get_video_url",
                "idpost" to postId
            ),
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to data,
                "Origin" to mainUrl
            )
        )

        val videoUrl = ajaxResponse.text.trim().takeIf { it.startsWith("http") }
            ?: return false

        callback.invoke(
            newExtractorLink(
                name = "NetFapX",
                source = "NetFapX",
                url = videoUrl,
                type = ExtractorLinkType.VIDEO
            ) {
                this.referer = data
            }
        )

        return true
    }
}