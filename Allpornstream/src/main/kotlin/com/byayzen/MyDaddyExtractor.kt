package com.byayzen

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import android.util.Log

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
        val res = app.get(url, referer = "https://diepornos.com/")
        val html = res.text
        Log.d(name, html.take(500))

        val sourceregex = Regex("""<source src=\\"(.*?)\\" title=\\"(.*?)\\"""")
        val matches = sourceregex.findAll(html).toList()
        Log.d(name, "${matches.size}")

        matches.forEach { match ->
            val rawurl = match.groupValues[1].replace("\\", "")
            val finalurl = if (rawurl.startsWith("//")) "https:$rawurl" else rawurl

            Log.d(name, finalurl)

            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = finalurl,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.referer = mainUrl
                }
            )
        }
    }
}