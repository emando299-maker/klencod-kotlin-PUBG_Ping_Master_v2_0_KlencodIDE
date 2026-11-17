package com.pingmaster.pubg

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.max

class LatencyChartView(context: Context) : View(context) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E6A0.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE
    }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF344057.toInt(); strokeWidth = 1.5f
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB8C2D8.toInt(); textSize = 28f
    }
    var values: List<Long> = emptyList()
        set(value) { field = value.takeLast(30); invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = 18f; val right = width - 18f
        val top = 22f; val bottom = height - 34f
        for (i in 0..3) {
            val y = top + (bottom - top) * i / 3f
            canvas.drawLine(left, y, right, y, grid)
        }
        if (values.isEmpty()) {
            canvas.drawText("Waiting for samples…", left, height / 2f, text)
            return
        }
        val maxValue = max(100L, values.maxOrNull() ?: 100L).toFloat()
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = if (values.size == 1) left else left + (right-left) * index / (values.size-1)
            val y = bottom - (bottom-top) * (value.toFloat()/maxValue).coerceIn(0f,1f)
            if (index == 0) path.moveTo(x,y) else path.lineTo(x,y)
        }
        canvas.drawPath(path, line)
        canvas.drawText("max ${maxValue.toLong()} ms", left, height - 6f, text)
    }
}
