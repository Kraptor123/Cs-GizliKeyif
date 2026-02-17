package com.kraptor


import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Chatrubate : MainAPI() {
    override var mainUrl = "https://chaturbate.com"
    override var name = "Chaturbate"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "/api/ts/roomlist/room-list/?limit=90" to "Featured",
        "/api/ts/roomlist/room-list/?genders=f&limit=90" to "Female",
        "/api/ts/roomlist/room-list/?genders=c&limit=90" to "Couples",
        "/api/ts/roomlist/room-list/?regions=NA&limit=90" to "North America",
        "/api/ts/roomlist/room-list/?regions=SA&limit=90" to "South America",
        "/api/ts/roomlist/room-list/?regions=AS&limit=90" to "Asia",
        "/api/ts/roomlist/room-list/?regions=ER&limit=90" to "Europe/Russia",
        "/api/ts/roomlist/room-list/?hashtags=fuckmachine&limit=90" to "Fuck Machine",
        "/api/ts/roomlist/room-list/?hashtags=ebony&limit=90" to "Ebony",
        "/api/ts/roomlist/room-list/?hashtags=milf&limit=90" to "MILF",
        "/api/ts/roomlist/room-list/?hashtags=teen&limit=90" to "Teen",
        "/api/ts/roomlist/room-list/?hashtags=latina&limit=90" to "Latina",
        "/api/ts/roomlist/room-list/?hashtags=asian&limit=90" to "Asian",
        "/api/ts/roomlist/room-list/?hashtags=squirt&limit=90" to "Squirt"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        var offset: Int
        if (page == 1) {
            offset = 0
        } else {
            offset = 90 * (page - 1)
        }
        val responseList =
            app.get("$mainUrl${request.data}&offset=$offset").parsedSafe<Response>()!!.rooms.map { room ->
                newLiveSearchResponse(
                    name = room.username,
                    url = "$mainUrl/${room.username}",
                    type = TvType.Live,
                    fix = false
                ) {
                    posterUrl = room.img
                    lang = null
                }
            }
        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true), hasNext = true)

    }

    override suspend fun search(query: String, page: Int): SearchResponseList {

        val aramaCevap = app.get("$mainUrl/api/ts/roomlist/room-list/?keywords=$query&limit=90&offset=${page}", referer = "${mainUrl}/", headers = mapOf("X-Requested-With" to "XMLHttpRequest"))
            .parsedSafe<Response>()!!.rooms.map { room -> newLiveSearchResponse(
                name = room.username,
                url = "$mainUrl/${room.username}",
                type = TvType.Live,
                fix = false
            ) {
                posterUrl = room.img
                lang = null
            }
            }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().toString()
            .replace("| PornHoarder.tv", "")
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        Log.d("kraptor_$name", "title = $title")

        return newLiveStreamLoadResponse(
            name = title,
            url = url,
            dataUrl = url
        ) {
            posterUrl = poster
            plot = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name","data = $data")
        val doc = app.get(data).document
        val script = doc.select("script").find { item-> item.html().contains("window.initialRoomDossier") }
        val json = script!!.html().substringAfter("window.initialRoomDossier = \"").substringBefore(";").unescapeUnicode()
        val m3u8Url = "\"hls_source\": \"(.*).m3u8\"".toRegex().find(json)?.groups?.get(1)?.value
        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = m3u8Url.toString()+".m3u8",
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = ""
                this.quality = Qualities.Unknown.value
            }
        )

        return true
    }

    data class Room(
        @JsonProperty("img")    val img: String       = "",
        @JsonProperty("username")  val username: String  = "",
        @JsonProperty("subject")  val subject: String  = "",
        @JsonProperty("tags")  val tags: List<String> = arrayListOf()

    )

    data class Response(
        @JsonProperty("all_rooms_count") val all_rooms_count: String      = "",
        @JsonProperty("room_list_id")   val room_list_id: String        = "",
        @JsonProperty("total_count")   val total_count: String        = "",
        @JsonProperty("rooms")  val rooms: List<Room> = arrayListOf()
    )
}

fun String.unescapeUnicode() = replace("\\\\u([0-9A-Fa-f]{4})".toRegex()) {
    String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
}
