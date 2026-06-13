package com.kraptor

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
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.extractors.VidStack
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import java.net.URI
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJSON
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import android.util.Base64
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.extractors.AesHelper
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.mapper
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.lagradost.cloudstream3.utils.M3u8Helper
import org.jsoup.Jsoup
import java.net.URL

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

class MixDropAG : MixDrop(){
    override var mainUrl = "https://mixdrop.ag"
}

class MixDropMy : MixDrop(){
    override var mainUrl = "https://mixdrop.my"
}

open class Vidguardto : ExtractorApi() {
    override val name = "Vidguard"
    override val mainUrl = "https://vidguard.to"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url)
        val resc = res.document.select("script:containsData(eval)").firstOrNull()?.data()
        resc?.let {
            val jsonStr2 = try { mapper.readValue<SvgObject>(runJS2(it)) } catch (e: Exception) { null } ?: return
            val watchlink = sigDecode(jsonStr2.stream)

            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = name,
                    url = watchlink,
                    INFER_TYPE
                ) {
                    this.referer = mainUrl
                    this.quality = Qualities.Unknown.value
                }
            )
        }
    }

    private fun sigDecode(url: String): String {
        val sig = url.split("sig=")[1].split("&")[0]
        var t = ""
        for (v in sig.chunked(2)) {
            val byteValue = Integer.parseInt(v, 16) xor 2
            t += byteValue.toChar()
        }
        val padding = when (t.length % 4) {
            2 -> "=="
            3 -> "="
            else -> ""
        }
        val decoded = Base64.decode(t + padding, Base64.DEFAULT)

        t = String(decoded).dropLast(5).reversed()
        val charArray = t.toCharArray()
        for (i in 0 until charArray.size - 1 step 2) {
            val temp = charArray[i]
            charArray[i] = charArray[i + 1]
            charArray[i + 1] = temp
        }
        val modifiedSig = String(charArray).dropLast(5)
        return url.replace(sig, modifiedSig)
    }

    private fun runJS2(hideMyHtmlContent: String): String {
        val rhino = Context.enter()
        rhino.initSafeStandardObjects()
        rhino.optimizationLevel = -1
        val scope: Scriptable = rhino.initSafeStandardObjects()
        scope.put("window", scope, scope)
        var result = ""
        try {
            rhino.evaluateString(scope, hideMyHtmlContent, "JavaScript", 1, null)
            val svgObject = scope.get("svg", scope)
            result = if (svgObject is NativeObject) {
                NativeJSON.stringify(Context.getCurrentContext(), scope, svgObject, null, null).toString()
            } else {
                Context.toString(svgObject)
            }
        } catch (e: Exception) {
        } finally {
            Context.exit()
        }
        return result
    }


}


open class LULUBASE : ExtractorApi() {
    override val name = "LuluStream"
    override val mainUrl = "https://luluvid.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("LULUSTREAM", "getUrl | $url | $referer")

        val response = app.get(url, referer = referer)
        Log.d("LULUSTREAM", "response | ${response.code}")

        val doc = response.document
        val scripts = doc.select("script").map { it.data() }
        Log.d("LULUSTREAM", "scripts | ${scripts.size}")

        val packed = scripts.firstOrNull {
            it.contains("eval(function(p,a,c,k,e,d)") && it.contains("m3u8")
        }
        Log.d("LULUSTREAM", "packed | ${packed != null}")

        if (packed == null) return

        val unpacked = JsUnpacker(packed).unpack()
        Log.d("LULUSTREAM", "unpacked | ${unpacked != null} | ${unpacked?.take(200)}")

        if (unpacked == null) return

        val m3u8 = Regex("""file:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""")
            .find(unpacked)?.groupValues?.get(1)
            ?: Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
                .find(unpacked)?.groupValues?.get(1)

        Log.d("LULUSTREAM", "m3u8 | $m3u8")

        if (m3u8 == null) return

        callback(
            newExtractorLink(
                name,
                name,
                m3u8
            ) {
                this.referer = mainUrl
                this.quality = Qualities.P1080.value
                this.headers = mapOf("Origin" to mainUrl)
            }
        )

        Log.d("LULUSTREAM", "done")
    }
}

class LULUSTREAM : LULUBASE() {
    override val name = "LuluStream"
    override val mainUrl = "https://lulustream.com"
}


class LULUVDO : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvdo.com"
}

