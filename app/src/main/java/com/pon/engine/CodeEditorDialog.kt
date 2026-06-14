package com.pon.engine

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.*

class CodeEditorDialog(
    private val context: Context,
    private val code: MiniCode,
    private val onSave: (MiniCode) -> Unit
) {
    private val codeTypes = listOf(
        "none", "move", "set_var", "print", "if", "play_sound", "wait", "spawn", "destroy"
    )

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0f0f1a"))
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(context).apply {
            text = "Edit Code Block"
            setTextColor(Color.parseColor("#ff6b9d"))
            textSize = 16f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        layout.addView(title)

        val typeLabel = TextView(context).apply {
            text = "Code Type:"
            setTextColor(Color.parseColor("#a78bfa"))
            textSize = 13f
            setPadding(0, 16, 0, 4)
        }
        layout.addView(typeLabel)

        val spinner = Spinner(context)
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, codeTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(codeTypes.indexOf(code.type).takeIf { it >= 0 } ?: 0)
        layout.addView(spinner)

        // Param fields (dynamic)
        val paramsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 0)
        }
        layout.addView(paramsLayout)

        fun updateParams(type: String) {
            paramsLayout.removeAllViews()
            val paramHints = when (type) {
                "move" -> listOf("direction (up/down/left/right/x/y)", "amount (px)")
                "set_var" -> listOf("variable name", "value")
                "print" -> listOf("text to print")
                "if" -> listOf("condition (e.g. x > 100)")
                "play_sound" -> listOf("sound name")
                "wait" -> listOf("seconds")
                "spawn" -> listOf("object name", "x", "y")
                "destroy" -> listOf("object name (or 'self')")
                else -> emptyList()
            }
            paramHints.forEachIndexed { i, hint ->
                val et = EditText(context).apply {
                    setText(code.params.getOrNull(i) ?: "")
                    setHint(hint)
                    setTextColor(Color.WHITE)
                    setHintTextColor(Color.parseColor("#444"))
                    setBackgroundColor(Color.parseColor("#111122"))
                    setPadding(12, 8, 12, 8)
                    tag = i
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.topMargin = 6
                    layoutParams = lp
                }
                paramsLayout.addView(et)
            }
        }

        updateParams(code.type)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { updateParams(codeTypes[pos]) }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val btnSave = Button(context).apply {
            text = "Save Block"
            setBackgroundColor(Color.parseColor("#ff6b9d"))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 20
            layoutParams = lp
        }
        btnSave.setOnClickListener {
            val newCode = MiniCode(
                type = codeTypes[spinner.selectedItemPosition],
                params = (0 until paramsLayout.childCount)
                    .map { (paramsLayout.getChildAt(it) as? EditText)?.text?.toString() ?: "" }
                    .toMutableList()
            )
            onSave(newCode)
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
