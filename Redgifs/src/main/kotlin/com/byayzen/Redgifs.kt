// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Redgifs : MainAPI() {
    override var mainUrl = "https://www.redgifs.com"
    override var name = "Redgifs"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)


    companion object {
        private var cachedtoken: String? = null
        private var lasttokentime: Long = 0
        private const val useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0"

        private fun getauthheaders(token: String?) = mapOf(
            "Authorization" to (token ?: ""),
            "User-Agent" to useragent,
            "Referer" to "https://www.redgifs.com/"
        )
    }

    override val mainPage = mainPageOf(
        "https://api.redgifs.com/v2/gifs/search?type=g&order=trending&count=30&verified=y" to "Trending",
        "https://api.redgifs.com/v2/gifs/search?type=g&order=latest&count=30" to "Latest",
        "https://api.redgifs.com/v2/gifs/search?type=g&order=top&count=30" to "Top"
    )

    private fun getredgifsid(url: String) = url.substringAfter("watch/").substringBefore("?").substringBefore("/").lowercase()

    private suspend fun getauthtoken(): String? {
        val isexpired = System.currentTimeMillis() - lasttokentime >= 600000
        if (cachedtoken != null && !isexpired) return cachedtoken

        val response = app.get("https://api.redgifs.com/v2/auth/temporary", headers = mapOf("User-Agent" to useragent))
        if (response.code != 200) return null

        return ("Bearer " + response.text.substringAfter("token\":\"").substringBefore("\"")).also {
            cachedtoken = it
            lasttokentime = System.currentTimeMillis()
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val apiurl = if (request.data.contains("?")) "${request.data}&page=$page" else "${request.data}?page=$page"
        val data = coresearch(apiurl)
        return newHomePageResponse(request.name, data?.gifs?.mapNotNull { it.toSearchResponse() } ?: emptyList(), true)
    }

    private fun Gif.toSearchResponse(): SearchResponse? {
        if (sexuality?.any { it.equals("gay", ignoreCase = true) } == true) return null
        val title = tags?.take(3)?.joinToString(" ") ?: id?.replaceFirstChar { it.uppercase() } ?: "Video"
        return newMovieSearchResponse(title, "https://www.redgifs.com/watch/$id", TvType.NSFW) {
            this.posterUrl = urls?.thumbnail ?: urls?.poster
        }
    }

    private suspend fun coresearch(url: String): SearchResponseData? {
        val token = getauthtoken() ?: return null
        return app.get(url, headers = getauthheaders(token)).parsedSafe<SearchResponseData>()
    }



    override suspend fun search(query: String, page: Int): SearchResponseList {
        val searchurl = "https://api.redgifs.com/v2/gifs/search?type=g&order=score&count=40&page=$page&query=$query"
        val data = coresearch(searchurl)
        val results = data?.gifs?.mapNotNull { it.toSearchResponse() } ?: emptyList()
        val hasmore = if (data?.total != null) (page * 40) < data.total else results.isNotEmpty()
        return newSearchResponseList(results, hasmore)
    }

    override suspend fun load(url: String): LoadResponse? {
        val videoid = getredgifsid(url)
        val token = getauthtoken() ?: return null
        val headers = getauthheaders(token)

        val maingif = app.get("https://api.redgifs.com/v2/gifs/$videoid", headers = headers)
            .parsedSafe<VideoResponse>()?.gif ?: return null

        val recs = coresearch("https://api.redgifs.com/v2/gifs/search?type=g&order=trending&count=15")
        val recommendations = recs?.gifs?.mapNotNull { if (it.id == videoid) null else it.toSearchResponse() } ?: emptyList()

        val actors = maingif.userName?.let { name ->
            listOf(Actor(name, maingif.urls?.thumbnail))
        }

        return newMovieLoadResponse(maingif.id ?: "Video", url, TvType.NSFW, url) {
            this.posterUrl = maingif.urls?.poster ?: maingif.urls?.thumbnail
            this.plot = maingif.description
            this.tags = maingif.tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val videoid = getredgifsid(data)
        val token = getauthtoken() ?: return false
        val urls = app.get("https://api.redgifs.com/v2/gifs/$videoid", headers = getauthheaders(token)).parsedSafe<VideoResponse>()?.gif?.urls ?: return false

        val sources = listOfNotNull(urls.hd?.let { Qualities.P1080.value to it }, urls.sd?.let { Qualities.P480.value to it })
        sources.forEach { (quality, link) ->
            callback.invoke(newExtractorLink(name, "$name ${if (quality == Qualities.P1080.value) "HD" else "SD"}", link, type = ExtractorLinkType.VIDEO) {
                this.quality = quality
                this.referer = "https://www.redgifs.com/"
            })
        }
        return sources.isNotEmpty()
    }

    data class SearchResponseData(@JsonProperty("gifs") val gifs: List<Gif>? = null, @JsonProperty("total") val total: Int? = null)
    data class VideoResponse(@JsonProperty("gif") val gif: Gif? = null)
    data class Gif(@JsonProperty("id") val id: String?, @JsonProperty("urls") val urls: Links?, @JsonProperty("userName") val userName: String?, @JsonProperty("description") val description: String?, @JsonProperty("tags") val tags: List<String>?, @JsonProperty("sexuality") val sexuality: List<String>?)
    data class Links(@JsonProperty("hd") val hd: String?, @JsonProperty("sd") val sd: String?, @JsonProperty("poster") val poster: String?, @JsonProperty("thumbnail") val thumbnail: String?)
}