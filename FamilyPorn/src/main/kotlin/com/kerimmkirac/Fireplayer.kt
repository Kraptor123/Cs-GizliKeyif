package com.kerimmkirac

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.fasterxml.jackson.annotation.JsonProperty
import android.util.Log
import com.lagradost.cloudstream3.mapper

class Fireplayer : ExtractorApi() {
    override var name = "Fireplayer"
    override var mainUrl = "https://watchstreamhd.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("Fireplayer", url)

        val videoid = url.substringAfter("/video/").substringBefore("?")
        Log.d("Fireplayer", videoid)

        val pageget = app.get(url, referer = referer)
        val cookies = pageget.cookies

        val posturl = "$mainUrl/player/index.php?data=$videoid&do=getVideo"
        val postdata = mapOf(
            "hash" to videoid,
            "r" to (referer ?: "")
        )

        val postheaders = mapOf(
            "Accept" to "*/*",
            "X-Requested-With" to "XMLHttpRequest",
            "Referer" to url,
            "Origin" to mainUrl,
            "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8"
        )

        val response = app.post(
            posturl,
            data = postdata,
            headers = postheaders,
            cookies = cookies
        ).text

        val json = mapper.readValue(response, FireResponse::class.java)

        val videolink = json.securedlink ?: json.videosource

        if (videolink != null) {
            Log.d("Fireplayer", videolink)
            callback(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = videolink,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$mainUrl/"
                    this.headers = mapOf("Origin" to mainUrl)
                }
            )
        }
    }
}

data class FireResponse(
    @JsonProperty("securedLink") val securedlink: String? = null,
    @JsonProperty("videoSource") val videosource: String? = null
)