// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import java.net.URLEncoder

class WatchPorn : MainAPI() {
    override var mainUrl = "https://watchporn.to"
    override var name = "WatchPorn"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)


    override val mainPage = mainPageOf(
        "${mainUrl}/top-rated/" to "Top Rated",
        "${mainUrl}/most-popular/" to "Most Popular",
        "${mainUrl}/categories/manyvids/" to "ManyVids",
        "${mainUrl}/categories/onlyfans/" to "OnlyFans",
        "${mainUrl}/categories/xvideosred/" to "XVideosRed",
        "${mainUrl}/categories/primalfetish/" to "PrimalFetish",
        "${mainUrl}/categories/brazzersexxtra/" to "BrazzersExxtra",
        "${mainUrl}/categories/julesjordan/" to "JulesJordan",
        "${mainUrl}/categories/pascalssubsluts/" to "PascalsSubSluts",
        "${mainUrl}/categories/tabooheat/" to "TabooHeat",
        "${mainUrl}/categories/evilangel/" to "Evilangel",
        "${mainUrl}/categories/outofthefamily/" to "OutOfTheFamily",
        "${mainUrl}/categories/missax/" to "MissaX",
        "${mainUrl}/categories/loveherfeet/" to "LoveHerFeet",
        "${mainUrl}/categories/mommyblowsbest/" to "MommyBlowsBest",
        "${mainUrl}/categories/alexlegend/" to "AlexLegend",
        "${mainUrl}/categories/analized/" to "Analized",
        "${mainUrl}/categories/analintroductions/" to "AnalIntroductions",
        "${mainUrl}/categories/blackedraw/" to "BlackedRaw",
        "${mainUrl}/categories/immeganlive/" to "ImMeganLive",
        "${mainUrl}/categories/vixen/" to "Vixen",
        "${mainUrl}/categories/rkprime/" to "RKPrime",
        "${mainUrl}/categories/puretaboo/" to "PureTaboo",
        "${mainUrl}/categories/deeper/" to "Deeper",
        "${mainUrl}/categories/tushy/" to "Tushy",
        "${mainUrl}/categories/mypervyfamily/" to "MyPervyFamily",
        "${mainUrl}/categories/familytherapy/" to "FamilyTherapy",
        "${mainUrl}/categories/hotwifexxx/" to "HotwifeXXX",
        "${mainUrl}/categories/sislovesme/" to "SisLovesMe",
        "${mainUrl}/categories/wcaproductions/" to "WCAProductions",
        "${mainUrl}/categories/jamieyoung/" to "JamieYoung",
        "${mainUrl}/categories/familystrokes/" to "FamilyStrokes",
        "${mainUrl}/categories/allherluv/" to "AllHerLuv",
        "${mainUrl}/categories/blacked/" to "Blacked",
        "${mainUrl}/categories/tightandteen/" to "TightAndTeen",
        "${mainUrl}/categories/nubiles/" to "Nubiles",
        "${mainUrl}/categories/tushyraw/" to "TushyRaw",
        "${mainUrl}/categories/dadcrush/" to "DadCrush",
        "${mainUrl}/categories/meana-wolf/" to "Meana Wolf",
        "${mainUrl}/categories/cosplay/" to "Cosplay",
        "${mainUrl}/categories/pervmom/" to "PervMom",
        "${mainUrl}/categories/willtilexxx/" to "WillTileXXX",
        "${mainUrl}/categories/bangbus/" to "BangBus",
        "${mainUrl}/categories/mylifeinmiami/" to "MyLifeInMiami",
        "${mainUrl}/categories/analvids/" to "AnalVids",
        "${mainUrl}/categories/pornworld/" to "PornWorld",
        "${mainUrl}/categories/brattysis/" to "BrattySis",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}$page/"
        val document = app.get(url).document
        val home = document.select("div.thumb.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = listOf(
                HomePageList(
                    name = request.name,
                    list = home,
                    isHorizontalImages = true
                )
            )
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("span.thumb__title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-webp") ?: this.selectFirst("img")?.attr("src"))
        val rating = this.select("span.thumb__meta-item").lastOrNull()?.text()?.replace("%", "")?.trim()

        return newMovieSearchResponse(title, "$href|$posterUrl", TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score = Score.from100(rating)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = "${mainUrl}/search/?q=$query&mode=async&function=get_block&block_id=list_videos_videos_list_search_result&category_ids=&sort_by=&from_videos=$page"
        val document = app.get(url).document

        val aramaCevap = document.select("div.thumb.item").mapNotNull { it.toMainPageResult() }
        val hasNext = document.selectFirst("li.next") != null

        return newSearchResponseList(aramaCevap, hasNext)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(data: String): LoadResponse? {
        val (url, storedPoster) = data.split("|").let {
            it[0] to it.getOrNull(1)
        }

        val response = app.get(url)
        val document = response.document
        val cookies = response.cookies.toString()

        val title = document.selectFirst("h1.single__content-title")?.text()?.trim() ?: return null
        val poster = storedPoster ?: document.selectFirst("meta[property=og:image]")?.attr("content")

        val tags = document.select("div.single__info-row:contains(Tags:) a").map { it.text().trim() }
        val actors = document.select("div.single__info-row:contains(Models:) a").map { Actor(it.text().trim()) }

        val durationText = document.selectFirst("div.fp-time-duration")?.text()?.trim()
        val parts = durationText?.split(":")?.mapNotNull { it.toIntOrNull() }
        val totalMinutes = when (parts?.size) {
            3 -> (parts[0] * 60) + parts[1]
            2 -> parts[0]
            else -> null
        }

        val recommendations = document.select("div.related-videos div.thumb.item").mapNotNull {
            it.toMainPageResult()
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, "$url|$title") {
            this.posterUrl = poster
            this.posterHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                "Referer" to "$mainUrl/",
                "Cookie" to cookies
            )
            this.tags = tags
            this.duration = totalMinutes
            this.recommendations = recommendations
            addActors(actors)

            Log.d("Cloudstream", "Loaded Video: $title")
        }
    }

    // --- Subtitle Cat & Subtitle Nexus Integration ---
    private suspend fun fetchSubtitles(
        query: String,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        if (query.isBlank()) return
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        // 1. Fetch from SubtitleCat (Hindi & English Subtitles)
        try {
            val catSearchUrl = "https://www.subtitlecat.com/index.php?search=$encodedQuery"
            val catDoc = app.get(catSearchUrl, timeout = 15).document

            val catLinks = catDoc.select("table.sub-table tbody tr td a, .sub-list a, td a")
            for (item in catLinks.take(3)) {
                val href = item.attr("href")
                if (href.isNotBlank()) {
                    val catPageUrl = fixUrlNull(href, "https://www.subtitlecat.com") ?: continue
                    val subPageDoc = app.get(catPageUrl, timeout = 10).document

                    val downloadElements = subPageDoc.select("a[href$=.srt], a[href$=.vtt], a#download_def, .sub-single a[href*='/sub/']")
                    for (element in downloadElements) {
                        val subUrl = fixUrlNull(element.attr("href"), "https://www.subtitlecat.com") ?: continue
                        val parentText = element.parents().select(".sub-single span, td").text().trim()
                            .replace(Regex("[\uD83D\uDC4D\uD83D\uDC4E]"), "")

                        val langName = when {
                            parentText.contains("hindi", ignoreCase = true) || subUrl.contains("-hi.") -> "Hindi"
                            parentText.contains("english", ignoreCase = true) || subUrl.contains("-en.") -> "English"
                            else -> parentText.ifBlank { "SubtitleCat" }
                        }

                        subtitleCallback(
                            SubtitleFile(
                                lang = "SubtitleCat ($langName)",
                                url = subUrl
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("WatchPorn", "SubtitleCat Error: ${e.message}")
        }

        // 2. Fetch from SubtitleNexus
        try {
            val nexusSearchUrl = "https://subtitlenexus.com/?s=$encodedQuery"
            val nexusDoc = app.get(nexusSearchUrl, timeout = 15).document

            val nexusResults = nexusDoc.select("article a, h2.entry-title a, div.post-title a")
            for (result in nexusResults.take(2)) {
                val pageUrl = fixUrlNull(result.attr("href")) ?: continue
                val pageDoc = app.get(pageUrl, timeout = 10).document

                val subLinks = pageDoc.select("a[href$=.srt], a[href$=.vtt], a[href*=/download/]")
                for (link in subLinks) {
                    val subUrl = fixUrlNull(link.attr("href")) ?: continue
                    val linkText = link.text().trim()
                    val langName = when {
                        linkText.contains("hindi", ignoreCase = true) -> "Hindi"
                        else -> linkText.ifBlank { "English" }
                    }
                    subtitleCallback(
                        SubtitleFile(
                            lang = "SubtitleNexus ($langName)",
                            url = subUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("WatchPorn", "SubtitleNexus Error: ${e.message}")
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val dataParts = data.split("|")
        val streamUrl = dataParts[0]
        val title = dataParts.getOrNull(1)

        // Title milne par SubtitleCat aur SubtitleNexus se Subtitles fetch honge
        if (!title.isNullOrBlank()) {
            val cleanTitle = title.replace(Regex("""\[.*?\]"""), "").replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
            if (cleanTitle.isNotBlank()) {
                fetchSubtitles(cleanTitle, subtitleCallback)
            }
        }

        val pageHtml = app.get(streamUrl).text
        return KtPlayerExtractor.getLinks(name, mainUrl, streamUrl, pageHtml, callback = callback)
    }
}
