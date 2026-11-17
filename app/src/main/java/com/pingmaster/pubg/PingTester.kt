package com.pingmaster.pubg

import java.net.InetSocketAddress
import java.net.Socket

object PingTester {
    data class Endpoint(val name: String, val host: String, val port: Int)

    // TCP connect latency only. It is not ICMP ping and not PUBG game-server latency.
    fun measure(endpoint: Endpoint, timeoutMs: Int = 1800): Long? {
        val socket = Socket()
        return try {
            val started = System.nanoTime()
            socket.connect(InetSocketAddress(endpoint.host, endpoint.port), timeoutMs)
            ((System.nanoTime() - started) / 1_000_000).coerceAtLeast(1)
        } catch (_: Exception) {
            null
        } finally {
            try { socket.close() } catch (_: Exception) { }
        }
    }
}
