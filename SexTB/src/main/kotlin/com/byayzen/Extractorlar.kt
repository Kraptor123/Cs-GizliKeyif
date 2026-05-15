// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.annotation.SuppressLint
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.extractors.VidStack
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import java.net.URI

open class DoodStream : ExtractorApi() {
    override var name = "DoodStream"
    override var mainUrl = "https://myvidplay.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val embedUrl = url.replace("doply.net", "myvidplay.com").replace("vide0.net", "myvidplay.com")
        Log.d("STB_Dood", "url = $url")
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
                this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
            })
    }
}

class DoodDoply : DoodStream() { override var mainUrl = "https://doply.net"; override var name = "DoodDoply" }
class DoodVideo : DoodStream() { override var mainUrl = "https://vide0.net"; override var name = "DoodVideo" }
class Ds2Play : DoodStream() { override var mainUrl = "https://ds2play.com" }
class d000d : DoodStream() { override var mainUrl = "https://d000d.com" }

open class Streamwish : ExtractorApi() {
    override var name = "Streamwish"
    override var mainUrl = "https://streamwish.to"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val responsecode = app.get(url)
        if (responsecode.code == 200) {
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
                    newExtractorLink(source = this.name, name = this.name, url = link, INFER_TYPE) {
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

class Streamhihi : Streamwish() { override var name = "Streamhihi"; override var mainUrl = "https://streamhihi.com" }
class Javsw : Streamwish() { override var mainUrl = "https://javsw.me"; override var name = "Javsw" }
open class VidHidePro : ExtractorApi() {
    override var name = "VidHidePro"
    override var mainUrl = "https://vidhidepro.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site",
            "Origin" to mainUrl,
            "User-Agent" to USER_AGENT
        )

        val response = app.get(getEmbedUrl(url), referer = referer)
        val script = if (!getPacked(response.text).isNullOrEmpty()) {
            var result = getAndUnpack(response.text)
            if (result.contains("var links")) { result = result.substringAfter("var links") }
            result
        } else {
            response.document.selectFirst("script:containsData(sources:)")?.data()
        } ?: return

        Regex(":\\s*\"(.*?m3u8.*?)\"").findAll(script).forEach { m3u8Match ->
            generateM3u8(name, fixUrl(m3u8Match.groupValues[1]), referer = "$mainUrl/", headers = headers).forEach(callback)
        }
    }

    private fun getEmbedUrl(url: String): String {
        return when {
            url.contains("/d/") -> url.replace("/d/", "/v/")
            url.contains("/download/") -> url.replace("/download/", "/v/")
            url.contains("/file/") -> url.replace("/file/", "/v/")
            url.contains("/e/") -> url.replace("/e/", "/v/") // Embed fix
            else -> url.replace("/f/", "/v/")
        }
    }
}

class VidhideVIP : VidHidePro() { override var mainUrl = "https://vidhidevip.com"; override var name = "VidhideVIP" }
class Javlion : VidHidePro() { override var mainUrl = "https://javlion.xyz"; override var name = "Javlion" }
class VidHidePro1 : VidHidePro() { override var mainUrl = "https://filelions.live" }
class VidHidePro2 : VidHidePro() { override var mainUrl = "https://filelions.online" }
class VidHidePro3 : VidHidePro() { override var mainUrl = "https://filelions.to" }
class VidHidePro4 : VidHidePro() { override var mainUrl = "https://kinoger.be" }
class VidHidePro6 : VidHidePro() { override var mainUrl = "https://vidhidepre.com" }
class VidHidePro7 : VidHidePro() { override var mainUrl = "https://vidhidehub.com" }
class Dhcplay : VidHidePro() { override var name = "DHC Play"; override var mainUrl = "https://dhcplay.com" }
class Smoothpre : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://smoothpre.com" }
class Dhtpre : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://dhtpre.com" }
class Peytonepre : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://peytonepre.com" }
class Movearnpre : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://movearnpre.com" }
class Dintezuvio : VidHidePro() { override var name = "EarnVids"; override var mainUrl = "https://dintezuvio.com" }
class HgLink : VidHidePro() { override var name = "HGLink"; override var mainUrl = "https://hglink.to" }
class RyderJet : VidHidePro() { override var name = "RyderJet"; override var mainUrl = "https://ryderjet.com" }

class MyCloudZ : VidHidePro() { override var mainUrl = "https://mycloudz.cc"; override var name = "MyCloudZ" }
class Turboplayers : StreamTape() { override var mainUrl = "https://turboplayers.xyz"; override var name = "Streamtape" }

open class LuluBase : ExtractorApi() {
    override val name = "LuluStream"
    override val mainUrl = "https://lulustream.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("LuluBase", url)
        try {
            val embedurl = url.replace("/d/", "/e/")
            Log.d("LuluBase", embedurl)

            val currenthost = try { URI(embedurl).host } catch (e: Exception) { "lulustream.com" }
            val currentorigin = "https://$currenthost"

            val requestheaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to (referer ?: embedurl)
            )

