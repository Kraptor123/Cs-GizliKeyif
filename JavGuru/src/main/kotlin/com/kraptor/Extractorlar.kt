package com.kraptor

import android.annotation.SuppressLint
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink

class DoodDoply : DoodStream() {
    override var mainUrl = "https://doply.net"
    override var name = "DoodDoply"
}

class DoodVideo : DoodStream() {
    override var mainUrl = "https://vide0.net"
    override var name = "DoodVideo"
}

open class DoodStream : ExtractorApi() {
    override var name = "DoodStream"
    override var mainUrl = "https://myvidplay.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val embedUrl = url.replace("doply.net", "myvidplay.com").replace("vide0.net", "myvidplay.com")
        Log.d("kraptor_Dood","url = $url")
        val response = app.get(
            embedUrl,
            referer = mainUrl,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
        ).text

        val md5Regex = Regex("/pass_md5/([^/]*)/([^/']*)")
        val md5Match = md5Regex.find(response)
        val md5Path = md5Match?.value.toString()
        val expiry = md5Match?.groupValues?.getOrNull(1) ?: ""
        val token = md5Match?.groupValues?.getOrNull(2) ?: ""
        val md5Url = mainUrl + md5Path

        val md5Response = app.get(
            md5Url,
            referer = embedUrl,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
        ).text

        val baseLink = md5Response.trim()
        val directLink = if (token.isNotEmpty() && expiry.isNotEmpty()) {
            "$baseLink?token=$token&expiry=${expiry}000"
        } else {
            baseLink
        }

        callback.invoke(
            newExtractorLink(
                source = this.name, name = this.name, url = directLink, type = INFER_TYPE
            ) {
                this.referer = "https://myvidplay.com"
                this.quality = Qualities.Unknown.value
                this.headers =
                    mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
            })
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
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val response = app.get(url).document
        val extractedpack = response.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data().toString()
        JsUnpacker(extractedpack).unpack()?.let { unPacked ->
            Regex("file:\"(.*?)\"").find(unPacked)?.groupValues?.get(1)?.let { link ->
                return listOf(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = link,
                        type = INFER_TYPE
                    ) {
                        this.quality = Qualities.Unknown.value
                        this.referer = "$mainUrl/"
                        this.headers = mapOf(
                            "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                            "Origin" to mainUrl
                        )
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

class Ds2Play : DoodStream() {
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

class d000d : DoodStream() {
    override var mainUrl = "https://d000d.com"
}


open class StreamTAPE : ExtractorApi() {
    override val name = "Streamtape"
    override val mainUrl = "https://streamtape.com"
    override val requiresReferer = true

    private val stapeHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:147.0) Gecko/20100101 Firefox/147.0",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        "Upgrade-Insecure-Requests" to "1",
        "Sec-Fetch-Dest" to "iframe",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "cross-site",
        "Sec-GPC" to "1",
    )

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val html = app.get(
            url,
            headers = stapeHeaders + mapOf("Referer" to (referer ?: "https://jav.guru/"))
        ).text

        val divUrl = Regex("""<div id="ideoolink"[^>]*>([^<]+)</div>""")
            .find(html)?.groupValues?.get(1)?.trim() ?: return
        val realToken = Regex("""token=([a-zA-Z0-9]+)['"]""")
            .findAll(html).lastOrNull()?.groupValues?.get(1) ?: return

        val getVideoUrl = "https:/${divUrl.replace(Regex("token=[a-zA-Z0-9]+"), "token=$realToken")}&stream=1"

        val finalUrl = app.get(
            getVideoUrl,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:147.0) Gecko/20100101 Firefox/147.0",
                "Accept" to "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5",
                "Referer" to "$mainUrl/",
                "Origin" to mainUrl,
            ),
            allowRedirects = false
        ).headers["location"] ?: run {
            Log.d("kraptor_$name", "HATA: Redirect location alınamadı")
            return
        }

        Log.d("kraptor_$name", "Final URL: $finalUrl")

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = finalUrl,
            ) {
                this.referer = "$mainUrl/"
                this.quality = Qualities.Unknown.value
            }
        )
    }
}


class Watchadsontape : StreamTAPE() {
    override var mainUrl = "https://watchadsontape.com"
}
class Stape : StreamTAPE() {
    override var mainUrl = "https://stape.fun"
}

class StreamTapeNet : StreamTAPE() {
    override var mainUrl = "https://streamtape.net"
}

class StreamTapeXyz : StreamTAPE() {
    override var mainUrl = "https://streamtape.xyz"
}

class ShaveTape : StreamTAPE() {
    override var mainUrl = "https://shavetape.cash"
}

class Lancewhoisdifficult: Voe() {
    override var mainUrl = "https://lancewhosedifficult.com"
}

class Javlesbians: Voe() {
    override var mainUrl = "https://javlesbians.com"
}