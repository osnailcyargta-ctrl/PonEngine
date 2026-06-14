package com.pon.engine

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*

class PonRunnerDialog(private val context: Context, private val project: PonProject) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val timers = mutableListOf<Runnable>()
    private val variables = mutableMapOf<String, Any>()
    private lateinit var gameCanvas: PonCanvas
    private lateinit var logView: TextView
    private lateinit var dialog: Dialog
    private var running = false

    fun show() {
        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a0a14"))
        }

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1a0a2e"))
            setPadding(16, 8, 8, 8)
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleTv = TextView(context).apply {
            text = "▶ ${project.name}"
            setTextColor(Color.parseColor("#ff6b9d"))
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnStop = Button(context).apply {
            text = "■ Stop"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#cc2222"))
            textSize = 12f
        }
        header.addView(titleTv)
        header.addView(btnStop)
        root.addView(header)

        // Game canvas
        gameCanvas = PonCanvas(context)
        val canvasLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        gameCanvas.layoutParams = canvasLp
        root.addView(gameCanvas)

        // Log view
        logView = TextView(context).apply {
            setTextColor(Color.parseColor("#00ff88"))
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 11f
            setPadding(8, 4, 8, 4)
            setBackgroundColor(Color.parseColor("#050510"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120)
        }
        root.addView(logView)

        btnStop.setOnClickListener { stopAndClose() }

        dialog.setContentView(root)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()

        running = true
        startProject()
    }

    private fun log(msg: String) {
        mainHandler.post {
            val current = logView.text.toString()
            val lines = current.lines().takeLast(8)
            logView.text = (lines + msg).joinToString("\n")
        }
    }

    private fun startProject() {
        // Execute ON_START slots
        project.slots.filter { !it.locked && it.eventType == EventType.ON_START }.forEach { slot ->
            executeSlot(slot)
        }

        // Setup EVERY_N_SECOND timers
        project.slots.filter { !it.locked && it.eventType == EventType.EVERY_N_SECOND }.forEach { slot ->
            val seconds = slot.eventParam.toLongOrNull() ?: 1L
            val r = object : Runnable {
                override fun run() {
                    if (running) {
                        executeSlot(slot)
                        mainHandler.postDelayed(this, seconds * 1000L)
                    }
                }
            }
            timers.add(r)
            mainHandler.postDelayed(r, seconds * 1000L)
        }

        // WASD / Arrow keys handled via canvas touch for mobile
        gameCanvas.onKeyEvent = { dir ->
            project.slots.filter { !it.locked && (it.eventType == EventType.WASD || it.eventType == EventType.ARROW_KEY) }.forEach { slot ->
                executeSlot(slot, mapOf("key" to dir))
            }
        }
    }

    private fun executeSlot(slot: CircleSlot, extra: Map<String, Any> = emptyMap()) {
        for (code in slot.codes) {
            if (!running) break
            if (code.type == "none") continue
            executeCode(code, extra)
        }
    }

    private fun executeCode(code: MiniCode, extra: Map<String, Any> = emptyMap()) {
        when (code.type) {
            "print" -> log(code.params.getOrNull(0) ?: "")
            "set_var" -> {
                val name = code.params.getOrNull(0) ?: return
                val value = code.params.getOrNull(1) ?: "0"
                variables[name] = value.toDoubleOrNull() ?: value
            }
            "move" -> {
                val dir = code.params.getOrNull(0) ?: "right"
                val amt = code.params.getOrNull(1)?.toFloatOrNull() ?: 10f
                mainHandler.post { gameCanvas.movePlayer(dir, amt) }
            }
            "wait" -> {
                val ms = ((code.params.getOrNull(0)?.toFloatOrNull() ?: 1f) * 1000).toLong()
                Thread.sleep(ms)
            }
            "spawn" -> {
                val name = code.params.getOrNull(0) ?: "obj"
                val x = code.params.getOrNull(1)?.toFloatOrNull() ?: 100f
                val y = code.params.getOrNull(2)?.toFloatOrNull() ?: 100f
                mainHandler.post { gameCanvas.spawnObject(name, x, y) }
            }
            "destroy" -> {
                val name = code.params.getOrNull(0) ?: "self"
                mainHandler.post { gameCanvas.destroyObject(name) }
            }
        }
    }

    private fun stopAndClose() {
        running = false
        timers.forEach { mainHandler.removeCallbacks(it) }
        timers.clear()
        dialog.dismiss()
    }
}
