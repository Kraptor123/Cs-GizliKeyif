package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class TurboVidHlsExtractor : ExtractorApi() {
    override var name = "TurboVidHls"
    override var mainUrl = "https://turbovidhls.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url.replace(".mp4", ""), referer = referer)
        val source = document.document.selectFirst("div#video_player")?.attr("data-hash").toString()
        val regex = Regex(pattern = " var urlPlay = '([^']*)';", options = setOf(RegexOption.IGNORE_CASE))

        val video = source.ifEmpty {
            Log.d("gizlikeyif_$name","source bos geldi")
            regex.find(document.text)?.groupValues[1].toString()
        }

        Log.d("gizlikeyif_$name","video = $video")
        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                video,
                type = INFER_TYPE
            ) {
                this.headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.5",
                    "Origin" to mainUrl,
                    "Sec-GPC" to "1",
                    "Connection" to "keep-alive",
                    "Sec-Fetch-Dest" to "empty",
                    "Sec-Fetch-Mode" to "cors",
                    "Sec-Fetch-Site" to "cross-site",
                    "Pragma" to "no-cache",
                    "Cache-Control" to "no-cache"
                )
            })
    }
}
