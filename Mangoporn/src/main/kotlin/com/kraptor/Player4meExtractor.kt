package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.AesHelper
import com.lagradost.cloudstream3.utils.*

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

        val response = app.get("$mainUrl/api/v1/video?id=$id", referer = "${mainUrl}/", headers = mapOf(
            "Host" to mainUrl.substringAfter("://"),
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Accept" to "*/*",
            "Cookie" to "popunderCount/=1",
        ))

        val sifreliYanit = response.text.trim()

        if (sifreliYanit.startsWith("<html>")) {
            return
        }

        val aesCoz = AesHelper.decryptAES(sifreliYanit, "kiemtienmua911ca", "1234567890oiuytr")

        val map = mapper.readValue<Yanit>(aesCoz)
        val videoUrl = map.source ?: map.hls ?: map.cf

        if (videoUrl != null) {
            callback.invoke(newExtractorLink(
                this.name,
                this.name,
                fixUrl(videoUrl),
                ExtractorLinkType.M3U8
            ) {
                this.referer = "${mainUrl}/"
                this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
            })
        }
    }
}

class Vip4me : Player4Me() {
    override var mainUrl = "https://vip.player4me.vip"
    override var name = "Player4Me"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Yanit(
    val hls: String? = null,
    val source: String? = null,
    val cf: String? = null
)