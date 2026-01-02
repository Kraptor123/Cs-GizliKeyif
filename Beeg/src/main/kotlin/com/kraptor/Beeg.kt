// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

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
        "$apiBeeg/facts/tag?slug=OldYoung&limit=48&offset=" to "OldYoung",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val responseString = app.get("${request.data}${page * 48}", referer = "${mainUrl}/").text
        val response: List<ApiCevap> = mapper.readValue(responseString)
        val items: List<SearchResponse> = response.flatMap { it.toMainPageResults() }

        return newHomePageResponse(HomePageList(request.name, items, true))
    }

    private fun ApiCevap.toMainPageResults(): List<SearchResponse> {
        return this.file.data.map { cevap ->
            val title = cevap.cd_value
            val apiDataJson = mapper.writeValueAsString(this.file)
            val apiTagsJson = mapper.writeValueAsString(this.tags)
            val posterUrl = "https://thumbs.externulls.com/videos/${cevap.cd_file}/49.webp?size=480x270"
            newMovieSearchResponse(title, "$apiDataJson|:$posterUrl|:$title|:$apiTagsJson", TvType.NSFW).apply {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val linkler = url.split("|:")
        val apiData = linkler[0]
        val apiTagsJson = linkler[3]
        val title = linkler[2]
        val poster = linkler[1]
        val tagsListesi = mapper.readValue<List<TagData>>(apiTagsJson)
        val tags = tagsListesi.flatMap { tagData ->
            tagData.data.flatMap { tag ->
                tag.td_value.split(",", ".").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }

        return newMovieLoadResponse(title, apiData, TvType.NSFW, apiData) {
            this.posterUrl = poster
            this.tags = tags
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "data = $data")

        val linkler = data.split("|")
        val apiDataJson = linkler[0]
        val apiData = try {
            mapper.readValue<ApiData>(apiDataJson)
        } catch (e: Exception) {
            Log.e("kraptor_$name", "Failed to parse apiData: ${e.message}")
            return false
        }

        if (apiData.hls_resources != null && !apiData.hls_resources.fl_cdn_multi.isNullOrBlank()) {
            val hlsUrl = "https://video.beeg.com/${apiData.hls_resources.fl_cdn_multi}"
            callback.invoke(
                newExtractorLink(
                this.name,
                this.name,
                hlsUrl,
                ExtractorLinkType.M3U8,
                {
                    this.referer = "$mainUrl/"
                }
            ))
        } else {
            val url = app.get("https://store.externulls.com/facts/file/${apiData.id}", referer = "${mainUrl}/").text
            val cevap = mapper.readValue<ApiStore>(url)
            val video = cevap.fc_facts.first().hls_resources.fl_cdn_multi
            val hlsUrl = "https://video.beeg.com/${video}"
            callback.invoke(
                newExtractorLink(
                    this.name,
                    this.name,
                    hlsUrl,
                    ExtractorLinkType.M3U8,
                    {
                        this.referer = "$mainUrl/"
                    }
                ))

        }

        return true
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiStore(
    val fc_facts: List<Boslar>
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Boslar(
    val hls_resources: HlsSource
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiCevap(
    val file: ApiData,
    val tags: List<TagData>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiData(
    val data: List<Icerik>,
    val hls_resources: HlsSource?,
    val qualities: Map<String, List<Videolar>>?,
    val id: Long
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Videolar(
    val quality: Int,
    val url: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Icerik(
    val cd_file: String,
    val cd_value: String
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class TagData(
    val data: List<Tagler>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tagler(
    val td_value: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HlsSource(
    val fl_cdn_multi: String
)

