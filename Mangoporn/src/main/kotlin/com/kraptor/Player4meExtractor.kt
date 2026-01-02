package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.AesHelper
import com.lagradost.cloudstream3.mapper
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.ExtractorLink

open class Player4Me : ExtractorApi() {
    override var name = "Player4Me"
    override var mainUrl = "https://my.player4me.online"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val id = url.substringAfter("#")
        val response = app.get("$mainUrl/api/v1/video?id=$id",referer= "${mainUrl}/", headers = mapOf(
            "Host" to "my.player4me.online",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Accept" to "*/*",
            "Cookie" to "popunderCount/=1",
        )).text

        val sifreliYanit = response.trim()

        val aesCoz = AesHelper.decryptAES(sifreliYanit, "kiemtienmua911ca", "1234567890oiuytr")

        val map = mapper.readValue<Yanit>(aesCoz)

        callback.invoke(newExtractorLink(
            this.name,
            this.name,
            fixUrl(map.hls),
            ExtractorLinkType.M3U8
        ) {
            this.referer = "${mainUrl}/"
            this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
        })


    }
}
@JsonIgnoreProperties(ignoreUnknown = true)
data class Yanit(
    val hls: String
)