package com.byayzen

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URI

open class LuluBase : ExtractorApi() {
    override val name = "LuluStream"
    override val mainUrl = "https://lulustream.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val embedUrl = url.replace("/d/", "/e/")

            val currentHost = try { URI(embedUrl).host } catch (e: Exception) { "lulustream.com" }
            val currentOrigin = "https://$currentHost"

            val requestHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to (referer ?: embedUrl)
            )

            val response = app.get(embedUrl, headers = requestHeaders)
            val html = response.text
            val m3u8Regex = """["']([^"']+\.m3u8[^"']*)["']""".toRegex()
            val match = m3u8Regex.find(html)

            if (match != null) {
                val m3u8Url = match.groupValues[1]

                val videoHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer" to embedUrl,
                    "Origin" to currentOrigin,
                    "Accept" to "*/*"
                )

                M3u8Helper.generateM3u8(
                    source = this.name,
                    streamUrl = m3u8Url,
                    referer = embedUrl,
                    headers = videoHeaders
                ).forEach(callback)
            }
        } catch (e: Exception) {
        }
    }
}

class LuluStream : LuluBase() {
    override val name = "LuluStream"
    override val mainUrl = "https://lulustream.com"
}

class LuluVid : LuluBase() {
    override val name = "LuluVid"
    override val mainUrl = "https://luluvid.com"
}

class LuluVdo : LuluBase() {
    override val name = "LuluVdo"
    override val mainUrl = "https://luluvdo.com"
}

class LuluPvp : LuluBase() {
    override val name = "LuluPvp"
    override val mainUrl = "https://lulupvp.com"
}