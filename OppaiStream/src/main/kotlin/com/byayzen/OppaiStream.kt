// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class OppaiStream : MainAPI() {
    override var mainUrl              = "https://oppai.stream"
    override var name                 = "OppaiStream"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "ahegao" to "Ahegao",
        "censored" to "Censored",
        "comedy" to "Comedy",
        "fantasy" to "Fantasy",
        "filmed" to "Filmed",
        "hd" to "HD",
        "harem" to "Harem",
        "incest" to "Incest",
        "inflation" to "Inflation",
        "lactation" to "Lactation",
        "mindbreak" to "Mind Break",
        "mindcontrol" to "Mind Control",
        "monster" to "Monster",
        "pov" to "POV",
        "plot" to "Plot",
        "tentacle" to "Tentacle",
        "uncensored" to "Uncensored",
        "vanilla" to "Vanilla",
        "watersports" to "Watersports",
        "x-ray" to "X-Ray",
        "yuri" to "Yuri",
        "anal" to "Anal",
        "bdsm" to "BDSM",
        "blowjob" to "BlowJob",
        "bondage" to "Bondage",
        "boobjob" to "BoobJob",
        "cowgirl" to "Cowgirl",
        "creampie" to "Creampie",
        "doggy" to "Doggy",
        "doublepenetration" to "Double Penetration",
        "facial" to "Facial",
        "footjob" to "FootJob",
        "gangbang" to "Gangbang",
        "handjob" to "HandJob",
        "masturbation" to "Masturbation",
        "missionary" to "Missionary",
        "ntr" to "NTR",
        "orgy" to "Orgy",
        "publicsex" to "Public Sex",
        "rape" to "Rape",
        "bigboobs" to "Big Boobs",
        "blackhair" to "Black Hair",
        "blondehair" to "Blonde Hair",
        "bluehair" to "Blue Hair",
        "brownhair" to "Brown Hair",
        "cosplay" to "Cosplay",
        "darkskin" to "Dark Skin",
        "futanari" to "Futanari",
        "glasses" to "Glasses",
        "greenhair" to "Green Hair",
        "gyaru" to "Gyaru",
        "loli" to "Loli",
        "maid" to "Maid",
        "milf" to "Milf",
        "nurse" to "Nurse",
        "pinkhair" to "Pink Hair",
        "pregnant" to "Pregnant",
        "purplehair" to "Purple Hair",
        "redhair" to "Red Hair",
        "schoolgirl" to "School Girl",
        "shorthair" to "Short Hair",
        "smallboobs" to "Small Boobs",
        "succubus" to "Succubus",
        "tsundere" to "Tsundere",
        "whitehair" to "White Hair",
        "shota" to "Shota",
        "uglybastard" to "Ugly Bastard",
        "virgin" to "Virgin"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "$mainUrl/actions/search.php?text=&order=recent&page=$page&limit=35&genres=${request.data}&blacklist=&studio=&ibt=0&swa=1"
        val document = app.get(url, referer = "$mainUrl/search?a=recent&p=$page&t=&g=${request.data}&b=&s=").document
        val home = document.select("div[class*='in-grid']").mapNotNull { it.tosearchresult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = "$mainUrl/actions/search.php?text=$query&order=recent&page=$page&limit=35&genres=&blacklist=&studio=&ibt=0&swa=1"
        val document = app.get(url, referer = "$mainUrl/search?a=recent&p=$page&t=$query&g=&b=&s=").document
        val results = document.select("div[class*='in-grid']").mapNotNull { it.tosearchresult() }
        return newSearchResponseList(results, hasNext = true)
    }

    private fun Element.tosearchresult(): SearchResponse? {
        val name = this.attr("name")
        val ep = this.attr("ep")
        val title = "$name $ep".trim().replaceFirstChar { it.uppercase() }
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val img = this.selectFirst("img[class*='cover-img-in']")
        val src = img?.attr("src") ?: img?.attr("original") ?: img?.attr("data-src")
        val poster = src?.let { if (it.startsWith("//")) "https:$it" else it }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
            this.posterHeaders = mapOf("Referer" to "$mainUrl/")
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        val plot = document.selectFirst("div.description h5")?.text()?.trim()
        val tags = document.select("div.tags a.tag h5").map { it.text() }
        val related = document.select("div.other-episodes div[class*='in-grid']").mapNotNull {
            it.tosearchresult()
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = plot
            this.tags = tags
            this.recommendations = related
            this.posterHeaders = mapOf("Referer" to "$mainUrl/")
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data)
        val html = response.text

        response.document.select("track").forEach { track ->
            val src = track.attr("src")
            val lang = track.attr("label").ifBlank { "English" }
            if (src.isNotBlank()) {
                val suburl = fixUrl(src).replace(" ", "%20")
                subtitleCallback(newSubtitleFile(lang = lang, url = suburl))
            }
        }

        val regex = Regex("""\"(\d+k?)\"\s*:\s*\"(https.*?)\"""")
        regex.findAll(html).forEach { match ->
            val quality = match.groupValues[1]
            val videourl = match.groupValues[2].replace("\\/", "/").replace(" ", "%20")
            val qualityid = when (quality) {
                "4k" -> Qualities.P2160.value
                "1080" -> Qualities.P1080.value
                "720" -> Qualities.P720.value
                else -> Qualities.Unknown.value
            }
            callback(
                newExtractorLink(
                    source = "OppaiStream",
                    name = "OppaiStream",
                    url = videourl,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.quality = qualityid
                    this.referer = "$mainUrl/"
                }
            )
        }
        return true
    }
}