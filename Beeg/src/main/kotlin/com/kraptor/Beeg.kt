// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

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

    override val mainPage = mainPageOf(
        "actors" to "Actors",
        "$apiBeeg/facts/tag?slug=WowGirls&limit=48&offset=" to "Wow Girls",
        "$apiBeeg/facts/tag?slug=BrattySis&limit=48&offset=" to "Bratty Sis",
        "$apiBeeg/facts/tag?slug=NubilesPorn&limit=48&offset=" to "Nubiles Porn",
        "$apiBeeg/facts/tag?slug=AdultTime&limit=48&offset=" to "Adult Time",
        "$apiBeeg/facts/tag?slug=UltraFilms&limit=48&offset=" to "Ultra Films",
        "$apiBeeg/facts/tag?slug=Blacked&limit=48&offset=" to "Blacked",
        "$apiBeeg/facts/tag?slug=NubileFilms&limit=48&offset=" to "Nubile Films",
        "$apiBeeg/facts/tag?slug=LetsDoeIt&limit=48&offset=" to "LetsDoeIt!",
        "$apiBeeg/facts/tag?slug=Tiny4K&limit=48&offset=" to "Tiny 4K",
        "$apiBeeg/facts/tag?slug=NaughtyAmerica&limit=48&offset=" to "Naughty America",
        "$apiBeeg/facts/tag?slug=FamilyXXX&limit=48&offset=" to "Family XXX",
        "$apiBeeg/facts/tag?slug=VixenCom&limit=48&offset=" to "Vixen",
        "$apiBeeg/facts/tag?slug=NewSensations&limit=48&offset=" to "New Sensations",
        "$apiBeeg/facts/tag?slug=PureTaboo&limit=48&offset=" to "Pure Taboo",
        "$apiBeeg/facts/tag?slug=StepSiblingsCaught&limit=48&offset=" to "Step Siblings Caught",
        "$apiBeeg/facts/tag?slug=MyFriendsHotMom&limit=48&offset=" to "My Friend's Hot Mom",
        "$apiBeeg/facts/tag?slug=DorcelClub&limit=48&offset=" to "Dorcel Club",
        "$apiBeeg/facts/tag?slug=PornForce&limit=48&offset=" to "Porn Force",
        "$apiBeeg/facts/tag?slug=MomsTeachSex&limit=48&offset=" to "Moms Teach Sex",
        "$apiBeeg/facts/tag?slug=BareBackStudios&limit=48&offset=" to "Bare Back Studios",
        "$apiBeeg/facts/tag?slug=PassionHD&limit=48&offset=" to "Passion HD",
        "$apiBeeg/facts/tag?slug=MyFamilyPies&limit=48&offset=" to "My Family Pies",
        "$apiBeeg/facts/tag?slug=HotWifeXXX&limit=48&offset=" to "Hot Wife XXX",
        "$apiBeeg/facts/tag?slug=21Naturals&limit=48&offset=" to "21 Naturals",
        "$apiBeeg/facts/tag?slug=TeenFidelity&limit=48&offset=" to "Teen Fidelity",
        "$apiBeeg/facts/tag?slug=NFBusty&limit=48&offset=" to "NF Busty",
        "$apiBeeg/facts/tag?slug=PornWorld&limit=48&offset=" to "Porn World",
        "$apiBeeg/facts/tag?slug=Tushy&limit=48&offset=" to "Tushy",
        "$apiBeeg/facts/tag?id=27173&limit=48&offset=" to "Main Page",
        "$apiBeeg/facts/tag?slug=Anal&limit=48&offset=" to "Anal",
        "$apiBeeg/facts/tag?slug=Japanese&limit=48&offset=" to "Japanese",
        "$apiBeeg/facts/tag?slug=BigTits&limit=48&offset=" to "BigTits",
        "$apiBeeg/facts/tag?slug=BigAss&limit=48&offset=" to "BigAss",
        "$apiBeeg/facts/tag?slug=MILF&limit=48&offset=" to "MILF",
        "$apiBeeg/facts/tag?slug=Lesbian&limit=48&offset=" to "Lesbian",
        "$apiBeeg/facts/tag?slug=POV&limit=48&offset=" to "POV",
        "$apiBeeg/facts/tag?slug=Creampie&limit=48&offset=" to "Creampie",
        "$apiBeeg/facts/tag?slug=Blowjob&limit=48&offset=" to "Blowjob",
        "$apiBeeg/facts/tag?slug=Hardcore&limit=48&offset=" to "Hardcore",
        "$apiBeeg/facts/tag?slug=Squirting&limit=48&offset=" to "Squirting",
        "$apiBeeg/facts/tag?slug=Russian&limit=48&offset=" to "Russian",
        "$apiBeeg/facts/tag?slug=LongerFull&limit=48&offset=" to "LongerFull",
        "$apiBeeg/facts/tag?slug=AsianGirl&limit=48&offset=" to "AsianGirl",
        "$apiBeeg/facts/tag?slug=Compilation&limit=48&offset=" to "Compilation",
        "$apiBeeg/facts/tag?slug=3some&limit=48&offset=" to "3some",
        "$apiBeeg/facts/tag?slug=Stockings&limit=48&offset=" to "Stockings",
        "$apiBeeg/facts/tag?slug=Deepthroat&limit=48&offset=" to "Deepthroat",
        "$apiBeeg/facts/tag?slug=Latina&limit=48&offset=" to "Latina",
        "$apiBeeg/facts/tag?slug=Babe&limit=48&offset=" to "Babe",
        "$apiBeeg/facts/tag?slug=Cumshot&limit=48&offset=" to "Cumshot",
        "$apiBeeg/facts/tag?slug=Gangbang&limit=48&offset=" to "Gangbang",
        "$apiBeeg/facts/tag?slug=Cosplay&limit=48&offset=" to "Cosplay",
        "$apiBeeg/facts/tag?slug=Masturbation&limit=48&offset=" to "Masturbation",
        "$apiBeeg/facts/tag?slug=Cuckold&limit=48&offset=" to "Cuckold",
        "$apiBeeg/facts/tag?slug=Lingerie&limit=48&offset=" to "Lingerie",
        "$apiBeeg/facts/tag?slug=Indian&limit=48&offset=" to "Indian",
        "$apiBeeg/facts/tag?slug=NaturalTits&limit=48&offset=" to "NaturalTits",
        "$apiBeeg/facts/tag?slug=Redhead&limit=48&offset=" to "Redhead",
        "$apiBeeg/facts/tag?slug=Solo&limit=48&offset=" to "Solo",
        "$apiBeeg/facts/tag?slug=FemaleOrgasm&limit=48&offset=" to "FemaleOrgasm",
        "$apiBeeg/facts/tag?slug=DP&limit=48&offset=" to "DP",
        "$apiBeeg/facts/tag?slug=Schoolgirl&limit=48&offset=" to "Schoolgirl",
        "$apiBeeg/facts/tag?slug=BBC&limit=48&offset=" to "BBC",
        "$apiBeeg/facts/tag?slug=Homemade&limit=48&offset=" to "Homemade",
        "$apiBeeg/facts/tag?slug=Classic&limit=48&offset=" to "Classic",
        "$apiBeeg/facts/tag?slug=Blonde&limit=48&offset=" to "Blonde",
        "$apiBeeg/facts/tag?slug=BDSM&limit=48&offset=" to "BDSM",
        "$apiBeeg/facts/tag?slug=Skinny&limit=48&offset=" to "Skinny",
        "$apiBeeg/facts/tag?slug=Cowgirl&limit=48&offset=" to "Cowgirl",
        "$apiBeeg/facts/tag?slug=Taboo&limit=48&offset=" to "Taboo",
        "$apiBeeg/facts/tag?slug=Public&limit=48&offset=" to "Public",
        "$apiBeeg/facts/tag?slug=Interracial&limit=48&offset=" to "Interracial",
        "$apiBeeg/facts/tag?slug=Orgy&limit=48&offset=" to "Orgy",
        "$apiBeeg/facts/tag?slug=MatureWoman&limit=48&offset=" to "MatureWoman",
        "$apiBeeg/facts/tag?slug=OldYoung&limit=48&offset=" to "OldYoung"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (request.data == "actors") {
            val responseString = app.get("$apiBeeg/tag/recommends?type=person&slug=index", headers = headerlar).text
            val players = runCatching { parseJson<List<Map<String, Any>>>(responseString) }.getOrNull() ?: emptyList()

            val items = players.mapNotNull {
                val name = it["tg_name"]?.toString() ?: return@mapNotNull null
                val slug = it["tg_slug"]?.toString() ?: return@mapNotNull null

                val thumbs = it["thumbs"] as? List<*>
                val firstthumb = thumbs?.getOrNull(0) as? Map<*, *>
                val crops = firstthumb?.get("crops") as? List<*>
                val cropdata = crops?.getOrNull(0) as? Map<*, *>

                val ptphoto = cropdata?.get("pt_photo")?.toString()
                val cropid = cropdata?.get("id")?.toString()
                val posterurl = if (ptphoto != null && cropid != null) "https://thumbs.externulls.com/photos/$ptphoto/to.webp?crop_id=$cropid&size_new=112x112" else ""

                newMovieSearchResponse(name, "$mainUrl/$slug", TvType.NSFW) {
                    this.posterUrl = posterurl
                }
            }
            return newHomePageResponse(HomePageList(request.name, items, true))
        } else {
            val responseString = app.get("${request.data}${page * 48}", referer = "${mainUrl}/").text
            val response: List<ApiCevap> = mapper.readValue(responseString)
            val items: List<SearchResponse> = response.flatMap { it.toMainPageResults() }

            return newHomePageResponse(HomePageList(request.name, items, true))
        }
    }

    private fun ApiCevap.toMainPageResults(): List<SearchResponse> {
        return this.file.data.map { cevap ->
            val title = cevap.cd_value
            val apiDataJson = mapper.writeValueAsString(this.file)
            val apiTagsJson = mapper.writeValueAsString(this.tags)
            val posterUrl =
                "https://thumbs.externulls.com/videos/${cevap.cd_file}/49.webp?size=480x270"
            newMovieSearchResponse(
                title,
                "$apiDataJson|:$posterUrl|:$title|:$apiTagsJson",
                TvType.NSFW
            ).apply {
                this.posterUrl = posterUrl
            }
        }
    }


    private val headerlar = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Origin" to "https://beeg.com",
        "Referer" to "https://beeg.com/",
        "Accept" to "application/json, text/plain, */*",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7"
    )

    override suspend fun search(query: String, page: Int): SearchResponseList {
        Log.d("beeg", query)

        val response = try {
            app.get("https://store.externulls.com/tag/recommends?type=person&slug=index", headers = headerlar).text
        } catch (e: Exception) {
            return newSearchResponseList(emptyList(), hasNext = false)
        }

        val players = try {
            parseJson<List<Map<String, Any>>>(response)
        } catch (e: Exception) {
            emptyList()
        }

        val results = players.filter {
            val name = it["tg_name"]?.toString() ?: ""
            name.contains(query, ignoreCase = true)
        }.mapNotNull {
            val name = it["tg_name"]?.toString() ?: return@mapNotNull null
            val slug = it["tg_slug"]?.toString() ?: return@mapNotNull null

            val thumbs = it["thumbs"] as? List<*>
            val firstthumb = thumbs?.getOrNull(0) as? Map<*, *>
            val crops = firstthumb?.get("crops") as? List<*>
            val cropdata = crops?.getOrNull(0) as? Map<*, *>

            val ptphoto = cropdata?.get("pt_photo")?.toString()
            val cropid = cropdata?.get("id")?.toString()
            val poster = if (ptphoto != null && cropid != null) "https://thumbs.externulls.com/photos/$ptphoto/to.webp?crop_id=$cropid&size_new=112x112" else ""

            Log.d("beeg", name)
            newMovieSearchResponse(name, "$mainUrl/$slug", TvType.NSFW) {
                this.posterUrl = poster
            }
        }

        return newSearchResponseList(results, hasNext = false)
    }

    override suspend fun load(url: String): LoadResponse? {
        Log.d("beeg", url)

        if (url.contains("|:")) {
            val linkler = url.split("|:")
            val apidata = linkler[0]
            val poster = linkler[1]
            val title = linkler[2]
            val apitagsjson = linkler[3]

            val tagslistesi = try {
                mapper.readValue<List<TagData>>(apitagsjson)
            } catch (e: Exception) {
                emptyList()
            }

            val tags = tagslistesi.flatMap { tagdata ->
                tagdata.data.flatMap { tag ->
                    tag.td_value.split(",", ".").map { it.trim() }.filter { it.isNotEmpty() }
                }
            }

            return newMovieLoadResponse(title, apidata, TvType.NSFW, apidata) {
                this.posterUrl = poster
                this.tags = tags
            }
        }

        val html = try { app.get(url, headers = headerlar).document } catch (e: Exception) { null }
        val title = html?.selectFirst("h1")?.text() ?: url.substringAfterLast("/").replaceFirstChar { it.uppercase() }

        val slug = url.removeSuffix("/").substringAfterLast("/")
        val allepisodes = mutableListOf<Episode>()

        for (i in 0..10) {
            val offset = i * 48
            val apiurl = "$apiBeeg/tag/videos/$slug?limit=48&offset=$offset"
            val jsonres = try { app.get(apiurl, headers = headerlar).text } catch (e: Exception) { null }

            if (jsonres.isNullOrBlank() || jsonres == "[]") break

            val videolist = try {
                parseJson<List<Map<String, Any>>>(jsonres)
            } catch (e: Exception) {
                emptyList()
            }

            if (videolist.isEmpty()) break

            val pageepisodes = videolist.mapNotNull { video ->
                val fileobj = video["file"] as? Map<*, *> ?: return@mapNotNull null
                val dataarray = fileobj["data"] as? List<*>
                val firstdata = dataarray?.getOrNull(0) as? Map<*, *>

                val eptitle = firstdata?.get("cd_value")?.toString() ?: "Video"
                val videoid = (video["id"] ?: fileobj["id"])?.toString() ?: return@mapNotNull null

                val durationinseconds = fileobj["fl_duration"]?.toString()?.toIntOrNull() ?: 0
                val duration = "${durationinseconds / 60}:${String.format("%02d", durationinseconds % 60)}"

                val epdatajson = try { mapper.writeValueAsString(fileobj) } catch (e: Exception) { return@mapNotNull null }

                Log.d("beeg", eptitle)

                newEpisode(epdatajson) {
                    this.name = eptitle
                    this.posterUrl = "https://thumbs.externulls.com/videos/$videoid/0.webp?size=480x270"
                    this.description = duration
                }
            }

            allepisodes.addAll(pageepisodes)
            if (pageepisodes.size < 48) break
        }

        Log.d("beeg", allepisodes.size.toString())

        return newTvSeriesLoadResponse(title, url, TvType.NSFW, allepisodes) {
            this.posterUrl = allepisodes.randomOrNull()?.posterUrl
            this.plot = title
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("beeg", data)

        val apidatajson = if (data.contains("|")) data.split("|")[0] else data
        val apidata = try { mapper.readValue<ApiData>(apidatajson) } catch (e: Exception) { null }

        val hlsmulti = apidata?.hls_resources?.fl_cdn_multi

        if (!hlsmulti.isNullOrBlank()) {
            callback.invoke(
                newExtractorLink(
                    this.name,
                    this.name,
                    "https://video.beeg.com/$hlsmulti",
                    ExtractorLinkType.M3U8,
                    { this.referer = "$mainUrl/" }
                )
            )
            return true
        }

        if (apidata?.id != null) {
            try {
                val res = app.get("$apiBeeg/facts/file/${apidata.id}", referer = "$mainUrl/").text
                val root = mapper.readTree(res)
                val video = root.get("file")?.get("hls_resources")?.get("fl_cdn_multi")?.asText()
                    ?: root.get("fc_facts")?.get(0)?.get("hls_resources")?.get("fl_cdn_multi")?.asText()

                if (video != null) {
                    callback.invoke(
                        newExtractorLink(
                            this.name,
                            this.name,
                            "https://video.beeg.com/$video",
                            ExtractorLinkType.M3U8,
                            { this.referer = "$mainUrl/" }
                        )
                    )
                    return true
                }
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiCevap(val file: ApiData, val tags: List<TagData>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiData(
    val data: List<Icerik>,
    val hls_resources: HlsSource? = null,
    val qualities: Map<String, List<Videolar>>? = null,
    val id: Long
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Videolar(val quality: Int, val url: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Icerik(val cd_file: String, val cd_value: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TagData(val data: List<Tagler>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tagler(val td_value: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HlsSource(val fl_cdn_multi: String? = null)




