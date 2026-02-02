package com.kraptor

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
        val fixedurl = if (url.startsWith("//")) "https:$url" else url
        val iframeAl = app.get(fixedurl).text
        val regex = Regex(pattern = "XPSYS\\('([^']*)'\\);", options = setOf(RegexOption.IGNORE_CASE))
        val videob64 = regex.find(iframeAl)?.groupValues?.get(1) ?: return null
        val video    = fixUrl(base64Decode(videob64))

        return listOf(newExtractorLink(
            source = "Xpornium",
            name   = "Xpornium",
            url    = video,
            type   = ExtractorLinkType.VIDEO
        ))
    }
}