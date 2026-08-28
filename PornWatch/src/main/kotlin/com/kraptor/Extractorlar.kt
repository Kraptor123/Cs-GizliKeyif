package com.kraptor

import kotlinx.coroutines.yield
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.interfaces.ECPublicKey
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.extractors.VidStack
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import java.net.URI
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJSON
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import android.util.Base64
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.extractors.AesHelper
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.mapper
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.lagradost.cloudstream3.utils.M3u8Helper
import org.jsoup.Jsoup
import java.net.URL
import kotlin.random.Random




// Kotlin port of ResolveURL's "Byse" resolver (Filemoon/Byse mirror network):
// https://github.com/Gujal00/ResolveURL/blob/master/script.module.resolveurl/lib/resolveurl/plugins/byse.py
open class Filemoon(
    override var mainUrl: String = "https://filemoon.to"
) : ExtractorApi() {
    override var name = "Filemoon"
    override val requiresReferer = false

    companion object {
        private const val TAG = "FileMoonExtractor"

        private const val UA =
            "Mozilla/5.0 (Linux; Android 10; TX6s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36"

        private val REDIRECT_DOMAINS = setOf(
            "boosteradx.online", "byse.sx", "streamlyplayer.online"
        )

        private val URL_REGEX = Regex(
            "(?://|\\.)((?:filemoon|cinegrab|moonmov|kerapoxy|furher|1azayf9w|81u6xl9d|f16px|sb1254w9megshle|" +
                    "smdfs40r|bf0skv|z1ekv717|l1afav|222i8x|8mhlloqo|96ar|xcoic|f51rm|c1z39|boosteradx|streamlyplayero?|moflix-stream|" +
                    "(?:embedplay)?byse[a-z0-9]*|dismz4n3wp6xnr3|[a-z0-9]{10,25})" +
                    "\\.(?:sx|top?|s?k?in|link|nl|wf|com|eu|art|pro|cc|xyz|org|fun|net|lol|online))" +
                    "/(?:(?:e|d|download|[a-zA-Z0-9_-]+)/)?([0-9a-zA-Z]+)(?:/.*)?"
        )

        fun canHandle(url: String): Boolean = URL_REGEX.containsMatchIn(url)

        private fun b64UrlEncode(bytes: ByteArray): String =
            Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

        private fun b64UrlDecode(value: String): ByteArray {
            val padded = value + "=".repeat((4 - value.length % 4) % 4)
            return Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP)
        }

        private fun sha256(bytes: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(bytes)

        private fun fh(value: Double): String = b64UrlEncode(sha256(value.toString().toByteArray(Charsets.US_ASCII)))

        private fun bigIntTo32Bytes(n: BigInteger): ByteArray {
            val raw = n.toByteArray()
            return when {
                raw.size == 32 -> raw
                raw.size > 32  -> raw.copyOfRange(raw.size - 32, raw.size)
                else           -> ByteArray(32 - raw.size) + raw
            }
        }

        private fun derToRawEcdsaSignature(der: ByteArray, componentLen: Int = 32): ByteArray {
            var offset = 1
            var seqLen = der[offset].toInt() and 0xFF
            offset++
            if (seqLen and 0x80 != 0) offset += (seqLen and 0x7F)

            offset++
            val rLen = der[offset].toInt() and 0xFF
            offset++
            val rBytes = der.copyOfRange(offset, offset + rLen)
            offset += rLen

            offset++
            val sLen = der[offset].toInt() and 0xFF
            offset++
            val sBytes = der.copyOfRange(offset, offset + sLen)

            fun fixedLen(b: ByteArray): ByteArray {
                var trimmed = b
                var start = 0
                while (start < trimmed.size - 1 && trimmed[start] == 0.toByte()) start++
                trimmed = trimmed.copyOfRange(start, trimmed.size)
                return if (trimmed.size >= componentLen) trimmed.copyOfRange(trimmed.size - componentLen, trimmed.size)
                else ByteArray(componentLen - trimmed.size) + trimmed
            }

            return fixedLen(rBytes) + fixedLen(sBytes)
        }

        private fun randomHex(byteLen: Int): String {
            val bytes = ByteArray(byteLen)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        private fun rot(state: IntArray) {
            state[0] += state[1]; state[3] = Integer.rotateLeft(state[3] xor state[0], 16)
            state[2] += state[3]; state[1] = Integer.rotateLeft(state[1] xor state[2], 12)
            state[0] += state[1]; state[3] = Integer.rotateLeft(state[3] xor state[0], 8)
            state[2] += state[3]; state[1] = Integer.rotateLeft(state[1] xor state[2], 7)
        }

        private fun powHash(input: ByteArray, out: IntArray, scratch: IntArray, state: IntArray) {
            state[0] = 1779033703
            state[1] = -1150833019
            state[2] = 1013904242
            state[3] = -1521486534
            for (i in input.indices) {
                state[0] += (input[i].toInt() and 0xFF)
                state[0] = Integer.rotateLeft(state[0], 7)
                rot(state)
            }
            repeat(8) { rot(state) }
            for (i in 0 until 512) {
                rot(state)
                scratch[i] = state[0] xor state[2]
            }
            val lt = 511
            val lr = -1640531535
            val hr = -2048144777
            repeat(2) {
                for (s in 0 until 512) {
                    val a = scratch[s] and lt
                    var c = scratch[s] + scratch[a]
                    c = Integer.rotateLeft(c, 13)
                    c = c xor (scratch[(s + 1) and 511] * lr)
                    scratch[s] = c
                    state[0] = state[0] xor c
                    rot(state)
                }
            }
            val chunk = 512 / 8
            for (i in 0 until 8) {
                rot(state)
                var s = state[0]
                val base = i * chunk
                for (c in 0 until chunk) {
                    val d = scratch[base + c]
                    s += d
                    s = Integer.rotateLeft(s, 5)
                    s = s xor (d * hr)
                }
                out[i] = s xor state[2]
            }
        }

        private fun leadingZeroBits(hash: IntArray): Int {
            var total = 0
            for (word in hash) {
                if (word == 0) {
                    total += 32
                    continue
                }
                return total + Integer.numberOfLeadingZeros(word)
            }
            return total
        }

        private suspend fun solvePow(nonce: String, difficulty: Int, timeoutSec: Double = 20.0): String? {
            if (difficulty <= 0) return "0"
            val start = System.currentTimeMillis()
            val prefix = "$nonce:"
            val prefixBytes = prefix.toByteArray(Charsets.US_ASCII)
            val buffer = ByteArray(64)
            System.arraycopy(prefixBytes, 0, buffer, 0, prefixBytes.size)
            val pLen = prefixBytes.size

            val out = IntArray(8)
            val scratch = IntArray(512)
            val state = IntArray(4)

            var s = 0L
            while (true) {
                for (iter in 0 until 1024) {
                    val sStr = s.toString()
                    val sLen = sStr.length
                    for (i in 0 until sLen) {
                        buffer[pLen + i] = sStr[i].code.toByte()
                    }
                    val input = buffer.copyOf(pLen + sLen)
                    powHash(input, out, scratch, state)
                    if (leadingZeroBits(out) >= difficulty) {
                        Log.d(TAG, "PoW solved in ${System.currentTimeMillis() - start}ms: s=$s")
                        return s.toString()
                    }
                    s++
                }
                if ((System.currentTimeMillis() - start) > timeoutSec * 1000) {
                    Log.e(TAG, "PoW timeout after ${System.currentTimeMillis() - start}ms")
                    return null
                }
                yield()
            }
        }
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d(TAG, "getUrl() -> url: $url, referer: $referer")
        try {
            val match = URL_REGEX.find(url)
            if (match == null) {
                Log.e(TAG, "URL regex ile eşleşmedi: $url")
                return
            }
            val matchedHost = match.groupValues[1]
            val mediaId     = match.groupValues[2]
            val host        = if (matchedHost in REDIRECT_DOMAINS) "streamlyplayero.online" else matchedHost

            Log.d(TAG, "Regex eşleşti -> host: $host, mediaId: $mediaId")
            resolve(host, mediaId, callback)
        } catch (e: Exception) {
            Log.e(TAG, "getUrl hatası: ${e.message}", e)
        }
    }

    private suspend fun resolve(host: String, mediaId: String, callback: (ExtractorLink) -> Unit) {
        val webUrl  = "https://$host/e/$mediaId"
        var ref     = "https://$host/"
        val headers = mutableMapOf(
            "User-Agent" to UA,
            "Referer"    to ref,
            "Origin"     to ref.trimEnd('/')
        )
        Log.d(TAG, "resolve() -> webUrl: $webUrl, ref: $ref")

        var embed = ""
        var details = fetchJson("${ref}api/videos/$mediaId/details", headers)
        if (details == null) {
            Log.d(TAG, "api/videos/$mediaId/details null döndü, embed/details deneniyor...")
            embed = "embed/"
            details = fetchJson("${ref}api/videos/$mediaId/${embed}details", headers)
                ?: run {
                    Log.e(TAG, "Video link not found for $webUrl")
                    return
                }
        }
        Log.d(TAG, "Details alındı: ${details.optString("title")}")

        val embedUrl = details.optString("embed_frame_url").orEmpty()
        if (embedUrl.isNotEmpty()) {
            val prevRef = ref
            ref = originOf(embedUrl)
            embed = "embed/"
            headers["Referer"] = embedUrl
            headers["Origin"] = ref.trimEnd('/')
            headers["X-Embed-Origin"] = prevRef.trimEnd('/')
            headers["X-Embed-Referer"] = prevRef
            headers["X-Embed-Parent"] = webUrl
            Log.d(TAG, "Embed frame yönlendirmesi -> ref: $ref, embedUrl: $embedUrl")
        }

        Log.d(TAG, "Settings isteniyor: ${ref}api/videos/$mediaId/${embed}settings")
        val settings = fetchJson("${ref}api/videos/$mediaId/${embed}settings", headers) ?: run {
            Log.e(TAG, "Settings alınamadı for $webUrl")
            return
        }

        val data = if (settings.optBoolean("captcha_required", false)) {
            Log.d(TAG, "Captcha gerekli, çözülüyor...")
            solveCaptchaAndGetPlayback(ref, mediaId, embed, headers)
        } else {
            Log.d(TAG, "Captcha gerekmiyor, doğrudan playback isteniyor...")
            postJson(
                "${ref}api/videos/$mediaId/${embed}playback",
                headers,
                buildFingerprint(16, 0.83, 0.94),
                40
            )
        } ?: run {
            Log.e(TAG, "Playback request failed for $webUrl")
            return
        }

        Log.d(TAG, "Playback verisi alındı, kaynaklar ayrıştırılıyor...")
        emitSources(data, ref, headers, callback)
    }

    private suspend fun solveCaptchaAndGetPlayback(
        ref: String,
        mediaId: String,
        embed: String,
        headers: MutableMap<String, String>
    ): JSONObject? {
        Log.d(TAG, "Access challenge isteniyor: ${ref}api/videos/access/challenge")
        val challenge = postJson("${ref}api/videos/access/challenge", headers, JSONObject(), 30)
            ?: run {
                Log.e(TAG, "Access challenge request failed")
                return null
            }

        Log.d(TAG, "Access attest gönderiliyor...")
        val attest = postJson("${ref}api/videos/access/attest", headers, buildAttestPayload(challenge), 30)
            ?: run {
                Log.e(TAG, "Access attest request failed")
                return null
            }

        val fingerprint = JSONObject().apply {
            put("token", attest.optString("token"))
            put("viewer_id", attest.optString("viewer_id"))
            put("device_id", attest.optString("device_id"))
            put("confidence", attest.optDouble("confidence"))
        }

        Log.d(TAG, "Captcha token/PoW isteniyor...")
        val captcha = postJson(
            "${ref}api/videos/$mediaId/${embed}captcha",
            headers,
            JSONObject().put("fingerprint", fingerprint),
            30
        ) ?: run {
            Log.e(TAG, "Captcha request failed")
            return null
        }

        val solution = solvePow(captcha.optString("pow_nonce"), captcha.optInt("pow_difficulty", 0))
            ?: run {
                Log.e(TAG, "Unable to solve captcha PoW")
                return null
            }

        Log.d(TAG, "Captcha doğrulanıyor (verify)...")
        val verifyBody = JSONObject().apply {
            put("pow_token", captcha.optString("pow_token"))
            put("solution", solution)
            put("fingerprint", fingerprint)
        }
        val verify = postJson("${ref}api/videos/$mediaId/${embed}captcha/verify", headers, verifyBody, 30)
            ?: run {
                Log.e(TAG, "Captcha verify request failed")
                return null
            }
        headers["X-Captcha-Token"] = verify.optString("token")

        Log.d(TAG, "Playback isteniyor: ${ref}api/videos/$mediaId/${embed}playback")
        return postJson(
            "${ref}api/videos/$mediaId/${embed}playback",
            headers,
            JSONObject().put("fingerprint", fingerprint),
            30
        )
    }

    private fun buildAttestPayload(challenge: JSONObject): JSONObject {
        val nonce = challenge.optString("nonce")
        val challengeId = challenge.optString("challenge_id")

        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = keyPairGenerator.generateKeyPair()

        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(keyPair.private)
        signer.update(nonce.toByteArray(Charsets.US_ASCII))
        val signature = b64UrlEncode(derToRawEcdsaSignature(signer.sign()))

        val publicKey = keyPair.public as ECPublicKey
        val publicKeyJwk = JSONObject().apply {
            put("crv", "P-256")
            put("ext", true)
            put("key_ops", JSONArray(listOf("verify")))
            put("kty", "EC")
            put("x", b64UrlEncode(bigIntTo32Bytes(publicKey.w.affineX)))
            put("y", b64UrlEncode(bigIntTo32Bytes(publicKey.w.affineY)))
        }

        return JSONObject().apply {
            put("viewer_id", "")
            put("device_id", "")
            put("challenge_id", challengeId)
            put("nonce", nonce)
            put("signature", signature)
            put("public_key", publicKeyJwk)
        }
    }

    private fun buildFingerprint(byteLen: Int, minConfidence: Double, maxConfidence: Double): JSONObject {
        val viewerId = randomHex(byteLen)
        val deviceId = randomHex(byteLen)
        val ctime = System.currentTimeMillis() / 1000
        val confidence = Math.round(Random.nextDouble(minConfidence, maxConfidence) * 100.0) / 100.0

        val tokenData = JSONObject().apply {
            put("viewer_id", viewerId)
            put("device_id", deviceId)
            put("confidence", confidence)
            put("iat", ctime)
            put("exp", ctime + 600)
        }
        val tokenBData = b64UrlEncode(tokenData.toString().toByteArray(Charsets.UTF_8))
        val tokenSig = b64UrlEncode(sha256(tokenBData.toByteArray(Charsets.UTF_8)))
        val token = "$tokenBData.$tokenSig"

        val fingerprint = JSONObject().apply {
            put("viewer_id", viewerId)
            put("device_id", deviceId)
            put("confidence", confidence)
            put("token", token)
        }
        return JSONObject().put("fingerprint", fingerprint)
    }

    private fun deriveKey(keyParts: JSONArray, version: Int?): ByteArray {
        val parts = if (version != null && version != 0) {
            listOf(keyParts.getString(version - 1), keyParts.getString(keyParts.length() - version))
        } else {
            (0 until keyParts.length()).map { keyParts.getString(it) }
        }
        return parts.map { b64UrlDecode(it) }.reduce { acc, bytes -> acc + bytes }
    }

    private fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, payload: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(payload)
    }

    private suspend fun emitSources(
        data: JSONObject,
        ref: String,
        headers: Map<String, String>,
        callback: (ExtractorLink) -> Unit
    ) {
        val sources = data.optJSONArray("sources")
        if (sources != null && sources.length() > 0) {
            emitFromArray(sources, ref, headers, callback)
            return
        }

        val playback = data.optJSONObject("playback") ?: run {
            Log.e(TAG, "No sources/playback in response")
            return
        }

        try {
            val iv = b64UrlDecode(playback.getString("iv"))
            val keyParts = playback.getJSONArray("key_parts")
            val version = if (playback.isNull("version")) null else playback.optInt("version")
            val key = deriveKey(keyParts, version)
            val payloadBytes = b64UrlDecode(playback.getString("payload"))

            val plaintext = aesGcmDecrypt(key, iv, payloadBytes)
            val decrypted = JSONObject(String(plaintext, Charsets.UTF_8))
            val decryptedSources = decrypted.optJSONArray("sources") ?: return

            val cleanHeaders = headers.toMutableMap().apply {
                remove("X-Embed-Parent")
                remove("X-Embed-Origin")
                remove("X-Embed-Referer")
                remove("X-Captcha-Token")
            }
            emitFromArray(decryptedSources, ref, cleanHeaders, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt playback payload: ${e.message}", e)
        }
    }

    private suspend fun emitFromArray(
        sources: JSONArray,
        ref: String,
        headers: Map<String, String>,
        callback: (ExtractorLink) -> Unit
    ) {
        for (i in 0 until sources.length()) {
            val source = sources.optJSONObject(i) ?: continue
            val label = source.optString("label").orEmpty()
            var sourceUrl = source.optString("url").orEmpty()
            if (sourceUrl.isEmpty()) continue

            if (sourceUrl.startsWith("/")) {
                sourceUrl = resolveRelative(ref, sourceUrl)
            }
            val finalUrl = followRedirect(sourceUrl, headers)
            val linkType = if (finalUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO

            callback.invoke(
                newExtractorLink(name, name, finalUrl, type = linkType) {
                    this.referer = headers["Referer"] ?: ref
                    this.quality = getQualityFromName(label)
                    this.headers = headers
                }
            )
        }
    }

    private suspend fun fetchJson(url: String, headers: Map<String, String>): JSONObject? {
        return try {
            val res = app.get(url, headers = headers, timeout = 30L)
            if (res.code !in 200..299) null else JSONObject(res.text)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: JSONObject,
        timeoutSec: Long
    ): JSONObject? {
        return try {
            val requestBody = body.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val res = app.post(url, requestBody = requestBody, headers = headers, timeout = timeoutSec)
            if (res.code !in 200..299) null else JSONObject(res.text)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun followRedirect(url: String, headers: Map<String, String>): String {
        return try {
            val res = app.get(url, headers = headers, allowRedirects = false)
            res.headers["location"] ?: res.headers["Location"] ?: res.url
        } catch (e: Exception) {
            url
        }
    }

    private fun resolveRelative(base: String, relative: String): String = try {
        URI(base).resolve(relative).toString()
    } catch (e: Exception) {
        relative
    }

    private fun originOf(url: String): String = try {
        URI(url).resolve("/").toString()
    } catch (e: Exception) {
        url
    }
}


open class Streamwish : ExtractorApi() {
    override var name = "Streamwish"
    override var mainUrl = "https://streamwish.to"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val responsecode = app.get(url)
        if (responsecode.code == 200) {
            val serverRes = responsecode.document
            val script = serverRes.selectFirst("script:containsData(sources)")?.data().toString()
            val headers = mapOf(
                "Accept" to "*/*",
                "Connection" to "keep-alive",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "cross-site",
                "Origin" to url,
            )
            Regex("file:\"(.*?)\"").find(script)?.groupValues?.get(1)?.let { link ->
                return listOf(
                    newExtractorLink(source = this.name, name = this.name, url = link, INFER_TYPE) {
                        this.referer = referer ?: ""
                        this.quality = getQualityFromName("")
                        this.headers = headers
                    }
                )
            }
        }
        return null
    }
}




class Streamhihi : Streamwish() { override var name = "Streamhihi"; override var mainUrl = "https://streamhihi.com" }
class Javsw : Streamwish() { override var mainUrl = "https://javsw.me"; override var name = "Javsw" }

open class VidHidePro : ExtractorApi() {
    override var name = "VidHidePro"
    override var mainUrl = "https://vidhidepro.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site",
            "Origin" to mainUrl,
            "User-Agent" to USER_AGENT
        )

        val response = app.get(getEmbedUrl(url), referer = referer)
        val script = if (!getPacked(response.text).isNullOrEmpty()) {
            var result = getAndUnpack(response.text)
            if (result.contains("var links")) { result = result.substringAfter("var links") }
            result
        } else {
            response.document.selectFirst("script:containsData(sources:)")?.data()
        } ?: return

        Regex(":\\s*\"(.*?m3u8.*?)\"").findAll(script).forEach { m3u8Match ->
            generateM3u8(name, fixUrl(m3u8Match.groupValues[1]), referer = "$mainUrl/", headers = headers).forEach(callback)
        }
    }

    private fun getEmbedUrl(url: String): String {
        return when {
            url.contains("/d/") -> url.replace("/d/", "/v/")
            url.contains("/download/") -> url.replace("/download/", "/v/")
            url.contains("/file/") -> url.replace("/file/", "/v/")
            url.contains("/e/") -> url.replace("/e/", "/v/")
            else -> url.replace("/f/", "/v/")
        }
    }
}

class VidhideVIP : VidHidePro() { override var mainUrl = "https://vidhidevip.com"; override var name = "VidhideVIP" }
class Javlion : VidHidePro() { override var mainUrl = "https://javlion.xyz"; override var name = "Javlion" }
class VidHidePro1 : VidHidePro() { override var mainUrl = "https://filelions.live" }
class VidHidePro2 : VidHidePro() { override var mainUrl = "https://filelions.online" }
class VidHidePro3 : VidHidePro() { override var mainUrl = "https://filelions.to" }
class VidHidePro4 : VidHidePro() { override var mainUrl = "https://kinoger.be" }
class VidHidePro6 : VidHidePro() { override var mainUrl = "https://vidhidepre.com" }
class VidHidePro7 : VidHidePro() { override var mainUrl = "https://vidhidehub.com" }
class Dhcplay : VidHidePro() { override var name = "DHC Play"; override var mainUrl = "https://dhcplay.com" }
class Smoothpre : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://smoothpre.com" }
class Dhtpre : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://dhtpre.com" }
class Peytonepre : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://peytonepre.com" }
class Movearnpre : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://movearnpre.com" }
class Dintezuvio : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://dintezuvio.com" }
class HgLink : VidHidePro() { override var name = "HGLink"; override var mainUrl = "https://hglink.to" }
class RyderJet : VidHidePro() { override var name = "RyderJet"; override var mainUrl = "https://ryderjet.com" }

class MyCloudZ : VidHidePro() { override var mainUrl = "https://mycloudz.cc"; override var name = "MyCloudZ" }
class Turboplayers : StreamTape() { override var mainUrl = "https://turboplayers.xyz"; override var name = "Streamtape" }




class Javclan : ExtractorApi() {
    override var name = "Javclan"
    override var mainUrl = "https://javclan.com"
    override val requiresReferer = true
    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val res = app.get(url, referer = referer)
        val script = res.document.selectFirst("script:containsData(sources)")?.data().toString()
        Regex("file:\"(.*?)\"").find(script)?.groupValues?.get(1)?.let { link ->
            return listOf(newExtractorLink(name, name, link, INFER_TYPE) { this.referer = referer ?: "" })
        }
        return null
    }
}

class Javggvideo : ExtractorApi() {
    override var name = "Javgg Video"
    override var mainUrl = "https://javggvideo.xyz"
    override val requiresReferer = false
    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val response = app.get(url).text
        val link = response.substringAfter("var urlPlay = '").substringBefore("';")
        return listOf(newExtractorLink(name, name, link, INFER_TYPE) { this.quality = Qualities.Unknown.value })
    }
}

