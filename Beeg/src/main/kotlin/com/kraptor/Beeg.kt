// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.util.Locale

class Beeg : MainAPI() {
    override var mainUrl = "https://beeg.com"
    override var name = "Beeg"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val instantLinkLoading = true
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val mapper = jacksonObjectMapper().registerKotlinModule()
    private val apiBeeg = "https://store.externulls.com"

    private val headerlar = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:157.0) Gecko/20100101 Firefox/157.0",
        "Origin" to "https://beeg.com",
        "Referer" to "https://beeg.com/",
        "Accept" to "application/json, text/plain, */*",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7"
    )

    private val categories = listOf(
        "index" to "Main Page",
        "WowGirls" to "Wow Girls",
        "BrattySis" to "Bratty Sis",
        "NubilesPorn" to "Nubiles Porn",
        "AdultTime" to "Adult Time",
        "UltraFilms" to "Ultra Films",
        "Blacked" to "Blacked",
        "NubileFilms" to "Nubile Films",
        "LetsDoeIt" to "LetsDoeIt!",
        "Tiny4K" to "Tiny 4K",
        "NaughtyAmerica" to "Naughty America",
        "FamilyXXX" to "Family XXX",
        "VixenCom" to "Vixen",
        "NewSensations" to "New Sensations",
        "PureTaboo" to "Pure Taboo",
        "StepSiblingsCaught" to "Step Siblings Caught",
        "MyFriendsHotMom" to "My Friend's Hot Mom",
        "DorcelClub" to "Dorcel Club",
        "PornForce" to "Porn Force",
        "MomsTeachSex" to "Moms Teach Sex",
        "BareBackStudios" to "Bare Back Studios",
        "PassionHD" to "Passion HD",
        "MyFamilyPies" to "My Family Pies",
        "HotWifeXXX" to "Hot Wife XXX",
        "21Naturals" to "21 Naturals",
        "TeenFidelity" to "Teen Fidelity",
        "NFBusty" to "NF Busty",
        "PornWorld" to "Porn World",
        "Tushy" to "Tushy",
        "Anal" to "Anal",
        "Japanese" to "Japanese",
        "BigTits" to "BigTits",
        "BigAss" to "BigAss",
        "MILF" to "MILF",
        "Lesbian" to "Lesbian",
        "POV" to "POV",
        "Creampie" to "Creampie",
        "Blowjob" to "Blowjob",
        "Hardcore" to "Hardcore",
        "Squirting" to "Squirting",
        "Russian" to "Russian",
        "LongerFull" to "LongerFull",
        "AsianGirl" to "AsianGirl",
        "Compilation" to "Compilation",
        "3some" to "3some",
        "Stockings" to "Stockings",
        "Deepthroat" to "Deepthroat",
        "Latina" to "Latina",
        "Babe" to "Babe",
        "Cumshot" to "Cumshot",
        "Gangbang" to "Gangbang",
        "Cosplay" to "Cosplay",
        "Masturbation" to "Masturbation",
        "Cuckold" to "Cuckold",
        "Lingerie" to "Lingerie",
        "Indian" to "Indian",
        "NaturalTits" to "NaturalTits",
        "Redhead" to "Redhead",
        "Solo" to "Solo",
        "FemaleOrgasm" to "FemaleOrgasm",
        "DP" to "DP",
        "Schoolgirl" to "Schoolgirl",
        "BBC" to "BBC",
        "Homemade" to "Homemade",
        "Classic" to "Classic",
        "Blonde" to "Blonde",
        "BDSM" to "BDSM",
        "Skinny" to "Skinny",
        "Cowgirl" to "Cowgirl",
        "Taboo" to "Taboo",
        "Public" to "Public",
        "Interracial" to "Interracial",
        "Orgy" to "Orgy",
        "MatureWoman" to "MatureWoman",
        "OldYoung" to "OldYoung"
    )

    override val mainPage = mainPageOf(
        "actors" to "Actors",
        *categories.map { (slug, title) ->
            "$apiBeeg/tag/videos/$slug?limit=48&offset=" to title
        }.toTypedArray()
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = if (request.data == "actors") {
            val responseString = app.get("$apiBeeg/tag/recommends?type=person&slug=index", headers = headerlar).text
            runCatching { mapper.readValue<List<ApiPersonRecommend>>(responseString) }
                .getOrNull().orEmpty()
                .mapNotNull { it.toSearchResponse() }
        } else {
            val responseString = app.get("${request.data}${page * 48}", headers = headerlar).text
            mapper.readValue<List<ApiCevap>>(responseString).mapNotNull { it.toMainPageResult() }
        }

        return newHomePageResponse(HomePageList(request.name, items, true))
    }

    private fun ApiCevap.toMainPageResult(): SearchResponse? {
        val firstContent = this.file?.data?.firstOrNull()
        val title = firstContent?.cdValue ?: return null
        val fileId = this.file.id?.toString() ?: firstContent.cdFile?.toString() ?: return null
        val posterUrl = "https://thumbs.externulls.com/videos/$fileId/49.webp?size=480x270"
        val apiDataJson = mapper.writeValueAsString(this.file)
        val apiTagsJson = mapper.writeValueAsString(this.tags ?: emptyList<ApiTag>())

        return newMovieSearchResponse(
            title,
            "$apiDataJson|:$posterUrl|:$title|:$apiTagsJson",
            TvType.NSFW
        ) {
            this.posterUrl = posterUrl
        }
    }

    private fun ApiPersonRecommend.toSearchResponse(): SearchResponse? {
        val name = this.tgName ?: return null
        val slug = this.tgSlug ?: return null

        return newMovieSearchResponse(name, "$mainUrl/$slug", TvType.NSFW) {
            this.posterUrl = extractActorPoster(thumbs).orEmpty()
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        Log.d("beeg", "search query: $query")

        val response = runCatching {
            app.get("$apiBeeg/tag/recommends?type=person&slug=index", headers = headerlar).text
        }.getOrNull() ?: return newSearchResponseList(emptyList(), hasNext = false)

        val results = runCatching { mapper.readValue<List<ApiPersonRecommend>>(response) }
            .getOrNull().orEmpty()
            .filter { it.tgName?.contains(query, ignoreCase = true) == true }
            .mapNotNull { it.toSearchResponse() }

        return newSearchResponseList(results, hasNext = false)
    }

    override suspend fun load(url: String): LoadResponse? {
        Log.d("beeg", "load url: $url")

        if (url.contains("|:")) {
            val parts = url.split("|:")
            val apiData = parts[0]
            val poster = parts.getOrNull(1)
            val title = parts.getOrNull(2) ?: "Video"
            val apiTagsJson = parts.getOrNull(3)

            val node = runCatching { mapper.readValue<ApiFile>(apiData) }.getOrNull()
            val fileId = node?.id?.toString()
                ?: runCatching { mapper.readTree(apiData).get("data")?.get(0)?.get("cd_file")?.asText() }.getOrNull()

            val actorsList = mutableListOf<ActorData>()
            val tagsList = mutableListOf<String>()
            var plotText = title
            var durationSec = node?.durationSeconds
            var firstSlug: String? = null

            if (!fileId.isNullOrBlank()) {
                val facts = runCatching {
                    mapper.readValue<ApiCevap>(app.get("$apiBeeg/facts/file/$fileId?tag=27173", headers = headerlar).text)
                }.getOrNull()

                durationSec = durationSec ?: facts?.file?.durationSeconds
                facts?.file?.data?.firstOrNull()?.cdValue?.takeIf { it.isNotBlank() }?.let { plotText = it }

                facts?.tags?.forEach { tag ->
                    val name = tag.tgName
                    val isPerson = tag.isPerson == true
                    if (name.isNullOrBlank()) return@forEach
                    if (!isPerson && (name.startsWith("1080p") || name.contains("Media") || name == "Index" || name == "Intro")) return@forEach

                    if (isPerson) {
                        actorsList.add(ActorData(Actor(name, extractActorPoster(tag.thumbs))))
                    } else {
                        tagsList.add(name.replace("{", "").replace("}", ""))
                    }
                    if (firstSlug == null && !tag.tgSlug.isNullOrBlank()) firstSlug = tag.tgSlug
                }
            }

            if (tagsList.isEmpty() && !apiTagsJson.isNullOrBlank()) {
                runCatching { mapper.readValue<List<TagData>>(apiTagsJson) }.getOrNull().orEmpty().forEach { tagData ->
                    tagData.data.orEmpty().forEach { tag ->
                        tag.tdValue?.split(",", ".")?.map { it.trim() }?.filter { it.isNotEmpty() }?.let(tagsList::addAll)
                    }
                }
            }

            val recsList = firstSlug?.takeIf { it.isNotBlank() }?.let { slug ->
                runCatching {
                    mapper.readValue<List<ApiCevap>>(
                        app.get("$apiBeeg/tag/videos/$slug?limit=24&offset=0", headers = headerlar).text
                    ).mapNotNull { it.toMainPageResult() }.filter { it.name != title }
                }.getOrNull()
            }.orEmpty()

            return newMovieLoadResponse(title, apiData, TvType.NSFW, apiData) {
                this.posterUrl = poster
                this.plot = plotText
                this.tags = tagsList.distinct()
                this.actors = actorsList.distinctBy { it.actor.name }
                durationSec?.takeIf { it > 0 }?.let { this.duration = it / 60 }
                this.recommendations = recsList
            }
        }

        val slug = url.removeSuffix("/").substringAfterLast("/")
        val title = runCatching { app.get(url, headers = headerlar).document }.getOrNull()?.selectFirst("h1")?.text()
            ?: slug.replaceFirstChar { it.uppercase() }

        val allEpisodes = mutableListOf<Episode>()

        for (i in 0..10) {
            val jsonRes = runCatching {
                app.get("$apiBeeg/tag/videos/$slug?limit=48&offset=${i * 48}", headers = headerlar).text
            }.getOrNull()
            if (jsonRes.isNullOrBlank() || jsonRes == "[]") break

            val videoList = runCatching { mapper.readValue<List<Map<String, Any>>>(jsonRes) }.getOrNull().orEmpty()
            if (videoList.isEmpty()) break

            val pageEpisodes = videoList.mapNotNull { video ->
                val fileObj = video["file"] as? Map<*, *> ?: return@mapNotNull null
                val firstData = (fileObj["data"] as? List<*>)?.getOrNull(0) as? Map<*, *>
                val videoId = (video["id"] ?: fileObj["id"])?.toString() ?: return@mapNotNull null
                val epDataJson = runCatching { mapper.writeValueAsString(fileObj) }.getOrNull() ?: return@mapNotNull null
                val durationSec = fileObj["fl_duration"]?.toString()?.toIntOrNull() ?: 0

                newEpisode(epDataJson) {
                    this.name = firstData?.get("cd_value")?.toString() ?: "Video"
                    this.posterUrl = "https://thumbs.externulls.com/videos/$videoId/0.webp?size=480x270"
                    if (durationSec > 0) {
                        this.description = "${durationSec / 60}:${String.format(Locale.US, "%02d", durationSec % 60)}"
                    }
                }
            }

            allEpisodes.addAll(pageEpisodes)
            if (pageEpisodes.size < 48) break
        }

        if (allEpisodes.isEmpty()) return null

        return newTvSeriesLoadResponse(title, url, TvType.NSFW, allEpisodes) {
            this.posterUrl = allEpisodes.randomOrNull()?.posterUrl
            this.plot = title
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("beeg", "loadLinks data: $data")

        val rawData = data.substringBefore("|")
        val videoId = if (rawData.trim().startsWith("{")) {
            runCatching { mapper.readValue<ApiFile>(rawData).id?.toString() }.getOrNull()
                ?: runCatching { mapper.readTree(rawData).get("data")?.get(0)?.get("cd_file")?.asText() }.getOrNull()
                ?: runCatching { mapper.readTree(rawData).get("cd_file")?.asText() }.getOrNull()
        } else {
            rawData.trim().ifEmpty { null }
        }

        if (videoId.isNullOrBlank()) return false

        val playUrlPath = runCatching {
            app.get("$apiBeeg/video/play_url/$videoId", headers = headerlar).text.trim().trim('"')
        }.getOrNull()

        if (playUrlPath.isNullOrBlank() || playUrlPath.contains("error")) return false

        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                "https://video.beeg.com/$playUrlPath",
                ExtractorLinkType.M3U8
            ) {
                this.referer = "$mainUrl/"
            }
        )
        return true
    }

    private fun extractActorPoster(thumbs: List<ApiThumb>?): String? {
        val firstThumb = thumbs?.firstOrNull() ?: return null
        val thumbId = firstThumb.id ?: return null
        val cropId = firstThumb.crops?.firstOrNull()?.id ?: return null
        return "https://thumbs.externulls.com/photos/$thumbId/to.webp?crop_id=$cropId&size_new=112x112"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ApiCevap(
    val file: ApiFile? = null,
    val tags: List<ApiTag>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ApiFile(
    val id: Long? = null,
    val data: List<ApiContent>? = null,
    @JsonProperty("fl_duration") val durationSeconds: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ApiContent(
    @JsonProperty("cd_file") val cdFile: Any? = null,
    @JsonProperty("cd_value") val cdValue: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ApiTag(
    val id: Long? = null,
    @JsonProperty("is_person") val isPerson: Boolean? = false,
    @JsonProperty("tg_name") val tgName: String? = null,
    @JsonProperty("tg_slug") val tgSlug: String? = null,
    val thumbs: List<ApiThumb>? = null,
    val data: List<ApiTagData>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ApiThumb(
    val id: Long? = null,
    val crops: List<ApiCrop>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ApiCrop(
    val id: Long? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ApiTagData(
    @JsonProperty("td_value") val tdValue: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ApiPersonRecommend(
    @JsonProperty("tg_name") val tgName: String? = null,
    @JsonProperty("tg_slug") val tgSlug: String? = null,
    val thumbs: List<ApiThumb>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TagData(
    val data: List<ApiTagData>? = null
)