class LULUVDOO : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://luluvdoo.com"
}

class LULUPVP : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulupvp.com"
}

class LULUDLC : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulu.dlc.ovh/"
}

class LULU0 : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://lulu0.ovh/"
}

class LULUX08 : LULUBASE() {
    override val name = "Lulustream"
    override val mainUrl = "https://x08.ovh/"
}


class VidNest : ExtractorApi() {
    override val name = "VidNest"
    override val mainUrl = "https://vidnest.io"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val docHeaders = mapOf(
            "Referer" to "https://vidnest.io/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )

        val text = app.get(url, headers = docHeaders).text

        val videoRegex = """file\s*:\s*["']([^"']+\.mp4[^"']*)["']""".toRegex()
        val labelRegex = """label\s*:\s*["']([^"']+)["']""".toRegex()

        val videoUrl = videoRegex.find(text)?.groupValues?.get(1)
        val label = labelRegex.find(text)?.groupValues?.get(1) ?: "VidNest"

        if (videoUrl != null) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = videoUrl,
                    type = ExtractorLinkType.VIDEO,
                    initializer = {
                        this.referer = "https://vidnest.io/"
                        this.quality = getQualityFromName(label)

                        this.headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0",
                            "Referer" to "https://vidnest.io/",
                            "Accept" to "*/*",
                            "Origin" to "https://vidnest.io",
                            "Connection" to "keep-alive",
                            "Sec-Fetch-Dest" to "video",
                            "Sec-Fetch-Mode" to "no-cors",
                            "Sec-Fetch-Site" to "same-site",
                            "Priority" to "u=4"
                        )
                    }
                )
            )
        }
    }
}

open class Filemoon : ExtractorApi() {
    override var name = "Filemoon"
    override var mainUrl = "https://filemoon.to"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val mediaId = Regex("""/(?:e|d|v|f|download)/([0-9a-zA-Z]+)""").find(url)?.groupValues?.get(1)
            ?: url.substringAfterLast("/").substringBefore("?")
        val host = url.substringAfter("://").substringBefore("/")
        val rootReferer = "https://$host/"
        val apiUrl = "https://$host/api/videos/$mediaId/embed/playback"
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Referer" to rootReferer,
            "Origin" to rootReferer.removeSuffix("/"),
            "X-Requested-With" to "XMLHttpRequest"
        )

        val response = app.get(apiUrl, headers = headers).text

        if (response.contains("video not found") || response.isBlank()) {
            return
        }

        val json = try { mapper.readValue<PlaybackResponse>(response) } catch (e: Exception) { null } ?: return

        val finalSources = json.sources ?: json.playback?.let { pb ->
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE,
                    SecretKeySpec(xn(pb.key_parts), "AES"), GCMParameterSpec(128, ft(pb.iv))
                )
                val decryptedData = cipher.doFinal(ft(pb.payload)).toString(Charsets.ISO_8859_1)
                mapper.readValue<PlaybackResponse>(decryptedData).sources
            } catch (e: Exception) {
                null
            }
        }

        finalSources?.let { processSources(it, url, callback) }
    }

    private suspend fun processSources(sources: List<VideoSource>, referer: String, callback: (ExtractorLink) -> Unit) {
        sources.forEach { source ->
            if (source.url.contains("m3u8")) {
                M3u8Helper.generateM3u8(this.name, source.url, referer).forEach(callback)
            } else {
                callback(
                    newExtractorLink(this.name, source.label ?: this.name, source.url) {
                        this.referer = referer
                        this.quality = Qualities.Unknown.value
                    }
                )
            }
        }
    }

    private fun ft(e: String): ByteArray {
        val t = e.replace("-", "+").replace("_", "/")
        val r = if (t.length % 4 == 0) 0 else 4 - (t.length % 4)
        return Base64.decode(t + "=".repeat(r), Base64.DEFAULT)
    }

    private fun xn(e: List<String>): ByteArray {
        var result = byteArrayOf()
        e.forEach { result += ft(it) }
        return result
    }


}





class FileMoon2 : Filemoon() {
    override var mainUrl = "https://filemoon.to" }
class FileMoonIn : Filemoon() {
    override var mainUrl = "https://filemoon.in" }
class FileMoonSx : Filemoon() {
    override var mainUrl = "https://filemoon.sx" }
class Bysedikamoum : Filemoon() {
    override var mainUrl = "https://bysedikamoum.com" }