class swhoi : Filesim() { override var mainUrl = "https://swhoi.com"; override var name = "Streamwish" }
class MixDropis : MixDrop() { override var mainUrl = "https://mixdrop.is" }
class Javmoon : Filesim() { override var mainUrl = "https://javmoon.me"; override var name = "FileMoon" }


class StbP2P : VidStack() { override var mainUrl = "https://stb.strp2p.com"; override var name = "STBP2P" }
class Playerupnone : VidStack() { override var mainUrl = "https://player.upn.one"; override var name = "UPNP2P" }

open class Turtleviplay : ExtractorApi() {
    override var name = "Turtleviplay"
    override var mainUrl = "https://turtleviplay.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url, referer = referer).document
        val m3u8 = res.selectFirst("#video_player")?.attr("data-hash") ?: return

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = m3u8,
                type = ExtractorLinkType.M3U8,
            ) {
                this.referer = url
                this.quality = Qualities.Unknown.value
                this.headers = mapOf(
                    "Origin" to "https://turtleviplay.xyz",
                    "Accept" to "*/*",
                )
            }
        )
    }
}

class Turboviplay : Turtleviplay() {
    override var name = "Turboviplay"
    override var mainUrl = "https://turboviplay.com"
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
            val jsonStr2 = try { mapper.readValue<SvgObject>(runJS2(it)) } catch (e: Exception) { null } ?: return
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
        val rhino = Context.enter()
        rhino.initSafeStandardObjects()
        rhino.optimizationLevel = -1
        val scope: Scriptable = rhino.initSafeStandardObjects()
        scope.put("window", scope, scope)
        var result = ""
        try {
            rhino.evaluateString(scope, hideMyHtmlContent, "JavaScript", 1, null)
            val svgObject = scope.get("svg", scope)
            result = if (svgObject is NativeObject) {
                NativeJSON.stringify(Context.getCurrentContext(), scope, svgObject, null, null).toString()
            } else {
                Context.toString(svgObject)
            }
        } catch (e: Exception) {
        } finally {
            Context.exit()
        }
        return result
    }


}


