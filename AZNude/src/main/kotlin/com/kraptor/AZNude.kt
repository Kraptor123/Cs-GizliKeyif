// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*


class AZNude : MainAPI() {
    override var mainUrl = "https://www.aznude.com"
    override var name = "AZNude"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/browse/tags/vids/topless/" to "topless",
        "${mainUrl}/browse/tags/vids/undressing/" to "undressing",
        "${mainUrl}/browse/tags/vids/black/" to "black",
        "${mainUrl}/browse/tags/vids/shower/" to "shower",
        "${mainUrl}/browse/tags/vids/pokies/" to "pokies",
        "${mainUrl}/browse/tags/vids/missionary/" to "missionary",
        "${mainUrl}/browse/tags/vids/stripper/" to "stripper",
        "${mainUrl}/browse/tags/vids/latina/" to "latina",
        "${mainUrl}/browse/tags/vids/breastfondling/" to "breast fondling",
        "${mainUrl}/browse/tags/vids/upskirt/" to "upskirt",
        "${mainUrl}/browse/tags/vids/doggystyle/" to "doggy style",
        "${mainUrl}/browse/tags/vids/threesome/" to "threesome",
        "${mainUrl}/browse/tags/vids/groupnudity/" to "group nudity",
        "${mainUrl}/browse/tags/vids/cunnilingus/" to "cunnilingus",
        "${mainUrl}/browse/tags/vids/bottomless/" to "bottomless",
        "${mainUrl}/browse/tags/vids/bbw/" to "BBW",
        "${mainUrl}/browse/tags/vids/milf/" to "milf",
        "${mainUrl}/browse/tags/vids/outdoornudity/" to "outdoor nudity",
        "${mainUrl}/browse/tags/vids/blowjob/" to "blowjob",
        "${mainUrl}/browse/tags/vids/publicnudity/" to "Public Nudity",
        "${mainUrl}/browse/tags/vids/reversecowgirl/" to "reverse cowgirl",
        "${mainUrl}/browse/tags/vids/fingering/" to "fingering",
        "${mainUrl}/browse/tags/vids/labia/" to "labia",
        "${mainUrl}/browse/tags/vids/bouncingboobs/" to "bouncing boobs",
        "${mainUrl}/browse/tags/vids/masturbating/" to "masturbating",
        "${mainUrl}/browse/tags/vids/orgasm/" to "orgasm",
        "${mainUrl}/browse/tags/vids/orgy/" to "orgy",
        "${mainUrl}/browse/tags/vids/indian/" to "indian",
        "${mainUrl}/browse/tags/vids/dildo/" to "dildo",
        "${mainUrl}/browse/tags/vids/roughsex/" to "rough sex",
        "${mainUrl}/browse/tags/vids/skinnydip/" to "skinny dip",
        "${mainUrl}/browse/tags/vids/scissoring/" to "scissoring",
        "${mainUrl}/browse/tags/vids/breastsucking/" to "breast sucking",
        "${mainUrl}/browse/tags/vids/handjob/" to "handjob",
        "${mainUrl}/browse/tags/vids/spanking/" to "spanking",
        "${mainUrl}/browse/tags/vids/penetration/" to "penetration",
        "${mainUrl}/browse/tags/vids/strapon/" to "strap on",
        "${mainUrl}/browse/tags/vids/anus/" to "anus",
        "${mainUrl}/browse/tags/vids/shaved/" to "shaved",
        "${mainUrl}/browse/tags/vids/cum/" to "cum",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page.html").document
        val home = document.select("div.media-list div.media-list-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("img")?.attr("alt") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val zamanText = this.selectFirst("span.video-timestamp")?.text()

        if (zamanText != null && zamanText.matches(Regex("^00:(?:[0-1]\\d|20)$"))) {
            return null
        }
        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }



