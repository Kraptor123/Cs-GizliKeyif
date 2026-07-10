// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object Hanimecozucu {

    private const val HANDSHAKE_KEY_SEED = "htv-insecure-handshake-v1"
    private const val AAD = "htv-insecure-v1"
    private const val SIGNATURE_SECRET = "Xkdi29"
    private const val SIGNATURE_VERSION = "mn2"

    private val handshakeKey: ByteArray by lazy {
        sha256(HANDSHAKE_KEY_SEED.toByteArray(Charsets.UTF_8))
    }

    private val aadBytes: ByteArray = AAD.toByteArray(Charsets.UTF_8)

    fun signatureCek(time: String, mainUrl: String): String {
        val message = "$time,$SIGNATURE_SECRET,$mainUrl,$SIGNATURE_VERSION,$time"
        return sha256(message.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun encryptHandshakeToken(payload: String): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(handshakeKey, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(aadBytes)

        val ctWithTag = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        val ct = ctWithTag.copyOfRange(0, ctWithTag.size - 16)
        val tag = ctWithTag.copyOfRange(ctWithTag.size - 16, ctWithTag.size)

        val innerJson = buildJsonObject {
            put("v", 1)
            put("alg", "AES-256-GCM")
            put("iv", base64UrlEncode(iv))
            put("tag", base64UrlEncode(tag))
            put("data", base64UrlEncode(ct))
        }.toString()

        return base64UrlEncode(innerJson.toByteArray(Charsets.UTF_8))
    }

    fun decryptXToken(xToken: String): String {
        val outerJson = String(base64UrlDecode(xToken), Charsets.UTF_8)
        val parsed = Json.parseToJsonElement(outerJson).jsonObject

        val iv = base64UrlDecode(parsed["iv"]!!.jsonPrimitive.content)
        val tag = base64UrlDecode(parsed["tag"]!!.jsonPrimitive.content)
        val data = base64UrlDecode(parsed["data"]!!.jsonPrimitive.content)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(handshakeKey, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(aadBytes)

        return String(cipher.doFinal(data + tag), Charsets.UTF_8)
    }

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun base64UrlEncode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun base64UrlDecode(s: String): ByteArray {
        val padded = if (s.length % 4 != 0) {
            s + "=".repeat(4 - (s.length % 4))
        } else s
        return Base64.getUrlDecoder().decode(padded)
    }

    @Serializable
    data class HandshakeResponse(
        @JsonProperty("sources")
        @SerialName("sources")
        val sources: List<HanimeSource> = emptyList()
    )

    @Serializable
    data class HanimeSource(
        @JsonProperty("src")
        @SerialName("src")
        val src: String = "",

        @JsonProperty("height")
        @SerialName("height")
        val height: Int = 0,

        @JsonProperty("label")
        @SerialName("label")
        val label: String = "",

        @JsonProperty("kind")
        @SerialName("kind")
        val kind: String = "normal"
    )
}

