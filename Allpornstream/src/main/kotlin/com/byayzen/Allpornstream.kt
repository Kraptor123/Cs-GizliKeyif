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
    override var mainUrl = "https://allpornstream.com/"
    override var name = "Allpornstream"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val appHeaders = mapOf(
        "RSC" to "1",
        "Accept" to "*/*",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:149.0) Gecko/20100101 Firefox/149.0"
    )

    override val mainPage = mainPageOf(
        "${mainUrl}/categories/brunette" to "Brunette",
        "${mainUrl}/categories/1080-p" to "1080 P",
        "${mainUrl}/categories/shaved-pussy" to "Shaved Pussy",
        "${mainUrl}/categories/anal" to "Anal",
        "${mainUrl}/categories/interracial" to "Interracial",
        "${mainUrl}/categories/small-tits" to "Small Tits",
        "${mainUrl}/categories/60-fps" to "60 Fps",
        "${mainUrl}/categories/latina" to "Latina",
        "${mainUrl}/categories/pov" to "Pov",
        "${mainUrl}/categories/asian" to "Asian",
        "${mainUrl}/categories/masturbation" to "Masturbation",
        "${mainUrl}/categories/ebony" to "Ebony",
        "${mainUrl}/categories/bisexual" to "Bisexual",
        "${mainUrl}/categories/naughtyamerica" to "Naughtyamerica",
        "${mainUrl}/categories/casting" to "Casting"
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

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val res = app.get(request.data, headers = appHeaders)
        val results = nextiparseet(res.text)
        return newHomePageResponse(request.name, results, hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "${mainUrl.removeSuffix("/")}/?search=${
            withContext(Dispatchers.IO) {
                URLEncoder.encode(query, "utf-8")
            }
        }"
        val res = app.get(url, headers = appHeaders)
        return nextiparseet(res.text)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val res = app.get(url, headers = appHeaders)
        val restext = res.text

        val postdata = restext.substringAfter("\"initialPost\":").substringBefore(",\"initialUrls\"")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

        val title = Regex("""video_title":"(.*?)"""").find(postdata)?.groupValues?.get(1) ?: return null
        val poster = Regex("""image_details":\["(.*?)"""").find(postdata)?.groupValues?.get(1)?.let { posteriduzenle(it) }
        val plot = Regex("""description":"(.*?)"""").find(postdata)?.groupValues?.get(1)
        val year = Regex("""item_publish_date":"(\d{4})""").find(postdata)?.groupValues?.get(1)?.toIntOrNull()
        val duration = Regex("""duration":"(\d+)m""").find(postdata)?.groupValues?.get(1)?.toIntOrNull()

        val alllinks = Regex("""embed_url":"(.*?)"""").findAll(postdata)
            .map { it.groupValues[1].replace("\\", "") }
            .distinct()
            .joinToString(",")

        val tags = Regex("""categories":\[(.*?)]""").find(postdata)?.groupValues?.get(1)
            ?.split(",")?.map { it.trim().removeSurrounding("\"") }

        val actors = Regex("""item_name":"(.*?)"""").findAll(postdata)
            .map { Actor(it.groupValues[1]) }
            .distinctBy { it.name }
            .toList()

        val recs = nextiparseet(restext).filter { it.url != url }

        return newMovieLoadResponse(title, url, TvType.NSFW, alllinks.ifBlank { url }) {
            this.posterUrl = poster
            this.plot = plot
            this.year = year
            this.tags = tags
            this.duration = duration
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
        val links = if (data.contains("/post/")) {
            val res = app.get(data, headers = appHeaders).text
            Regex("""embed_url":"(.*?)"""").findAll(res)
                .map { it.groupValues[1].replace("\\", "") }
                .toList()
        } else {
            data.split(",")
        }

        links.forEach { link ->
            val clean = link.trim()
            if (clean.isNotBlank()) {
                loadExtractor(clean, mainUrl, subtitleCallback, callback)
            }
        }
        return true
    }
}