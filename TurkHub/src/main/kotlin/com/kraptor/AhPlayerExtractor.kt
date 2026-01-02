package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class AhPlayerExtractor : ExtractorApi() {
    override var name = "AhPlayer"
    override var mainUrl = "https://ahplayer.site"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        val document = app.get(url, referer = referer, headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Sec-GPC" to "1",
            "Connection" to "keep-alive",
            "Cookie" to "lang=1",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "cross-site",
            "Sec-Fetch-User" to "?1",
            "Priority" to "u=4",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache"
        )).document

        val evalScript = document.selectFirst("script:containsData(eval)")?.data().toString()



        val unpacker = getAndUnpack(evalScript)



        val hlsler = Regex(pattern = "\"hls[0-9]+\":\"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE)).findAll(unpacker)

        hlsler.forEach { hls ->
            val m3u8 = fixUrl(hls.groupValues[1])
            Log.d("kraptor_${this.name}","m3u8 = $m3u8")
            callback.invoke(newExtractorLink(
                this.name,
                this.name,
                m3u8,
                type = ExtractorLinkType.M3U8,
                {
                    this.referer = "${mainUrl}/"
                }
            ))

        }


    }
}
