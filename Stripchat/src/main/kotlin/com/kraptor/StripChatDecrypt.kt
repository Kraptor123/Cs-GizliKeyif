package com.kraptor

import com.lagradost.api.Log
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// --- Global cache ---
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

/**
 * Try to fetch decode key from site (keeps same signature as your original)
 * fetchText: suspend (String, Map<String,String>?) -> String?  (pass { url, headers -> app.get(url, headers).text } )
 */
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

        // find Doppio name or path
        val doppioName = run {
            Regex("""require\(['"]\./(Doppio[^'"]*\.js)['"]\)""").find(mainJs)?.groupValues?.get(1)
                ?: Regex("""/v$version/(Doppio[^'"]*\.js)""").find(mainJs)?.groupValues?.get(1)
                ?: Regex("""Doppio[^'"]*\.js""").find(mainJs)?.value
        } ?: return null

        val doppioUrl = "$origin/v$version/$doppioName"
        val doppioJs = fetchText(doppioUrl, mapOf("User-Agent" to "Mozilla/5.0", "Referer" to mainJsUrl)) ?: return null

        // Try multiple patterns; group 1 is decodeKey candidate
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
//                    Log.d("kraptor_Stripchat", "Fetched decodeKey (partial): ${decodeKey.take(24)}...")
                    return decodeKey
                }
            }
        }
        null
    } catch (e: Exception) {
//        android.util.Log.w("kraptor_Stripchat", "fetchDecodeKeyFromSite error: ${e.message}")
        null
    }
}

/**
 * Decode + FILE extraction
 * - m3u8TextIn: playlist content
 * - fetchText: suspend (String, Map<String,String>?) -> String?
 */
@OptIn(ExperimentalEncodingApi::class)
suspend fun decodeM3u8MouflonFilesFixed(
    m3u8TextIn: String,
    fetchText: suspend (String, Map<String, String>?) -> String?,
    cacheDir: File,
    triedFreshKey: Boolean = false
): String {
    if (!m3u8TextIn.contains("#EXT-X-MOUFLON")) return m3u8TextIn

    // Helper: clear decoded/variant caches when key changes (optional)
    fun clearDecodedCaches() {
        try {
            cacheDir.listFiles()?.filter {
                it.name.startsWith("stripchat_decoded_") || it.name.startsWith("stripchat_variant_")
            }?.forEach { it.delete() }
//            Log.d("kraptor_Stripchat", "Cleared decoded/variant cache files")
        } catch (e: Exception) {
//            android.util.Log.w("kraptor_Stripchat", "clearDecodedCaches error: ${e.message}")
        }
    }

    // Load cached key once
    if (cachedDecodeKey.isNullOrEmpty()) {
        cachedDecodeKey = readKeyFileOrNull(cacheDir)
//        if (!cachedDecodeKey.isNullOrEmpty()) Log.d("kraptor_Stripchat", "Loaded decode key from key.txt")
    }

    val originalLines = m3u8TextIn.split("\n")
    val lines = originalLines.toMutableList()

    // Extract ALL PSCH/PKEY occurrences in order
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

    // If none, return original
    if (pEntries.isEmpty()) {
//        Log.d("kraptor_Stripchat", "No PSCH/PKEY entries found; returning original")
        return m3u8TextIn
    }

//    Log.d("kraptor_Stripchat", "Found ${pEntries.size} PSCH/PKEY entries; trying from last -> first")

    // We'll try each pkey starting from last to first
    val tryOrder = pEntries.asReversed()

    // function that attempts decode using current cachedDecodeKey and returns Pair(successBool, resultText)
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
                // try utf8 fallback
                try {
                    val utf8 = String(outBytes, Charsets.UTF_8).trim()
                    if (utf8.contains("/") || utf8.contains(".mp4") || utf8.contains(".ts")) {
                        candidate = utf8
                    } else {
                        candidate = outBytes.map { b -> if (b in 32..126) b.toInt().toChar() else '.' }.joinToString("").trim()
                    }
                } catch (_: Exception) {
                    candidate = outBytes.map { b -> if (b in 32..126) b.toInt().toChar() else '.' }.joinToString("").trim()
                }
            }

            var replaced = false
            for (j in (i + 1) until minOf(tmpLines.size, i + 8)) {
                val next = tmpLines[j].trim()
                if (next.isEmpty()) continue
                if (next.contains(".mp4") || next.contains(".ts") || next.contains(".m4s") || next.contains("media.mp4")) {
                    val original = tmpLines[j]
                    val finalCandidate = when {
                        candidate.startsWith("http://") || candidate.startsWith("https://") -> candidate
                        else -> {
                            try {
                                val baseUri = java.net.URI(original)
                                baseUri.resolve(candidate).toString()
                            } catch (_: Exception) {
                                original.replace("media.mp4", candidate).replace("media.ts", candidate)
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

    // Try each pkey
    for (entry in tryOrder) {
        val pkey = entry.pkey
//        Log.d("kraptor_Stripchat", "Trying pkey='$pkey'")

        // 1) try with cached key first (if exists)
        if (!cachedDecodeKey.isNullOrEmpty()) {
            val (ok, result) = attemptDecodeWithKey(cachedDecodeKey)
            if (ok) {
//                Log.d("kraptor_Stripchat", "Decode succeeded with existing cached key")
                return result
            } else {
//                Log.d("kraptor_Stripchat", "Cached key failed for this playlist; will try fetching fresh key for this pkey")
            }
        }

        // 2) try fetch fresh key for this pkey
        if (pkey.isNotEmpty()) {
            val freshKey = fetchDecodeKeyFromSite(pkey, fetchText, cacheDir)
            if (!freshKey.isNullOrEmpty()) {
                // if key changed, clear decoded caches and update saved key
                if (freshKey != cachedDecodeKey) {
//                    Log.d("kraptor_Stripchat", "Key changed for pkey '$pkey' -> updating cache")
                    cachedDecodeKey = freshKey
                    saveKeyFile(cacheDir, freshKey)
                    clearDecodedCaches()
                }
                val (ok2, result2) = attemptDecodeWithKey(freshKey)
                if (ok2) {
//                    Log.d("kraptor_Stripchat", "Decode succeeded with freshly fetched key for pkey '$pkey'")
                    return result2
                } else {
//                    Log.w("kraptor_Stripchat", "Fresh key failed for pkey '$pkey', trying previous pkeys if any")
                }
            } else {
//                Log.w("kraptor_Stripchat", "Could not fetch fresh key for pkey '$pkey'")
            }
        } else {
//            Log.d("kraptor_Stripchat", "pkey empty for this PSCH entry; skipping fetch")
        }
        // if reached here, this pkey failed -> continue to previous pkey
    }

    // If none succeeded: log and return original (or partial replaced if you prefer)
//    Log.w("kraptor_Stripchat", "All pkey attempts failed; returning original playlist (no reliable decode)")
    return m3u8TextIn
}
