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
        val iframeAl = app.get(url, referer = extReferer).document
        val packedScript = iframeAl.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data().toString()
        val evalRegex = Regex("""eval\(.*?\)\)\)""", RegexOption.DOT_MATCHES_ALL)
        val packedCode = evalRegex.find(packedScript)?.value
        val unpackedJs = JsUnpacker(packedCode).unpack().toString()
        val regex = Regex(pattern = "file:\"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))
        val videoUrl = regex.find(unpackedJs)?.groupValues[1].toString()
        Log.d("kraptor_$name", "videoUrl = ${videoUrl}")


        return listOf(
            newExtractorLink(
                source = name,
                name = name,
                url = videoUrl,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to extReferer)
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }
    }