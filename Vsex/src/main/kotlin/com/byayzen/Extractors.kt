package com.byayzen

import android.annotation.SuppressLint
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJSON
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import java.net.URI
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import java.nio.charset.Charset

class DoodPmExtractor : DoodLaExtractor() {
    override var mainUrl = "https://dood.pm"
}

class MixDropAG : MixDrop(){
    override var mainUrl = "https://mixdrop.ag"
}

class MixDropMy : MixDrop(){
    override var mainUrl = "https://mixdrop.my"
}

open class Vidguardto : ExtractorApi() {
    override val name = "Vidguard"
    override val mainUrl = "https://vidguard.to"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url)
        val resc = res.document.select("script:containsData(eval)").firstOrNull()?.data()
        resc?.let {
            val jsonStr2 = AppUtils.parseJson<SvgObject>(runJS2(it))
            val watchlink = sigDecode(jsonStr2.stream)

            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = name,
                    url = watchlink,
                    INFER_TYPE
                ) {
                    this.referer = mainUrl
                    this.quality = Qualities.Unknown.value
                }
            )
        }
    }

    private fun sigDecode(url: String): String {
        val sig = url.split("sig=")[1].split("&")[0]
        var t = ""
        for (v in sig.chunked(2)) {
            val byteValue = Integer.parseInt(v, 16) xor 2
            t += byteValue.toChar()
        }
        val padding = when (t.length % 4) {
            2 -> "=="
            3 -> "="
            else -> ""
        }
        // Düzeltilen kısım: Base64.getDecoder.decode yerine Base64.decode
        val decoded = Base64.decode(t + padding, Base64.DEFAULT)

        t = String(decoded).dropLast(5).reversed()
        val charArray = t.toCharArray()
        for (i in 0 until charArray.size - 1 step 2) {
            val temp = charArray[i]
            charArray[i] = charArray[i + 1]
            charArray[i + 1] = temp
        }
        val modifiedSig = String(charArray).dropLast(5)
        return url.replace(sig, modifiedSig)
    }

    private fun runJS2(hideMyHtmlContent: String): String {
        Log.d("runJS", "start")
        val rhino = Context.enter()
        rhino.initSafeStandardObjects()
        rhino.optimizationLevel = -1
        val scope: Scriptable = rhino.initSafeStandardObjects()
        scope.put("window", scope, scope)
        var result = ""
        try {
            Log.d("runJS", "Executing JavaScript: $hideMyHtmlContent")
            rhino.evaluateString(scope, hideMyHtmlContent, "JavaScript", 1, null)
            val svgObject = scope.get("svg", scope)
            result = if (svgObject is NativeObject) {
                NativeJSON.stringify(Context.getCurrentContext(), scope, svgObject, null, null).toString()
            } else {
                Context.toString(svgObject)
            }
        } catch (e: Exception) {
            Log.e("runJS", "Error executing JavaScript", e)
        } finally {
            Context.exit()
        }
        return result
    }

    data class SvgObject(
        val stream: String,
        val hash: String
    )
}



open class LuluBase : ExtractorApi() {
    override val name = "LuluStream"
    override val mainUrl = "https://lulustream.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val embedUrl = url.replace("/d/", "/e/")

            val currentHost = try { URI(embedUrl).host } catch (e: Exception) { "lulustream.com" }
            val currentOrigin = "https://$currentHost"

            val requestHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to (referer ?: embedUrl)
            )

            val response = app.get(embedUrl, headers = requestHeaders)
            val html = response.text
            val m3u8Regex = """["']([^"']+\.m3u8[^"']*)["']""".toRegex()
            val match = m3u8Regex.find(html)

            if (match != null) {
                val m3u8Url = match.groupValues[1]

                val videoHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer" to embedUrl,
                    "Origin" to currentOrigin,
                    "Accept" to "*/*"
                )

                M3u8Helper.generateM3u8(
                    source = this.name,
                    streamUrl = m3u8Url,
                    referer = embedUrl,
                    headers = videoHeaders
                ).forEach(callback)
            }
        } catch (e: Exception) {
        }
    }
}

class LuluStream : LuluBase() {
    override val name = "LuluStream"
    override val mainUrl = "https://lulustream.com"
}

class LuluVid : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvid.com"
}

class LuluVdo : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvdo.com"
}

class LuluVdoo : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvdoo.com"
}

class LuluPvp : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulupvp.com"
}

class Luludlc : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulu.dlc.ovh"
}

class Lulu0 : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulu0.ovh"
}

