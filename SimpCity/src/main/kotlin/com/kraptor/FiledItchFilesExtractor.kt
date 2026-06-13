package com.kraptor

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities

class FiledItchFilesExtractor : ExtractorApi() {
    override var name = "FileDitch"
    override var mainUrl = "https://fileditchfiles.me"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url, referer = referer).text
        val sourceUrl = Regex("""<source\s+src="([^"]+)"""")
            .find(doc)?.groupValues?.getOrNull(1)
            ?.replace("&amp;", "&")
            ?.trim()

        if (!sourceUrl.isNullOrBlank()) {
            emitLink(sourceUrl, callback)
            return
        }

        val downloadUrl = Regex("""href="(https://[^"]*donotshare[^"]+)"""")
            .find(doc)?.groupValues?.getOrNull(1)
            ?.replace("&amp;", "&")
            ?.trim()

        if (!downloadUrl.isNullOrBlank()) {
            emitLink(downloadUrl, callback)
            return
        }

        val videoSrc = Regex("""<video[^>]*src="([^"]+)"""")
            .find(doc)?.groupValues?.getOrNull(1)
            ?.replace("&amp;", "&")
            ?.trim()

        if (!videoSrc.isNullOrBlank()) {
            emitLink(videoSrc, callback)
            return
        }

        val anyCdnUrl = Regex("""(https://[a-z0-9]*\.?donotsharethesetemplinksyouidiot\.st/[^"'\s<>]+)""")
            .find(doc)?.groupValues?.getOrNull(1)
            ?.replace("&amp;", "&")
            ?.trim()

        if (!anyCdnUrl.isNullOrBlank()) {
            emitLink(anyCdnUrl, callback)
            return
        }
    }

    private suspend fun emitLink(
        videoUrl: String,
        callback: (ExtractorLink) -> Unit
    ) {
        callback.invoke(
            newExtractorLink(
                source = "FileDitch",
                name = "FileDitch",
                url = videoUrl,
                type = INFER_TYPE
            ) {
                this.referer = "https://fileditchfiles.me/"
                this.quality = Qualities.Unknown.value
            }
        )
    }
}
