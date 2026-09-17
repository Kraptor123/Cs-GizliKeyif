// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.Inflater


class RouVideo : MainAPI() {
    override var mainUrl = "https://rou.video"
    override var name = "RouVIDEO"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/t/糖心Vlog" to "TangXin Vlog",
        "${mainUrl}/t/單體作品" to "Solo Works",
        "${mainUrl}/t/AI短劇" to "AI Short Drama",
        "${mainUrl}/t/中出" to "Creampie",
        "${mainUrl}/t/巨乳" to "Big Tits",
        "${mainUrl}/t/人妻" to "Mature Wife",
        "${mainUrl}/t/中文字幕" to "Chinese Subtitles",
        "${mainUrl}/t/絲襪" to "Stockings",
        "${mainUrl}/t/自拍流出" to "Leaked/Amateur",
        "${mainUrl}/t/國產AV" to "Chinese AV",
        "${mainUrl}/t/探花" to "Tanhua",
        "${mainUrl}/t/OnlyFans" to "OnlyFans",
        "${mainUrl}/t/麻豆傳媒" to "Model Media",
        "${mainUrl}/t/日本" to "Japanese"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1 || request.data.endsWith("/home")) {
            request.data
        } else {
            if (request.data.contains("?")) {
                "${request.data}&page=$page"
            } else {
                "${request.data}?page=$page"
            }
        }

        val document = app.get(url).document
        val items = document.select("div.grid a[href^='/v/']").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = true
            ),
            hasNext = if (request.data.endsWith("/home")) false else items.isNotEmpty()
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = if (page <= 1) {
            "$mainUrl/search?q=$query"
        } else {
            "$mainUrl/search?q=$query&page=$page"
        }

        val document = app.get(url).document
        val items = document.select("div.grid a[href^='/v/']").mapNotNull {
            it.toSearchResult()
        }

        return newSearchResponseList(items, hasNext = items.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.text()?.ifEmpty { return null } ?: return null
        val rawHref =
            if (this.tagName() == "a") this.attr("href") else this.selectFirst("a")?.attr("href")
                .orEmpty()
        val href = fixUrlNull(rawHref.ifEmpty { return null }) ?: return null
        val posterUrl = fixUrlNull(this.select("img").lastOrNull()?.attr("src")?.ifEmpty { null })

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.ifEmpty { null } ?: return null
        val posterUrl = fixUrlNull(
            document.selectFirst("video.rv-player-video")?.attr("poster")?.ifEmpty { null })
        val rawDescription = document.selectFirst("details p")?.text()?.ifEmpty { null }
        val plot = if (rawDescription == "這部影片還沒有簡介。") null else rawDescription

        val tags = document.select("a[href^='/t/']").mapNotNull {
            it.text().removePrefix("#").trim().ifEmpty { null }
        }

        val recommendations = mutableListOf<SearchResponse>()
        val scriptElements = document.select("script")
        for (script in scriptElements) {
            val scriptData = script.data()
            if (scriptData.contains("\"coverImageUrl\"") && scriptData.contains("\"name\"")) {
                val matches =
                    Regex("""\{"id":"([^"]+)".*?"name":"([^"]+)".*?"coverImageUrl":"([^"]+)"""").findAll(
                        scriptData
                    )
                for (match in matches) {
                    val recId = match.groupValues[1]
                    val recTitle = match.groupValues[2]
                    val recPoster = fixUrlNull(match.groupValues[3].replace("\\/", "/"))
                    val recHref = fixUrlNull("/v/$recId") ?: continue

                    recommendations.add(
                        newMovieSearchResponse(recTitle, recHref, TvType.NSFW) {
                            this.posterUrl = recPoster
                        }
                    )
                }
                if (recommendations.isNotEmpty()) break
            }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
            this.plot = plot
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val html = app.get(data).text

        val ev = Regex("""ev:(?:\$\w+\[\d+]=)?\{d:"([^"]+)",k:(\d+)\}""").find(html) ?: return false
        val encrypted = ev.groupValues[1]
        val key = ev.groupValues[2].toInt()

        val decoded = try {
            val bytes = Base64.decode(encrypted, Base64.DEFAULT)
            String(bytes.map { ((it.toInt() and 0xFF) - key).toByte() }.toByteArray(), Charsets.ISO_8859_1)
        } catch (_: Throwable) {
            return false
        }

        val videoUrl = Regex(""""videoUrl":"([^"]+)"""").find(decoded)?.groupValues?.get(1) ?: return false

        val signedUrl = app.get(
            fixUrl(videoUrl),
            headers = mapOf("Origin" to mainUrl, "Referer" to data, "Cookie" to "ob=1"),
            allowRedirects = false
        ).headers["Location"]?.let(::fixUrl) ?: return false

        val pngBytes = try {
            app.get(
                signedUrl,
                headers = mapOf("Origin" to mainUrl, "Referer" to "$mainUrl/", "Accept-Encoding" to "identity")
            ).okhttpResponse.body.bytes()
        } catch (_: Throwable) {
            return false
        }

        val playlist = RouVideoLocalProxy.decodePngPlaylist(pngBytes) ?: return false
        val localUrl = RouVideoLocalProxy.publish(playlist, signedUrl, mainUrl)
        val quality = Regex("""-(\d+)/index\.png""").find(signedUrl)?.groupValues?.get(1)?.toIntOrNull()
            ?: Qualities.Unknown.value

        callback(
            newExtractorLink(name, name, localUrl, ExtractorLinkType.M3U8) {
                referer = data
                this.quality = quality
            }
        )
        return true
    }
}

object RouVideoLocalProxy {
    private data class Session(
        val playlist: String,
        val segments: List<String>,
        val mainUrl: String,
        val createdAt: Long
    )

    private val sessions = ConcurrentHashMap<String, Session>()
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Volatile
    private var server: ServerSocket? = null
    @Volatile
    private var serverPort: Int = -1

    fun start(): Int {
        if (server?.isClosed == false) return serverPort
        synchronized(this) {
            if (server?.isClosed == false) return serverPort
            val socket = ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"))
            server = socket
            serverPort = socket.localPort
            Thread({ acceptLoop(socket) }, "RouVideoProxy").apply {
                isDaemon = true
                start()
            }
            return serverPort
        }
    }

    fun publish(rawPlaylist: String, remoteBase: String, mainUrl: String): String {
        val port = start()
        val token = UUID.randomUUID().toString().replace("-", "")
        val segmentList = ArrayList<String>()
        val rewritten = rewritePlaylist(rawPlaylist, remoteBase, segmentList)

        cleanup()
        sessions[token] = Session(
            playlist = rewritten,
            segments = segmentList,
            mainUrl = mainUrl,
            createdAt = System.currentTimeMillis()
        )
        return "http://127.0.0.1:$port/rou/$token/playlist.m3u8"
    }

    fun decodePngPlaylist(bytes: ByteArray): String? {
        val decoded = decodePngBytes(bytes) ?: return null
        val text = String(decoded, StandardCharsets.UTF_8)
        return if (text.contains("#EXTM3U")) text else null
    }

    private fun rewritePlaylist(text: String, baseUrl: String, segmentList: ArrayList<String>): String {
        val sb = java.lang.StringBuilder(text.length / 2)
        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("#")) {
                if (line.startsWith("#EXT-X-KEY")) {
                    val replaced = Regex("""URI=\"([^\"]+)\"""").replace(line) { match ->
                        val target = resolveUrl(baseUrl, match.groupValues[1])
                        val idx = segmentList.size
                        segmentList.add(target)
                        "URI=\"seg/$idx.ts\""
                    }
                    sb.append(replaced).append('\n')
                } else {
                    sb.append(line).append('\n')
                }
            } else {
                val target = resolveUrl(baseUrl, line)
                val idx = segmentList.size
                segmentList.add(target)
                sb.append("seg/$idx.ts\n")
            }
        }
        return sb.toString()
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            try {
                val clientSocket = socket.accept()
                Thread({
                    try {
                        handle(clientSocket)
                    } catch (_: Throwable) {
                    } finally {
                        try { clientSocket.close() } catch (_: Throwable) {}
                    }
                }, "RouVideoProxyClient").apply {
                    isDaemon = true
                    start()
                }
            } catch (_: Throwable) {
                if (socket.isClosed) break
            }
        }
    }

    private fun handle(socket: Socket) {
        socket.soTimeout = 15000
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))
        val requestLine = reader.readLine() ?: return
        val headers = HashMap<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            val sep = line.indexOf(':')
            if (sep > 0) {
                headers[line.substring(0, sep).trim().lowercase()] = line.substring(sep + 1).trim()
            }
        }
        val parts = requestLine.split(' ')
        if (parts.size < 2) return
        val method = parts[0]
        if (method != "GET" && method != "HEAD") return
        val path = parts[1].substringBefore('?')
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.size < 3 || segments[0] != "rou") return

        val token = segments[1]
        val session = sessions[token] ?: return

        if (segments[2] == "playlist.m3u8") {
            sendPlaylist(socket, session.playlist, method == "HEAD")
            return
        }

        if (segments[2] == "seg" && segments.size >= 4) {
            val segIdx = segments[3].substringBefore(".ts").toIntOrNull() ?: return
            val remoteUrl = session.segments.getOrNull(segIdx) ?: return
            proxyRemote(socket, remoteUrl, session.mainUrl, headers, method == "HEAD")
            return
        }
    }

    private fun sendPlaylist(socket: Socket, playlist: String, headOnly: Boolean) {
        try {
            val bytes = playlist.toByteArray(StandardCharsets.UTF_8)
            val out = socket.getOutputStream()
            writeHeaders(out, 200, "application/vnd.apple.mpegurl; charset=utf-8", bytes.size.toLong(), false, null)
            if (!headOnly) out.write(bytes)
            out.flush()
        } catch (_: Throwable) {}
    }

    private fun proxyRemote(
        socket: Socket,
        remoteUrl: String,
        mainUrl: String,
        incomingHeaders: Map<String, String>,
        headOnly: Boolean
    ) {
        val uri = try { URI(remoteUrl) } catch (_: Throwable) { return }
        val isPngUrl = uri.path?.endsWith(".png", true) == true
        val builder = Request.Builder().url(remoteUrl)
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("Referer", "$mainUrl/")
            .header("Origin", mainUrl)

        if (!isPngUrl) {
            incomingHeaders["range"]?.let { builder.header("Range", it) }
        }
        val request = builder.method(if (headOnly) "HEAD" else "GET", null).build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body
                val contentType = response.header("Content-Type").orEmpty()
                val isPng = isPngUrl || contentType.contains("image/png", true)

                if (headOnly) {
                    val length = body.contentLength()
                    val out = socket.getOutputStream()
                    writeHeaders(out, response.code, if (isPng) "video/mp2t" else contentType.ifEmpty { "application/octet-stream" }, length, length < 0, null)
                    out.flush()
                    return
                }

                val raw = body.bytes()
                val decoded = if (isPng) decodePngBytes(raw) ?: raw else raw
                val outContentType = if (isPng) "video/mp2t" else contentType.ifEmpty { "application/octet-stream" }
                val out = socket.getOutputStream()

                val rangeHeader = incomingHeaders["range"]
                if (rangeHeader != null && rangeHeader.startsWith("bytes=", true)) {
                    val totalSize = decoded.size.toLong()
                    val rangeValue = rangeHeader.substringAfter("bytes=").trim()
                    val rangeParts = rangeValue.split("-")
                    val start = rangeParts[0].toLongOrNull() ?: 0L
                    val end = if (rangeParts.size > 1 && rangeParts[1].isNotEmpty()) {
                        rangeParts[1].toLongOrNull()?.coerceAtMost(totalSize - 1) ?: (totalSize - 1)
                    } else {
                        totalSize - 1
                    }
                    if (start < totalSize && start <= end) {
                        val chunk = decoded.copyOfRange(start.toInt(), (end + 1).toInt())
                        writeHeaders(out, 206, outContentType, chunk.size.toLong(), false, "bytes $start-$end/$totalSize")
                        out.write(chunk)
                        out.flush()
                        return
                    }
                }

                writeHeaders(out, response.code, outContentType, decoded.size.toLong(), false, response.header("Content-Range"))
                out.write(decoded)
                out.flush()
            }
        } catch (_: Throwable) {
        }
    }

    private fun writeHeaders(
        output: OutputStream,
        code: Int,
        contentType: String,
        length: Long,
        chunked: Boolean,
        contentRange: String?
    ) {
        val reason = if (code == 200) "OK" else if (code == 206) "Partial Content" else "Error"
        output.write("HTTP/1.1 $code $reason\r\n".toByteArray(StandardCharsets.US_ASCII))
        output.write("Content-Type: $contentType\r\n".toByteArray(StandardCharsets.US_ASCII))
        if (chunked) {
            output.write("Transfer-Encoding: chunked\r\n".toByteArray(StandardCharsets.US_ASCII))
        } else {
            output.write("Content-Length: ${length.coerceAtLeast(0)}\r\n".toByteArray(StandardCharsets.US_ASCII))
        }
        if (contentRange != null) output.write("Content-Range: $contentRange\r\n".toByteArray(StandardCharsets.US_ASCII))
        output.write("Accept-Ranges: bytes\r\n".toByteArray(StandardCharsets.US_ASCII))
        output.write("Access-Control-Allow-Origin: *\r\n".toByteArray(StandardCharsets.US_ASCII))
        output.write("Connection: close\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
    }

    private fun resolveUrl(base: String, ref: String): String {
        if (ref.startsWith("http://", true) || ref.startsWith("https://", true)) return ref
        return try {
            val baseUri = URI(base)
            val resolved = baseUri.resolve(ref)
            if (!ref.contains('?') && resolved.rawQuery == null && baseUri.rawQuery != null) {
                URI(resolved.scheme, resolved.userInfo, resolved.host, resolved.port, resolved.path, baseUri.rawQuery, resolved.fragment).toString()
            } else {
                resolved.toString()
            }
        } catch (_: Throwable) {
            ref
        }
    }

    private fun cleanup() {
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { now - it.value.createdAt > 15 * 60 * 1000L }
        while (sessions.size > 2) {
            val oldest = sessions.entries.minByOrNull { it.value.createdAt } ?: break
            sessions.remove(oldest.key)
        }
    }

    private fun isPng(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        return bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
    }

    private fun readU32(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun inflate(bytes: ByteArray): ByteArray? {
        val inflater = Inflater()
        inflater.setInput(bytes)
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsDictionary() || inflater.needsInput()) return null
                } else {
                    output.write(buffer, 0, count)
                }
            }
            return output.toByteArray()
        } catch (_: Throwable) {
            return null
        } finally {
            inflater.end()
        }
    }

    private fun decodePngBytes(bytes: ByteArray): ByteArray? {
        if (!isPng(bytes)) return bytes
        var offset = 8
        while (offset + 12 <= bytes.size) {
            val length = readU32(bytes, offset)
            if (length < 0) return null
            val dataOffset = offset + 8
            val next = dataOffset.toLong() + length.toLong() + 4L
            if (next > bytes.size) return null
            if (bytes[offset + 4].toInt() == 'r'.code &&
                bytes[offset + 5].toInt() == 'o'.code &&
                bytes[offset + 6].toInt() == 'U'.code &&
                bytes[offset + 7].toInt() == 'd'.code
            ) {
                if (length < 1) return null
                val flag = bytes[dataOffset].toInt() and 0xFF
                val payload = bytes.copyOfRange(dataOffset + 1, dataOffset + length)
                return if ((flag and 1) != 0) inflate(payload) else payload
            }
            offset = next.toInt()
        }
        return null
    }
}
