package com.pon.engine

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.*

class SlotEditorDialog(
    private val context: Context,
    private val project: PonProject,
    private val slotIndex: Int,
    private val onSave: () -> Unit
) {
    fun show() {
        val slot = project.slots[slotIndex]
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0f0f1a"))
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(context).apply {
            text = "Edit Slot ${slotIndex + 1} Event"
            setTextColor(Color.parseColor("#ff6b9d"))
            textSize = 16f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        layout.addView(title)

        // Event type spinner
        val eventLabel = TextView(context).apply {
            text = "Event Type:"
            setTextColor(Color.parseColor("#a78bfa"))
            textSize = 13f
            setPadding(0, 16, 0, 4)
        }
        layout.addView(eventLabel)

        val eventTypes = EventType.values()
        val spinner = Spinner(context)
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item,
            eventTypes.map { "${it.icon} ${it.label}" })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(eventTypes.indexOf(slot.eventType))
        layout.addView(spinner)

        // Event param (for EVERY_N_SECOND)
        val paramLabel = TextView(context).apply {
            text = "Param (e.g. seconds for Timer):"
            setTextColor(Color.parseColor("#a78bfa"))
            textSize = 13f
            setPadding(0, 12, 0, 4)
        }
        layout.addView(paramLabel)

        val paramInput = EditText(context).apply {
            setText(slot.eventParam)
            hint = "e.g. 1 for every 1 second"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#444"))
            setBackgroundColor(Color.parseColor("#111122"))
            setPadding(12, 8, 12, 8)
        }
        layout.addView(paramInput)

        // Save button
        val btnSave = Button(context).apply {
            text = "Save"
            setBackgroundColor(Color.parseColor("#ff6b9d"))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 20
            layoutParams = lp
        }
        btnSave.setOnClickListener {
            slot.eventType = eventTypes[spinner.selectedItemPosition]
            slot.eventParam = paramInput.text.toString().trim()
            onSave()
            dialog.dismiss()
        }
        layout.addView(btnSave)

        val scroll = ScrollView(context)
        scroll.addView(layout)
        dialog.setContentView(scroll)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
        }
        dialog.show()
    }
}
