package com.pon.engine

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var slotContainer: FrameLayout
    private lateinit var tvSlotIndex: TextView
    private lateinit var tvProjectName: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnRun: Button
    private lateinit var btnNewProject: Button

    private var project = PonProject()
    private var currentSlotIndex = 0

    private val adManager by lazy { AdManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        slotContainer = findViewById(R.id.slotContainer)
        tvSlotIndex = findViewById(R.id.tvSlotIndex)
        tvProjectName = findViewById(R.id.tvProjectName)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnRun = findViewById(R.id.btnRun)
        btnNewProject = findViewById(R.id.btnNewProject)

        // Load saved project
        loadProject()

        btnPrev.setOnClickListener {
            if (currentSlotIndex > 0) { currentSlotIndex--; renderSlot() }
        }
        btnNext.setOnClickListener {
            if (currentSlotIndex < project.slots.size - 1) { currentSlotIndex++; renderSlot() }
            else showAddSlotDialog()
        }
        btnRun.setOnClickListener { runProject() }
        btnNewProject.setOnClickListener { showNewProjectDialog() }

        renderSlot()
    }

    private fun renderSlot() {
        val slot = project.slots[currentSlotIndex]
        tvSlotIndex.text = "Slot ${currentSlotIndex + 1} / ${project.slots.size}"
        tvProjectName.text = project.name

        slotContainer.removeAllViews()
        val view = LayoutInflater.from(this).inflate(R.layout.view_circle_slot, slotContainer, false)

        val circleView = view.findViewById<CircleView>(R.id.circleView)
        val tvEventIcon = view.findViewById<TextView>(R.id.tvEventIcon)
        val tvEventName = view.findViewById<TextView>(R.id.tvEventName)
        val tvCodeCount = view.findViewById<TextView>(R.id.tvCodeCount)
        val lockOverlay = view.findViewById<FrameLayout>(R.id.lockOverlay)
        val btnUnlock = view.findViewById<Button>(R.id.btnUnlock)
        val btnEditEvent = view.findViewById<Button>(R.id.btnEditEvent)
        val codeGrid = view.findViewById<GridLayout>(R.id.codeGrid)

        val usedBlocks = slot.codes.count { it.type != "none" }
        circleView.locked = slot.locked
        circleView.usedBlocks = usedBlocks
        circleView.totalBlocks = 8
        circleView.invalidate()

        tvEventIcon.text = slot.eventType.icon
        tvEventName.text = slot.eventType.label + if (slot.eventParam.isNotEmpty()) " (${slot.eventParam})" else ""
        tvCodeCount.text = "$usedBlocks/8 blocks"

        if (slot.locked) {
            lockOverlay.visibility = View.VISIBLE
            btnEditEvent.visibility = View.GONE
            btnUnlock.setOnClickListener { unlockSlot(currentSlotIndex) }
        } else {
            lockOverlay.visibility = View.GONE
            btnEditEvent.visibility = View.VISIBLE
            btnEditEvent.setOnClickListener { openEditor(currentSlotIndex) }
        }

        // Mini code blocks
        codeGrid.removeAllViews()
        for (i in 0 until 8) {
            val code = slot.codes[i]
            val chip = layoutInflater.inflate(android.R.layout.simple_list_item_1, codeGrid, false) as TextView
            chip.text = if (code.type == "none") "[ ${i+1} ]" else codeLabel(code)
            chip.setTextColor(if (code.type == "none") 0xFF444466.toInt() else 0xFFff6b9d.toInt())
            chip.setBackgroundColor(if (code.type == "none") 0xFF111122.toInt() else 0xFF2a0a2e.toInt())
            chip.textSize = 10f
            chip.setPadding(8, 6, 8, 6)
            val params = GridLayout.LayoutParams()
            params.setMargins(4, 4, 4, 4)
            chip.layoutParams = params
            if (!slot.locked) {
                chip.setOnClickListener { openCodeEditor(currentSlotIndex, i) }
            }
            codeGrid.addView(chip)
        }

        slotContainer.addView(view)
    }

    private fun codeLabel(code: MiniCode): String {
        return when (code.type) {
            "move" -> "move ${code.params.getOrNull(0) ?: ""}"
            "set_var" -> "${code.params.getOrNull(0) ?: "var"} = ${code.params.getOrNull(1) ?: "0"}"
            "print" -> "print \"${code.params.getOrNull(0) ?: ""}\""
            "if" -> "if ${code.params.getOrNull(0) ?: "?"}"
            "play_sound" -> "sound: ${code.params.getOrNull(0) ?: ""}"
            "wait" -> "wait ${code.params.getOrNull(0) ?: "1"}s"
            "spawn" -> "spawn ${code.params.getOrNull(0) ?: "obj"}"
            "destroy" -> "destroy ${code.params.getOrNull(0) ?: "self"}"
            else -> code.type
        }
    }

    private fun unlockSlot(index: Int) {
        adManager.showRewardedAd(object : AdManager.AdCallback {
            override fun onRewarded() {
                project.slots[index].locked = false
                saveProject()
                renderSlot()
                Toast.makeText(this@MainActivity, "Slot ${index + 1} unlocked!", Toast.LENGTH_SHORT).show()
            }
            override fun onFailed(reason: String) {
                Toast.makeText(this@MainActivity, "Ad failed: $reason", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddSlotDialog() {
        if (project.slots.size >= 50) {
            Toast.makeText(this, "Max 50 slots!", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Add New Slot?")
            .setMessage("Slot ${project.slots.size + 1} will be locked. Watch an ad to unlock it.")
            .setPositiveButton("Add + Unlock with Ad") { _, _ ->
                val newSlot = CircleSlot(id = project.slots.size, locked = true)
                project.slots.add(newSlot)
                currentSlotIndex = project.slots.size - 1
                saveProject()
                renderSlot()
                unlockSlot(currentSlotIndex)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openEditor(slotIndex: Int) {
        SlotEditorDialog(this, project, slotIndex) {
            saveProject()
            renderSlot()
        }.show()
    }

    private fun openCodeEditor(slotIndex: Int, codeIndex: Int) {
        CodeEditorDialog(this, project.slots[slotIndex].codes[codeIndex]) { updatedCode ->
            project.slots[slotIndex].codes[codeIndex] = updatedCode
            saveProject()
            renderSlot()
        }.show()
    }

    private fun runProject() {
        PonRunnerDialog(this, project).show()
    }

    private fun showNewProjectDialog() {
        val input = EditText(this)
        input.hint = "Project name"
        AlertDialog.Builder(this)
            .setTitle("New Project")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "My Game" }
                project = PonProject(name = name)
                currentSlotIndex = 0
                saveProject()
                renderSlot()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveProject() {
        getSharedPreferences("pon", Context.MODE_PRIVATE)
            .edit().putString("project", project.toJson()).apply()
    }

    private fun loadProject() {
        val json = getSharedPreferences("pon", Context.MODE_PRIVATE).getString("project", null)
        if (json != null) {
            try { project = PonProject.fromJson(json) } catch (e: Exception) { project = PonProject() }
        }
    }
}
