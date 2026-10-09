// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import java.net.URLEncoder

open class HQCloud : ExtractorApi() {
    override val name = "HQCloud"
    override val mainUrl = "https://hgcloud.to"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("HQCloud", "getUrl: $url")

        val path = Regex("""(https?://[^/]+)(/[^?]+)""").find(url)?.groupValues?.get(2) ?: ""
        if (path.isEmpty()) {
            Log.d("HQCloud", "Could not extract path from $url")
            return
        }

        val inputHost = Regex("""https?://([^/]+)""").find(url)?.groupValues?.get(1)
        val domains = (listOfNotNull(inputHost) + listOf(
            "vibuxer.com",
            "audinifer.com",
            "streamhg.com",
            "dhcplay.com",
            "cybervynx.com",
            "hgcloud.to",
            "hglink.to"
        )).distinct()

        var html = ""
        var baseUrl = ""

        for (domain in domains) {
            val newUrl = "https://$domain$path"
            try {
                val response = app.get(newUrl, referer = referer ?: "https://hgcloud.to/")
                val text = response.text
                if (text.length > 2000 && (text.contains("eval(function") || text.contains("file_id"))) {
                    html = text
                    baseUrl = "https://$domain"
                    Log.d("HQCloud", "Fetched player HTML from $newUrl (length=${text.length})")
                    break
                }
            } catch (e: Exception) {
                Log.d("HQCloud", "Error fetching $newUrl: ${e.message}")
            }
        }

        if (html.isEmpty()) {
            Log.d("HQCloud", "No valid player HTML found across domains")
            return
        }

        val fileId = Regex("""\$\.cookie\('file_id',\s*'([^']+)'""").find(html)?.groupValues?.get(1)
        val aff = Regex("""\$\.cookie\('aff',\s*'([^']+)'""").find(html)?.groupValues?.get(1) ?: ""
        val refUrl = Regex("""\$\.cookie\('ref_url',\s*'([^']+)'""").find(html)?.groupValues?.get(1)

        val cookieString = buildString {
            if (!fileId.isNullOrEmpty()) append("file_id=$fileId; ")
            if (aff.isNotEmpty()) append("aff=$aff; ")
            append("tsn=7")
            if (refUrl != null) append("; ref_url=${URLEncoder.encode(refUrl, "UTF-8")}")
        }

        var unpacked = getAndUnpack(html)
        if (unpacked.isEmpty()) {
            val packerRegex = Regex(
                """(?s)eval\(function\(p,a,c,k,e,d\)\{.*?\}\('((?:[^'\\]|\\.)*)',\s*\d+,\s*\d+,\s*'((?:[^'\\]|\\.)*)'\s*(?:\.split\('\|'\))?\)"""
            )
            val match = packerRegex.find(html)
            if (match != null) {
                var p = match.groupValues[1]
                val k = match.groupValues[2].split("|")
                for (i in k.indices.reversed()) {
                    val word = i.toString(36)
                    if (k[i].isNotEmpty()) {
                        p = p.replace(Regex("\\b$word\\b"), k[i])
                    }
                }
                unpacked = p
            }
        }

        if (unpacked.isEmpty()) {
            Log.d("HQCloud", "Unpacking script failed")
            return
        }

        val hls2 = Regex(""""hls2"\s*:\s*"([^"]+)"""").find(unpacked)?.groupValues?.get(1)
        val hls4 = Regex(""""hls4"\s*:\s*"([^"]+)"""").find(unpacked)?.groupValues?.get(1)
        val hls3 = Regex(""""hls3"\s*:\s*"([^"]+)"""").find(unpacked)?.groupValues?.get(1)

        fun fixStreamUrl(rawUrl: String?): String? {
            if (rawUrl.isNullOrBlank()) return null
            return if (rawUrl.startsWith("/")) "$baseUrl$rawUrl" else rawUrl
        }

        val extractedUrls = listOfNotNull(
            fixStreamUrl(hls2),
            fixStreamUrl(hls4),
            fixStreamUrl(hls3)
        ).distinct()

        val candidateUrls = if (extractedUrls.isNotEmpty()) {
            extractedUrls
        } else {
            Regex("""["']([^"']*(?:m3u8|/stream/)[^"']*)["']""").findAll(unpacked)
                .mapNotNull { fixStreamUrl(it.groupValues[1]) }
                .distinct()
                .toList()
        }

        if (candidateUrls.isEmpty()) {
            Log.d("HQCloud", "No stream URLs found in unpacked script")
            return
        }

        val headers = mutableMapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to "$baseUrl/"
        )
        if (cookieString.isNotBlank()) {
            headers["Cookie"] = cookieString
        }

        var linksFound = false
        for (streamUrl in candidateUrls) {
            Log.d("HQCloud", "Generating M3U8 for $streamUrl")
            val m3u8Links = generateM3u8(
                source = this.name,
                streamUrl = streamUrl,
                referer = "$baseUrl/",
                headers = headers
            )
            if (m3u8Links.isNotEmpty()) {
                m3u8Links.forEach(callback)
                linksFound = true
                break
            }
        }

        if (!linksFound) {
            val fallbackUrl = candidateUrls.first()
            Log.d("HQCloud", "M3u8Helper returned empty, fallback to raw link: $fallbackUrl")
            callback.invoke(
                newExtractorLink(
                    name = this.name,
                    source = this.name,
                    url = fallbackUrl,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$baseUrl/"
                    if (headers.isNotEmpty()) {
                        this.headers = headers
                    }
                }
            )
        }
    }
}

class HQLinks : HQCloud() {
    override val name = "HQLinks"
    override var mainUrl = "https://hglink.to"
}

class Vibuxer : HQCloud() {
    override val name = "Vibuxer"
    override var mainUrl = "https://vibuxer.com"
}

class Audinifer : HQCloud() {
    override val name = "Audinifer"
    override var mainUrl = "https://audinifer.com"
}