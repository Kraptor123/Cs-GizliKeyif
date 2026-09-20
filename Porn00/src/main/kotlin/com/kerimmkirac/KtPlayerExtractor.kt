// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import okhttp3.Request
import java.util.TreeMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

data class KtPlayerSlot(
    @JsonProperty("key")   val key: String,
    @JsonProperty("url")   val url: String,
    @JsonProperty("label") val label: String? = null
)

object KtPlayerExtractor {

    private const val TAG         = "KtPlayerExtractor"
    private const val OBFUSCATED  = "function/0/"
    private const val HASH_LENGTH = 32

    private val FLASHVARS = Regex("""(video_(?:alt_)?url\d*(?:_text)?)\s*:\s*(['"])(.*?)\2""")
    private val LICENSE   = Regex("""license_code\s*:\s*(['"])([^'"]*)\1""")
    private val SLOT_KEY  = Regex("""^video_(?:alt_)?url\d*$""")
    private val MEDIA_URL = Regex("""/get_file/|remote_control\.php|\.(?:mp4|m4v|mov|mkv|webm|m3u8|mpd)(?:[/?#&]|$)""", RegexOption.IGNORE_CASE)
    private val ANY_URL   = Regex("""^https?://""")
    private val NEVER     = Regex("""(?!)""")

    private val DROPPED_HEADERS = setOf("host", "connection", "content-length", "accept-encoding", "range")

    private val SCRIPT = """
        (function () {
            try {
                var out = [];
                if (typeof flashvars !== 'undefined' && flashvars) {
                    for (var k in flashvars) {
                        if (!/^video_(alt_)?url\d*${'$'}/.test(k)) continue;
                        var v = flashvars[k];
                        if (typeof v !== 'string' || v.indexOf('http') !== 0) continue;
                        out.push({ key: k, url: v, label: flashvars[k + '_text'] || null });
                    }
                }
                var players = document.getElementsByTagName('video');
                for (var i = 0; i < players.length; i++) {
                    var src = players[i].currentSrc || players[i].src || '';
                    if (src.indexOf('http') === 0) out.push({ key: 'video_element', url: src, label: null });
                }
                return out.length ? JSON.stringify(out) : null;
            } catch (e) {
                return null;
            }
        })();
    """.trimIndent()

    private data class Resolved(val slots: List<KtPlayerSlot>, val request: Request?)

    suspend fun getLinks(
        sourceName: String,
        mainUrl: String,
        pageUrl: String,
        pageHtml: String,
        cookieHeaders: Map<String, String> = emptyMap(),
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val slots  = slotsFrom(pageHtml)
        val direct = slots.filter { isPlayable(it.url) }

        Log.d(TAG, "$sourceName slots = ${slots.map { "${it.key}=${it.label}" }}, playable = ${direct.size}")

        val resolved = if (direct.isEmpty()) resolveWithWebView(mainUrl, pageUrl, slots, cookieHeaders) else null
        val headers  = playbackHeaders(pageUrl, resolved?.request, cookieHeaders)

        val playable = (resolved?.slots?.filter { isPlayable(it.url) } ?: direct)
            .distinctBy { it.url }
            .sortedByDescending { getQualityFromName(it.label) }

        if (playable.isEmpty()) {
            Log.d(TAG, "$sourceName no playable url for $pageUrl, rejected ${slots.size} slot(s)")
            return false
        }

        playable.forEach { slot ->
            callback.invoke(
                newExtractorLink(
                    source = sourceName,
                    name   = sourceName,
                    url    = slot.url,
                    type   = INFER_TYPE,
                    initializer = {
                        this.quality = getQualityFromName(slot.label)
                        this.referer = pageUrl
                        this.headers = headers
                    }
                )
            )
        }

        Log.d(TAG, "$sourceName emitted ${playable.size} link(s) for $pageUrl")
        return true
    }

    private fun slotsFrom(html: String): List<KtPlayerSlot> {
        val vars = LinkedHashMap<String, String>()
        FLASHVARS.findAll(html).forEach { match ->
            vars.getOrPut(match.groupValues[1]) { match.groupValues[3].trim() }
        }

        val license = LICENSE.find(html)?.groupValues?.get(2)

        return vars.filterKeys { SLOT_KEY.matches(it) }
            .filterValues { it.isNotEmpty() }
            .mapNotNull { (key, url) ->
                val real = deobfuscate(url, license) ?: return@mapNotNull null
                KtPlayerSlot(key, real, vars["${key}_text"]?.ifEmpty { null })
            }
    }

