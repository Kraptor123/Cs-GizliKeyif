package com.kraptor

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities

class TurboCr : ExtractorApi() {
    override var name = "TurboCr"
    override var mainUrl = "https://turbo.cr"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val videoId = Regex("""turbo\.cr/(?:v|embed)/([a-zA-Z0-9_-]+)""")
            .find(url)?.groupValues?.getOrNull(1)

        val response = app.get(
            "https://turbo.cr/api/sign?v=$videoId",
            headers = mapOf(
                "Referer" to url,
                "Accept" to "application/json"
            )
        )

        val json = response.text
        val signedUrl = Regex(""""url"\s*:\s*"([^"]+)"""")
            .find(json)?.groupValues?.getOrNull(1)
            ?.replace("\\u0026", "&")
            ?.replace("\\/", "/")
            ?.replace("&amp;", "&")

        callback.invoke(
            newExtractorLink(
                source = "TurboCr",
                name = "TurboCr",
                url = signedUrl.toString(),
                type = INFER_TYPE
            ) {
                this.referer = "https://turbo.cr/"
                this.quality = Qualities.Unknown.value
                this.headers = mapOf("Referer" to "https://turbo.cr/")
            }
        )
    }
}
