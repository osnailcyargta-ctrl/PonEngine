package com.pon.engine

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CircleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var locked = false
    var usedBlocks = 0
    var totalBlocks = 8

    private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - 16f

        // Background circle
        paintBg.color = if (locked) Color.parseColor("#1a0a0a") else Color.parseColor("#1a0a2e")
        canvas.drawCircle(cx, cy, radius, paintBg)

        // Outer ring
        paintRing.color = if (locked) Color.parseColor("#333") else Color.parseColor("#a78bfa")
        canvas.drawCircle(cx, cy, radius, paintRing)

        // Progress arc (blocks used)
        if (!locked && usedBlocks > 0) {
            paintProgress.color = Color.parseColor("#ff6b9d")
            val sweep = 360f * usedBlocks / totalBlocks
            val rect = RectF(cx - radius + 8f, cy - radius + 8f, cx + radius - 8f, cy + radius - 8f)
            canvas.drawArc(rect, -90f, sweep, false, paintProgress)
        }
    }
}
