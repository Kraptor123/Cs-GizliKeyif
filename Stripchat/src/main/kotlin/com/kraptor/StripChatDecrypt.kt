package com.kraptor

import com.lagradost.api.Log
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private var cachedDecodeKey: String? = null

private fun padB64(s: String): String {
    if (s.isEmpty()) return s
    val mod = s.length % 4
    return if (mod == 0) s else s + "=".repeat((4 - mod) % 4)
}

private fun sha256Bytes(key: String): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))

private fun xorBytes(data: ByteArray, hash: ByteArray): ByteArray {
    val out = ByteArray(data.size)
    for (i in data.indices) out[i] = (data[i].toInt() xor (hash[i % hash.size].toInt())).toByte()
    return out
}

private fun isLikelyFilename(s: String): Boolean {
    val t = s.trim()
    return t.contains(".mp4") || t.contains(".ts") || t.contains("/") || t.contains("_") || t.contains("-")
}

private fun readKeyFileOrNull(cacheDir: File): String? =
    try { File(cacheDir, "key.txt").takeIf { it.exists() }?.readText()?.trim() } catch (_: Exception) { null }

private fun saveKeyFile(cacheDir: File, key: String) {
    try { File(cacheDir, "key.txt").writeText(key) } catch (_: Exception) {}
}

