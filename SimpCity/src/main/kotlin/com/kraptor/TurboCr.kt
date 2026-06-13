package com.kraptor

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.newExtractorLink
import android.util.Log
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
        Log.d("kraptor_TurboCr", url)

        val videoId = Regex("""turbo\.cr/(?:v|embed)/([a-zA-Z0-9_-]+)""")
            .find(url)?.groupValues?.getOrNull(1)

        Log.d("kraptor_SimpCity", "Direkt: Video ID = $videoId, sign isteği atılıyor...")

            val response = app.get(
                "https://turbo.cr/api/sign?v=$videoId",
                headers = mapOf(
                    "Referer" to url,
                    "Accept" to "application/json"
                )
            )

            val json = response.text
            Log.d("kraptor_SimpCity", "Direkt: Sign API response = $json")

            // JSON'dan signed URL'yi çıkar
            // Response: {"success":true,"url":"https://dl7.turbocdn.st/data/<hash>.mp4?exp=...&token=...&fn=..."}
            val signedUrl = Regex(""""url"\s*:\s*"([^"]+)"""")
                .find(json)?.groupValues?.getOrNull(1)
                ?.replace("\\u0026", "&")
                ?.replace("\\/", "/")
                ?.replace("&amp;", "&")

            Log.d("kraptor_BunkrCr", "Direkt: Signed URL = $signedUrl")

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