            val response = app.get(embedurl, headers = requestheaders)
            val html = response.text
            val unpacked = getAndUnpack(html)

            val m3u8regex = """["']([^"']+\.m3u8[^"']*)["']""".toRegex()
            val match = m3u8regex.find(unpacked)

            if (match != null) {
                var m3u8url = match.groupValues[1]
                Log.d("LuluBase", m3u8url)

                if (m3u8url.contains("index-v1-a1.m3u8")) {
                    m3u8url = m3u8url.replace("index-v1-a1.m3u8", "master.m3u8")
                    Log.d("LuluBase", m3u8url)
                }

                val videoheaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer" to embedurl,
                    "Origin" to currentorigin,
                    "Accept" to "*/*"
                )

                callback(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = m3u8url,
                    ) {
                        headers = videoheaders.toMutableMap()
                        quality = getQualityFromName(m3u8url)
                    }
                )
            }
        } catch (e: Exception) {
            Log.d("LuluBase", e.message ?: "Exception")
        }
    }
}

class LuluStream : LuluBase() {
    override val name = "LuluStream"
    override val mainUrl = "https://lulustream.com"
}

class LuluVid : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvid.com"
}

class LuluVdo : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvdo.com"
}

class LuluVdoo : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvdoo.com"
}

class LuluPvp : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulupvp.com"
}

class Luludlc : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulu.dlc.ovh/"
}

class Lulu0 : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulu0.ovh/"
}

class Lulux08 : LuluBase() {
    override val name = "Lulustream"
    override val mainUrl = "https://x08.ovh/"
}

class Javclan : ExtractorApi() {
    override var name = "Javclan"
    override var mainUrl = "https://javclan.com"
    override val requiresReferer = true
    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val res = app.get(url, referer = referer)
        val script = res.document.selectFirst("script:containsData(sources)")?.data().toString()
        Regex("file:\"(.*?)\"").find(script)?.groupValues?.get(1)?.let { link ->
            return listOf(newExtractorLink(name, name, link, INFER_TYPE) { this.referer = referer ?: "" })
        }
        return null
    }
}

class Javggvideo : ExtractorApi() {
    override var name = "Javgg Video"
    override var mainUrl = "https://javggvideo.xyz"
    override val requiresReferer = false
    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val response = app.get(url).text
        val link = response.substringAfter("var urlPlay = '").substringBefore("';")
        return listOf(newExtractorLink(name, name, link, INFER_TYPE) { this.quality = Qualities.Unknown.value })
    }
}

class swhoi : Filesim() { override var mainUrl = "https://swhoi.com"; override var name = "Streamwish" }
class MixDropis : MixDrop() { override var mainUrl = "https://mixdrop.is" }
class Javmoon : Filesim() { override var mainUrl = "https://javmoon.me"; override var name = "FileMoon" }


class StbP2P : VidStack() { override var mainUrl = "https://stb.strp2p.com"; override var name = "STBP2P" }
class Playerupnone : VidStack() { override var mainUrl = "https://player.upn.one"; override var name = "UPNP2P" }

open class Turtleviplay : ExtractorApi() {
    override var name = "Turtleviplay"
    override var mainUrl = "https://turtleviplay.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url, referer = referer).document
        val m3u8 = res.selectFirst("#video_player")?.attr("data-hash") ?: return

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = m3u8,
                type = ExtractorLinkType.M3U8,
            ) {
                this.referer = url
                this.quality = Qualities.Unknown.value
                this.headers = mapOf(
                    "Origin" to "https://turtleviplay.xyz",
                    "Accept" to "*/*",
                )
            }
        )
    }
}

class Turboviplay : Turtleviplay() {
    override var name = "Turboviplay"
    override var mainUrl = "https://turboviplay.com"
}
