package com.byayzen

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.getQualityFromName

class OKHD : ExtractorApi() {
    override var name = "OKHD"
    override var mainUrl = "https://okhd.nu"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url, referer = referer)
        val unpacked = getAndUnpack(res.text)

        val fileUrl = Regex("""file:\s*["']([^"']+)["']""").find(unpacked)?.groupValues?.get(1)
            ?: Regex("""sources:\s*\[\{\s*file:\s*["']([^"']+)["']""").find(unpacked)?.groupValues?.get(1)

        if (fileUrl != null) {
            val isM3u8 = fileUrl.contains(".m3u8")
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = fileUrl,
                    type = if (isM3u8) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                ) {
                    this.referer = url
                    this.quality = getQualityFromName("")
                    this.headers = mapOf(
                        "Origin" to mainUrl,
                        "Accept" to "*/*",
                        "Connection" to "keep-alive",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:154.0) Gecko/20100101 Firefox/154.0"
                    )
                }
            )
        }
    }
}
