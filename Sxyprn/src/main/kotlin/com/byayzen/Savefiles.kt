package com.byayzen

import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.app

class SaveFiles : ExtractorApi() {
    override val name = "SaveFiles"
    override val mainUrl = "https://savefiles.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 1. Sayfa isteği için Headerlar
        val docHeaders = mapOf(
            "Referer" to "https://savefiles.com/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )

        // Sayfayı çek
        val text = app.get(url, headers = docHeaders).text

        // 2. Regex ile .m3u8 Linkini Bul
        // file:"https://..." yapısını yakalar
        val regex = """file\s*:\s*["']([^"']+\.m3u8[^"']*)["']""".toRegex()
        val match = regex.find(text)

        if (match != null) {
            val m3u8Url = match.groupValues[1]

            // 3. Oynatıcı Headerları (Senin verdiğin JSON'a göre)
            val videoHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0",
                "Referer" to "https://savefiles.com/",
                "Origin" to "https://savefiles.com",
                "Accept" to "*/*",
                "Connection" to "keep-alive",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "same-site"
            )

            // 4. M3U8 Helper ile Linkleri Oluştur ve Gönder
            // Bu yöntem 720p, 480p, 360p gibi seçenekleri otomatik ayıklar.
            M3u8Helper.generateM3u8(
                source = this.name,
                streamUrl = m3u8Url,
                referer = "https://savefiles.com/",
                headers = videoHeaders
            ).forEach(callback)
        }
    }
}