package com.kerimmkirac

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.getQualityFromString
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.fixUrl
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink

open class CdnVidExtractor : ExtractorApi() {
    override val name = "CdnFast"
    override val mainUrl = "https://cdnvid.icu"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        val data = url.substringAfterLast("-")

        val document = app.post("$mainUrl/player/ajax_sources.php", referer = referer, data = mapOf("vid" to data)).text

        val mapper = jacksonObjectMapper().registerKotlinModule()

        val response: GelenVideo = mapper.readValue<GelenVideo>(document)

        response.source.forEach { video ->
            val videoUrl = fixUrl(video.file)
            val videoLabel = video.label

            Log.d("kraptor_${this.name}","videoUrl = $videoUrl")
            Log.d("kraptor_${this.name}","videoLabel = $videoLabel")

            callback.invoke(newExtractorLink(
                source = this.name,
                name   = this.name,
                url    = videoUrl,
                type   = INFER_TYPE
            ) {
                this.quality = getQualityFromName(videoLabel)
            })
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GelenVideo(
    val source: List<Icerik>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Icerik(
    val file: String,
    val label: String
)
