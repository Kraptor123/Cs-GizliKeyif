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
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

open class CdnFastExtractor : ExtractorApi() {
    override val name = "CdnFast"
    override val mainUrl = "https://cdnfast.sbs"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url, referer = referer).document

        val scriptText = document.select("script").joinToString("\n") { it.html() }
        val fileRegex = """file:\s*["']([^"']+)["']""".toRegex()
        val fileMatch = fileRegex.find(scriptText)
        val playlistPath = fileMatch?.groupValues?.get(1)

        val baseUrl = url.substringBefore("/player.php")
        val fullM3u8Url = "$baseUrl$playlistPath"

        Log.d("kraptor_${this.name}","fullM3u8Url = $fullM3u8Url")

        val playlistResponse = app.get(fullM3u8Url, referer = baseUrl)
        val playlistContent = playlistResponse.text


        callback.invoke(
            newExtractorLink(
                name = "PornoAnne",
                source = "PornoAnne",
                url = fullM3u8Url,
                type = ExtractorLinkType.M3U8
            ){
                this.referer = baseUrl
                this.quality = Qualities.Unknown.value
            }
        )
    }
}
