package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.AesHelper
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.*

open class UpnsLiveExtractor() : ExtractorApi() {

    override val name = "UpnsLive"
    override val mainUrl = "https://plyr.upns.live"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        Log.d("kraptor_$name", "url = $url")

        val id = url.substringAfter("#")

        val istek = app.get(
            "${mainUrl}/api/v1/video?id=$id",
            referer = "${mainUrl}/",
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0")
        ).text.trim()

        Log.d("kraptor_$name", "istek = $istek")
        Log.d("kraptor_$name", "istek length = ${istek.length}")

        // Python byte değerlerinin ASCII karşılıkları (normal string)
        val key = "kiemtienmua911ca"
        val iv = "1234567890oiuytr"

        // Hex string çift sayıda karakter olmalı
        val edataHex = if (istek.length % 2 != 0) {
            istek.dropLast(1)
        } else {
            istek
        }

        Log.d("kraptor_$name", "edataHex length = ${edataHex.length}")

        try {
            val aesCoz = AesHelper.decryptAES(edataHex, key, iv)
            Log.d("kraptor_$name", "aesCoz = $aesCoz")

            // JSON parse et
            val jsonData = parseJson<VideoResponse>(aesCoz)

            if (jsonData?.title != null) {
                // Video URL'sini oluştur: mainUrl + /djx/ + title
                val videoUrl = jsonData.cf ?: jsonData.source

                Log.d("kraptor_$name", "videoUrl = $videoUrl")

                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = name,
                        url = videoUrl.toString(),
                        type = ExtractorLinkType.M3U8,
                        initializer = {
                            this.referer = "${mainUrl}/"
                            this.headers =
                                mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0")
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("kraptor_$name", "Decryption error: ${e.message}")
        }
    }

    // JSON response için data class
    data class VideoResponse(
        val torrentTrackers: List<String>?,
        val title: String?,
        val version: String?,
        val thumbnail: String?,
        val metric: Metric?,
        val userId: String?,
        val iceServers: List<IceServer>?,
        val poster: String?,
        val visitorCountry: String?,
        val source: String?,
        val cf: String?,
        val player: Player?,
        val swarmId: String?
    )

    data class Metric(
        val os: String?,
        val cfDomain: String?,
        val streamerId: String?,
        val userId: String?,
        val videoId: String?,
        val timezone: String?,
        val playerId: String?,
        val language: String?,
        val platform: String?,
        val browser: String?,
        val country: String?,
        val city: String?,
        val ipAddress: String?,
        val impression: Int?
    )

    data class IceServer(
        val urls: String?
    )

    data class Player(
        val allowDownload: Boolean?,
        val isPremium: Boolean?,
        val allowErotic: Boolean?,
        val iframeApi: Boolean?,
        val restrictEmbed: String?,
        val translation: String?,
        val defaultSubtitle: String?,
        val ui: String?,   // JSON string, ayrı modele parse etmek istersen ayrıca yapabilirsin
        val pickSubtitle: Boolean?,
        val logo: String?,
        val id: String?,
        val userId: String?,
        val defaultAudio: String?,
        val allowExternal: Boolean?,
        val allowAdblock: Boolean?,
        val restrictCountry: String?
    )
}