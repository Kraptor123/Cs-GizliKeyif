package com.kraptor

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

// Oynatici data: playlist'i kabul etmedigi icin playlist ve AES anahtari 127.0.0.1 uzerinden verilir.
object LocalResourceServer {
    private class Resource(val bytes: ByteArray, val contentType: String)

    private val resources = ConcurrentHashMap<String, Resource>()
    private val counter = AtomicInteger()
    private var server: ServerSocket? = null

    @Synchronized
    private fun start(): ServerSocket {
        server?.takeIf { !it.isClosed }?.let { return it }
        val s = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        server = s
        Thread {
            while (!s.isClosed) {
                val client = try { s.accept() } catch (e: Exception) { break }
                Thread { handle(client) }.apply { isDaemon = true }.start()
            }
        }.apply { isDaemon = true }.start()
        return s
    }

    /** Kaynagi kaydeder, oynaticinin acabilecegi http://127.0.0.1:port/... adresini dondurur. */
    fun register(bytes: ByteArray, contentType: String, ext: String): String {
        val port = start().localPort
        if (resources.size > 64) resources.clear() // eski kayitlari at, sinirsiz buyumesin
        val name = "${counter.incrementAndGet()}.$ext"
        resources[name] = Resource(bytes, contentType)
        return "http://127.0.0.1:$port/$name"
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            try {
                val input = s.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                val requestLine = input.readLine() ?: return
                while (true) { if (input.readLine().isNullOrEmpty()) break } // basliklari atla
                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0) ?: return
                val name = parts.getOrNull(1)?.substringAfter("/")?.substringBefore("?").orEmpty()
                val res = resources[name]
                val out = s.getOutputStream()
                if (res == null) {
                    out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                } else {
                    out.write(
                        ("HTTP/1.1 200 OK\r\nContent-Type: ${res.contentType}\r\nContent-Length: ${res.bytes.size}\r\n" +
                            "Access-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n").toByteArray()
                    )
                    if (method != "HEAD") out.write(res.bytes)
                }
                out.flush()
            } catch (_: Exception) {
            }
        }
    }
}
