package com.kraptor

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class MyDaddyExtractor : ExtractorApi() {
    override val name = "MyDaddy"
    override val mainUrl = "https://mydaddy.cc"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val response = app.get(url, referer = referer).text

        val regex = Regex(pattern = "a href='([^']*)'", options = setOf(RegexOption.IGNORE_CASE))

        val videolar = regex.findAll(response)

        videolar.forEach { video ->
            val video = video.groupValues[1]
            callback.invoke(newExtractorLink(
                this.name,
                this.name,
                fixUrl(video),
                type = INFER_TYPE
            ) {
                this.referer = "$referer"
                this.quality = getQualityFromName(video.substringAfterLast("/").substringBefore("."))
            })
        }

    }
}