open class LULUBASE : ExtractorApi() {
    override val name = "LuluStream"
    override val mainUrl = "https://luluvid.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("LULUSTREAM", "getUrl | $url | $referer")

        val response = app.get(url, referer = referer)
        Log.d("LULUSTREAM", "response | ${response.code}")

        val doc = response.document
        val scripts = doc.select("script").map { it.data() }
        Log.d("LULUSTREAM", "scripts | ${scripts.size}")

        val packed = scripts.firstOrNull {
            it.contains("eval(function(p,a,c,k,e,d)") && it.contains("m3u8")
        }
        Log.d("LULUSTREAM", "packed | ${packed != null}")

        if (packed == null) return

        val unpacked = JsUnpacker(packed).unpack()
        Log.d("LULUSTREAM", "unpacked | ${unpacked != null} | ${unpacked?.take(200)}")

        if (unpacked == null) return

        val m3u8 = Regex("""file:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""")
            .find(unpacked)?.groupValues?.get(1)
            ?: Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
                .find(unpacked)?.groupValues?.get(1)

        Log.d("LULUSTREAM", "m3u8 | $m3u8")

        if (m3u8 == null) return

        callback(
            newExtractorLink(
                name,
                name,
                m3u8
            ) {
                this.referer = mainUrl
                this.quality = Qualities.P1080.value
                this.headers = mapOf("Origin" to mainUrl)
            }
        )

