// ! Bu araç @kerimmkirac ve @Kraptor123 tarafından | @@Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class HotLeak : MainAPI() {
    override var mainUrl = "https://hotleak.vip"
    override var name = "HotLeak"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        
        "$mainUrl/creators" to "Hepsi",
        "$mainUrl/hot" to "Hot",
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?page=$page").document
        val home = document.select("div.item").mapNotNull {
            it.toMainPageResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val aTag = selectFirst("a") ?: return null
        val href = fixUrlNull(aTag.attr("href")) ?: return null
        if (href.contains("energizeio.com")) return null
        val title = selectFirst("div.movie-name > h3")?.text()?.trim() ?: return null
        val posterUrl = fixUrlNull(selectFirst("img.post-thumbnail")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("$mainUrl/search?search=$query&page=$page").document

        val aramaCevap = document.select("div.item").mapNotNull { it.toSearchResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = selectFirst("a") ?: return null
        val href = fixUrlNull(aTag.attr("href")) ?: return null
        if (href.contains("energizeio.com")) return null
        val title = selectFirst("div.movie-name > h3")?.text()?.trim() ?: return null
        val posterUrl = fixUrlNull(selectFirst("img.post-thumbnail")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }



    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.actor-name > h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("img.model-thumbnail")?.attr("src"))
        val plot = document.selectFirst("div.actor-movie > span")?.text()?.trim()
        val actor = listOf(Actor(title))

        val standardRecommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val suggestedRecommendations = document.select("ul.movies-comming li.movie-item").mapNotNull { it.toRecommendationResult() }
        val recommendations = standardRecommendations + suggestedRecommendations

        val userSlug = url.substringAfterLast("/")

        val cookies = mapOf(
            "qzqz0" to "1"
        )
        val headers = mapOf(
            "x-requested-with" to "XMLHttpRequest",
            "referer" to "$mainUrl/$userSlug/video"
        )
        val episodes = mutableListOf<Episode>()
        coroutineScope {
            val pageRequests = (1..50).map { page ->
                async {
                    val videoListUrl = "$mainUrl/$userSlug?page=$page&type=videos&order=0"
                    app.get(videoListUrl, headers = headers, cookies = cookies)
                        .parsedSafe<List<Map<String, Any>>>()
                }
            }

            for (deferred in pageRequests) {
                val response = deferred.await()
                if (response.isNullOrEmpty()) {
                    break
                }

                response.forEach { video ->
                    val id = video["id"]?.toString() ?: return@forEach
                    val streamUrl = video["stream_url_play"]?.toString() ?: return@forEach
                    val thumb = video["thumbnail"]?.toString()
                    val desc = video["description"]?.toString() ?: ""
                    val pubDate = video["published_date"]?.toString() ?: ""

                    episodes.add(
                        newEpisode(
                            url = "$userSlug|$streamUrl",
                            {
                                this.name = "ID: $id"
                                this.posterUrl = thumb
                                this.description = "Published: $pubDate\nDescription: $desc"
                            }
                        )
                    )
                }
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.NSFW, episodes) {
            this.posterUrl = poster
            this.plot = plot
            this.recommendations = recommendations
            addActors(actor)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val aTag = selectFirst("a") ?: return null
        val href = fixUrlNull(aTag.attr("href")) ?: return null
        val title = selectFirst("span.name")?.text()?.trim() ?: return null
        val posterUrl = fixUrlNull(selectFirst("img.post-thumbnail")?.attr("src"))
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
        val (userSlug, originUrl) = data.split("|")
        val video = originUrl
            .drop(16)
            .dropLast(16)
            .reversed()

        val urlCoz = base64Decode(video)
        val username = userSlug.substringAfterLast("/")

        callback.invoke(
            newExtractorLink(
                name   = "HotLeak $username",
                source = "HotLeak $username",
                url    = urlCoz,
                type   = ExtractorLinkType.M3U8
            ) {
                this.referer = "$mainUrl/"
                this.quality = Qualities.Unknown.value
            }
        )
        return true
    }
}
