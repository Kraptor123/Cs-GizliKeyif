package com.kerimmkirac

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink

open class BestWish : ExtractorApi() {
    override val name = "BestWish"
    override val mainUrl = "https://bestwish.lol"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val data = url.substringAfterLast("/")

        val videoReq = app.get("${mainUrl}/ajax/stream?filecode=$data", referer = url,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 OPR/120.0.0.0 (Edition developer)",
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.5",
                "Connection" to "keep-alive",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "same-origin",
                "sec-ch-ua-platform" to "\"Windows\"",
                "sec-ch-ua" to "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Opera\";v=\"120\"",
                "sec-ch-ua-mobile" to "?0",
            )
        ).text

        val mapper = jacksonObjectMapper().registerKotlinModule()

        val response: Stream = mapper.readValue<Stream>(videoReq)

        val videoUrl = response.streaming_url

        Log.d("kraptor_BestWish","videoUrl =$videoUrl")

        callback.invoke(newExtractorLink(
            source = this.name,
            name   = this.name,
            url    = videoUrl,
            type   = ExtractorLinkType.M3U8,
            initializer = {
                this.referer = "${mainUrl}/"
            }
        ))


    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Stream(
    val streaming_url: String
)