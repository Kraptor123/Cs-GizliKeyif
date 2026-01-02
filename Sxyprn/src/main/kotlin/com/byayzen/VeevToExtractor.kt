package com.kraptor

import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

open class VeevToExtractor() : ExtractorApi() {

    override val name = "VeevTo"
    override val mainUrl = "https://veev.to"
    override val requiresReferer = false

    // Helper function to convert string to int (similar to Python's js_int)
    private fun jsInt(x: String): Int {
        return x.toIntOrNull() ?: 0
    }

    // LZW decoder (equivalent to Python's veev_decode)
    private fun veevDecode(encoded: String): String {
        try {
            val result = mutableListOf<String>()
            val lut = mutableMapOf<Int, String>()
            var n = 256
            var c = encoded[0].toString()
            result.add(c)

            for (char in encoded.substring(1)) {
                val code = char.code
                val nc = if (code < 256) {
                    char.toString()
                } else {
                    lut[code] ?: (c + c[0])
                }
                result.add(nc)
                lut[n] = c + nc[0]
                n++
                c = nc
            }

            return result.joinToString("")
        } catch (e: Exception) {
            Log.e("kraptor_VeevTo", "Error in veevDecode: ${e.message}")
            return encoded
        }
    }

    // Build array from encoded string (equivalent to Python's build_array)
    private fun buildArray(encoded: String): List<List<Int>> {
        try {
            val d = mutableListOf<List<Int>>()
            val chars = encoded.toCharArray().toMutableList()

            if (chars.isEmpty()) {
                Log.e("kraptor_VeevTo", "buildArray: Empty input")
                return d
            }

            var count = jsInt(chars.removeAt(0).toString())
            Log.d("kraptor_VeevTo", "buildArray: Initial count = $count")

            while (count > 0) {
                val currentArray = mutableListOf<Int>()
                for (i in 0 until count) {
                    if (chars.isEmpty()) {
                        Log.e("kraptor_VeevTo", "buildArray: Not enough characters for count $count")
                        break
                    }
                    val charValue = chars.removeAt(0).toString()
                    val intValue = jsInt(charValue)
                    currentArray.add(0, intValue) // Prepend to match Python behavior
                }
                d.add(currentArray)
                Log.d("kraptor_VeevTo", "buildArray: Added array $currentArray")

                if (chars.isEmpty()) {
                    Log.d("kraptor_VeevTo", "buildArray: No more characters")
                    break
                }

                count = jsInt(chars.removeAt(0).toString())
                Log.d("kraptor_VeevTo", "buildArray: Next count = $count")
            }

            Log.d("kraptor_VeevTo", "buildArray: Final array = $d")
            return d
        } catch (e: Exception) {
            Log.e("kraptor_VeevTo", "Error in buildArray: ${e.message}")
            return emptyList()
        }
    }

    // Helper to convert hex string to UTF-8 string (FIXED VERSION)
    private fun hexToString(hex: String): String {
        try {
            // Remove any whitespace
            val cleanHex = hex.trim()

            // Ensure even length
            val paddedHex = if (cleanHex.length % 2 != 0) "0$cleanHex" else cleanHex

            // Convert hex to bytes
            val bytes = paddedHex.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()

            // Decode as UTF-8
            return String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e("kraptor_VeevTo", "Error in hexToString: ${e.message}")
            throw e
        }
    }

