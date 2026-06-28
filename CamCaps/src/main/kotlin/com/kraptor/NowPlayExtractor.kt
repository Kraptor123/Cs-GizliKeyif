package com.kraptor


import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*


class Vidello : NowPlayExtractor(){
    override val name = "Vidello"
    override val mainUrl = "https://vidello.net"
    override val requiresReferer = true
}
open class NowPlayExtractor : ExtractorApi() {
    override val name = "NowPlay"
    override val mainUrl = "https://nowplay.to/"
    override val requiresReferer = true


    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        Log.d("kraptor_${this.name}", "url = $url")
        val pageHtml = app.get(url, referer = extRef).text

        val unpack = getAndUnpack(pageHtml)

        Log.d("kraptor_${this.name}", "unpack = $unpack")

        val fileUrl = unpack.substringAfter("file:\"").substringBefore("\"")

        callback.invoke(
            newExtractorLink(
                source = name,
                name = "NowPlay",
                url = fileUrl,
                type = INFER_TYPE
            ) {
                quality = Qualities.Unknown.value
                this.referer = url
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0"
                )
            }
        )
    }
}