    override suspend fun search(query: String): List<SearchResponse> {
        val apiUrl = "https://main-aq7es5tiuq-uc.a.run.app/app/cache/initial-search?q=${query}&gender=f&type=null&sortByDate=DESC&dateRange=anytime"
        val jsonString = app.get(apiUrl, referer = "${mainUrl}/").textLarge
        Log.d("kraptor_$name", "jsonString = ${jsonString}")

        return try {
            val mapper = jacksonObjectMapper().registerKotlinModule()
            Log.d("kraptor_$name", "Jackson parsing başladı")
            val searchWrapper: SearchWrapper = mapper.readValue(jsonString)
            Log.d("kraptor_$name", "Jackson parsing başarılı, celebs count: ${searchWrapper.data.celebs.size}")
            val results = mutableListOf<SearchResponse>()

            // Celebs'leri ekle (sadece /view/celeb/ içerenler)
            searchWrapper.data.celebs
                .filter { it.url.contains("/view/celeb/") }
                .forEach { celeb ->
                    val href = fixUrlNull(celeb.url).toString()
                    Log.d("kraptor_$name", "href = ${href}")
                    Log.d("kraptor_$name", "celeb.text = ${celeb.text}")
                    Log.d("kraptor_$name", "celeb.thumb = ${fixUrlNull(celeb.thumb)}")
                    results.add(
                        newMovieSearchResponse(
                            name = celeb.text,
                            url = href,
                            type = TvType.NSFW
                        ) {
                            posterUrl = fixUrlNull("https://cdn2.aznude.com${celeb.thumb}")
                            posterHeaders = mapOf("referer" to "${mainUrl}/")
                        }
                    )
                }

            searchWrapper.data.movies
                .filter { it.url.contains("/view/movie/") }
                .forEach { movies ->
                    val href = fixUrlNull(movies.url).toString()
                    Log.d("kraptor_$name", "href = ${href}")
                    Log.d("kraptor_$name", "celeb.text = ${movies.text}")
                    Log.d("kraptor_$name", "celeb.thumb = ${fixUrlNull(movies.thumb)}")
                    results.add(
                        newMovieSearchResponse(
                            name = movies.text,
                            url = href,
                            type = TvType.NSFW
                        ) {
                            posterUrl = fixUrlNull("https://cdn2.aznude.com${movies.thumb}")
                            posterHeaders = mapOf("referer" to "${mainUrl}/")
                        }
                    )
                }

            // Videos'ları ekle (sadece /view/celeb/ içerenler)
            searchWrapper.data.videos
                .filter { it.url.contains("/view/celeb/") }
                .forEach { video ->
                    val href = fixUrlNull(video.url).toString()
                    Log.d("kraptor_$name", "video href = ${href}")
                    Log.d("kraptor_$name", "video.text = ${video.text}")
                    results.add(
                        newMovieSearchResponse(
                            name = video.text,
                            url = href,
                            type = TvType.NSFW
                        ) {
                            posterUrl = fixUrlNull(video.thumb)
                        }
                    )
                }

            // Stories'leri ekle (sadece /view/celeb/ içerenler)
            searchWrapper.data.stories
                .filter { it.url.contains("/view/celeb/") }
                .forEach { story ->
                    val href = fixUrlNull(story.url).toString()
                    Log.d("kraptor_$name", "story href = ${href}")
                    Log.d("kraptor_$name", "story.text = ${story.text}")
                    results.add(
                        newMovieSearchResponse(
                            name = story.text,
                            url = href,
                            type = TvType.NSFW
                        ) {
                            posterUrl = fixUrlNull(story.thumb)
                        }
                    )
                }

            Log.d("kraptor_$name", "Total results: ${results.size}")
            results
        } catch (e: Exception) {
            Log.e("kraptor_$name", "Jackson parsing error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        if (url.contains("/view/celeb/") || (url.contains("/view/movie/"))) {
            val title = document.selectFirst("h1")?.text() ?: return null
            val poster = fixUrlNull(document.selectFirst("div.single-page-banner_wrapper img")?.attr("src"))
            val score  = document.selectFirst("span.rating-score")?.text()
            val tags = document.select("div.col-md-12 h2.video-tags a").map { it.text() }
            val recommendations = document.select("div.container:nth-child(18) div.col-lg-2").mapNotNull { it.toRecommendationResult() }
            val bolumler        = document.select("div.media-list-item.video-list-item").map { bolum ->
                val bolumHref   = bolum.selectFirst("a")?.attr("href").toString()
                val poster      = bolum.selectFirst("img")?.attr("src")
                val bolumIsim   = bolum.selectFirst("img")?.attr("alt")
                newEpisode(bolumHref,{
                    this.name      = bolumIsim
                    this.posterUrl = poster
                })
            }

            return newTvSeriesLoadResponse(title, url, TvType.NSFW, bolumler) {
                this.posterUrl = poster
                this.plot = "$title +18"
                this.tags = tags
                this.recommendations = recommendations
                this.score = Score.from5(score)
            }
        } else {

            val title = document.selectFirst("meta[name=title]")?.attr("content") ?: return null
            val poster = fixUrlNull(document.selectFirst("link[rel=preload][as=image]")?.attr("href"))
            val description = document.selectFirst("meta[name=description]")?.attr("content")
            val tags = document.select("div.col-md-12 h2.video-tags a").map { it.text() }
            val recommendations = document.select("div.col-lg-3 a.video").mapNotNull { it.toRecommendationResult() }

            return newMovieLoadResponse(title, url, TvType.NSFW, url) {
                this.posterUrl = poster
                this.plot = description
                this.tags = tags
                this.recommendations = recommendations
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("img")?.attr("title") ?: return null
        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val zamanText = this.selectFirst("span.play-icon-active2.video-time")?.text()

        if (zamanText != null && zamanText.matches(Regex("^00:(?:[0-1]\\d|20)$"))) {
            return null
        }

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document
        val scriptElements = document.select("script")

        scriptElements.forEach { script ->
            val scriptContent = script.html()

            if (scriptContent.contains("jwplayer") && scriptContent.contains("setup") && scriptContent.contains("playlist")) {

                val sourcesRegex = """sources:\s*\[\s*(.*?)\s*\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
                val sourcesMatch = sourcesRegex.find(scriptContent)

                sourcesMatch?.let { match ->
                    val sourcesContent = match.groupValues[1]
                    val sourceRegex =
                        """\{\s*file:\s*"([^"]+)",\s*label:\s*"([^"]+)"(?:,\s*default:\s*"true")?\s*\}""".toRegex()
                    val sourceMatches = sourceRegex.findAll(sourcesContent)

                    sourceMatches.forEach { sourceMatch ->
                        val videoUrl = sourceMatch.groupValues[1]
                        val quality = sourceMatch.groupValues[2]

                        // Quality değerini Qualities enum'una çevir
                        val qualityValue = when (quality.uppercase()) {
                            "LQ" -> Qualities.P240.value
                            "HQ" -> Qualities.P480.value
                            "HD" -> Qualities.P720.value
                            "FHD" -> Qualities.P1080.value
                            "4K" -> Qualities.P2160.value
                            else -> Qualities.Unknown.value
                        }

                        callback.invoke(
                            newExtractorLink(
                                source = "AZNude $quality",
                                name = "AZNude $quality",
                                url = videoUrl,
                                type = INFER_TYPE,
                                {
                                    this.quality = qualityValue
                                    this.referer = "${mainUrl}/"
                                }
                        ))
                    }
                }
            }
        }

        return true
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchWrapper(
    val count: Count,
    val data: Data
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Count(
    val celebs: Int,
    val movies: Int,
    val stories: Int,
    val videos: Int
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Data(
    val celebs: List<Actor>,
    val movies: List<Movies> = emptyList(),
    val stories: List<Story> = emptyList(),
    val videos: List<Video> = emptyList()
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Movies(
    val text: String,
    val thumb: String?,
    val url: String,
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class Video(
    val text: String,
    val thumb: String,
    val url: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Actor(
    val text: String,
    val thumb: String,
    val url: String,
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Story(
    val text: String,
    val thumb: String,
    val url: String,
)