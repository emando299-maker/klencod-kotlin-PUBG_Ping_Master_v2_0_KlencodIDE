package com.pingmaster.pubg

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val endpoints = listOf(
        PingTester.Endpoint("Cloudflare", "1.1.1.1", 443),
        PingTester.Endpoint("Google", "8.8.8.8", 443)
    )
    private data class Stats(
        val values: MutableList<Long> = mutableListOf(),
        var failures: Int = 0,
        var attempts: Int = 0
    ) {
        fun jitter(): Double? = if (values.size < 2) null
            else values.zipWithNext { a, b -> abs(b-a).toDouble() }.average()
        fun loss(): Double = if (attempts == 0) 0.0 else failures * 100.0 / attempts
        fun average(): Double? = values.takeIf { it.isNotEmpty() }?.average()
    }
    private val stats = endpoints.associate { it.name to Stats() }
    private val latestLabels = mutableMapOf<String, TextView>()
    private val detailLabels = mutableMapOf<String, TextView>()
    private lateinit var statusText: TextView
    private lateinit var overallText: TextView
    private lateinit var chart: LatencyChartView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var historyText: TextView
    private var running = false
    private var cycle = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        updateNetworkStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 20)
            setBackgroundColor(Color.rgb(10, 15, 28))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(label("PUBG PING MASTER", 25, Color.WHITE, true))
        content.addView(label("NETWORK DIAGNOSTICS • VERSION 2.0", 12, 0xFF00DCAA.toInt(), true).apply {
            setPadding(0, 5, 0, 20)
        })
        statusText = label("Checking network…", 14, Color.LTGRAY)
        content.addView(statusText)

        content.addView(sectionTitle("SERVER TESTS"))
        endpoints.forEach { endpoint ->
            val card = card()
            card.addView(label(endpoint.name, 16, Color.WHITE, true))
            val latest = label("-- ms", 30, 0xFF00E6A0.toInt(), true)
            latestLabels[endpoint.name] = latest
            card.addView(latest)
            val details = label("Average: --  |  Jitter: --  |  Loss: --", 12, Color.LTGRAY)
            detailLabels[endpoint.name] = details
            card.addView(details)
            content.addView(card, margins(0, 8))
        }

        content.addView(sectionTitle("LATENCY TREND • LAST 30 SAMPLES"))
        chart = LatencyChartView(this).apply { setBackgroundColor(Color.rgb(22,31,49)) }
        content.addView(chart, LinearLayout.LayoutParams(-1, 190).apply { topMargin = 8 })

        overallText = label("No test data yet", 13, Color.LTGRAY)
        content.addView(overallText, margins(0, 12))
        startButton = Button(this).apply {
            text = "START TEST"
            setOnClickListener { startTest() }
        }
        stopButton = Button(this).apply {
            text = "STOP"
            isEnabled = false
            setOnClickListener { stopTest() }
        }
        content.addView(startButton, margins(0, 8))
        content.addView(stopButton, margins(0, 8))

        content.addView(sectionTitle("RECENT RESULTS"))
        historyText = label("No tests yet", 12, Color.LTGRAY)
        content.addView(historyText)
        content.addView(label(
            "Important: these are TCP connection-time checks to public endpoints, not ICMP ping and not PUBG game-server ping. The actual PUBG ping must be read from the in-game indicator. Different servers are measured and reported separately; their values are never mixed to calculate jitter.",
            12, 0xFFFFC35F.toInt()
        ).apply { setPadding(0, 22, 0, 14) })

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size.toFloat(); setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun sectionTitle(value: String) = label(value, 14, Color.WHITE, true).apply {
        setPadding(0, 22, 0, 4)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(12, 16, 12, 16)
        setBackgroundColor(Color.rgb(22,31,49))
    }

    private fun margins(top: Int, bottom: Int) =
        LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, top, 0, bottom) }

    private fun updateNetworkStatus() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        statusText.text = when {
            caps == null -> "No network connection detected"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Connected via Wi-Fi"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected via mobile data"
            else -> "Connected to network"
        }
    }

    private fun startTest() {
        if (running) return
        running = true
        cycle = 0
        stats.values.forEach { it.values.clear(); it.failures = 0; it.attempts = 0 }
        startButton.isEnabled = false
        stopButton.isEnabled = true
        historyText.text = "Testing each endpoint independently…"
        testNextEndpoint(0)
    }

    private fun testNextEndpoint(index: Int) {
        if (!running) return
        if (index >= endpoints.size) {
            cycle++
            updateSummary()
            handler.postDelayed({ testNextEndpoint(0) }, 700)
            return
        }
        val endpoint = endpoints[index]
        executor.execute {
            val result = PingTester.measure(endpoint)
            handler.post {
                if (!running) return@post
                val s = stats.getValue(endpoint.name)
                s.attempts++
                if (result == null) {
                    s.failures++
                    latestLabels.getValue(endpoint.name).text = "TIMEOUT"
                    latestLabels.getValue(endpoint.name).setTextColor(0xFFFF6666.toInt())
                } else {
                    s.values.add(result)
                    latestLabels.getValue(endpoint.name).text = "$result ms"
                    latestLabels.getValue(endpoint.name).setTextColor(
                        when { result < 60 -> 0xFF00E6A0.toInt(); result < 120 -> 0xFFFFCD4B.toInt(); else -> 0xFFFF6666.toInt() }
                    )
                }
                val avg = s.average()?.let { "%.1f".format(it) } ?: "--"
                val jitter = s.jitter()?.let { "%.1f ms".format(it) } ?: "--"
                detailLabels.getValue(endpoint.name).text =
                    "Avg: $avg ms  |  Jitter: $jitter  |  Loss: %.1f%%".format(s.loss())
                if (endpoint.name == endpoints.first().name && result != null) {
                    chart.values = s.values.toList()
                }
                historyText.text = endpoints.joinToString("\n") { ep ->
                    val st = stats.getValue(ep.name)
                    "${ep.name}: ${st.values.size} replies / ${st.attempts} attempts"
                }
                testNextEndpoint(index + 1)
            }
        }
    }

    private fun updateSummary() {
        val all = stats.values
        val count = all.sumOf { it.values.size }
        val loss = all.sumOf { it.failures }
        val attempts = all.sumOf { it.attempts }
        overallText.text = "Completed rounds: $cycle  •  Replies: $count  •  Failed: $loss / $attempts"
    }

    private fun stopTest() {
        running = false
        startButton.isEnabled = true
        stopButton.isEnabled = false
        updateSummary()
        historyText.append("\nTest stopped.")
    }

    override fun onDestroy() {
        running = false
        executor.shutdownNow()
        super.onDestroy()
    }
}