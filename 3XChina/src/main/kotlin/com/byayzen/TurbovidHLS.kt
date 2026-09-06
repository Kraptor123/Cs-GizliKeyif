package com.byayzen

import android.util.Log
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

class TurbovidHLS : ExtractorApi() {
    override val name            = "TurbovidHLS"
    override val mainUrl         = "https://turbovidhls.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (com.lagradost.cloudstream3.SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("TurbovidHLS", "getUrl: $url")

        val response = app.get(
            url,
            referer = referer ?: "$mainUrl/"
        ).document

        val videoUrl = response.selectFirst("#video_player")?.attr("data-hash")?.ifEmpty { null }
            ?: Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""").find(response.html())?.groupValues?.get(1)
            ?: return

        val m3u8Response = app.get(
            videoUrl,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer"    to "$mainUrl/",
                "Origin"     to mainUrl
            )
        ).text

        val matches = Regex("""#EXT-X-STREAM-INF:.*?RESOLUTION=(\d+)x(\d+).*?\n(https?://[^\s]+)""").findAll(m3u8Response).toList()

        matches.forEach { match ->
            val height    = match.groupValues[2].toIntOrNull() ?: Qualities.Unknown.value
            val streamUrl = match.groupValues[3].trim()

            callback(
                newExtractorLink(
                    source = name,
                    name   = name,
                    url    = streamUrl,
                    type   = INFER_TYPE
                ) {
                    this.referer = ""
                    this.headers = mutableMapOf(
                        "User-Agent" to USER_AGENT
                    )
                    this.quality = height
                }
            )
        }
    }
}