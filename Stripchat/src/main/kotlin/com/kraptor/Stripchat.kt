package com.kraptor

import com.fasterxml.jackson.annotation.JsonProperty
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Stripchat : MainAPI() {
    override var mainUrl              = "https://stripchat.com"
    override var name                 = "Stripchat"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded
    private val apiUrl = "$mainUrl/api/front/models/get-list"
    private val excludeIdsMap : MutableMap<String,MutableList<Int>> = mutableMapOf()

    override val mainPage = mainPageOf(
            "girls" to "Girls",
            "couples" to "Couples",
//            "men" to "Men",
//            "trans" to "Trans",
        )
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
            val map = mapOf(
                "favoriteIds" to mutableListOf<String>(),
                "limit" to 60,
                "offset" to 0,
                "primaryTag" to request.data,
                "sortBy" to "viewersRating",
                "userRole" to "guest",
                "improveTs" to false,
                "excludeModelIds" to (excludeIdsMap[request.data] ?: mutableListOf<Int>()),
                "isRecommendationDisabled" to false,)
            val eList : MutableList<Int> = mutableListOf<Int>()
            val responseList = app.post(apiUrl, json = map).parsedSafe<Response>()!!.models.map { model->
                eList.add(model.id.toInt())
                newLiveSearchResponse(
                    name      = model.username,
                    url       = "$mainUrl/${model.username}",
                    type      = TvType.Live,
                    fix = false
                ) {
                    posterUrl = model.previewUrlThumbSmall
                    lang = null
                }
            }
            if(excludeIdsMap[request.data].isNullOrEmpty())
            {
                excludeIdsMap[request.data] = mutableListOf<Int>()
                excludeIdsMap[request.data]?.addAll(eList)
            }
            else
            {
                excludeIdsMap[request.data]?.addAll(eList)
            }
            return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true),hasNext = true)

    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.select(".model-list-item-username").text()
        val href = mainUrl + this.select(".model-list-item-link").attr("href")
        val posterUrl = this.selectFirst(".image-background")?.attr("src")
        return newLiveSearchResponse(
            name      = title,
            url       = href,
            type      = TvType.Live,
            fix = false
        ) {
            this.posterUrl = posterUrl
            lang = null
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val headers = mutableMapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
        )
        val doc = app.get("$mainUrl/search/models/$query",headers = headers).document
        return doc.select(".model-list-item").map { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val headers = mutableMapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
        )
        val document = app.get(url,headers = headers).document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().toString().replace("| PornHoarder.tv","")
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
    

         return newLiveStreamLoadResponse(
            name      = title,
            url       = url,
            dataUrl   = url
         ) {
             posterUrl = poster
             plot = description
         }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document
        val script = doc.select("script").find { item-> item.html().contains("window.__PRELOADED_STATE__") }
        val json = script!!.html().unescapeUnicode()
        val streamName = json.substringAfter("\"streamName\":\"").substringBefore("\",")
        val streamHost = json.substringAfter("\"hlsStreamHost\":\"").substringBefore("\",")
        val hlsUrlTemplate = json.substringAfter("\"hlsStreamUrlTemplate\":\"").substringBefore("\",")
        val finalm3u8Url = hlsUrlTemplate.replace("{cdnHost}",streamHost).replace("{streamName}",streamName).replace("{suffix}","_auto")
        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = finalm3u8Url,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = ""
                this.quality = Qualities.Unknown.value
            }
        )

        return true
    }

    data class Model(
        @JsonProperty("hlsPlaylist")    val hlsPlaylist: String       = "",
        @JsonProperty("id")  val id: String  = "",
        @JsonProperty("previewUrlThumbSmall")  val previewUrlThumbSmall: String  = "",
        @JsonProperty("username")  val username: String  = ""

    )

    data class Response(
        @JsonProperty("models")  val models: List<Model> = arrayListOf()
    )
}

fun String.unescapeUnicode() = replace("\\\\u([0-9A-Fa-f]{4})".toRegex()) {
    String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
}
