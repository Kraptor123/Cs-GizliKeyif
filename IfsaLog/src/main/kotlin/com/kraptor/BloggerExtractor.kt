package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink

open class BloggerExtractor : ExtractorApi() {
    override val name            = "Blogger"
    override val mainUrl         = "https://www.blogger.com/"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        Log.d("kraptor_${this.name}", "url = $url")
        val headers = mapOf(
            "Accept" to "*/*",
            "Connection" to "keep-alive",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site",
            "Origin" to mainUrl,
            "User-Agent" to USER_AGENT,
        )

        val document = app.get(url, headers).text

        val regex = Regex(pattern = "\"play_url\":\"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))

        val videoUrl = regex.find(document)?.groupValues[1].toString()

        loadExtractor(videoUrl, subtitleCallback, callback)

    }
}