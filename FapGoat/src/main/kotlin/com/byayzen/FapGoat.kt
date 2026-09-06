// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class FapGoat : MainAPI() {
    override var mainUrl = "https://fapgoat.com"
    override var name = "FapGoat"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    override val mainPage = mainPageOf(
        "${mainUrl}/latest-updates/" to "Latest Updates",
        "${mainUrl}/categories/anal/" to "Anal",
        "${mainUrl}/categories/big-ass/" to "Big Ass",
        "${mainUrl}/categories/big-tits/" to "Big Tits",
        "${mainUrl}/categories/taboo/" to "Taboo",
        "${mainUrl}/categories/creampie/" to "Creampie",
        "${mainUrl}/categories/ebony/" to "Ebony",
        "${mainUrl}/categories/pov/" to "POV",
        "${mainUrl}/categories/hotwife/" to "Hotwife",
        "${mainUrl}/categories/stepsister/" to "Stepsister",
        "${mainUrl}/categories/teen/" to "Teen",
        "${mainUrl}/categories/18-19-years-old/" to "18 & 19 Years Old",
        "${mainUrl}/categories/milf/" to "MILF",
        "${mainUrl}/top-rated/" to "Top Rated",
        "${mainUrl}/most-popular/" to "Most Popular",
        "${mainUrl}/categories/brunette/" to "Brunette",
        "${mainUrl}/categories/hardcore/" to "Hardcore",
        "${mainUrl}/categories/massage/" to "Massage",
        "${mainUrl}/categories/footjob/" to "Footjob",
        "${mainUrl}/categories/cougar/" to "Cougar",
        "${mainUrl}/categories/foot-fetish/" to "Foot Fetish",
        "${mainUrl}/categories/squirt/" to "Squirt",
        "${mainUrl}/categories/stepmom/" to "Stepmom",
        "${mainUrl}/categories/blonde/" to "Blonde",
        "${mainUrl}/categories/blowjob/" to "Blowjob",
        "${mainUrl}/categories/cumshot/" to "Cumshot",
        "${mainUrl}/categories/asian/" to "Asian",
        "${mainUrl}/categories/amateur/" to "Amateur",
        "${mainUrl}/categories/bbw/" to "BBW",
        "${mainUrl}/categories/facial/" to "Facial",
        "${mainUrl}/categories/feet/" to "Feet",
        "${mainUrl}/categories/fetish/" to "Fetish",
        "${mainUrl}/categories/group/" to "Group",
        "${mainUrl}/categories/handjob/" to "Handjob",
        "${mainUrl}/categories/interracial/" to "Interracial",
        "${mainUrl}/categories/latina/" to "Latina",
        "${mainUrl}/categories/mature/" to "Mature",
        "${mainUrl}/categories/lesbian/" to "Lesbian",
        "${mainUrl}/categories/redhead/" to "Redhead",
        "${mainUrl}/categories/step-fantasy/" to "Step Fantasy",
        "${mainUrl}/categories/toys/" to "Toys",
        "${mainUrl}/categories/rough/" to "Rough",
        "${mainUrl}/categories/threesome/" to "Threesome",
        "${mainUrl}/categories/lingerie/" to "Lingerie",
        "${mainUrl}/categories/cuckold/" to "Cuckold",
        "${mainUrl}/categories/stockings/" to "Stockings",
        "${mainUrl}/categories/pantyhose/" to "Pantyhose",
        "${mainUrl}/categories/stepdad/" to "Stepdad",
        "${mainUrl}/categories/double-penetration/" to "Double Penetration",
        "${mainUrl}/categories/femdom/" to "Femdom",
        "${mainUrl}/categories/stepbrother/" to "Stepbrother",
        "${mainUrl}/categories/stepdaughter/" to "Stepdaughter",
        "${mainUrl}/categories/stepson/" to "Stepson",
        "${mainUrl}/categories/facesitting/" to "Facesitting",
        "${mainUrl}/categories/older-younger/" to "Older / Younger",
        "${mainUrl}/categories/edging/" to "Edging",
        "${mainUrl}/categories/wrestling/" to "Wrestling",
        "${mainUrl}/categories/solo/" to "Solo",
        "${mainUrl}/categories/petite/" to "Petite",
        "${mainUrl}/categories/anal-creampie/" to "Anal Creampie",
        "${mainUrl}/categories/cosplay/" to "Cosplay",
        "${mainUrl}/categories/futanari/" to "Futanari",
        "${mainUrl}/categories/cfnm/" to "CFNM",
        "${mainUrl}/categories/pawg/" to "PAWG",
        "${mainUrl}/categories/strap-on/" to "Strap-on",
        "${mainUrl}/categories/gloryhole/" to "Gloryhole"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            request.data
        } else {
            val separator = if (request.data.contains("?")) "&" else "?"
            "${request.data}${separator}mode=async&function=get_block&block_id=list_videos_common_videos_list&sort_by=post_date&from=$page"
        }

        val document = app.get(
            url     = url,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        ).document

        val home = document.select("div.thumbs div.thumb.item").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkTag   = this.selectFirst("a") ?: return null
        val title     = this.selectFirst("div.title")?.text()?.trim() ?: return null
        val href      = fixUrlNull(linkTag.attr("href").ifEmpty { return null }) ?: return null
        val imgTag    = this.selectFirst("img")
        val posterUrl = fixUrlNull(
            imgTag?.attr("data-original")?.ifEmpty { null }
                ?: imgTag?.attr("src")?.ifEmpty { null }
        )

        return newMovieSearchResponse(title, "${href}kraptor$posterUrl", TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search/$query/"
        } else {
            "$mainUrl/search/$query/?mode=async&function=get_block&block_id=list_videos_videos_list_search_result&q=$query&category_ids=&sort_by=&from_videos=$page&from_albums=$page"
        }

        val document = app.get(
            url     = url,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        ).document

        val searchAnswer = document.select("div.thumbs div.thumb.item").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(searchAnswer)
    }

    override suspend fun load(url: String): LoadResponse? {
        val split      = url.split("kraptor")
        val currentUrl = split[0].trim()
        val posterUrl  = split.getOrNull(1)?.trim()?.ifEmpty { null }

        Log.d(name, "Load aşaması: $currentUrl")
        val document = app.get(currentUrl).document

        val title           = document.selectFirst("h1.title")?.text()?.trim() ?: return null
        val description     = document.selectFirst("div.description")?.text()?.trim()
        val duration        = getDurationFromString(document.selectFirst("div.count-item:has(svg.icon-oclock)")?.text()?.trim())
        val tags            = document.select("div.video-taxonomy a").map { it.text().trim() }.distinct()
        val actors          = document.select("a.model-link").map { Actor(it.text().trim()) }
        val recommendations = document.select("div.thumbs div.thumb.item").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, currentUrl, TvType.NSFW, currentUrl) {
            this.posterUrl       = posterUrl
            this.plot            = description
            this.duration        = duration
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(name, "data = $data")
        val response = app.get(data).text

        val links = listOfNotNull(
            Regex("""video_alt_url:\s*'(https?:[^']+)'""").find(response)?.groupValues?.get(1)?.let { it to Qualities.P1080.value },
            Regex("""video_url:\s*'(https?:[^']+)'""").find(response)?.groupValues?.get(1)?.let { it to Qualities.P720.value }
        )

        if (links.isEmpty()) return false

        links.forEach { (url, quality) ->
            callback.invoke(
                newExtractorLink(
                    source      = this.name,
                    name        = this.name,
                    url         = url,
                    type        = INFER_TYPE,
                    initializer = {
                        this.quality = quality
                        this.referer = "$mainUrl/"
                    }
                )
            )
        }

        return true
    }
    }