        Log.d("LULUSTREAM", "done")
    }
}

class LULUSTREAM : LULUBASE() {
    override val name = "LuluStream"
    override val mainUrl = "https://lulustream.com"
}


class LULUVDO : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvdo.com"
}

class LULUVDOO : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvdoo.com"
}

class LULUPVP : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulupvp.com"
}

class LULUDLC : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulu.dlc.ovh/"
}

class LULU0 : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulu0.ovh/"
}

class LULUX08 : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://x08.ovh/"
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
        Log.d("Player4Me", url)
        val id = url.substringAfter("#")

        Log.d("Player4Me", id)
        val response = app.get("$mainUrl/api/v1/video?id=$id", referer = "${mainUrl}/", headers = mapOf(
            "Host" to mainUrl.substringAfter("://"),
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Accept" to "*/*",
            "Cookie" to "popunderCount/=1",
        ))
        Log.d("Player4Me", "${response.code}")

        val sifreliYanit = response.text.trim()
        Log.d("Player4Me", sifreliYanit.take(50))

        if (sifreliYanit.startsWith("<html>")) {
            return
        }

        val aesCoz = AesHelper.decryptAES(sifreliYanit, "kiemtienmua911ca", "1234567890oiuytr")

        val map = mapper.readValue<Yanit>(aesCoz)
        val videoUrl = map.source ?: map.hls ?: map.cf
        Log.d("Player4Me", "$videoUrl")

        if (videoUrl != null) {
            Log.d("Player4Me", videoUrl)
            callback.invoke(newExtractorLink(
                this.name,
                this.name,
                fixUrl(videoUrl),
                ExtractorLinkType.M3U8
            ) {
                this.referer = "${mainUrl}/"
                this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
            })
        } else {
            Log.d("Player4Me", "Bitiş")
        }
    }
}


class Vip4me : Player4Me() {
    override var mainUrl = "https://vip.player4me.vip"
    override var name = "Player4Me"
}

class RPMShare : Player4Me() {
    override var mainUrl = "https://my.rpmplay.online"
    override var name = "Player4Me"
}

class UpnsOnline : Player4Me() {
    override var mainUrl = "https://my.upns.online"
    override var name = "Player4Me"
}

class EmbedSeek : Player4Me() {
    override var mainUrl = "https://my.embedseek.online"
    override var name = "Player4Me"
}

class VipSeekPlayer : Player4Me() {
    override var mainUrl = "https://vip.seekplayer.vip"
    override var name = "Player4Me"
}

class EasyVidPlayer : Player4Me() {
    override var mainUrl = "https://p.easyvidplayer.com"
    override var name = "Player4Me"
}

class VipEasyVidPlayer : Player4Me() {
    override var mainUrl = "https://vip.easyvidplayer.com"
    override var name = "Player4Me"
}




open class DoodStream : ExtractorApi() {
    override var name = "DoodStream"
    override var mainUrl = "https://myvidplay.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val embedUrl = url.replace("doply.net", "myvidplay.com")
        val response = app.get(
            embedUrl,
            referer = mainUrl,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
        ).text

        val md5Regex = Regex("/pass_md5/([^/]*)/([^/']*)")
        val md5Match = md5Regex.find(response)
        val md5Path = md5Match?.value.toString()
        val expiry = md5Match?.groupValues?.getOrNull(1) ?: ""
        val token = md5Match?.groupValues?.getOrNull(2) ?: ""
        val md5Url = mainUrl + md5Path

        val md5Response = app.get(
            md5Url,
            referer = embedUrl,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
        ).text

        val baseLink = md5Response.trim()
        val directLink = if (token.isNotEmpty() && expiry.isNotEmpty()) {
            "$baseLink?token=$token&expiry=${expiry}000"
        } else {
            baseLink
        }

        callback.invoke(
            newExtractorLink(
                source = this.name, name = this.name, url = directLink, type = INFER_TYPE
            ) {
                this.referer = "https://myvidplay.com"
                this.quality = Qualities.Unknown.value
                this.headers =
                    mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
            })
    }
}

class Playmogo : DoodStream() {
    override var mainUrl = "https://playmogo.com"
    override var name = "DoodStream"
}

class DoodDoply : DoodStream() {
    override var mainUrl = "https://doply.net"
    override var name = "DoodStream"
}

class DoodPmExtractor : DoodStream() {
    override var mainUrl = "https://dood.pm"
}

class DoodVideo : DoodStream() {
    override var mainUrl = "https://vide0.net";
}
class Ds2Play : DoodStream() {
    override var mainUrl = "https://ds2play.com"
}
class d000d : DoodStream() {
    override var mainUrl = "https://d000d.com"
}

class Dooood : DoodStream() {
    override var mainUrl = "https://dooood.com"
}




class javclan : ExtractorApi() {
    override var name = "Javclan"
    override var mainUrl = "https://javclan.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val responsecode=app.get(url,referer=referer)
        if (responsecode.code==200) {
            val serverRes = responsecode.document
            val script = serverRes.selectFirst("script:containsData(sources)")?.data().toString()
            val headers = mapOf(
                "Accept" to "*/*",
                "Connection" to "keep-alive",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "cross-site",
                "Origin" to url,
            )
            Regex("file:\"(.*?)\"").find(script)?.groupValues?.get(1)?.let { link ->
                return listOf(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = link,
                        INFER_TYPE
                    ) {
                        this.referer = referer ?: ""
                        this.quality = getQualityFromName("")
                        this.headers = headers
                    }
                )
            }
        }
        return null
    }
}

