// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class Allpornstream : MainAPI() {
    override var mainUrl        = "https://allpornstream.com"
    override var name           = "Allpornstream"
    override val hasMainPage    = true
    override var lang           = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus      = VPNStatus.MightBeNeeded

    private val appHeaders = mapOf(
        "Accept"     to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:157.0) Gecko/20100101 Firefox/157.0"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/producers/only-fans"            to "Only Fans",
        "$mainUrl/producers/evil-angel"           to "Evil Angel",
        "$mainUrl/producers/brazzers-exxtra"      to "Brazzers Exxtra",
        "$mainUrl/producers/sex-mex"              to "Sex Mex",
        "$mainUrl/producers/team-skeet"           to "Team Skeet",
        "$mainUrl/producers/atk-girlfriends"      to "ATK Girlfriends",
        "$mainUrl/producers/porn-world"           to "Porn World",
        "$mainUrl/producers/jacquie-et-michel-tv" to "Jacquie Et Michel TV",
        "$mainUrl/producers/brazzers"             to "Brazzers",
        "$mainUrl/producers/elegant-angel"        to "Elegant Angel",
        "$mainUrl/producers/taboo-heat"           to "Taboo Heat",
        "$mainUrl/producers/japan-hdv"            to "Japan HDV",
        "$mainUrl/producers/my-pervy-family"      to "My Pervy Family",
        "$mainUrl/producers/private"              to "Private",
        "$mainUrl/producers/penthouse-gold"       to "Penthouse Gold",
        "$mainUrl/producers/bellesa-house"        to "Bellesa House",
        "$mainUrl/producers/new-sensations"       to "New Sensations",
        "$mainUrl/producers/jules-jordan"         to "Jules Jordan",
        "$mainUrl/producers/naughty-america"      to "Naughty America",
        "$mainUrl/producers/rocco-siffredi"       to "Rocco Siffredi",
        "$mainUrl/producers/only-tarts"           to "Only Tarts",
        "$mainUrl/producers/nubiles-porn"         to "Nubiles Porn",
        "$mainUrl/producers/bang-bros"            to "Bang Bros",
        "$mainUrl/producers/blacked"              to "Blacked",
        "$mainUrl/producers/club-sweethearts"     to "Club Sweethearts",
        "$mainUrl/producers/hussie-pass"          to "Hussie Pass",
        "$mainUrl/producers/girls-way"            to "Girls Way",
        "$mainUrl/producers/bellesa-blind-date"   to "Bellesa Blind Date",
        "$mainUrl/producers/tadpole-x-studio"     to "Tadpole X Studio",
        "$mainUrl/producers/nubile-films"         to "Nubile Films"
    )

    private fun posteriduzenle(url: String): String {
        return if (url.startsWith("http")) {
            val encodedurl = URLEncoder.encode(url.replace("\\", ""), "utf-8")
            "${mainUrl.removeSuffix("/")}/api/images?src=$encodedurl&width=384&quality=60"
        } else {
            fixUrl(url)
        }
    }

    private fun nextiparseet(html: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        val blocks = html.split("data-href")
        val titleregex = Regex("""data-title["\\:=]+([^"\\]+)""")
        val imageregex = Regex("""data-images.*?\[.*?(https?://[^"\\'&;]+)""")

        for (i in 1 until blocks.size) {
            val block = blocks[i]
            val hrefmatch = Regex("""^[\\":=\s]*([^"'\\]+)""").find(block)
            val href = hrefmatch?.groupValues?.get(1)?.trim() ?: continue
            if (!href.startsWith("/post/")) continue

            val title = titleregex.find(block)?.groupValues?.get(1)?.trim() ?: continue
            val poster = imageregex.find(block)?.groupValues?.get(1) ?: continue
            if (poster.contains("placeholder", true)) continue

            results.add(newMovieSearchResponse(title, fixUrl(href), TvType.NSFW) {
                this.posterUrl = posteriduzenle(poster)
            })
        }
        return results.distinctBy { it.url }
    }

    private fun parseItemList(html: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        try {
            val jsonLd = Regex("""<script[^>]*type=["']application/ld\+json["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(html)
                .map { it.groupValues[1].trim() }
                .firstOrNull { it.contains("itemListElement") }

            if (jsonLd != null) {
                val jsonObject = org.json.JSONObject(jsonLd)
                val items      = jsonObject.optJSONArray("itemListElement")
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item  = items.optJSONObject(i) ?: continue
                        val title = item.optString("name").trim()
                        val url   = item.optString("url").trim()
                        if (title.isEmpty() || url.isEmpty()) continue

                        val posterArray = item.optJSONArray("thumbnailUrl")
                        val rawPoster   = if (posterArray != null && posterArray.length() > 0) {
                            posterArray.optString(0)
                        } else {
                            item.optString("thumbnailUrl")
                        }
                        val posterUrl = if (rawPoster.isNotBlank()) posteriduzenle(rawPoster) else null

                        results.add(newMovieSearchResponse(title, fixUrl(url), TvType.NSFW) {
                            this.posterUrl = posterUrl
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("Allpornstream", "parseItemListError=${e.message}")
        }

        if (results.isEmpty()) {
            results.addAll(nextiparseet(html))
        }

        return results.distinctBy { it.url }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            if (request.data.contains("?")) {
                "${request.data}&page=$page"
            } else {
                "${request.data}?page=$page"
            }
        }
        Log.d("Allpornstream", "page=$page url=$url")
        val res     = app.get(url, headers = appHeaders)
        val results = parseItemList(res.text)
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = results,
                isHorizontalImages = true
            ),
            hasNext = results.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val encodedQuery = withContext(Dispatchers.IO) {
            URLEncoder.encode(query, "utf-8")
        }
        val url = if (page == 1) {
            "${mainUrl.removeSuffix("/")}/posts?search=$encodedQuery"
        } else {
            "${mainUrl.removeSuffix("/")}/posts?search=$encodedQuery&page=$page"
        }
        Log.d("Allpornstream", "query=$query page=$page url=$url")
        val res     = app.get(url, headers = appHeaders)
        val results = parseItemList(res.text)
        return newSearchResponseList(results, hasNext = results.isNotEmpty())
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    private fun postdataAl(restext: String): String {
        return restext.substringAfter("\"initialPost\":").substringBefore(",\"initialUrls\"")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace(Regex("""\\u([0-9a-fA-F]{4})""")) { match -> match.groupValues[1].toInt(16).toChar().toString() }
    }

    private fun linkleri(postdata: String): List<String> {
        val linkdata = Regex(""""link":\[(.*?)],"direct"""").find(postdata)?.groupValues?.get(1).orEmpty()
        return Regex("""\["[^"]*","(https://[^"]+)"]""").findAll(linkdata)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    override suspend fun load(url: String): LoadResponse? {
        Log.d("Allpornstream", "url=$url")
        val res     = app.get(url, headers = appHeaders)
        val restext = res.text

        val postdata = postdataAl(restext)

        val title    = Regex("""video_title":"(.*?)"""").find(postdata)?.groupValues?.get(1) ?: return null
        val poster   = Regex("""image_details":\["(.*?)"""").find(postdata)?.groupValues?.get(1)?.let { posteriduzenle(it) }
        val plot     = Regex("""description":"(.*?)"""").find(postdata)?.groupValues?.get(1)
        val year     = Regex("""item_publish_date":"(\d{4})""").find(postdata)?.groupValues?.get(1)?.toIntOrNull()
        val duration = Regex("""duration":"(\d+)m""").find(postdata)?.groupValues?.get(1)?.toIntOrNull()

        val alllinks = linkleri(postdata).joinToString(",")

        val tags = Regex("""categories":\[(.*?)]""").find(postdata)?.groupValues?.get(1)
            ?.split(",")?.map { it.trim().removeSurrounding("\"") }

        val actors = Regex(""""actors":\[(.*?)],"producers"""").find(postdata)?.groupValues?.get(1).orEmpty()
            .split("},{")
            .mapNotNull { block ->
                val name = Regex(""""item_name":"(.*?)"""").find(block)?.groupValues?.get(1) ?: return@mapNotNull null
                val thumb = Regex(""""item_thumb_url":"(.*?)"""").find(block)?.groupValues?.get(1)
                    ?.takeIf { it.startsWith("http") }
                    ?.let { posteriduzenle(it) }
                Actor(name, thumb)
            }
            .distinctBy { it.name }

        val recs = parseItemList(restext).filter { it.url != url }

        return newMovieLoadResponse(title, url, TvType.NSFW, alllinks.ifBlank { url }) {
            this.posterUrl       = poster
            this.plot            = plot
            this.year            = year
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recs
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("Allpornstream", "data=$data")
        val links = if (data.contains("/post/")) {
            linkleri(postdataAl(app.get(data, headers = appHeaders).text))
        } else {
            data.split(",")
        }

        var found = false
        links.forEach { link ->
            val clean = link.trim()
            if (clean.isNotBlank()) {
                if (loadExtractor(clean, mainUrl, subtitleCallback, callback)) {
                    found = true
                }
            }
        }
        return found
    }
}