private suspend fun fetchDecodeKeyFromSite(
    pkey: String,
    fetchText: suspend (String, Map<String, String>?) -> String?,
    cacheDir: File
): String? {
    if (pkey.isEmpty()) return null
    return try {
        val cfgUrl = "https://stripchat.com/api/front/v3/config/static"
        val cfgText = fetchText(cfgUrl, mapOf("User-Agent" to "Mozilla/5.0")) ?: return null
        val staticJson = JSONObject(cfgText).getJSONObject("static")
        val origin = staticJson.getJSONObject("features").optString("MMPExternalSourceOrigin", "")
        val version = staticJson.getJSONObject("featuresV2")
            .getJSONObject("playerModuleExternalLoading")
            .optString("mmpVersion", "")
        if (origin.isEmpty() || version.isEmpty()) return null

        val mainJsUrl = "$origin/v$version/main.js"
        val mainJs = fetchText(mainJsUrl, mapOf("User-Agent" to "Mozilla/5.0", "Referer" to "https://stripchat.com")) ?: return null


        val doppioName = run {
            Regex("""require\(['"]\./(Doppio[^'"]*\.js)['"]\)""").find(mainJs)?.groupValues?.get(1)
                ?: Regex("""/v$version/(Doppio[^'"]*\.js)""").find(mainJs)?.groupValues?.get(1)
                ?: Regex("""Doppio[^'"]*\.js""").find(mainJs)?.value
        } ?: return null

        val doppioUrl = "$origin/v$version/$doppioName"
        val doppioJs = fetchText(doppioUrl, mapOf("User-Agent" to "Mozilla/5.0", "Referer" to mainJsUrl)) ?: return null

        val patterns = listOf(
            Regex("""["']?${Regex.escape(pkey)}["']?\s*[:=]\s*['"]([^'"]{6,400})['"]"""),
            Regex("""["']${Regex.escape(pkey)}["']\s*:\s*['"]([^'"]{6,400})['"]"""),
            Regex("""${Regex.escape(pkey)}\s*[:=]\s*([A-Za-z0-9_\-+/=]{6,400})""")
        )
        for (pat in patterns) {
            val m = pat.find(doppioJs)
            if (m != null) {
                val decodeKey = m.groupValues[1].trim()
                if (decodeKey.isNotEmpty()) {
                    cachedDecodeKey = decodeKey
                    saveKeyFile(cacheDir, decodeKey)
                    return decodeKey
                }
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}


@OptIn(ExperimentalEncodingApi::class)
suspend fun decodeM3u8MouflonFilesFixed(
    m3u8TextIn: String,
    fetchText: suspend (String, Map<String, String>?) -> String?,
    cacheDir: File,
    triedFreshKey: Boolean = false
): String {
    if (!m3u8TextIn.contains("#EXT-X-MOUFLON")) return m3u8TextIn

    fun clearDecodedCaches() {
        try {
            cacheDir.listFiles()?.filter {
                it.name.startsWith("stripchat_decoded_") || it.name.startsWith("stripchat_variant_")
            }?.forEach { it.delete() }
        } catch (e: Exception) {
        }
    }

    if (cachedDecodeKey.isNullOrEmpty()) {
        cachedDecodeKey = readKeyFileOrNull(cacheDir)
    }

    val originalLines = m3u8TextIn.split("\n")
    val lines = originalLines.toMutableList()

    data class PEntry(val psch: String, val pkey: String)

    val pEntries = mutableListOf<PEntry>()
    for (l in originalLines) {
        val t = l.trim()
        if (t.uppercase().startsWith("#EXT-X-MOUFLON:PSCH")) {
            val parts = t.split(':', limit = 4)
            val psch = if (parts.size > 2) parts[2] else ""
            val pkey = if (parts.size > 3) parts[3] else ""
            pEntries.add(PEntry(psch, pkey))
        }
    }

    if (pEntries.isEmpty()) {
        return m3u8TextIn
    }


    val tryOrder = pEntries.asReversed()

    suspend fun attemptDecodeWithKey(currentKey: String?): Pair<Boolean, String> {
        if (currentKey.isNullOrEmpty()) return Pair(false, m3u8TextIn)
        val tmpLines = lines.toMutableList()
        val fileTagRegex = Regex("""#EXT-X-MOUFLON:FILE:(.+)""", RegexOption.IGNORE_CASE)
        var invalidCount = 0

        for (i in tmpLines.indices) {
            val line = tmpLines[i]
            val match = fileTagRegex.find(line) ?: continue

            var encRaw = match.groupValues[1].trim()
            encRaw = encRaw.replace(Regex("""\s+"""), "")
            var enc = encRaw.replace('-', '+').replace('_', '/')
            enc = padB64(enc)

            val decodedBytes = try {
                Base64.Default.decode(enc)
            } catch (e: Exception) {
                invalidCount++
                continue
            }

            val outBytes = xorBytes(decodedBytes, sha256Bytes(currentKey))

            var candidate = String(outBytes, Charsets.ISO_8859_1).trim()
            val nonPrintable = candidate.count { it.code < 32 || it.code > 126 }
            val hasManyNonPrintable = nonPrintable > (candidate.length / 2)

            if (hasManyNonPrintable) {

                try {
                    val utf8 = String(outBytes, Charsets.UTF_8).trim()
                    if (utf8.contains("/") || utf8.contains(".mp4") || utf8.contains(".ts")) {
                        candidate = utf8
                    } else {
                        candidate =
                            outBytes.map { b -> if (b in 32..126) b.toInt().toChar() else '.' }
                                .joinToString("").trim()
                    }
                } catch (_: Exception) {
                    candidate = outBytes.map { b -> if (b in 32..126) b.toInt().toChar() else '.' }
                        .joinToString("").trim()
                }
            }

            var replaced = false
            for (j in (i + 1) until minOf(tmpLines.size, i + 8)) {
                val next = tmpLines[j].trim()
                if (next.isEmpty()) continue
                if (next.contains(".mp4") || next.contains(".ts") || next.contains(".m4s") || next.contains(
                        "media.mp4"
                    )
                ) {
                    val original = tmpLines[j]
                    val finalCandidate = when {
                        candidate.startsWith("http://") || candidate.startsWith("https://") -> candidate
                        else -> {
                            try {
                                val baseUri = java.net.URI(original)
                                baseUri.resolve(candidate).toString()
                            } catch (_: Exception) {
                                original.replace("media.mp4", candidate)
                                    .replace("media.ts", candidate)
                            }
                        }
                    }

                    if (!isLikelyFilename(finalCandidate)) {
                        invalidCount++
                        break
                    }

                    tmpLines[j] = finalCandidate
                    replaced = true
                    break
                }
            }
            if (!replaced) invalidCount++
        }

        val success = invalidCount == 0
        return Pair(success, tmpLines.joinToString("\n"))
    }


    for (entry in tryOrder) {
        val pkey = entry.pkey
        if (!cachedDecodeKey.isNullOrEmpty()) {
            val (ok, result) = attemptDecodeWithKey(cachedDecodeKey)
            if (ok) {
                return result
            } else {
            }
        }

        if (pkey.isNotEmpty()) {
            val freshKey = fetchDecodeKeyFromSite(pkey, fetchText, cacheDir)
            if (!freshKey.isNullOrEmpty()) {
                if (freshKey != cachedDecodeKey) {
                    cachedDecodeKey = freshKey
                    saveKeyFile(cacheDir, freshKey)
                    clearDecodedCaches()
                }
                val (ok2, result2) = attemptDecodeWithKey(freshKey)
                if (ok2) return result2
            }
        }
    }

    return m3u8TextIn
}