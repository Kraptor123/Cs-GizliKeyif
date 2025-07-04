package com.kraptor

import android.annotation.SuppressLint
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink

open class DoodJav : ExtractorApi() {
    override var name = "DoodStream"
    override var mainUrl = "https://d000d.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val changedurl=url.replace("dooood.com","d000d.com")
        val response0 = app.get(changedurl).text // html of DoodStream page to look for /pass_md5/...
        val md5 =mainUrl+(Regex("/pass_md5/[^']*").find(response0)?.value ?: return null)  // get https://dood.ws/pass_md5/...
        val trueUrl = app.get(md5, referer = url).text + "zUEJeL3mUN?token=" + md5.substringAfterLast("/")   //direct link to extract  (zUEJeL3mUN is random)
        val quality = Regex("\\d{3,4}p").find(response0.substringAfter("<title>").substringBefore("</title>"))?.groupValues?.get(0)
        return listOf(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = trueUrl
            ) {
                this.referer = mainUrl
                this.quality = getQualityFromName(quality)
            }
        ) // links are valid in 8h

    }
}

open class javclan : ExtractorApi() {
    override var name = "Javclan"
    override var mainUrl = "https://javclan.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val responsecode=app.get(url,referer=referer)
        if (responsecode.code==200) {
            val serverRes = responsecode.document
            val script = serverRes.selectFirst("script:containsData(sources)")?.data().toString()
            val headers = mapOf(
                "Accept" to "*/*",
                "Connection" to "keep-alive",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "cross-site",
                "Origin" to url,
            )
            Regex("file:\"(.*?)\"").find(script)?.groupValues?.get(1)?.let { link ->
                return listOf(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = link,
                        INFER_TYPE
                    ) {
                        this.referer = referer ?: ""
                        this.quality = getQualityFromName("")
                        this.headers = headers
                    }
                )
            }
        }
        return null
    }
}

class Streamhihi : Streamwish() {
    override var name = "Streamhihi"
    override var mainUrl = "https://streamhihi.com"
}

open class Streamwish : ExtractorApi() {
    override var name = "Streamwish"
    override var mainUrl = "https://streamwish.to"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val responsecode=app.get(url)
        if (responsecode.code==200) {
            val serverRes = responsecode.document
            //Log.d("Test12","$serverRes")
            val script = serverRes.selectFirst("script:containsData(sources)")?.data().toString()
            //Log.d("Test12","$script")
            val headers = mapOf(
                "Accept" to "*/*",
                "Connection" to "keep-alive",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "cross-site",
                "Origin" to url,
            )
            Regex("file:\"(.*?)\"").find(script)?.groupValues?.get(1)?.let { link ->
                return listOf(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = link,
                        INFER_TYPE
                    ) {
                        this.referer = referer ?: ""
                        this.quality = getQualityFromName("")
                        this.headers = headers
                    }
                )
            }
        }
        return null
    }
}

open class Maxstream : ExtractorApi() {
    override var name = "Maxstream"
    override var mainUrl = "https://maxstream.org"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val response = app.get(url).document
        val extractedpack =response.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data().toString()
        JsUnpacker(extractedpack).unpack()?.let { unPacked ->
            Regex("file:\"(.*?)\"").find(unPacked)?.groupValues?.get(1)?.let { link ->
                return listOf(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = link,
                        INFER_TYPE
                    ) {
                        this.referer = referer ?: ""
                        this.quality = Qualities.Unknown.value
                    }
                )
            }
        }
        return null
    }
}

open class Vidhidepro : ExtractorApi() {
    override val name = "Vidhidepro"
    override val mainUrl = "https://vidhidepro.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
    ): List<ExtractorLink>? {
        val response =app.get(url).document
        //val response = app.get(url, referer = referer)
        val script = response.selectFirst("script:containsData(sources)")?.data().toString()
        //Log.d("Test9871",script)
        Regex("sources:.\\[.file:\"(.*)\".*").find(script)?.groupValues?.get(1)?.let { link ->
            if (link.contains("m3u8"))
                return listOf(
                    newExtractorLink(
                        source = name,
                        name = name,
                        url = link,
                        ExtractorLinkType.M3U8
                    ) {
                        this.referer = referer ?: "$mainUrl/"
                        this.quality = Qualities.Unknown.value
                    }
                )
        }
        return null
    }
}

class Ds2Play : DoodLaExtractor() {
    override var mainUrl = "https://ds2play.com"
}


open class Javggvideo : ExtractorApi() {
    override val name = "Javgg Video"
    override val mainUrl = "https://javggvideo.xyz"
    override val requiresReferer = false

    @SuppressLint("SuspiciousIndentation")
    override suspend fun getUrl(
        url: String,
        referer: String?,
    ): List<ExtractorLink>? {
        val response =app.get(url).text
        val link = response.substringAfter("var urlPlay = '").substringBefore("';")
        return listOf(
            newExtractorLink(
                source = name,
                name = name,
                url = link,
                INFER_TYPE
            ) {
                this.referer = referer ?: "$mainUrl/"
                this.quality = Qualities.Unknown.value
            }
        )
    }
}

class Javlion : Vidhidepro() {
    override var mainUrl = "https://javlion.xyz"
    override val name = "Javlion"
}

class VidhideVIP : Vidhidepro() {
    override var mainUrl = "https://vidhidevip.com"
    override val name = "VidhideVIP"
}
class Javsw : Streamwish() {
    override var mainUrl = "https://javsw.me"
    override var name = "Javsw"
}

class swhoi : Filesim() {
    override var mainUrl = "https://swhoi.com"
    override var name = "Streamwish"
}

class MixDropis : MixDrop(){
    override var mainUrl = "https://mixdrop.is"
}


class Javmoon : Filesim() {
    override val mainUrl = "https://javmoon.me"
    override val name = "FileMoon"
}

class d000d : DoodLaExtractor() {
    override var mainUrl = "https://d000d.com"
}
