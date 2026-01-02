// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.w3c.dom.*
import javax.xml.parsers.DocumentBuilderFactory
import android.util.Base64

open class VideoHu : ExtractorApi() {
    override val name = "Videa"
    override val mainUrl = "https://videa.hu"
    override val requiresReferer = false

    companion object {
        private const val STATIC_SECRET = "xHb0ZvME5q8CBcoQi6AngerDu3FGO9fkUlwPmLVY_RTzj2hJIS4NasXWKy1td7p"

        // RC4 decryption
        fun rc4(cipher: ByteArray, key: String): String {
            val S = ByteArray(256) { it.toByte() }
            val K = key.toByteArray(Charsets.UTF_8)
            var j = 0
            for (i in 0 until 256) {
                j = (j + S[i] + K[i % K.size]) and 0xFF
                S[i] = S[j].also { S[j] = S[i] }
            }
            val result = ByteArray(cipher.size)
            var i = 0
            j = 0
            for (n in cipher.indices) {
                i = (i + 1) and 0xFF
                j = (j + S[i]) and 0xFF
                S[i] = S[j].also { S[j] = S[i] }
                val Kbyte = S[(S[i] + S[j]) and 0xFF]
                result[n] = (cipher[n].toInt() xor Kbyte.toInt()).toByte()
            }
            return result.toString(Charsets.UTF_8)
        }
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("kraptor_$name", "Fetching URL: $url")
        // Download initial page or player
        val pageContent = app.get(url, referer = referer).text

        // Determine player URL
        val playerUrl = if (url.contains("videa.hu/player")) {
            url
        } else {
            Regex("<iframe.*?src=\\\"(/player\\?[^\\\"]+)\\\"")
                .find(pageContent)?.groupValues?.get(1)
                ?.let { url.substringBefore("/videa.hu") + it } ?: url
        }

        // Download player page
        val playerResp = app.get(playerUrl, referer = url)
        val playerHtml = playerResp.text

        // Extract nonce
        val nonce = Regex("_xt\\s*=\\s*\\\"([^\\\"]+)\\\"")
            .find(playerHtml)?.groupValues?.get(1)
            ?: throw Error("Nonce not found")
        val l = nonce.substring(0, 32)
        val s = nonce.substring(32)
        var result = ""
        for (i in 0 until 32) {
            val idx = STATIC_SECRET.indexOf(l[i])
            result += s[i - (idx - 31)]
        }

        // Build query parameters
        val randomSeed = (1..8).map { ('A'..'Z') + ('a'..'z') + ('0'..'9') }
            .joinToString("") { it[randomSeedIndex()].toString() }
        val tParam = result.substring(0, 16)
        val query = mapOf(
            "v" to url.substringAfterLast("v="),
            "_s" to randomSeed,
            "_t" to tParam
        )

        // Request XML info
        val xmlResp = app.get("https://videa.hu/player/xml", referer = playerUrl, params = query)
        val xmlBody = xmlResp.text

        val xmlString = if (xmlBody.trimStart().startsWith("<?xml")) {
            xmlBody
        } else {
            // Encrypted: base64 -> rc4
            val b64 = Base64.decode(xmlBody, Base64.DEFAULT)
            val key = result.substring(16) + randomSeed + xmlResp.headers["x-videa-xs"]
            rc4(b64, key)
        }

        // Parse XML
        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = db.parse(xmlString.byteInputStream())
        val video = doc.getElementsByTagName("video").item(0) as Element
        val title = video.getElementsByTagName("title").item(0).textContent

        val sources = doc.getElementsByTagName("video_source")
        val hashValues = doc.getElementsByTagName("hash_values").item(0) as? Element

        for (i in 0 until sources.length) {
            val src = sources.item(i) as Element
            var videoUrl = src.textContent
            val name = src.getAttribute("name")
            val exp = src.getAttribute("exp")
            if (hashValues != null && hashValues.getElementsByTagName("hash_value_$name").length > 0) {
                val hash = hashValues.getElementsByTagName("hash_value_$name").item(0)?.textContent
                videoUrl = updateUrl(videoUrl, mapOf("md5" to hash, "expires" to exp))
            }
            callback(
                newExtractorLink(
                    source = name,
                    name = title,
                    url = fixUrl(videoUrl)
            ) {
                    this.referer = url
                    this.quality = Qualities.Unknown.value
                    this.type = ExtractorLinkType.VIDEO
                }
            )
        }
    }

    // Helpers
    private fun randomSeedIndex(): Int = (0 until 62).random()
    private fun updateUrl(url: String, params: Map<String, String?>): String =
        url + params.entries.joinToString("&", prefix = if (url.contains("?")) "&" else "?") { "${it.key}=${it.value}" }
    private fun fixUrl(url: String): String = if (url.startsWith("//")) "https:$url" else url
}
