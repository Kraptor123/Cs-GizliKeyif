package com.byayzen

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

open class xVideosExtractor : ExtractorApi() {
    override val name = "XVideos"
    override val mainUrl = "https://www.xvideos.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).text


        val hlsRegex = Regex("""html5player\.setVideoHLS\(['"]([^'"]+)['"]""")
        val hlsUrl = hlsRegex.find(document)?.groupValues?.get(1)

        if (hlsUrl != null) {
            callback(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = hlsUrl,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$mainUrl/"
                }
            )
            return
        }

        // HLS yoksa dogrudan MP4 linkleri
        listOf("High" to Qualities.P720.value, "Low" to Qualities.P360.value).forEach { (key, quality) ->
            val mp4Url = Regex("""html5player\.setVideoUrl$key\(['"]([^'"]+)['"]""").find(document)?.groupValues?.get(1) ?: return@forEach
            callback(
                newExtractorLink(
                    source = this.name,
                    name = "${this.name} $key",
                    url = mp4Url,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.referer = "$mainUrl/"
                    this.quality = quality
                }
            )
        }
    }
}