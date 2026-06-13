package com.kraptor

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.newExtractorLink
import android.util.Log
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import java.net.URI
import java.net.URLEncoder

open class BunkrExtractor : BunkrCrExtractor() {
    override var name = "Bunkr"
    override var mainUrl = "https://bunkr.ru"
}

open class CDNBunkrExtractor : BunkrCrExtractor() {
    override var name = "Bunkr"
    override var mainUrl = "https://cdn12.bunkr.ru"
}

open class BunkrCrExtractor : ExtractorApi() {
    override var name = "Bunkr"
    override var mainUrl = "https://bunkr.cr"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("kraptor_BunkrCr", "Gelen URL: $url")


        if (isOldCdnUrl(url)) {
            Log.d("kraptor_BunkrCr", "Eski CDN URL tespit edildi → /f/ sayfasına yönlendiriliyor")
            handleOldCdnUrl(url, referer, callback)
            return
        }

        if (isNewCdnUrl(url)) {
            Log.d("kraptor_BunkrCr", "Yeni CDN URL tespit edildi, doğrudan imzalanıyor")
            signAndCallback(url, "https://glb-apisign.cdn.cr/sign", "https://bunkr.cr/", callback)
            return
        }

        var pageUrl = url
        if (url.contains("/v/")) {
            pageUrl = url.replace("/v/", "/f/")
            Log.d("kraptor_BunkrCr", "/v/ → /f/ çevirimi: $pageUrl")
        }

        fetchPageAndSign(pageUrl, referer, callback)
    }

    private suspend fun fetchPageAndSign(
        pageUrl: String,
        referer: String?,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(pageUrl, referer = referer).text

        val cdnUrl = Regex("""var\s+jsCDN\s*=\s*"([^"]+)"""")
            .find(doc)?.groupValues?.getOrNull(1)
            ?.unescapeJs()
            ?.trim()

        val signApiUrl = Regex("""var\s+signUrl\s*=\s*"([^"]+)"""")
            .find(doc)?.groupValues?.getOrNull(1)
            ?.unescapeJs()
            ?.trim()

        Log.d("kraptor_BunkrCr", "Sayfa: $pageUrl")
        Log.d("kraptor_BunkrCr", "CDN URL  = $cdnUrl")
        Log.d("kraptor_BunkrCr", "Sign API = $signApiUrl")

        if (cdnUrl.isNullOrBlank() || signApiUrl.isNullOrBlank()) {
            Log.w("kraptor_BunkrCr", "jsCDN/signUrl bulunamadı, fallback deneniyor...")

            // Fallback 1: Download linki (dl.bunkr.cr/file/<id>)
            val dlLink = Regex("""href="(https://dl\.bunkr\.\w+/file/\d+)"""")
                .find(doc)?.groupValues?.getOrNull(1)

            if (!dlLink.isNullOrBlank()) {
                Log.d("kraptor_BunkrCr", "Download linki bulundu: $dlLink")
                callback.invoke(
                    newExtractorLink(
                        source = "Bunkr",
                        name = "Bunkr",
                        url = dlLink,
                        type = INFER_TYPE
                    ) {
                        this.referer = "https://bunkr.cr/"
                        this.quality = Qualities.Unknown.value
                    }
                )
                return
            }

            Log.e("kraptor_BunkrCr", "Hiçbir video kaynağı bulunamadı!")
            return
        }

        signAndCallback(cdnUrl, signApiUrl, pageUrl, callback)
    }

    private suspend fun handleOldCdnUrl(
        cdnUrl: String,
        referer: String?,
        callback: (ExtractorLink) -> Unit
    ) {

        val slug = cdnUrl.substringAfterLast("/")
        val fPageUrl = "https://bunkr.cr/f/$slug"

        Log.d("kraptor_BunkrCr", "Eski CDN slug: $slug")
        Log.d("kraptor_BunkrCr", "/f/ sayfası: $fPageUrl")

        try {
            fetchPageAndSign(fPageUrl, referer, callback)
        } catch (e: Exception) {
            Log.w("kraptor_BunkrCr", "/f/ sayfası çekilemedi, alternatif domain deneniyor: $e")
            // Alternatif: bunkr.ru domainini dene
            try {
                val altPageUrl = "https://bunkr.ru/f/$slug"
                fetchPageAndSign(altPageUrl, referer, callback)
            } catch (e2: Exception) {
                Log.e("kraptor_BunkrCr", "Alternatif domain de başarısız: $e2")
            }
        }
    }

    private suspend fun signAndCallback(
        cdnUrl: String,
        signApiUrl: String,
        referer: String,
        callback: (ExtractorLink) -> Unit
    ) {
        val cdnUri = URI(cdnUrl)
        val path = cdnUri.path

        Log.d("kraptor_BunkrCr", "Sign edilecek CDN: ${cdnUri.host}")
        Log.d("kraptor_BunkrCr", "Sign edilecek path: $path")

        val signResponse = app.get(
            "$signApiUrl?path=${URLEncoder.encode(path, "UTF-8")}",
            headers = mapOf(
                "Referer" to referer,
                "Accept" to "application/json"
            )
        )

        val signJson = signResponse.text
        Log.d("kraptor_BunkrCr", "Sign API response: $signJson")

        val token = Regex(""""token"\s*:\s*"([^"]+)"""")
            .find(signJson)?.groupValues?.getOrNull(1)

        val ex = Regex(""""ex"\s*:\s*"?(\d+)"?""")
            .find(signJson)?.groupValues?.getOrNull(1)

        Log.d("kraptor_BunkrCr", "Token: $token | Ex: $ex")

        if (token.isNullOrBlank() || ex.isNullOrBlank()) {
            Log.e("kraptor_BunkrCr", "Token veya ex alınamadı! Sign API yanıtı: $signJson")
            return
        }

        val signedUrl = "$cdnUrl?token=$token&ex=$ex"
        Log.d("kraptor_BunkrCr", "Signed URL: $signedUrl")

        callback.invoke(
            newExtractorLink(
                source = "Bunkr",
                name = "Bunkr",
                url = signedUrl,
                type = INFER_TYPE
            ) {
                this.referer = "https://bunkr.cr/"
                this.quality = Qualities.Unknown.value
            }
        )
    }

    private fun String.unescapeJs(): String {
        return this
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("&amp;", "&")
    }

    companion object {
        private val OLD_CDN_PATTERNS = listOf(
            Regex("""^https?://cdn\d+\.bunkr\.\w+/"""),
            Regex("""^https?://media\d*\.bunkr\.\w+/""")
        )

        private val NEW_CDN_PATTERNS = listOf(
            Regex("""^https?://[a-z0-9-]+\.cdn\.cr/"""),
            Regex("""^https?://[a-z0-9-]+\.scdn\.st/""")
        )

        fun isOldCdnUrl(url: String): Boolean {
            return OLD_CDN_PATTERNS.any { it.containsMatchIn(url) }
        }

        fun isNewCdnUrl(url: String): Boolean {
            return NEW_CDN_PATTERNS.any { it.containsMatchIn(url) }
        }
    }
}
