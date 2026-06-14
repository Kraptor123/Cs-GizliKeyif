package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.newExtractorLink

open class PerverZijaExtractor : ExtractorApi() {
    override var name = "PerverZija"
    override var mainUrl = "https://pervl2.xtremestream.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("kraptor_PerverZijaExtract", "url = $url")

//        val text = app.get(url, referer = referer).text

//        val minHd = text.substringAfterLast("minhd: ").substringBefore(",")

        val liste = listOf("480","720","1080","2160")

//        Log.d("kraptor_PerverZijaExtract", "minHd = $minHd")

        liste.forEach { kalite ->
            val videoQuality = kalite.toIntOrNull() ?: 720

            val changeUrl = url.replace("index.php","xs1.php") + "&q=$videoQuality"

            Log.d("kraptor_PerverZijaExtract", "changeUrl = $changeUrl")

            callback.invoke(newExtractorLink(
                source = this.name,
                name = this.name,
                url = changeUrl,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = "${mainUrl}/"
                this.quality = videoQuality
            })
        }
    }
}