    // Decode URL using transformations (FIXED VERSION - matches Python exactly)
    private fun decodeUrl(encoded: String, tArray: List<Int>): String {
        try {
            var ds = encoded
            Log.d("kraptor_VeevTo", "decodeUrl: Initial encoded = $ds")

            // Process each transformation in the array
            for (t in tArray) {
                Log.d("kraptor_VeevTo", "decodeUrl: Processing transformation $t")

                // Check if we need to reverse
                if (t == 1) {
                    ds = ds.reversed()
                    Log.d("kraptor_VeevTo", "decodeUrl: After reverse = $ds")
                }

                // Hex decode - THIS HAPPENS AFTER EACH TRANSFORMATION
                ds = hexToString(ds)
                Log.d("kraptor_VeevTo", "decodeUrl: After hex decode = $ds")

                // Remove base64 string
                ds = ds.replace("dXRmOA==", "")
                Log.d("kraptor_VeevTo", "decodeUrl: After replace = $ds")
            }

            Log.d("kraptor_VeevTo", "decodeUrl: Final decoded = $ds")
            return ds
        } catch (e: Exception) {
            Log.e("kraptor_VeevTo", "Error in decodeUrl: ${e.message}")
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
            Log.d("kraptor_VeevTo", "Getting VeevTo URL: $url")

            // Step 1: Get initial page content
            val initialResponse = app.get(url, referer = "${mainUrl}/")
            val pageHtml = initialResponse.text

            // Extract media_id from URL (handle redirects)
            val mediaId = initialResponse.url.split("/").lastOrNull { it.isNotEmpty() } ?:
            url.split("/").lastOrNull { it.isNotEmpty() } ?: ""
            Log.d("kraptor_VeevTo", "Media ID: $mediaId")

            // Step 2: Extract encoded string using regex
            val regex = Regex("""[\.\s'](?:fc|_vvto\[[^\]]*)(?:['\]]*)?\s*[:=]\s*['"]([^'"]+)""")
            val encodedStrings = regex.findAll(pageHtml).map { it.groupValues[1] }.toList()
            Log.d("kraptor_VeevTo", "Found encoded strings: $encodedStrings")

            if (encodedStrings.isEmpty()) {
                Log.e("kraptor_VeevTo", "No encoded strings found in page")
                return
            }

            // Step 3: Try each encoded string to find valid 'ch'
            var ch: String? = null
            for (f in encodedStrings.reversed()) {
                Log.d("kraptor_VeevTo", "Trying encoded string: $f")
                val decoded = veevDecode(f)
                Log.d("kraptor_VeevTo", "Decoded to: $decoded")
                if (decoded != f) {
                    ch = decoded
                    Log.d("kraptor_VeevTo", "Found valid ch: $ch")
                    break
                }
            }

            if (ch == null) {
                Log.e("kraptor_VeevTo", "Failed to decode valid 'ch' value")
                return
            }

            // Step 4: Build transformation array
            val tArrays = buildArray(ch)
            if (tArrays.isEmpty()) {
                Log.e("kraptor_VeevTo", "Failed to build transformation array")
                return
            }

            // Step 5: Construct API URL
            val apiUrl = "${mainUrl}/dl?" + listOf(
                "op=player_api",
                "cmd=gi",
                "file_code=${URLEncoder.encode(mediaId, StandardCharsets.UTF_8.toString())}",
                "ch=${URLEncoder.encode(ch, StandardCharsets.UTF_8.toString())}",
                "ie=1"
            ).joinToString("&")
            Log.d("kraptor_VeevTo", "API URL: $apiUrl")

            // Step 6: Get API response
            val apiResponse = app.get(apiUrl, referer = url).text
            Log.d("kraptor_VeevTo", "API Response: $apiResponse")

            val jsonResponse = JSONObject(apiResponse)

            // Check response status
            if (jsonResponse.optString("status") != "success") {
                Log.e("kraptor_VeevTo", "API response status is not success: ${jsonResponse.optString("status")}")
                return
            }

            // Get file object
            val fileObj = jsonResponse.optJSONObject("file")
            if (fileObj == null) {
                Log.e("kraptor_VeevTo", "No file object in API response")
                return
            }

            // Check file status
            if (fileObj.optString("file_status") != "OK") {
                Log.e("kraptor_VeevTo", "File status is not OK: ${fileObj.optString("file_status")}")
                return
            }

            // Step 7: Extract encoded video URLs
            val dvArray = fileObj.optJSONArray("dv")
            if (dvArray == null || dvArray.length() == 0) {
                Log.e("kraptor_VeevTo", "No video sources in API response")
                return
            }

            // CRITICAL FIX: Use only the first transformation array
            val tArray = tArrays[0]
            Log.d("kraptor_VeevTo", "Using transformation array: $tArray")

            // Process each video source
            for (i in 0 until dvArray.length()) {
                val source = dvArray.getJSONObject(i)
                val encodedUrl = source.optString("s")
                Log.d("kraptor_VeevTo", "Source $i encoded URL: $encodedUrl")

                if (encodedUrl.isNotEmpty()) {
                    try {
                        // CRITICAL: First decode with veevDecode, then with decodeUrl
                        val firstDecode = veevDecode(encodedUrl)
                        Log.d("kraptor_VeevTo", "Source $i after veevDecode: $firstDecode")

                        val finalUrl = decodeUrl(firstDecode, tArray)
                        Log.d("kraptor_VeevTo", "Source $i decoded URL: $finalUrl")

                        // Check if URL looks valid
                        if (finalUrl.startsWith("http")) {
                            // Get quality info if available
                            val quality = source.optString("vid_title", "Unknown")

                            Log.d("kraptor_VeevTo", "✅ Final video URL: $finalUrl")
                            callback.invoke(
                                newExtractorLink(
                                    source = "VeevTo",
                                    name = "VeevTo ($quality)",
                                    url = finalUrl,
                                    type = ExtractorLinkType.VIDEO
                                ) {
                                    this.referer = "${mainUrl}/"
                                }
                            )
                        } else {
                            Log.e("kraptor_VeevTo", "Decoded URL doesn't look valid: $finalUrl")
                        }
                    } catch (e: Exception) {
                        Log.e("kraptor_VeevTo", "Error decoding URL: ${e.message}", e)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("kraptor_VeevTo", "Error in VeevTo getUrl: ${e.message}", e)
        }
    }
}