open class StreamTAPE : ExtractorApi() {
    override val name = "Streamtape"
    override val mainUrl = "https://streamtape.com"
    override val requiresReferer = true

    private val stapeHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        "Upgrade-Insecure-Requests" to "1",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "none",
        "Sec-Fetch-User" to "?1",
        "Sec-GPC" to "1",
    )

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            Log.d("StreamtapeDebug", "İstek başladı: $url")
            val response = app.get(
                url,
                headers = stapeHeaders
            )
            val html = response.text
            Log.d("StreamtapeDebug", "HTML alındı, boyut: ${html.length}")

            val parsedUrl = java.net.URL(url)
            val host = parsedUrl.host
            val path = parsedUrl.path
            val id = path.substringAfterLast("/")

            val corsMatch = Regex("""expires=([^&"']+)&ip=([^&"']+)""").find(html)?.groupValues
            val expires = corsMatch?.get(1)
            val ip = corsMatch?.get(2)
            val realToken = Regex("""token=([a-zA-Z0-9\-_]+)""").findAll(html).lastOrNull()?.groupValues?.get(1)

            Log.d("StreamtapeDebug", "ID: $id")
            Log.d("StreamtapeDebug", "Expires: $expires")
            Log.d("StreamtapeDebug", "IP: $ip")
            Log.d("StreamtapeDebug", "Token: $realToken")

            if (expires.isNullOrEmpty() || ip.isNullOrEmpty() || realToken.isNullOrEmpty()) {
                Log.d("StreamtapeDebug", "HATA: Gerekli parametreler bulunamadı")
                return
            }

            val getVideoUrl = "https://$host/get_video?id=$id&expires=$expires&ip=$ip&token=$realToken&stream=1"
            Log.d("StreamtapeDebug", "Get Video URL: $getVideoUrl")

            val location = app.get(
                getVideoUrl,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
                    "Accept" to "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5",
                    "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Range" to "bytes=0-",
                    "Referer" to "https://$host$path",
                    "Cookie" to "_b=kube12",
                    "Sec-Fetch-Dest" to "video",
                    "Sec-Fetch-Mode" to "cors",
                    "Sec-Fetch-Site" to "same-origin",
                    "Accept-Encoding" to "identity",
                    "Sec-GPC" to "1"
                ),
                allowRedirects = false
            ).headers["location"]

            if (location.isNullOrEmpty()) {
                Log.d("StreamtapeDebug", "HATA: Location bulunamadı")
                return
            }

            Log.d("StreamtapeDebug", "Final URL: $location")

            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = location,
                ) {
                    this.referer = "https://$host/"
                    this.quality = Qualities.Unknown.value
                }
            )
        } catch (e: Exception) {
            Log.d("StreamtapeDebug", "Hata: ${e.message}")
        }
    }
}