class VidNest : ExtractorApi() {
    override val name = "VidNest"
    override val mainUrl = "https://vidnest.io"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val docHeaders = mapOf(
            "Referer" to "https://vidnest.io/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )

        val text = app.get(url, headers = docHeaders).text

        val videoRegex = """file\s*:\s*["']([^"']+\.mp4[^"']*)["']""".toRegex()
        val labelRegex = """label\s*:\s*["']([^"']+)["']""".toRegex()

        val videoUrl = videoRegex.find(text)?.groupValues?.get(1)
        val label = labelRegex.find(text)?.groupValues?.get(1) ?: "VidNest"

        if (videoUrl != null) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = videoUrl,
                    type = ExtractorLinkType.VIDEO,
                    initializer = {
                        this.referer = "https://vidnest.io/"
                        this.quality = getQualityFromName(label)

                        this.headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0",
                            "Referer" to "https://vidnest.io/",
                            "Accept" to "*/*",
                            "Origin" to "https://vidnest.io",
                            "Connection" to "keep-alive",
                            "Sec-Fetch-Dest" to "video",
                            "Sec-Fetch-Mode" to "no-cors",
                            "Sec-Fetch-Site" to "same-site",
                            "Priority" to "u=4"
                        )
                    }
                )
            )
        }
    }
}

open class Filemoon : ExtractorApi() {
    override var name = "Filemoon"
    override var mainUrl = "https://filemoon.to"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val mediaId = Regex("""/(?:e|d|v|f|download)/([0-9a-zA-Z]+)""").find(url)?.groupValues?.get(1)
            ?: url.substringAfterLast("/").substringBefore("?")
        val host = url.substringAfter("://").substringBefore("/")
        val rootReferer = "https://$host/"
        val apiUrl = "https://$host/api/videos/$mediaId/embed/playback"
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Referer" to rootReferer,
            "Origin" to rootReferer.removeSuffix("/"),
            "X-Requested-With" to "XMLHttpRequest"
        )

        val response = app.get(apiUrl, headers = headers).text

        if (response.contains("video not found") || response.isBlank()) {
            return
        }

        val json = try { parseJson<PlaybackResponse>(response) } catch (e: Exception) { null } ?: return

        val finalSources = json.sources ?: json.playback?.let { pb ->
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE,
                    SecretKeySpec(xn(pb.key_parts), "AES"), GCMParameterSpec(128, ft(pb.iv))
                )
                val decryptedData = cipher.doFinal(ft(pb.payload)).toString(Charsets.ISO_8859_1)
                parseJson<PlaybackResponse>(decryptedData).sources
            } catch (e: Exception) {
                null
            }
        }

        finalSources?.let { processSources(it, url, callback) }
    }

    private suspend fun processSources(sources: List<VideoSource>, referer: String, callback: (ExtractorLink) -> Unit) {
        sources.forEach { source ->
            Log.d("Filemoon", "Kaynak: ${source.label}p -> ${source.url.take(30)}...")
            if (source.url.contains("m3u8")) {
                M3u8Helper.generateM3u8(this.name, source.url, referer).forEach(callback)
            } else {
                callback(
                    newExtractorLink(this.name, source.label ?: this.name, source.url) {
                        this.referer = referer
                        this.quality = Qualities.Unknown.value
                    }
                )
            }
        }
    }

    private fun ft(e: String): ByteArray {
        val t = e.replace("-", "+").replace("_", "/")
        val r = if (t.length % 4 == 0) 0 else 4 - (t.length % 4)
        return Base64.decode(t + "=".repeat(r), Base64.DEFAULT)
    }

    private fun xn(e: List<String>): ByteArray {
        var result = byteArrayOf()
        e.forEach { result += ft(it) }
        return result
    }

    data class PlaybackResponse(val sources: List<VideoSource>? = null, val playback: PlaybackData? = null)
    data class PlaybackData(val iv: String, val payload: String, val key_parts: List<String>)
    data class VideoSource(val url: String, val label: String? = null)
}





class FileMoon2 : Filemoon() { override var mainUrl = "https://filemoon.to" }
class FileMoonIn : Filemoon() { override var mainUrl = "https://filemoon.in" }
class FileMoonSx : Filemoon() { override var mainUrl = "https://filemoon.sx" }
class Bysedikamoum : Filemoon() { override var mainUrl = "https://bysedikamoum.com" }
class Bysezoexe : Filemoon() { override var mainUrl = "https://bysezoxexe.com" }
class Filemoonx08 : Filemoon() { override var mainUrl = "https://x08.ovh" }