class Bysezoexe : Filemoon() {
    override var mainUrl = "https://bysezoxexe.com" }
class Filemoonx08 : Filemoon() {
    override var mainUrl = "https://x08.ovh" }






open class Player4Me : ExtractorApi() {
    override var name = "Player4Me"
    override var mainUrl = "https://my.player4me.online"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("Player4Me", url)
        val id = url.substringAfter("#")

        Log.d("Player4Me", id)
        val response = app.get("$mainUrl/api/v1/video?id=$id", referer = "${mainUrl}/", headers = mapOf(
            "Host" to mainUrl.substringAfter("://"),
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Accept" to "*/*",
            "Cookie" to "popunderCount/=1",
        ))
        Log.d("Player4Me", "${response.code}")

        val sifreliYanit = response.text.trim()
        Log.d("Player4Me", sifreliYanit.take(50))

        if (sifreliYanit.startsWith("<html>")) {
            return
        }

        val aesCoz = AesHelper.decryptAES(sifreliYanit, "kiemtienmua911ca", "1234567890oiuytr")

        val map = mapper.readValue<Yanit>(aesCoz)
        val videoUrl = map.source ?: map.hls ?: map.cf
        Log.d("Player4Me", "$videoUrl")

        if (videoUrl != null) {
            Log.d("Player4Me", videoUrl)
            callback.invoke(newExtractorLink(
                this.name,
                this.name,
                fixUrl(videoUrl),
                ExtractorLinkType.M3U8
            ) {
                this.referer = "${mainUrl}/"
                this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
            })
        } else {
            Log.d("Player4Me", "Bitiş")
        }
    }
}


class Vip4me : Player4Me() {
    override var mainUrl = "https://vip.player4me.vip"
    override var name = "Player4Me"
}

class RPMShare : Player4Me() {
    override var mainUrl = "https://my.rpmplay.online"
    override var name = "Player4Me"
}

class UpnsOnline : Player4Me() {
    override var mainUrl = "https://my.upns.online"
    override var name = "Player4Me"
}

class EmbedSeek : Player4Me() {
    override var mainUrl = "https://my.embedseek.online"
    override var name = "Player4Me"
}

class VipSeekPlayer : Player4Me() {
    override var mainUrl = "https://vip.seekplayer.vip"
    override var name = "Player4Me"
}

class EasyVidPlayer : Player4Me() {
    override var mainUrl = "https://p.easyvidplayer.com"
    override var name = "Player4Me"
}

class VipEasyVidPlayer : Player4Me() {
    override var mainUrl = "https://vip.easyvidplayer.com"
    override var name = "Player4Me"
}




open class DoodStream : ExtractorApi() {
    override var name = "DoodStream"
    override var mainUrl = "https://myvidplay.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val embedUrl = url.replace("doply.net", "myvidplay.com")
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

class Playmogo : DoodStream() {
    override var mainUrl = "https://playmogo.com"
    override var name = "DoodStream"
}

class DoodDoply : DoodStream() {
    override var mainUrl = "https://doply.net"
    override var name = "DoodStream"
}

class DoodPmExtractor : DoodStream() {
    override var mainUrl = "https://dood.pm"
}

class DoodVideo : DoodStream() {
    override var mainUrl = "https://vide0.net";
}
class Ds2Play : DoodStream() {
    override var mainUrl = "https://ds2play.com"
}
class d000d : DoodStream() {
    override var mainUrl = "https://d000d.com"
}

class Dooood : DoodStream() {
    override var mainUrl = "https://dooood.com"
}




class javclan : ExtractorApi() {
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






open class StreamTAPE : ExtractorApi() {
    override val name = "Streamtape"
    override val mainUrl = "https://streamtape.com"
    override val requiresReferer = true

    private val stapeHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        "Upgrade-Insecure-Requests" to "1",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "none",
        "Sec-Fetch-User" to "?1",
        "Sec-GPC" to "1",
    )

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            Log.d("StreamtapeDebug", "İstek başladı: $url")
            val response = app.get(
                url,
                headers = stapeHeaders
            )
            val html = response.text
            Log.d("StreamtapeDebug", "HTML alındı, boyut: ${html.length}")

            val parsedUrl = java.net.URL(url)
            val host = parsedUrl.host
            val path = parsedUrl.path
            val id = path.substringAfterLast("/")

            val corsMatch = Regex("""expires=([^&"']+)&ip=([^&"']+)""").find(html)?.groupValues
            val expires = corsMatch?.get(1)
            val ip = corsMatch?.get(2)
            val realToken = Regex("""token=([a-zA-Z0-9\-_]+)""").findAll(html).lastOrNull()?.groupValues?.get(1)

            Log.d("StreamtapeDebug", "ID: $id")
            Log.d("StreamtapeDebug", "Expires: $expires")
            Log.d("StreamtapeDebug", "IP: $ip")
            Log.d("StreamtapeDebug", "Token: $realToken")

            if (expires.isNullOrEmpty() || ip.isNullOrEmpty() || realToken.isNullOrEmpty()) {
                Log.d("StreamtapeDebug", "HATA: Gerekli parametreler bulunamadı")
                return
            }

            val getVideoUrl = "https://$host/get_video?id=$id&expires=$expires&ip=$ip&token=$realToken&stream=1"
            Log.d("StreamtapeDebug", "Get Video URL: $getVideoUrl")

            val location = app.get(
                getVideoUrl,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
                    "Accept" to "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5",
                    "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Range" to "bytes=0-",
                    "Referer" to "https://$host$path",
                    "Cookie" to "_b=kube12",
                    "Sec-Fetch-Dest" to "video",
                    "Sec-Fetch-Mode" to "cors",
                    "Sec-Fetch-Site" to "same-origin",
                    "Accept-Encoding" to "identity",
                    "Sec-GPC" to "1"
                ),
                allowRedirects = false
            ).headers["location"]

            if (location.isNullOrEmpty()) {
                Log.d("StreamtapeDebug", "HATA: Location bulunamadı")
                return
            }

            Log.d("StreamtapeDebug", "Final URL: $location")

            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = location,
                ) {
                    this.referer = "https://$host/"
                    this.quality = Qualities.Unknown.value
                }
            )
        } catch (e: Exception) {
            Log.d("StreamtapeDebug", "Hata: ${e.message}")
        }
    }
}



class Watchadsontape : StreamTAPE() {
    override var mainUrl = "https://watchadsontape.com"
}
class Stape : StreamTAPE() {
    override var mainUrl = "https://stape.fun"
}

class StreamTapeNet : StreamTAPE() {
    override var mainUrl = "https://streamtape.net/"
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



open class CloudWish : ExtractorApi() {
    override val name = "CloudWish"
    override val mainUrl = "https://cloudwish.xyz"
    override val requiresReferer = true

    private val baseHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        "Upgrade-Insecure-Requests" to "1",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "none",
        "Sec-Fetch-User" to "?1",
        "Sec-GPC" to "1",
    )

    private fun unpack(packedJs: String): String? {
        try {
            val pattern = Regex(
                """\}\('((?:[^'\\]|\\.)*)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'((?:[^'\\]|\\.)*)'""",
                RegexOption.DOT_MATCHES_ALL
            )
            val match = pattern.find(packedJs)

            if (match == null) {
                Log.d("CloudWishDebug", "Regex No Match")
                return null
            }

            val p = match.groupValues[1]
                .replace("\\'", "'")
                .replace("\\\\", "\\")
            val a = match.groupValues[2].toInt()
            val c = match.groupValues[3].toInt()
            val kRaw = match.groupValues[4]
            val k = kRaw.split("|").toMutableList()

            Log.d("CloudWishDebug", "Unpack Init")

            while (k.size < c) {
                k.add("")
            }

            var result = p
            for (i in (c - 1) downTo 0) {
                if (k[i].isNotEmpty()) {
                    val token = Integer.toString(i, a)
                    result = result.replace("\\b$token\\b".toRegex(RegexOption.IGNORE_CASE), k[i])
                }
            }

            return result
        } catch (e: Exception) {
            Log.d("CloudWishDebug", "Unpack Error")
            return null
        }
    }

    open class CloudWish : ExtractorApi() {
        override val name = "CloudWish"
        override val mainUrl = "https://cloudwish.xyz"
        override val requiresReferer = true

        private val baseHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1",
            "Sec-GPC" to "1",
        )

        private fun unpack(packedJs: String): String? {
            try {
                val pattern = Regex(
                    """\}\('((?:[^'\\]|\\.)*)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'((?:[^'\\]|\\.)*)'""",
                    RegexOption.DOT_MATCHES_ALL
                )
                val match = pattern.find(packedJs)

                if (match == null) {
                    Log.d("CloudWishDebug", "Regex Eşleşmedi")
                    return null
                }

                val p = match.groupValues[1]
                    .replace("\\'", "'")
                    .replace("\\\\", "\\")
                val a = match.groupValues[2].toInt()
                val c = match.groupValues[3].toInt()
                val kRaw = match.groupValues[4]
                val k = kRaw.split("|").toMutableList()

                Log.d("CloudWishDebug", "Unpack Hazırlanıyor")

                while (k.size < c) {
                    k.add("")
                }

                var result = p
                for (i in (c - 1) downTo 0) {
                    if (k[i].isNotEmpty()) {
                        val token = Integer.toString(i, a)
                        result =
                            result.replace("\\b$token\\b".toRegex(RegexOption.IGNORE_CASE), k[i])
                    }
                }

                return result
            } catch (e: Exception) {
                Log.d("CloudWishDebug", "Unpack Hatası")
                return null
            }
        }

        override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
        ) {
            Log.d("CloudWishDebug", "İşlem Başladı")
            try {
                val response = app.get(url, headers = baseHeaders)
                val html = response.text
                Log.d("CloudWishDebug", "Sayfa Alındı")

                val document = Jsoup.parse(html)

                val packedScript = document.select("script")
                    .map { it.data() }
                    .firstOrNull { it.contains("eval(function(p,a,c,k,e,d)") }

                if (packedScript == null) {
                    Log.d("CloudWishDebug", "Paketli Script Yok")
                    return
                }

                Log.d("CloudWishDebug", "Unpack Başlatılıyor")

                val unpacked = unpack(packedScript)
                if (unpacked.isNullOrEmpty()) {
                    Log.d("CloudWishDebug", "Unpacked Hata")
                    return
                }

                Log.d("CloudWishDebug", "Unpack Başarılı")

                val parsedUrl = java.net.URL(url)
                val host = parsedUrl.host

                val m3u8Pattern =
                    Regex("""(https?://[^\s"'<>]+master\.m3u8[^\s"'<>]*|/stream/[^\s"'<>]+master\.m3u8)""")
                val m3u8Urls = m3u8Pattern.findAll(unpacked)
                    .map { it.groupValues[1] }
                    .distinct()
                    .toList()

                val masterUrl = m3u8Urls.firstOrNull { it.startsWith("/stream/") }

                if (masterUrl != null) {
                    val fullUrl = "https://$host$masterUrl"
                    Log.d("CloudWishDebug", "Master Link Bulundu")
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = name,
                            url = fullUrl,
                        ) {
                            this.referer = url
                            this.quality = Qualities.Unknown.value
                            this.headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:151.0) Gecko/20100101 Firefox/151.0",
                                "Sec-GPC" to "1",
                                "Sec-Fetch-Dest" to "empty",
                                "Sec-Fetch-Mode" to "cors",
                                "Sec-Fetch-Site" to "same-origin",
                            )
                        }
                    )
                } else if (m3u8Urls.isNotEmpty()) {
                    Log.d("CloudWishDebug", "Çok fazla Video Mevcut")
                    for (m3u8Url in m3u8Urls) {
                        val fullUrl =
                            if (m3u8Url.startsWith("/")) "https://$host$m3u8Url" else m3u8Url
                        val quality = when {
                            fullUrl.contains("/hls4/") -> Qualities.P1080.value
                            fullUrl.contains("/hls3/") -> Qualities.P720.value
                            else -> Qualities.Unknown.value
                        }
                        callback.invoke(
                            newExtractorLink(source = name, name = name, url = fullUrl) {
                                this.referer = url
                                this.quality = quality
                            }
                        )
                    }
                } else {
                    Log.d("CloudWishDebug", "Yayın Bulunamadı")
                }

            } catch (e: Exception) {
                Log.d("CloudWishDebug", "Genel Hata")
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Yanit(
    val hls: String? = null,
    val source: String? = null,
    val cf: String? = null
)

data class PlaybackResponse(val sources: List<VideoSource>? = null, val playback: PlaybackData? = null)
data class PlaybackData(val iv: String, val payload: String, val key_parts: List<String>)
data class VideoSource(val url: String, val label: String? = null)

data class SvgObject(
    val stream: String,
    val hash: String
)


