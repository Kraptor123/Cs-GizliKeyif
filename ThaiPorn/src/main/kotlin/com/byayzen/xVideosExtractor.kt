package com.byayzen

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
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

        hlsUrl?.let { videoUrl ->
            callback(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = videoUrl,
                )
            )
        }
    }
}