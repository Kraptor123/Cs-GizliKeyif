package com.kraptor

import android.util.Log
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
        val embedUrl = url.replace("/d/", "/e/")

        val currentHost = try {
            URI(embedUrl).host
        } catch (e: Exception) {
            "lulustream.com"
        }
        val currentOrigin = "https://$currentHost"

        val requestHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer" to (referer ?: embedUrl)
        )

        val response = app.get(embedUrl, headers = requestHeaders)
        val html = response.document

        val eval = html.selectFirst("script[type=text/javascript]:containsData(eval)")?.data() ?: ""

        val unpack = getAndUnpack(eval)

        val regex = Regex(pattern = "\\{sources:\\[\\{file:\"([^\"]*)\"\\}", options = setOf(RegexOption.IGNORE_CASE))

        val video = regex.find(unpack)?.groupValues[1]

        callback.invoke(newExtractorLink(
            source = name,
            name = name,
            url = video ?: "",
            type = ExtractorLinkType.M3U8,
            {
                this.headers = requestHeaders
            }

        ))

        Log.d("kraptor_Lulu","video = $video")

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