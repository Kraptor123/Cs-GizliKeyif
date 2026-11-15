package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink

open class Abstream : ExtractorApi() {
    override var name = "Abstream"
    override var mainUrl = "https://abstream.to"
    override val requiresReferer = true
    // null: embed sayfasının referer'ı ile oynat
    open val streamReferer: String? = null

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val extReferer = referer ?: ""
        val response = app.get(url, referer = extReferer, headers = mapOf("Referer" to extReferer, "Accept-Language" to "en-US,en;q=0.5"))
        val htmlContent = response.text.let { if (it.contains("eval(function(p,a,c,k,e")) getAndUnpack(it) else it }
        
        
        val vidstackRegex = Regex("""file:"([^"]*)"""", RegexOption.DOT_MATCHES_ALL)
        val videoUrl = vidstackRegex.find(htmlContent)?.groupValues?.get(1)
        
        Log.d("kraptor_Abstream", "videoUrl = $videoUrl")
        
        return if (videoUrl != null) {
            listOf(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = videoUrl,
                    type = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to (streamReferer ?: extReferer), "Accept-Language" to "en-US,en;q=0.5")
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                }
            )
        } else {
            Log.d("kraptor_LiveCamRips", "Video URL bulunamadı")
            emptyList()
        }
    }
}

// packed jwplayer, CDN sadece kendi referer'ını kabul ediyor
class UqloadVc : Abstream() {
    override var name = "Uqload"
    override var mainUrl = "https://uqload.vc"
    override val streamReferer get() = "$mainUrl/"
}

class VidaraTo : ExtractorApi() {
    override var name = "Vidara"
    override var mainUrl = "https://vidara.to"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val filecode = url.substringBefore("?").trimEnd('/').substringAfterLast("/")
        val stream = app.post(
            "$mainUrl/api/stream",
            headers = mapOf("Origin" to mainUrl, "Referer" to url),
            json = mapOf("filecode" to filecode, "device" to "web")
        ).parsedSafe<VidaraResponse>()?.streamingUrl ?: return null

        return listOf(
            newExtractorLink(name, name, stream, ExtractorLinkType.M3U8) {
                this.referer = "$mainUrl/"
                this.headers = mapOf("Origin" to mainUrl)
            }
        )
    }

    data class VidaraResponse(
        @com.fasterxml.jackson.annotation.JsonProperty("streaming_url") val streamingUrl: String? = null
    )
}
