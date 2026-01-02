package com.kraptor

//import android.util.Log
import com.kraptor.decodeM3u8MouflonFilesFixed
import com.lagradost.api.Log
import kotlinx.coroutines.*
import okhttp3.*
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Live Stream destekli Socket-based HTTP Proxy Server
 */
class SimpleProxyServer(
    private val port: Int,
    private val cacheDir: File,
    private val fetchFunction: suspend (String, Map<String, String>?) -> String?
) {

    private val TAG = "SimpleProxyServer"
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val liveStreams = ConcurrentHashMap<String, LiveStreamInfo>()
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    data class LiveStreamInfo(
        val variantUrl: String,
        val baseUrl: String,
        val psch: String?,
        val pkey: String?,
        val referer: String
    )

    companion object {
        fun getEphemeralPort(): Int {
            ServerSocket(0).use { return it.localPort }
        }
    }

    fun start(): Int {
        if (isRunning) return port

        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            Log.d(TAG, "Server started on port: $port")

            GlobalScope.launch(Dispatchers.IO) {
                acceptConnections()
            }

            return port
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server: ${e.message}")
            throw e
        }
    }

    private suspend fun acceptConnections() {
        while (isRunning) {
            try {
                val socket = serverSocket?.accept() ?: break
                GlobalScope.launch(Dispatchers.IO) {
                    handleClient(socket)
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Error accepting connection: ${e.message}")
                }
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            val input = socket.getInputStream().bufferedReader()
            val output = socket.getOutputStream()

            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val uri = parts[1]
            Log.d(TAG, "Request: $uri")

            // Read headers
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = input.readLine()
                if (line.isNullOrEmpty()) break
                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    headers[line.substring(0, colonIndex).trim().lowercase()] =
                        line.substring(colonIndex + 1).trim()
                }
            }

            when {
                uri.startsWith("/live/") -> {
                    val id = uri.removePrefix("/live/").removeSuffix(".m3u8")
                    handleLiveStream(output, id, headers)
                }

                uri.startsWith("/proxy?") -> {
                    val query = uri.substringAfter("?")
                    val params = parseQuery(query)
                    val url = params["url"]

                    if (url != null) {
                        proxyRequest(output, url, headers)
                    } else {
                        sendResponse(output, 400, "text/plain", "Missing url parameter")
                    }
                }

                else -> {
                    sendResponse(output, 200, "text/plain", "SimpleProxyServer running on port $port")
                }
            }

            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client: ${e.message}")
        }
    }

    private suspend fun handleLiveStream(output: java.io.OutputStream, id: String, headers: Map<String, String>) {
        val streamInfo = liveStreams[id]
        if (streamInfo == null) {
            sendResponse(output, 404, "text/plain", "Stream not found")
            return
        }

        try {
            Log.d(TAG, "Fetching live playlist from: ${streamInfo.variantUrl}")

            // Fetch fresh playlist
            val variantText = runBlocking {
                fetchFunction(streamInfo.variantUrl, mapOf("Referer" to streamInfo.referer))
            }

            if (variantText == null) {
                sendResponse(output, 502, "text/plain", "Failed to fetch upstream playlist")
                return
            }

            Log.d(TAG, "Fetched playlist, length: ${variantText.length}")

            // Decode on-the-fly
            val decoded = decodeM3u8MouflonFilesFixed(variantText, { url, hdrs ->
                runBlocking {
                    fetchFunction(url, hdrs ?: mapOf("Referer" to streamInfo.referer))
                }
            }, cacheDir)

            Log.d(TAG, "Decoded playlist, length: ${decoded.length}")

            // Rewrite URLs to proxy
            val rewritten = rewritePlaylist(decoded, streamInfo.baseUrl, streamInfo.psch, streamInfo.pkey)

            Log.d(TAG, "Returning live playlist with ${rewritten.lines().count { !it.startsWith("#") }} segments")

            sendResponse(output, 200, "application/vnd.apple.mpegurl", rewritten)

        } catch (e: Exception) {
            Log.e(TAG, "Error serving live stream: ${e.message}")
            sendResponse(output, 500, "text/plain", "Error: ${e.message}")
        }
    }

    private fun sendResponse(output: java.io.OutputStream, code: Int, contentType: String, body: String) {
        val response = buildString {
            append("HTTP/1.1 $code OK\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.toByteArray().size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Cache-Control: no-cache\r\n")
            append("\r\n")
            append(body)
        }
        output.write(response.toByteArray())
        output.flush()
    }

    private fun proxyRequest(output: java.io.OutputStream, url: String, requestHeaders: Map<String, String>) {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", requestHeaders["user-agent"] ?: "Mozilla/5.0")

            // Forward important headers
            requestHeaders["range"]?.let { requestBuilder.header("Range", it) }
            requestHeaders["referer"]?.let { requestBuilder.header("Referer", it) }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    sendResponse(output, 502, "text/plain", "Upstream error: ${response.code}")
                    return
                }

                val body = response.body?.bytes() ?: ByteArray(0)
                val contentType = response.header("Content-Type") ?: "application/octet-stream"

                val header = buildString {
                    append("HTTP/1.1 ${response.code} OK\r\n")
                    append("Content-Type: $contentType\r\n")
                    append("Content-Length: ${body.size}\r\n")
                    append("Access-Control-Allow-Origin: *\r\n")

                    // Forward important headers
                    response.header("Content-Range")?.let { append("Content-Range: $it\r\n") }
                    response.header("Accept-Ranges")?.let { append("Accept-Ranges: $it\r\n") }

                    append("\r\n")
                }

                output.write(header.toByteArray())
                output.write(body)
                output.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Proxy error: ${e.message}")
            sendResponse(output, 502, "text/plain", "Proxy error: ${e.message}")
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        return query.split("&").mapNotNull { param ->
            val kv = param.split("=", limit = 2)
            if (kv.size == 2) {
                URLDecoder.decode(kv[0], "UTF-8") to URLDecoder.decode(kv[1], "UTF-8")
            } else null
        }.toMap()
    }

    fun registerLiveStream(
        variantUrl: String,
        baseUrl: String,
        psch: String?,
        pkey: String?,
        referer: String
    ): String {
        val id = UUID.randomUUID().toString()
        liveStreams[id] = LiveStreamInfo(variantUrl, baseUrl, psch, pkey, referer)
        Log.d(TAG, "Registered live stream: $id")
        return "http://127.0.0.1:$port/live/${id}.m3u8"
    }

    private fun rewritePlaylist(content: String, baseUrl: String, psch: String?, pkey: String?): String {
        val lines = content.split("\n")
        return lines.joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                line
            } else {
                // Make absolute URL
                val absolute = try {
                    java.net.URI(baseUrl).resolve(trimmed).toString()
                } catch (e: Exception) {
                    trimmed
                }

                // Add psch/pkey if needed
                val withParams = addParams(absolute, psch, pkey)

                // Proxy it
                "http://127.0.0.1:$port/proxy?url=${URLEncoder.encode(withParams, "UTF-8")}"
            }
        }
    }

    private fun addParams(url: String, psch: String?, pkey: String?): String {
        if (psch.isNullOrEmpty() && pkey.isNullOrEmpty()) return url

        val separator = if (url.contains("?")) "&" else "?"
        val params = mutableListOf<String>()
        if (!psch.isNullOrEmpty()) params.add("psch=$psch")
        if (!pkey.isNullOrEmpty()) params.add("pkey=$pkey")

        return url + separator + params.joinToString("&")
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
            liveStreams.clear()
            Log.d(TAG, "Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server: ${e.message}")
        }
    }
}