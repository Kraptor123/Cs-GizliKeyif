package com.kraptor

import android.util.Log
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.AesHelper
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

open class PlayerizExtractor() : ExtractorApi() {

    override val name = "Playeriz"
    override val mainUrl = "https://playeriz.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        val extRef = referer ?: ""

        Log.d("kraptor_$name", "url = $url")

        val istek = app.get(
            url,
            referer = extRef,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0")
        ).document

        val response = istek.select("script").find { it.data().contains("eval(function(p,a,c,k,e,d)") }?.data()?.let { getAndUnpack(it) } ?: ""

        val regex = Regex(pattern = "file:\"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))

        val video = regex.findAll(response)




        video.forEach { video ->
            if (video.groupValues[1].contains(".vtt") || video.groupValues[1].contains(".srt")) {
               Log.d("kraptor_$name","subtitle")
            } else {
                Log.d("kraptor_$name", "video = ${video.groupValues[1]}")
                callback.invoke(
                    newExtractorLink(
                    this.name,
                    this.name,
                    video.groupValues[1],
                    type = ExtractorLinkType.M3U8,
                    {
                        this.referer = referer.toString()
                    }
                ))
            }
        }

    }
}
