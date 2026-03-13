package com.kraptor

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Server1uns : VidStack() {
    override var name = "Server1uns"
    override var mainUrl = "https://server1.uns.bio"
    override var requiresReferer = true
}

class MyUpns : VidStack() {
    override var name = "UPNS"
    override var mainUrl = "https://my.upns.online"
    override var requiresReferer = true
}

class MyEmbedSeek : VidStack() {
    override var name = "UPNS"
    override var mainUrl = "https://my.embedseek.online"
    override var requiresReferer = true
}

class VipSeekPlayer : VidStack() {
    override var name = "UPNS"
    override var mainUrl = "https://vip.seekplayer.vip"
    override var requiresReferer = true
}

class Videosh : VidStack() {
    override var name = "Videosh"
    override var mainUrl = "https://videosh.upns.live"
    override var requiresReferer = true
}

class LiveCamR : VidStack() {
    override var name = "LiveCamRips"
    override var mainUrl = "https://videosh.upns.live"
    override var requiresReferer = true
}

open class VidStack : ExtractorApi() {
    override var name = "Vidstack"
    override var mainUrl = "https://vidstack.io"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:147.0) Gecko/20100101 Firefox/147.0",
            "Origin" to "https://videosh.upns.live",
            "Referer" to "https://videosh.upns.live/"
        )

        val hash = url.substringAfterLast("#").substringAfter("/")
        val baseurl = getBaseUrl(url)

        val encoded = app.get("$baseurl/api/v1/video?id=$hash&r=livecamrips.to", headers = headers).text.trim()

        val key = "kiemtienmua911ca"
        val ivList = listOf("1234567890oiuytr", "0123456789abcdef")

        val decryptedText = ivList.firstNotNullOfOrNull { iv ->
            try {
                AesHelper.decryptAES(encoded, key, iv)
            } catch (e: Exception) {
                null
            }
        } ?: throw Exception("Failed to decrypt with all IVs")

        val m3u8 = Regex("\"source\":\"(.*?)\"").find(decryptedText)
            ?.groupValues?.get(1)
            ?.replace("\\/", "/") ?: ""

        if (m3u8.isNotEmpty()) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = m3u8
                ) {
                    this.referer = "https://videosh.upns.live/"
                    this.quality = getQualityFromName(this.name)
                    this.headers = headers
                    this.type = ExtractorLinkType.M3U8
                }
            )
        }
    }

    private fun getBaseUrl(url: String): String {
        return try {
            URI(url).let { "${it.scheme}://${it.host}" }
        } catch (e: Exception) {
            mainUrl
        }
    }

    private fun getQualityFromName(name: String): Int {
        val lower = name.lowercase().trim()
        return when {
            "1080" in lower || "1080p" in lower -> 1080
            "720" in lower || "720p" in lower -> 720
            "480" in lower || "480p" in lower -> 480
            "360" in lower || "360p" in lower -> 360
            else -> 0
        }
    }
}

object AesHelper {
    private const val TRANSFORMATION = "AES/CBC/PKCS5PADDING"

    fun decryptAES(inputHex: String, key: String, iv: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val ivSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(inputHex.hexToByteArray())
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Hex string must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}