// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

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
        val home = document.select("div.col-lg-3 a.video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("img")?.attr("title") ?: return null
        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val zamanText = this.selectFirst("span.play-icon-active2.video-time")?.text()

        if (zamanText != null && zamanText.matches(Regex("^00:(?:[0-1]\\d|20)$"))) {
            return null
        }
        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val apiUrl =
            "https://search-aznude.aznude.workers.dev/initial-search?q=${query}&gender=f&type=null&sortByDate=DESC&dateRange=anytime"
        val jsonString = app.get(apiUrl, referer = "${mainUrl}/").textLarge
        Log.d("kraptor_$name", "jsonString = ${jsonString}")
        try {
            // Parse the JSON
            val wrapper = Json { ignoreUnknownKeys = true }
                .decodeFromString<SearchWrapper>(jsonString)
            Log.d("kraptor_$name", "videos count = ${wrapper.count.videos}")
            Log.d("kraptor_$name", "actual videos list size = ${wrapper.data.videos.size}")

            // Check if videos list is empty
            if (wrapper.data.videos.isEmpty()) {
                Log.d("kraptor_$name", "No videos found in response")
                return emptyList()
            }

            // Process videos with regex filter
            return wrapper.data.videos.mapNotNull { video ->
                val zamanText = video.duration // örn: "00:14"
                Log.d("kraptor_$name", "Processing video: ${video.text}, duration: $zamanText")

                // 00:00–00:20 arasıysa null dön (very short videos)
                if (zamanText.matches(Regex("^00:(?:[0-1]\\d|20)$"))) {
                    Log.d("kraptor_$name", "Filtered out short video: ${video.text} (${zamanText})")
                    return@mapNotNull null
                }

                Log.d("kraptor_$name", "video = ${video.text} url = ${video.url} poster = ${video.thumb}")

                // Create SearchResponse
                newMovieSearchResponse(
                    name = video.text,
                    url = fixUrlNull(video.url)!!,
                    type = TvType.NSFW
                ) {
                    posterUrl = fixUrlNull(video.thumb)
                }
            }
        } catch (e: Exception) {
            Log.e("kraptor_$name", "Error parsing search response", e)
            return emptyList()
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

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


@Serializable
data class SearchWrapper(
    val count: Count,
    val data: Data
)

@Serializable
data class Count(
    val celebs: Int,
    val movies: Int,
    val stories: Int,
    val videos: Int
)

@Serializable
data class Data(
    val celebs: List<Actor>,
    val movies: List<JsonObject> = emptyList(),
    val stories: List<JsonObject> = emptyList(),
    val videos: List<Video> = emptyList() // Make sure this has default empty list
)

@Serializable
data class Video(
    @SerialName("video_id") val id: Long,
    val text: String,
    val thumb: String,
    val url: String,
    val duration: String,   // "mm:ss"
    @SerialName("date_added") val dateAdded: String,
    // Add other fields as needed with defaults
    @SerialName("views_1day") val views1day: Int = 0,
    @SerialName("views_7days") val views7days: Int = 0,
    @SerialName("views_month") val viewsMonth: Int = 0,
    @SerialName("views_3months") val views3months: Int = 0,
    @SerialName("views_6months") val views6months: Int = 0,
    @SerialName("views_year") val viewsYear: Int = 0,
    @SerialName("views_alltime") val viewsAlltime: Int = 0,
    @SerialName("date_modified") val dateModified: String = ""
)

@Serializable
data class Actor(
    @SerialName("celeb_id") val id: Long,
    val text: String,
    val thumb: String,
    val url: String,
    @SerialName("date_added") val dateAdded: String,
    @SerialName("date_modified") val dateModified: String,
    @SerialName("views_1day") val views1day: Int = 0,
    @SerialName("views_7days") val views7days: Int = 0,
    @SerialName("views_month") val viewsMonth: Int = 0,
    @SerialName("views_3months") val views3months: Int = 0,
    @SerialName("views_6months") val views6months: Int = 0,
    @SerialName("views_year") val viewsYear: Int = 0,
    @SerialName("views_alltime") val viewsAlltime: Int = 0
)