package com.kraptor

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.newExtractorLink
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
        if (isOldCdnUrl(url)) {
            handleOldCdnUrl(url, referer, callback)
            return
        }
        if (isNewCdnUrl(url)) {
            signAndCallback(url, "https://glb-apisign.cdn.cr/sign", "https://bunkr.cr/", callback)
            return
        }
        var pageUrl = url
        if (url.contains("/v/")) {
            pageUrl = url.replace("/v/", "/f/")
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

        if (cdnUrl.isNullOrBlank() || signApiUrl.isNullOrBlank()) {
            val dlLink = Regex("""href="(https://dl\.bunkr\.\w+/file/\d+)"""")
                .find(doc)?.groupValues?.getOrNull(1)
            if (!dlLink.isNullOrBlank()) {
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
        try {
            fetchPageAndSign(fPageUrl, referer, callback)
        } catch (e: Exception) {
            try {
                val altPageUrl = "https://bunkr.ru/f/$slug"
                fetchPageAndSign(altPageUrl, referer, callback)
            } catch (e2: Exception) {
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
        val signResponse = app.get(
            "$signApiUrl?path=${URLEncoder.encode(path, "UTF-8")}",
            headers = mapOf(
                "Referer" to referer,
                "Accept" to "application/json"
            )
        )
        val signJson = signResponse.text
        val token = Regex(""""token"\s*:\s*"([^"]+)"""")
            .find(signJson)?.groupValues?.getOrNull(1)
        val ex = Regex(""""ex"\s*:\s*"?(\d+)"?""")
            .find(signJson)?.groupValues?.getOrNull(1)

        if (token.isNullOrBlank() || ex.isNullOrBlank()) {
            return
        }
        val signedUrl = "$cdnUrl?token=$token&ex=$ex"
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
