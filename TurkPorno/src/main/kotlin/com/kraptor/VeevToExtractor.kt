package com.kraptor

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

open class VeevToExtractor : ExtractorApi() {

    override val name            = "VeevTo"
    override val mainUrl         = "https://veev.to"
    override val requiresReferer = false

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:156.0) Gecko/20100101 Firefox/156.0"

    private fun jsInt(x: String): Int = x.toIntOrNull() ?: 0

    private fun veevDecode(encoded: String): String {
        try {
            val result = mutableListOf<String>()
            val lut    = mutableMapOf<Int, String>()
            var n      = 256
            var c      = encoded[0].toString()
            result.add(c)

            for (char in encoded.substring(1)) {
                val code = char.code
                val nc   = if (code < 256) char.toString() else lut[code] ?: (c + c[0])
                result.add(nc)
                lut[n] = c + nc[0]
                n++
                c = nc
            }

            return result.joinToString("")
        } catch (e: Exception) {
            return encoded
        }
    }

    private fun buildArray(encoded: String): List<List<Int>> {
        try {
            val d     = mutableListOf<List<Int>>()
            val chars = encoded.toCharArray().toMutableList()

            if (chars.isEmpty()) return d

            var count = jsInt(chars.removeAt(0).toString())
            while (count > 0) {
                val currentArray = mutableListOf<Int>()
                for (i in 0 until count) {
                    if (chars.isEmpty()) break
                    val charValue = chars.removeAt(0).toString()
                    currentArray.add(0, jsInt(charValue))
                }
                d.add(currentArray)

                if (chars.isEmpty()) break
                count = jsInt(chars.removeAt(0).toString())
            }

            return d
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun hexToString(hex: String): String {
        val cleanHex  = hex.trim()
        val paddedHex = if (cleanHex.length % 2 != 0) "0$cleanHex" else cleanHex
        val bytes     = paddedHex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun decodeUrl(encoded: String, tArray: List<Int>): String {
        try {
            var ds = encoded
            for (t in tArray) {
                if (t == 1) ds = ds.reversed()
                ds = hexToString(ds).replace("dXRmOA==", "")
            }
            return ds
        } catch (e: Exception) {
            return encoded
        }
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val initialResponse = app.get(
                url,
                referer = referer ?: "${mainUrl}/",
                headers = mapOf("User-Agent" to userAgent)
            )
            val pageHtml = initialResponse.text

            val mediaId = Regex("""(?:/e/|/d/|file_code=)([a-zA-Z0-9]+)""").find(url)?.groupValues?.get(1)
                ?: initialResponse.url.split("/").lastOrNull { it.isNotEmpty() }
                ?: url.split("/").lastOrNull { it.isNotEmpty() }
                ?: ""

            val regex = Regex("""[\.\s'](?:fc|_vvto\[[^\]]*)(?:['\]]*)?\s*[:=]\s*['"]([^'"]+)""")
            val encodedStrings = regex.findAll(pageHtml).map { it.groupValues[1] }.toMutableList()

            if (encodedStrings.isEmpty()) {
                val fallbackRegex = Regex("""_vvto\[[^\]]+\]\s*=\s*["']([^"']+)["']""")
                fallbackRegex.findAll(pageHtml).forEach { encodedStrings.add(it.groupValues[1]) }
            }

            var ch: String? = null
            for (f in encodedStrings.reversed()) {
                val decoded = veevDecode(f)
                if (decoded != f) {
                    ch = decoded
                    break
                }
            }

            if (ch == null) return

            val tArrays = buildArray(ch)
            if (tArrays.isEmpty()) return

            val fileId = Regex("""Cookies\.set\('file_id',\s*'([^']+)'\)""").find(pageHtml)?.groupValues?.get(1)
            val aff    = Regex("""Cookies\.set\('aff',\s*'([^']+)'\)""").find(pageHtml)?.groupValues?.get(1)

            val cookieHeader = mutableListOf<String>()
            if (!fileId.isNullOrEmpty()) cookieHeader.add("file_id=$fileId")
            if (!aff.isNullOrEmpty()) cookieHeader.add("aff=$aff")

            val apiUrl = "${mainUrl}/dl?op=player_api&cmd=gi&file_code=${
                URLEncoder.encode(mediaId, StandardCharsets.UTF_8.toString())
            }&r=${
                URLEncoder.encode(referer ?: mainUrl, StandardCharsets.UTF_8.toString())
            }&ch=${
                URLEncoder.encode(ch, StandardCharsets.UTF_8.toString())
            }&ie=1"

            val apiHeaders = mutableMapOf(
                "User-Agent" to userAgent,
                "Accept" to "application/json, text/plain, */*",
                "Referer" to url
            )
            if (cookieHeader.isNotEmpty()) {
                apiHeaders["Cookie"] = cookieHeader.joinToString("; ")
            }

            val apiResponse = app.get(apiUrl, headers = apiHeaders).text
            val jsonResponse = JSONObject(apiResponse)

            if (jsonResponse.optString("status") != "success") return
            val fileObj = jsonResponse.optJSONObject("file") ?: return
            if (fileObj.optString("file_status") != "OK") return

            val dvArray = fileObj.optJSONArray("dv") ?: return
            val tArray  = tArrays[0]

            for (i in 0 until dvArray.length()) {
                val source     = dvArray.getJSONObject(i)
                val encodedUrl = source.optString("s")

                if (encodedUrl.isNotEmpty()) {
                    try {
                        val firstDecode = veevDecode(encodedUrl)
                        val finalUrl    = decodeUrl(firstDecode, tArray)

                        if (finalUrl.startsWith("http")) {
                            val quality = source.optString("vid_title", "")
                            val label   = if (quality.isNotBlank()) "$name ($quality)" else name

                            callback.invoke(
                                newExtractorLink(
                                    source = name,
                                    name   = label,
                                    url    = finalUrl,
                                    type   = ExtractorLinkType.VIDEO
                                ) {
                                    this.referer = "${mainUrl}/"
                                    this.headers = mapOf("User-Agent" to userAgent)
                                }
                            )
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
        }
    }
}