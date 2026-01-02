package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink

open class Xpornium : ExtractorApi() {
    override var name = "Xpornium"
    override var mainUrl = "https://xpornium.net"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val iframeAl = app.get(url).text
        val regex = Regex(pattern = "XPSYS\\('([^']*)'\\);", options = setOf(RegexOption.IGNORE_CASE))
        val videob64 = regex.find(iframeAl)?.groupValues[1].toString()
        val video    = fixUrl(base64Decode(videob64))
        Log.d("kraptor_$name", "videob64 = ${videob64}")
        Log.d("kraptor_$name", "video = ${video}")

        return listOf(newExtractorLink(
            source = "Xpornium",
            name   = "Xpornium",
            url    = video,
            type   = ExtractorLinkType.VIDEO
        ))
    }
}