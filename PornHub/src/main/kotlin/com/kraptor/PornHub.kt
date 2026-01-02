// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class PornHub : MainAPI() {
    override var mainUrl              = "https://www.pornhub.com"
    override var name                 = "PornHub"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    private val cookies = mapOf(Pair("hasVisited", "1"), Pair("accessAgeDisclaimerPH", "1"))

    override val mainPage = mainPageOf(
        "${mainUrl}/video"                  to "Featured",
        "${mainUrl}/categories/teen"                  to "18-25",
        "${mainUrl}/video?c=105"                      to "60FPS",
        "${mainUrl}/video?c=3"                        to "Amateur",
        "${mainUrl}/video?c=35"                       to "Anal",
        "${mainUrl}/video?c=98"                       to "Arab",
        "${mainUrl}/video?c=1"                        to "Asian",
        "${mainUrl}/categories/babe"                  to "Babe",
//        "${mainUrl}/video?c=89"                       to "Babysitter (18+)",
//        "${mainUrl}/video?c=6"                        to "BBW",
//        "${mainUrl}/video?c=141"                      to "Behind The Scenes",
        "${mainUrl}/video?c=4"                        to "Big Ass",
//        "${mainUrl}/video?c=7"                        to "Big Dick",
//        "${mainUrl}/video?c=8"                        to "Big Tits",
//        "${mainUrl}/video?c=76"                       to "Bisexual Male",
        "${mainUrl}/video?c=9"                        to "Blonde",
//        "${mainUrl}/video?c=13"                       to "Blowjob",
//        "${mainUrl}/video?c=10"                       to "Bondage",
//        "${mainUrl}/video?c=102"                      to "Brazilian",
//        "${mainUrl}/video?c=96"                       to "British",
        "${mainUrl}/video?c=11"                       to "Brunette",
        "${mainUrl}/video?c=14"                       to "Bukkake",
//        "${mainUrl}/video?c=86"                       to "Cartoon",
//        "${mainUrl}/video?c=90"                       to "Casting",
//        "${mainUrl}/video?c=12"                       to "Celebrity",
//        "${mainUrl}/video?c=732"                      to "Closed Captions",
//        "${mainUrl}/categories/college"               to "College (18+)",
//        "${mainUrl}/video?c=57"                       to "Compilation",
        "${mainUrl}/video?c=241"                      to "Cosplay",
//        "${mainUrl}/video?c=15"                       to "Creampie",
//        "${mainUrl}/video?c=242"                      to "Cuckold",
//        "${mainUrl}/video?c=16"                       to "Cumshot",
//        "${mainUrl}/video?c=100"                      to "Czech",
//        "${mainUrl}/video/search?search=deepthroat"   to "Deepthroat",
//        "${mainUrl}/described-video"                  to "Described Video",
//        "${mainUrl}/video?c=72"                       to "Double Penetration",
        "${mainUrl}/video?c=17"                       to "Ebony",
//        "${mainUrl}/video?c=55"                       to "Euro",
//        "${mainUrl}/video?c=115"                      to "Exclusive",
//        "${mainUrl}/video?c=93"                       to "Feet",
//        "${mainUrl}/video?c=502"                      to "Female Orgasm",
//        "${mainUrl}/video?c=18"                       to "Fetish",
//        "${mainUrl}/video?c=592"                      to "Fingering",
//        "${mainUrl}/video?c=19"                       to "Fisting",
//        "${mainUrl}/video?c=94"                       to "French",
//        "${mainUrl}/video?c=32"                       to "Funny",
//        "${mainUrl}/video?c=881"                      to "Gaming",
//        "${mainUrl}/video?c=80"                       to "Gangbang",
//        "${mainUrl}/video?c=95"                       to "German",
//        "${mainUrl}/video?c=20"                       to "Handjob",
//        "${mainUrl}/video?c=21"                       to "Hardcore",
        "${mainUrl}/hd"                               to "HD Porn",
//        "${mainUrl}/categories/hentai"                to "Hentai",
//        "${mainUrl}/video?c=101"                      to "Indian",
//        "${mainUrl}/interactive"                      to "Interactive",
//        "${mainUrl}/video?c=25"                       to "Interracial",
//        "${mainUrl}/video?c=97"                       to "Italian",
//        "${mainUrl}/video?c=111"                      to "Japanese",
//        "${mainUrl}/video?c=103"                      to "Korean",
//        "${mainUrl}/video?c=26"                       to "Latina",
//        "${mainUrl}/video?c=27"                       to "Lesbian",
//        "${mainUrl}/video?c=78"                       to "Massage",
//        "${mainUrl}/video?c=22"                       to "Masturbation",
        "${mainUrl}/video?c=28"                       to "Mature",
        "${mainUrl}/video?c=29"                       to "MILF",
//        "${mainUrl}/video?c=512"                      to "Muscular Men",
//        "${mainUrl}/video?c=121"                      to "Music",
//        "${mainUrl}/video?c=181"                      to "Old/Young (18+)",
//        "${mainUrl}/video?c=2"                        to "Orgy",
//        "${mainUrl}/video?c=201"                      to "Parody",
//        "${mainUrl}/video?c=53"                       to "Party",
//        "${mainUrl}/video?c=211"                      to "Pissing",
//        "${mainUrl}/video?c=891"                      to "Podcast",
//        "${mainUrl}/popularwithwomen"                 to "Popular With Women",
//        "${mainUrl}/categories/pornstar"              to "Pornstar",
//        "${mainUrl}/video?c=41"                       to "POV",
//        "${mainUrl}/video?c=24"                       to "Public",
//        "${mainUrl}/video?c=131"                      to "Pussy Licking",
//        "${mainUrl}/video?c=31"                       to "Reality",
//        "${mainUrl}/video?c=42"                       to "Red Head",
//        "${mainUrl}/video?c=81"                       to "Role Play",
//        "${mainUrl}/video?c=522"                      to "Romantic",
//        "${mainUrl}/video?c=67"                       to "Rough Sex",
//        "${mainUrl}/video?c=99"                       to "Russian",
//        "${mainUrl}/video?c=88"                       to "School (18+)",
//        "${mainUrl}/sfw"                              to "SFW",
//        "${mainUrl}/video?c=59"                       to "Small Tits",
//        "${mainUrl}/video?c=91"                       to "Smoking",
//        "${mainUrl}/video?c=492"                      to "Solo Female",
//        "${mainUrl}/video?c=92"                       to "Solo Male",
//        "${mainUrl}/video?c=69"                       to "Squirt",
//        "${mainUrl}/video?c=444"                      to "Step Fantasy",
//        "${mainUrl}/video?c=542"                      to "Strap On",
//        "${mainUrl}/video?c=33"                       to "Striptease",
//        "${mainUrl}/video?c=562"                      to "Tattooed Women",
//        "${mainUrl}/video?c=65"                       to "Threesome",
//        "${mainUrl}/video?c=23"                       to "Toys",
//        "${mainUrl}/transgender"                      to "Transgender",
//        "${mainUrl}/video?c=138"                      to "Verified Amateurs",
//        "${mainUrl}/video?c=482"                      to "Verified Couples",
//        "${mainUrl}/video?c=139"                      to "Verified Models",
//        "${mainUrl}/video?c=43"                       to "Vintage",
//        "${mainUrl}/vr"                               to "Virtual Reality",
//        "${mainUrl}/video?c=61"                       to "Webcam",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (request.data.contains("video?")){
            app.get("${request.data}&page=$page", referer = "${mainUrl}/", cookies = cookies).document
        } else {
            app.get("${request.data}?page=$page", referer = "${mainUrl}/", cookies = cookies).document
        }
        val home     = document.select("div.gridWrapper li.pcVideoListItem").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val trailer   = this.selectFirst("img")?.attr("data-mediabook")

        return newMovieSearchResponse(title, "${href}kraptor${trailer?.substringAfter("//")}", TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/video/search?search=$query&page=$page").document

        val aramaCevap = document.select("div.gridWrapper li.pcVideoListItem").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val split    = url.split("kraptor")
        val url      = split[0]
        val trailer  = "https://${split[1]}"
        val document = app.get(url, referer = url, cookies = cookies).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags            = document.select("div.tagsWrapper a").map { it.text() }
        val duration        = document.selectFirst("var.duration")?.text()?.split(":")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("ul#relatedVideosListing li").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("a.pstar-list-btn").map {
            Actor(it.text(),
            it.selectFirst("img.avatar")?.attr("src")
        ) }


        Log.d("kraptor_PHub","trailer =$trailer")

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer, "${mainUrl}/", true)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data, referer = "${mainUrl}/" , cookies = cookies).document

        val script = document.selectFirst("script:containsData(var flashvars)")?.data()?.substringAfter(" = ")
            ?.substringBefore(";") ?: ""

        val mapper = mapper.readValue<Phub>(script)

        val cevaplar = mapper.mediaDefinitions

        cevaplar?.forEach { cevap ->
            val video = cevap.videoUrl ?: ""
            Log.d("kraptor_$name", "video = ${video}")
            val quality = cevap.quality.toString()

            val format = cevap.format ?: ""
            callback.invoke(newExtractorLink(
                this.name,
                this.name,
                video,
                type = when(format){
                    "mp4" -> ExtractorLinkType.VIDEO
                    "hls" -> ExtractorLinkType.M3U8
                    else -> INFER_TYPE
                }
            ) {
                this.referer = "${mainUrl}/"
                this.quality = getQualityFromName(quality)
            })
        }

        return true
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Phub(
    val mediaDefinitions: List<PhubVideo>?
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PhubVideo(
    val format: String?,
    val videoUrl: String?,
    val quality: Any?
)