class Watchadsontape : StreamTAPE() {
    override var mainUrl = "https://watchadsontape.com"
}
class Stape : StreamTAPE() {
    override var mainUrl = "https://stape.fun"
}

class StreamTapeNet : StreamTAPE() {
    override var mainUrl = "https://streamtape.net/"
}

class StreamTapeXyz : StreamTAPE() {
    override var mainUrl = "https://streamtape.xyz"
}

class ShaveTape : StreamTAPE() {
    override var mainUrl = "https://shavetape.cash"
}

class Lancewhoisdifficult: Voe() {
    override var mainUrl = "https://lancewhosedifficult.com"
}

class Javlesbians: Voe() {
    override var mainUrl = "https://javlesbians.com"
}

class Stevenfamilyedge : Voe() {
    override var mainUrl = "https://stevenfamilyedge.com"
}



open class CloudWish : ExtractorApi() {
    override val name = "CloudWish"
    override val mainUrl = "https://cloudwish.xyz"
    override val requiresReferer = true

    private val baseHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        "Upgrade-Insecure-Requests" to "1",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "none",
        "Sec-Fetch-User" to "?1",
        "Sec-GPC" to "1",
    )

    private fun unpack(packedJs: String): String? {
        try {
            val pattern = Regex(
                """\}\('((?:[^'\\]|\\.)*)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'((?:[^'\\]|\\.)*)'""",
                RegexOption.DOT_MATCHES_ALL
            )
            val match = pattern.find(packedJs)

            if (match == null) {
                Log.d("CloudWishDebug", "Regex No Match")
                return null
            }

            val p = match.groupValues[1]
                .replace("\\'", "'")
                .replace("\\\\", "\\")
            val a = match.groupValues[2].toInt()
            val c = match.groupValues[3].toInt()
            val kRaw = match.groupValues[4]
            val k = kRaw.split("|").toMutableList()

            Log.d("CloudWishDebug", "Unpack Init")

            while (k.size < c) {
                k.add("")
            }

            var result = p
            for (i in (c - 1) downTo 0) {
                if (k[i].isNotEmpty()) {
                    val token = Integer.toString(i, a)
                    result = result.replace("\\b$token\\b".toRegex(RegexOption.IGNORE_CASE), k[i])
                }
            }

            return result
        } catch (e: Exception) {
            Log.d("CloudWishDebug", "Unpack Error")
            return null
        }
    }

    open class CloudWish : ExtractorApi() {
        override val name = "CloudWish"
        override val mainUrl = "https://cloudwish.xyz"
        override val requiresReferer = true

        private val baseHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1",
            "Sec-GPC" to "1",
        )

        private fun unpack(packedJs: String): String? {
            try {
                val pattern = Regex(
                    """\}\('((?:[^'\\]|\\.)*)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'((?:[^'\\]|\\.)*)'""",
                    RegexOption.DOT_MATCHES_ALL
                )
                val match = pattern.find(packedJs)

                if (match == null) {
                    Log.d("CloudWishDebug", "Regex Eşleşmedi")
                    return null
                }

                val p = match.groupValues[1]
                    .replace("\\'", "'")
                    .replace("\\\\", "\\")
                val a = match.groupValues[2].toInt()
                val c = match.groupValues[3].toInt()
                val kRaw = match.groupValues[4]
                val k = kRaw.split("|").toMutableList()

                Log.d("CloudWishDebug", "Unpack Hazırlanıyor")

                while (k.size < c) {
                    k.add("")
                }

                var result = p
                for (i in (c - 1) downTo 0) {
                    if (k[i].isNotEmpty()) {
                        val token = Integer.toString(i, a)
                        result =
                            result.replace("\\b$token\\b".toRegex(RegexOption.IGNORE_CASE), k[i])
                    }
                }

                return result
            } catch (e: Exception) {
                Log.d("CloudWishDebug", "Unpack Hatası")
                return null
            }
        }

        override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
        ) {
            Log.d("CloudWishDebug", "İşlem Başladı")
            try {
                val response = app.get(url, headers = baseHeaders)
                val html = response.text
                Log.d("CloudWishDebug", "Sayfa Alındı")

                val document = Jsoup.parse(html)

                val packedScript = document.select("script")
                    .map { it.data() }
                    .firstOrNull { it.contains("eval(function(p,a,c,k,e,d)") }

                if (packedScript == null) {
                    Log.d("CloudWishDebug", "Paketli Script Yok")
                    return
                }

                Log.d("CloudWishDebug", "Unpack Başlatılıyor")

                val unpacked = unpack(packedScript)
                if (unpacked.isNullOrEmpty()) {
                    Log.d("CloudWishDebug", "Unpacked Hata")
                    return
                }

                Log.d("CloudWishDebug", "Unpack Başarılı")

                val parsedUrl = java.net.URL(url)
                val host = parsedUrl.host

                val m3u8Pattern =
                    Regex("""(https?://[^\s"'<>]+master\.m3u8[^\s"'<>]*|/stream/[^\s"'<>]+master\.m3u8)""")
                val m3u8Urls = m3u8Pattern.findAll(unpacked)
                    .map { it.groupValues[1] }
                    .distinct()
                    .toList()

                val masterUrl = m3u8Urls.firstOrNull { it.startsWith("/stream/") }

                if (masterUrl != null) {
                    val fullUrl = "https://$host$masterUrl"
                    Log.d("CloudWishDebug", "Master Link Bulundu")
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = name,
                            url = fullUrl,
                        ) {
                            this.referer = url
                            this.quality = Qualities.Unknown.value
                            this.headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
                                "Sec-GPC" to "1",
                                "Sec-Fetch-Dest" to "empty",
                                "Sec-Fetch-Mode" to "cors",
                                "Sec-Fetch-Site" to "same-origin",
                            )
                        }
                    )
                } else if (m3u8Urls.isNotEmpty()) {
                    Log.d("CloudWishDebug", "Çok fazla Video Mevcut")
                    for (m3u8Url in m3u8Urls) {
                        val fullUrl =
                            if (m3u8Url.startsWith("/")) "https://$host$m3u8Url" else m3u8Url
                        val quality = when {
                            fullUrl.contains("/hls4/") -> Qualities.P1080.value
                            fullUrl.contains("/hls3/") -> Qualities.P720.value
                            else -> Qualities.Unknown.value
                        }
                        callback.invoke(
                            newExtractorLink(source = name, name = name, url = fullUrl) {
                                this.referer = url
                                this.quality = quality
                            }
                        )
                    }
                } else {
                    Log.d("CloudWishDebug", "Yayın Bulunamadı")
                }

            } catch (e: Exception) {
                Log.d("CloudWishDebug", "Genel Hata")
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Yanit(
    val hls: String? = null,
    val source: String? = null,
    val cf: String? = null
)


data class SvgObject(
    val stream: String,
    val hash: String
)


class KPFilemoonSx : Filemoon() { override var name = "Filemoon"; override var mainUrl = "https://filemoon.sx" }
class KPFilemoonIn : Filemoon() { override var name = "Filemoon"; override var mainUrl = "https://filemoon.in" }
class KPFilemoonLink : Filemoon() { override var name = "Filemoon"; override var mainUrl = "https://filemoon.link" }
class KPFilemoonWf : Filemoon() { override var name = "Filemoon"; override var mainUrl = "https://filemoon.wf" }
class KPFilemoonEu : Filemoon() { override var name = "Filemoon"; override var mainUrl = "https://filemoon.eu" }
class KPFilemoonArt : Filemoon() { override var name = "Filemoon"; override var mainUrl = "https://filemoon.art" }
class KPFilemoonNl : Filemoon() { override var name = "Filemoon"; override var mainUrl = "https://filemoon.nl" }
class KPCinegrab : Filemoon() { override var name = "Cinegrab"; override var mainUrl = "https://cinegrab.com" }
class KPMoonmov : Filemoon() { override var name = "Moonmov"; override var mainUrl = "https://moonmov.pro" }
class KPNineSixAr : Filemoon() { override var name = "96ar"; override var mainUrl = "https://96ar.com" }
class KPKerapoxy : Filemoon() { override var name = "Kerapoxy"; override var mainUrl = "https://kerapoxy.cc" }
class KPFurher : Filemoon() { override var name = "Furher"; override var mainUrl = "https://furher.in" }
class KPOneAzayf9w : Filemoon() { override var name = "1azayf9w"; override var mainUrl = "https://1azayf9w.xyz" }
class KPEightOneU6xl9d : Filemoon() { override var name = "81u6xl9d"; override var mainUrl = "https://81u6xl9d.xyz" }
class KPSmdfs40r : Filemoon() { override var name = "Smdfs40r"; override var mainUrl = "https://smdfs40r.skin" }
class KPC1z39 : Filemoon() { override var name = "C1z39"; override var mainUrl = "https://c1z39.com" }
class KPBf0skv : Filemoon() { override var name = "Bf0skv"; override var mainUrl = "https://bf0skv.org" }
class KPZ1ekv717 : Filemoon() { override var name = "Z1ekv717"; override var mainUrl = "https://z1ekv717.fun" }
class KPL1afav : Filemoon() { override var name = "L1afav"; override var mainUrl = "https://l1afav.net" }
class KPTwoTwoTwoi8x : Filemoon() { override var name = "222i8x"; override var mainUrl = "https://222i8x.lol" }
class KPEightMhlloqo : Filemoon() { override var name = "8mhlloqo"; override var mainUrl = "https://8mhlloqo.fun" }
class KPF51rm : Filemoon() { override var name = "F51rm"; override var mainUrl = "https://f51rm.com" }
class KPXcoic : Filemoon() { override var name = "Xcoic"; override var mainUrl = "https://xcoic.com" }
class KPBoosteradx : Filemoon() { override var name = "Boosteradx"; override var mainUrl = "https://boosteradx.online" }
class KPStreamlyplayer : Filemoon() { override var name = "Streamlyplayer"; override var mainUrl = "https://streamlyplayer.online" }
class KPStreamlyplayero : Filemoon() { override var name = "Streamlyplayero"; override var mainUrl = "https://streamlyplayero.online" }
class KPBysewihe : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysewihe.com" }
class KPByselapuix : Filemoon() { override var name = "Byse"; override var mainUrl = "https://byselapuix.com" }
class KPEmbedplaybyse : Filemoon() { override var name = "Byse"; override var mainUrl = "https://embedplaybyse.top" }
class KPSb1254w9megshle : Filemoon() { override var name = "Sb1254w9megshle"; override var mainUrl = "https://sb1254w9megshle.org" }
class KPMoflixStream : Filemoon() { override var name = "MoflixStream"; override var mainUrl = "https://moflix-stream.link" }
class KPBysezoxexe : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysezoxexe.com" }
class KPF16px : Filemoon() { override var name = "F16px"; override var mainUrl = "https://f16px.com" }
class KPBysesayeveum : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysesayeveum.com" }
class KPBysetayico : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysetayico.com" }
class KPBysevepoin : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysevepoin.com" }
class KPBysezejataos : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysezejataos.com" }
class KPBysekoze : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysekoze.com" }
class KPBysesukior : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysesukior.com" }
class KPBysejikuar : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysejikuar.com" }
class KPBysefujedu : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysefujedu.com" }
class KPBysedikamoum : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysedikamoum.com" }
class KPBysebuho : Filemoon() { override var name = "Byse"; override var mainUrl = "https://bysebuho.com" }
class KPByseSx : Filemoon() { override var name = "Byse"; override var mainUrl = "https://byse.sx" }
class KPByseqekaho : Filemoon() { override var name = "Byse"; override var mainUrl = "https://byseqekaho.com" }