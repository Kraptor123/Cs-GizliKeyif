// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

// Site Bunny Shield arkasinda: sadece UA+Referer ile gelen istekleri PoW challenge'ina (403, CDN-Challenge)
// dusuruyor, tarayici gibi basliklar tasiyan istegi gecirir. WebView/PoW cozumu gerekmiyor.
class BunnyShieldKiller : Interceptor {
    companion object {
        // Sayfa, poster ve video istekleri ayni masaustu UA ile gider.
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"
    }

    // Site cerezleri (kt_acctoken, PHPSESSID, ...). Video URL'sindeki v-acctoken bunlara bagli.
    private val jar = ConcurrentHashMap<String, String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(withBrowserHeaders(chain.request()))
        response.headers("Set-Cookie").forEach { line ->
            val pair = line.substringBefore(";")
            val name = pair.substringBefore("=").trim()
            val value = pair.substringAfter("=", "").trim()
            if (name.isEmpty()) return@forEach
            if (value.isEmpty()) jar.remove(name) else jar[name] = value
        }
        return response
    }

    private fun withBrowserHeaders(request: Request): Request {
        val b = request.newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-User", "?1")
        cookies()?.let { b.header("Cookie", listOfNotNull(request.header("Cookie"), it).joinToString("; ")) }
        return b.build()
    }

    private fun cookies(): String? =
        jar.entries.joinToString("; ") { "${it.key}=${it.value}" }.takeIf { it.isNotEmpty() }

    /** Oynatici (get_file) icin basliklar: medya istegi gibi gorunmeli, yoksa Shield 403 verir. */
    fun mediaHeaders(referer: String): Map<String, String> = buildMap {
        put("Referer", referer)
        put("User-Agent", USER_AGENT)
        put("Accept", "*/*")
        put("Accept-Language", "en-US,en;q=0.9")
        put("Sec-Fetch-Dest", "video")
        put("Sec-Fetch-Mode", "no-cors")
        put("Sec-Fetch-Site", "same-origin")
        cookies()?.let { put("Cookie", it) }
    }
}
