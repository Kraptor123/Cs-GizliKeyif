package com.kraptor

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

open class Turtleviplay : ExtractorApi() {
    override var name = "Turtleviplay"
    override var mainUrl = "https://turtleviplay.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url, referer = referer).document
        val m3u8 = res.selectFirst("#video_player")?.attr("data-hash") ?: return

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = m3u8,
                type = ExtractorLinkType.M3U8,
            ) {
                this.referer = url
                this.quality = Qualities.Unknown.value
                this.headers = mapOf(
                    "Origin" to "https://turtleviplay.xyz",
                    "Accept" to "*/*",
                )
            }
        )
    }
}

class Turboviplay : Turtleviplay() {
    override var name = "Turboviplay"
    override var mainUrl = "https://turboviplay.com"
}
