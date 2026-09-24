package com.kraptor

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.httpsify
import com.lagradost.cloudstream3.utils.newExtractorLink

open class W3playExtractor : ExtractorApi() {

    override val name            = "W3play"
    override val mainUrl         = "https://w3play.cdnly.pics"
    override val requiresReferer = true

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:156.0) Gecko/20100101 Firefox/156.0"

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val embedHeaders = mapOf(
                "User-Agent"                to userAgent,
                "Accept"                    to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Sec-Fetch-Dest"            to "iframe",
                "Sec-Fetch-Mode"            to "navigate",
                "Sec-Fetch-Site"            to "cross-site",
                "Upgrade-Insecure-Requests" to "1"
            )

            val document = try {
                app.get(url, referer = referer ?: "${mainUrl}/", headers = embedHeaders).document
            } catch (e: Exception) {
                null
            }

            val videoUrl = document?.selectFirst("video[data-source]")?.attr("data-source")
                ?: document?.selectFirst("video source[src]")?.attr("src")
                ?: document?.selectFirst("video[src]")?.attr("src")
                ?: let {
                    val hash = url.substringAfterLast("/e/").substringAfterLast("/").substringBefore("?").substringBefore("#")
                    if (hash.length >= 32) "https://play.hianime1.cfd/xlove/$hash.mp4" else null
                }
                ?: return

            val type = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO

            val videoHeaders = mapOf(
                "User-Agent"      to userAgent,
                "Accept"          to "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5",
                "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                "Referer"         to "${mainUrl}/",
                "Origin"          to mainUrl,
                "Sec-Fetch-Dest"  to "video",
                "Sec-Fetch-Mode"  to "cors",
                "Sec-Fetch-Site"  to "cross-site",
                "Accept-Encoding" to "identity"
            )

            callback.invoke(
                newExtractorLink(
                    source = name,
                    name   = name,
                    url    = httpsify(videoUrl),
                    type   = type
                ) {
                    this.referer = "${mainUrl}/"
                    this.headers = videoHeaders
                }
            )
        } catch (e: Exception) {
        }
    }
}

class CdnlyExtractor : W3playExtractor() {
    override val name    = "Cdnly"
    override val mainUrl = "https://cdnly.pics"
}
