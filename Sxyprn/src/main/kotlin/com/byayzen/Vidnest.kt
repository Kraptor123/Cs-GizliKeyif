package com.byayzen

import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.newExtractorLink

class VidNest : ExtractorApi() {
    override val name = "VidNest"
    override val mainUrl = "https://vidnest.io"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val docHeaders = mapOf(
            "Referer" to "https://vidnest.io/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )

        val text = app.get(url, headers = docHeaders).text

        val videoRegex = """file\s*:\s*["']([^"']+\.mp4[^"']*)["']""".toRegex()
        val labelRegex = """label\s*:\s*["']([^"']+)["']""".toRegex()

        val videoUrl = videoRegex.find(text)?.groupValues?.get(1)
        val label = labelRegex.find(text)?.groupValues?.get(1) ?: "VidNest"

        if (videoUrl != null) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = videoUrl,
                    type = ExtractorLinkType.VIDEO,
                    initializer = {
                        this.referer = "https://vidnest.io/"
                        this.quality = getQualityFromName(label)

                        this.headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0",
                            "Referer" to "https://vidnest.io/",
                            "Accept" to "*/*",
                            "Origin" to "https://vidnest.io",
                            "Connection" to "keep-alive",
                            "Sec-Fetch-Dest" to "video",
                            "Sec-Fetch-Mode" to "no-cors",
                            "Sec-Fetch-Site" to "same-site",
                            "Priority" to "u=4"
                        )
                    }
                )
            )
        }
    }
}