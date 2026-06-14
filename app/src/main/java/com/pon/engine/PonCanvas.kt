package com.pon.engine

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

data class PonObject(val name: String, var x: Float, var y: Float, val color: Int = Color.parseColor("#ff6b9d"))

class PonCanvas(context: Context) : View(context) {

    var onKeyEvent: ((String) -> Unit)? = null

    private val objects = mutableListOf<PonObject>()
    private var playerX = 200f
    private var playerY = 400f

    private val paintPlayer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ff6b9d")
        style = Paint.Style.FILL
    }
    private val paintObj = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintBg = Paint().apply { color = Color.parseColor("#0f0f1a") }
    private val paintGrid = Paint().apply {
        color = Color.parseColor("#1a1a2e")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    // D-pad touch areas
    private val dpadSize = 140f
    private val dpadPad = 20f

    override fun onDraw(canvas: Canvas) {
        // BG
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

        // Grid
        var x = 0f
        while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), paintGrid); x += 40f }
        var y = 0f
        while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, paintGrid); y += 40f }

        // Objects
        for (obj in objects) {
            paintObj.color = obj.color
            canvas.drawCircle(obj.x, obj.y, 20f, paintObj)
        }

        // Player
        canvas.drawRoundRect(playerX - 20f, playerY - 20f, playerX + 20f, playerY + 20f, 8f, 8f, paintPlayer)

        // D-pad
        drawDpad(canvas)
    }

    private fun drawDpad(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33ffffff")
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        val cx = dpadPad + dpadSize / 2
        val cy = height - dpadPad - dpadSize / 2
        val third = dpadSize / 3

        // Up
        canvas.drawRoundRect(cx - third/2, cy - dpadSize/2, cx + third/2, cy - third/2, 8f, 8f, paint)
        canvas.drawText("▲", cx, cy - dpadSize/2 + third * 0.75f, textPaint)
        // Down
        canvas.drawRoundRect(cx - third/2, cy + third/2, cx + third/2, cy + dpadSize/2, 8f, 8f, paint)
        canvas.drawText("▼", cx, cy + dpadSize/2 - 4f, textPaint)
        // Left
        canvas.drawRoundRect(cx - dpadSize/2, cy - third/2, cx - third/2, cy + third/2, 8f, 8f, paint)
        canvas.drawText("◀", cx - dpadSize/2 + third * 0.75f, cy + 10f, textPaint)
        // Right
        canvas.drawRoundRect(cx + third/2, cy - third/2, cx + dpadSize/2, cy + third/2, 8f, 8f, paint)
        canvas.drawText("▶", cx + dpadSize/2 - third * 0.25f, cy + 10f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val tx = event.x; val ty = event.y
            val cx = dpadPad + dpadSize / 2
            val cy = height - dpadPad - dpadSize / 2
            val third = dpadSize / 3

            when {
                tx in (cx - third/2)..(cx + third/2) && ty < cy - third/2 -> onKeyEvent?.invoke("up")
                tx in (cx - third/2)..(cx + third/2) && ty > cy + third/2 -> onKeyEvent?.invoke("down")
                ty in (cy - third/2)..(cy + third/2) && tx < cx - third/2 -> onKeyEvent?.invoke("left")
                ty in (cy - third/2)..(cy + third/2) && tx > cx + third/2 -> onKeyEvent?.invoke("right")
            }
        }
        return true
    }

    fun movePlayer(dir: String, amt: Float) {
        when (dir) {
            "up", "w" -> playerY -= amt
            "down", "s" -> playerY += amt
            "left", "a" -> playerX -= amt
            "right", "d" -> playerX += amt
        }
        playerX = playerX.coerceIn(20f, width - 20f)
        playerY = playerY.coerceIn(20f, height - 20f)
        invalidate()
    }

    fun spawnObject(name: String, x: Float, y: Float) {
        objects.add(PonObject(name, x, y))
        invalidate()
    }

    fun destroyObject(name: String) {
        if (name == "self") objects.clear()
        else objects.removeAll { it.name == name }
        invalidate()
    }
}
