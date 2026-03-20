// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import android.util.Base64

class Pornslash : MainAPI() {
    override var mainUrl              = "https://www.pornslash.com"
    override var name                 = "Pornslash"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/cat/3d-porn" to "3D Porn",
        "${mainUrl}/cat/amateur" to "Amateur",
        "${mainUrl}/cat/anal" to "Anal",
        "${mainUrl}/cat/arab" to "Arab",
        "${mainUrl}/cat/asian" to "Asian",
        "${mainUrl}/cat/bbc" to "BBC",
        "${mainUrl}/cat/bbw" to "Bbw",
        "${mainUrl}/cat/bdsm" to "Bdsm",
        "${mainUrl}/cat/behind-the-scene" to "Behind The Scene",
        "${mainUrl}/cat/big-ass" to "Big Ass",
        "${mainUrl}/cat/big-dick" to "Big Dick",
        "${mainUrl}/cat/big-tits" to "Big Tits",
        "${mainUrl}/cat/blonde" to "Blonde",
        "${mainUrl}/cat/blowjob" to "Blowjob",
        "${mainUrl}/cat/brunette" to "Brunette",
        "${mainUrl}/cat/bukkake" to "Bukkake",
        "${mainUrl}/cat/butt-plug" to "Butt Plug",
        "${mainUrl}/cat/casting" to "Casting",
        "${mainUrl}/cat/chubby" to "Chubby",
        "${mainUrl}/cat/college" to "College",
        "${mainUrl}/cat/cosplay" to "Cosplay",
        "${mainUrl}/cat/cougar" to "Cougar",
        "${mainUrl}/cat/creampie" to "Creampie",
        "${mainUrl}/cat/cuckold" to "Cuckold",
        "${mainUrl}/cat/curvy" to "Curvy",
        "${mainUrl}/cat/deepthroat" to "Deepthroat",
        "${mainUrl}/cat/dildo" to "Dildo",
        "${mainUrl}/cat/doggystyle" to "Doggystyle",
        "${mainUrl}/cat/double-penetration" to "Double Penetration",
        "${mainUrl}/cat/ebony" to "Ebony",
        "${mainUrl}/cat/facesitting" to "Facesitting",
        "${mainUrl}/cat/facial" to "Facial",
        "${mainUrl}/cat/fake-tits" to "Fake Tits",
        "${mainUrl}/cat/family" to "Family",
        "${mainUrl}/cat/fingering" to "Fingering",
        "${mainUrl}/cat/fisting" to "Fisting",
        "${mainUrl}/cat/footjob" to "Footjob",
        "${mainUrl}/cat/gangbang" to "Gangbang",
        "${mainUrl}/cat/getting-pissed" to "Getting Pissed",
        "${mainUrl}/cat/glory-hole" to "Glory Hole",
        "${mainUrl}/cat/granny" to "Granny",
        "${mainUrl}/cat/group-sex" to "Group Sex",
        "${mainUrl}/cat/gym" to "Gym",
        "${mainUrl}/cat/hairy-pussy" to "Hairy Pussy",
        "${mainUrl}/cat/handjob" to "Handjob",
        "${mainUrl}/cat/hardcore" to "Hardcore",
        "${mainUrl}/cat/hentai" to "Hentai",
        "${mainUrl}/cat/homemade" to "Homemade",
        "${mainUrl}/cat/indian" to "Indian",
        "${mainUrl}/cat/interracial" to "Interracial",
        "${mainUrl}/cat/jav" to "Jav",
        "${mainUrl}/cat/latex" to "Latex",
        "${mainUrl}/cat/latina" to "Latina",
        "${mainUrl}/cat/lesbian" to "Lesbian",
        "${mainUrl}/cat/lingerie" to "Lingerie",
        "${mainUrl}/cat/maid" to "Maid",
        "${mainUrl}/cat/massage" to "Massage",
        "${mainUrl}/cat/masturbating" to "Masturbating",
        "${mainUrl}/cat/mature" to "Mature",
        "${mainUrl}/cat/milf" to "Milf",
        "${mainUrl}/cat/nurse" to "Nurse",
        "${mainUrl}/cat/oiled" to "Oiled",
        "${mainUrl}/cat/old-young" to "Old Young",
        "${mainUrl}/cat/orgy" to "Orgy",
        "${mainUrl}/cat/outdoor" to "Outdoor",
        "${mainUrl}/cat/parody" to "Parody",
        "${mainUrl}/cat/party" to "Party",
        "${mainUrl}/cat/pawg" to "Pawg",
        "${mainUrl}/cat/petite" to "Petite",
        "${mainUrl}/cat/pissing" to "Pissing",
        "${mainUrl}/cat/pov" to "Pov",
        "${mainUrl}/cat/pregnant" to "Pregnant",
        "${mainUrl}/cat/public" to "Public",
        "${mainUrl}/cat/redhead" to "Redhead",
        "${mainUrl}/cat/schoolgirl" to "Schoolgirl",
        "${mainUrl}/cat/secretary" to "Secretary",
        "${mainUrl}/cat/short-hair" to "Short Hair",
        "${mainUrl}/cat/shower" to "Shower",
        "${mainUrl}/cat/sister" to "Sister",
        "${mainUrl}/cat/skinny" to "Skinny",
        "${mainUrl}/cat/small-tits" to "Small Tits",
        "${mainUrl}/cat/squirting" to "Squirting",
        "${mainUrl}/cat/striptease" to "Striptease",
        "${mainUrl}/cat/tattoo" to "Tattoo",
        "${mainUrl}/cat/teacher" to "Teacher",
        "${mainUrl}/cat/teen" to "Teen",
        "${mainUrl}/cat/thai" to "Thai",
        "${mainUrl}/cat/threesome" to "Threesome",
        "${mainUrl}/cat/triple-penetration" to "Triple Penetration",
        "${mainUrl}/cat/vintage" to "Vintage",
        "${mainUrl}/cat/webcam" to "Webcam",
        "${mainUrl}/cat/yoga-pants" to "Yoga Pants"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}?p=$page"
        val document = app.get(url).document
        val home = document.select("div.video-item").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".video-title a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst(".thumbnail img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) "${mainUrl}/search/$query" else "${mainUrl}/search/$query?p=$page"
        val document = app.get(url).document
        val results = document.select("div.video-item").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.video-page-title")?.text()?.trim() ?: return null
        val posterStyle = document.selectFirst(".cover-background")?.attr("style")
        val poster = fixUrlNull(posterStyle?.substringAfter("url(\"")?.substringBefore("\")")?.takeIf { it.isNotBlank() } ?: document.selectFirst("meta[property=og:image]")?.attr("content"))
        val tags = document.select(".video-tag").map { it.text() }
        val recommendations = document.select(".related .video-item").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val embedUrl = document.selectFirst("script[type=\"application/ld+json\"]")?.data()?.let { json ->
            Regex("\"embedUrl\":\\s*\"(.*?)\"").find(json)?.groupValues?.get(1)
        } ?: return false

        val embedResponse = app.get(embedUrl, referer = "$mainUrl/").text

        val ptBase64 = Regex("const Pt\\s*=\\s*atob\\(\"(.*?)\"\\)").find(embedResponse)?.groupValues?.get(1)
            ?: Regex("Pt\\s*=\\s*\"(.*?)\"").find(embedResponse)?.groupValues?.get(1)

        val masterId = Regex("master/(.*?)\"").find(embedResponse)?.groupValues?.get(1)
            ?: Regex("fetch\\(\"(.*?)/master/(.*?)\"\\)").find(embedResponse)?.groupValues?.get(2)

        if (ptBase64 != null && masterId != null) {
            val host = String(Base64.decode(ptBase64, Base64.DEFAULT))
            val finalUrl = "$host/master/$masterId"

            Log.d("kraptor_$name", "Found Video: $finalUrl")

            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = finalUrl,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$mainUrl/"
                    this.quality = Qualities.Unknown.value
                    this.headers = mapOf(
                        "Origin" to mainUrl,
                        "Referer" to "$mainUrl/"
                    )
                }
            )
        }
        return true
    }
}