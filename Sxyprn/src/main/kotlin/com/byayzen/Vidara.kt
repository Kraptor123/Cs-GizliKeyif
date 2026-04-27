package com.byayzen

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink

class Vidara : ExtractorApi() {
    override val name = "Vidara"
    override val mainUrl = "https://vidara.so"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink> {
        val filecode = url.trimEnd('/').split("/").last()

        Log.d("Vidara", filecode)

        val response = app.post(
            "$mainUrl/api/stream",
            headers = mapOf(
                "Accept" to "*/*",
                "Origin" to mainUrl,
                "Referer" to url
            ),
            json = mapOf(
                "filecode" to filecode,
                "device" to "web"
            )
        ).parsedSafe<StreamResponse>()

        val streamurl = response?.streamingurl ?: return emptyList()

        Log.d("Vidara", streamurl)

        return listOf(
            newExtractorLink(
                source = name,
                name = name,
                url = streamurl,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = mainUrl
                this.headers = mapOf(
                    "Origin" to mainUrl,
                    "Referer" to url
                )
            }
        )
    }

    data class StreamResponse(
        @JsonProperty("streaming_url") val streamingurl: String? = null
    )
}