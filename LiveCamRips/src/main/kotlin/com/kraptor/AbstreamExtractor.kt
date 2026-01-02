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

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val extReferer = referer ?: ""
        val response = app.get(url, referer = extReferer, headers = mapOf("Referer" to extReferer, "Accept-Language" to "en-US,en;q=0.5"))
        val htmlContent = response.text
        
        
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
                    headers = mapOf("Referer" to extReferer, "Accept-Language" to "en-US,en;q=0.5")
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                }
            )
        } else {
            Log.d("kraptor_LiveCamRips", "Video URL bulunamadı")
            emptyList()
        }
    }
}