    private fun deobfuscate(url: String, license: String?): String? {
        if (!url.startsWith(OBFUSCATED)) return url

        val raw   = url.removePrefix(OBFUSCATED)
        val token = licenseToken(license.orEmpty())
        if (token.size < HASH_LENGTH) {
            Log.d(TAG, "cannot build license token from '$license'")
            return null
        }

        val schemeEnd = raw.indexOf("://")
        val hostEnd   = if (schemeEnd < 0) -1 else raw.indexOf('/', schemeEnd + 3)
        if (hostEnd < 0) return null

        val rest  = raw.substring(hostEnd)
        val path  = rest.substringBefore("?")
        val parts = path.split("/").toMutableList()
        if (parts.size < 4 || parts[3].length < HASH_LENGTH) return null

        val hash    = parts[3].take(HASH_LENGTH)
        val indices = IntArray(HASH_LENGTH) { it }
        var accum   = 0

        for (source in HASH_LENGTH - 1 downTo 0) {
            accum += token[source]
            val target = (source + accum) % HASH_LENGTH
            val swap   = indices[source]
            indices[source] = indices[target]
            indices[target] = swap
        }

        parts[3] = indices.map { hash[it] }.joinToString("") + parts[3].substring(HASH_LENGTH)

        return raw.substring(0, hostEnd) + parts.joinToString("/") + rest.substring(path.length)
    }

    private fun licenseToken(licenseCode: String): List<Int> {
        val license = licenseCode.replace("$", "")
        if (license.isEmpty() || license.any { !it.isDigit() }) return emptyList()

        val digits = license.map { it - '0' }
        val masked = license.replace('0', '1')
        val center = masked.length / 2
        val front  = masked.take(center + 1).toLongOrNull() ?: return emptyList()
        val back   = masked.substring(center).toLongOrNull() ?: return emptyList()
        val scaled = (4 * abs(front - back)).toString().take(center + 1)

        val token = mutableListOf<Int>()
        scaled.forEachIndexed { index, char ->
            for (offset in 0 until 4) {
                val digit = digits.getOrNull(index + offset) ?: return emptyList()
                token += (digit + (char - '0')) % 10
            }
        }
        return token
    }

    private suspend fun resolveWithWebView(
        mainUrl: String,
        pageUrl: String,
        staticSlots: List<KtPlayerSlot>,
        cookieHeaders: Map<String, String>
    ): Resolved {
        Log.d(TAG, "no playable url in html, falling back to WebViewResolver for $pageUrl")

        val stale     = staticSlots.map { it.url }.toSet()
        val labels    = staticSlots.associate { it.key to it.label }
        val siteHost  = hostOf(mainUrl)
        val fresh     = AtomicReference<List<KtPlayerSlot>>(emptyList())
        val mediaHits = CopyOnWriteArrayList<Request>()

        val resolver = WebViewResolver(
            interceptUrl   = NEVER,
            additionalUrls = listOf(ANY_URL),
            userAgent      = null,
            useOkhttp      = false,
            script         = SCRIPT,
            scriptCallback = { payload ->
                val changed = parseSlots(payload).filter { it.url !in stale }
                if (changed.isNotEmpty()) {
                    Log.d(TAG, "player exposed ${changed.size} url(s): ${changed.map { it.key }}")
                    fresh.set(changed.map { it.copy(label = it.label ?: labels[it.key]) })
                }
            },
            timeout        = 20_000L
        )

        val (_, seen) = resolver.resolveUsingWebView(
            url             = pageUrl,
            referer         = mainUrl,
            headers         = cookieHeaders,
            requestCallBack = { request ->
                val url = request.url.toString()
                if (isFreshMedia(url, siteHost, stale)) {
                    Log.d(TAG, "media request: $url")
                    mediaHits.add(request)
                }
                fresh.get().isNotEmpty() || mediaHits.isNotEmpty()
            }
        )

        val request = mediaHits.firstOrNull()
            ?: seen.firstOrNull { isFreshMedia(it.url.toString(), siteHost, stale) }

        val slots = fresh.get().ifEmpty {
            listOfNotNull(request?.let { KtPlayerSlot("video_request", it.url.toString()) })
        }

        Log.d(TAG, "webview done: ${slots.size} slot(s), ${mediaHits.size} media hit(s), ${seen.size} request(s)")

        return Resolved(slots, request)
    }

    private fun parseSlots(raw: String?): List<KtPlayerSlot> {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.removeSurrounding("\"") == "null") return emptyList()

        val json = if (trimmed.startsWith("\"")) {
            AppUtils.tryParseJson<String>(trimmed) ?: return emptyList()
        } else {
            trimmed
        }

        return AppUtils.tryParseJson<List<KtPlayerSlot>>(json).orEmpty()
    }

    private fun playbackHeaders(
        pageUrl: String,
        request: Request?,
        cookieHeaders: Map<String, String>
    ): Map<String, String> = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER).apply {
        put("Referer", pageUrl)
        putAll(cookieHeaders)
        request?.headers?.forEach { (key, value) ->
            if (key.lowercase() !in DROPPED_HEADERS && value.isNotBlank()) put(key, value)
        }
        if (request != null) WebViewResolver.webViewUserAgent?.let { put("User-Agent", it) }
    }

    private fun isPlayable(url: String): Boolean =
        url.startsWith("http") && MEDIA_URL.containsMatchIn(url)

    private fun isFreshMedia(url: String, siteHost: String, stale: Set<String>): Boolean =
        url !in stale && isPlayable(url) && isSameSite(url, siteHost)

    private fun hostOf(url: String): String =
        url.substringAfter("://").substringBefore("/").substringBefore(":").removePrefix("www.").lowercase()

    private fun isSameSite(url: String, siteHost: String): Boolean =
        hostOf(url).let { it == siteHost || it.endsWith(".$siteHost") }
}
