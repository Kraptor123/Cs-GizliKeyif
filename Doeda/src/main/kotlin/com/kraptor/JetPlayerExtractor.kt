package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import okhttp3.FormBody
import org.json.JSONObject

open class JetPlayer : ExtractorApi() {
    override val name = "JetPlayer"
    override val mainUrl = "https://jetplayer.net"
    override val requiresReferer = true


    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
//        Log.d("kraptor_${this.name}", "url = $url")
        val pageHtml = app.get(url, referer = extRef).text
        val hash = Regex("var hash = '([a-f0-9]+)'").find(pageHtml)?.groupValues?.get(1)
            ?: throw ErrorLoadingException("Hash bulunamadı")
        val alt = Regex("id=\"alternative\" value=\"(\\d+)\"").find(pageHtml)?.groupValues?.get(1)
            ?: "0"
        val ord = Regex("id=\"order\" value=\"(\\d+)\"").find(pageHtml)?.groupValues?.get(1)
            ?: "0"

        // AJAX isteği ile kaynakları al
        val form = FormBody.Builder()
            .add("vid", hash)
            .add("alternative", alt)
            .add("ord", ord)
            .build()
        val ajaxResponse = app.post("$mainUrl/jet/ajax_sources.php", requestBody = form, referer = mainUrl).text

//        Log.d("kraptor_${this.name}", "ajaxResponse = $ajaxResponse")

        // JSON parse
        val json = JSONObject(ajaxResponse)
        if (!json.optBoolean("status", false)) {
            throw ErrorLoadingException("Video kaynağı alınamadı")
        }

        // Kaynak dizisi
        val sources = json.optJSONArray("source")
            ?: throw ErrorLoadingException("source alanı yok")

//        Log.d("kraptor_${this.name}", "sources = $sources")

        // İlk mp4 linkini bul
        var videoUrl: String? = null
        for (i in 0 until sources.length()) {
            val item = sources.getJSONObject(i)
            val file = item.optString("file")
            if (file.endsWith(".mp4")) {
                videoUrl = file
                break
            }
        }

        if (videoUrl.isNullOrEmpty()) {
            throw ErrorLoadingException("MP4 linki bulunamadı")
        }

//        Log.d("kraptor_${this.name}", "videoUrl = $videoUrl")
        callback.invoke(
            newExtractorLink(
                source = name,
                name = "JetPlayer MP4",
                url = videoUrl,
                type = ExtractorLinkType.VIDEO
            ) {
                quality = Qualities.Unknown.value
                this.referer = url
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0"
                )
            }
        )
    }
}
