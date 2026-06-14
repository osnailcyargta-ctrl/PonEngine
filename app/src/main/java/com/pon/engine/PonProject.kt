package com.pon.engine

import org.json.JSONArray
import org.json.JSONObject

// Event types
enum class EventType(val label: String, val icon: String) {
    ON_START("On Start", "▶"),
    EVERY_N_SECOND("Every N Sec", "⏱"),
    WASD("WASD Keys", "⌨"),
    ARROW_KEY("Arrow Keys", "↕"),
    ON_KEY_PRESS("Key Press", "⬇"),
    ON_KEY_CLICK("Key Click", "👆")
}

// A single mini code block (1 of 8 per slot)
data class MiniCode(
    var type: String = "none",     // e.g. "move", "set_var", "print", "if", etc.
    var params: MutableList<String> = mutableListOf()
)

// A circle slot = 1 event + up to 8 mini codes
data class CircleSlot(
    var id: Int = 0,
    var eventType: EventType = EventType.ON_START,
    var eventParam: String = "",   // e.g. N for EVERY_N_SECOND
    var codes: MutableList<MiniCode> = MutableList(8) { MiniCode() },
    var locked: Boolean = false    // locked until ad watched
)

data class PonProject(
    var name: String = "My Game",
    var slots: MutableList<CircleSlot> = mutableListOf(
        CircleSlot(id = 0, locked = false),  // first slot always free
        CircleSlot(id = 1, locked = true),
        CircleSlot(id = 2, locked = true)
    ),
    var maxSlots: Int = 3,
    var variables: MutableMap<String, String> = mutableMapOf()
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("maxSlots", maxSlots)
        val slotsArr = JSONArray()
        for (slot in slots) {
            val s = JSONObject()
            s.put("id", slot.id)
            s.put("eventType", slot.eventType.name)
            s.put("eventParam", slot.eventParam)
            s.put("locked", slot.locked)
            val codesArr = JSONArray()
            for (code in slot.codes) {
                val c = JSONObject()
                c.put("type", code.type)
                val pArr = JSONArray(); code.params.forEach { pArr.put(it) }
                c.put("params", pArr)
                codesArr.put(c)
            }
            s.put("codes", codesArr)
            slotsArr.put(s)
        }
        obj.put("slots", slotsArr)
        return obj.toString(2)
    }

    companion object {
        fun fromJson(json: String): PonProject {
            val obj = JSONObject(json)
            val project = PonProject(
                name = obj.optString("name", "My Game"),
                maxSlots = obj.optInt("maxSlots", 3)
            )
            project.slots.clear()
            val slotsArr = obj.optJSONArray("slots") ?: JSONArray()
            for (i in 0 until slotsArr.length()) {
                val s = slotsArr.getJSONObject(i)
                val slot = CircleSlot(
                    id = s.optInt("id", i),
                    eventType = try { EventType.valueOf(s.optString("eventType", "ON_START")) } catch (e: Exception) { EventType.ON_START },
                    eventParam = s.optString("eventParam", ""),
                    locked = s.optBoolean("locked", i > 0)
                )
                val codesArr = s.optJSONArray("codes") ?: JSONArray()
                slot.codes.clear()
                for (j in 0 until minOf(codesArr.length(), 8)) {
                    val c = codesArr.getJSONObject(j)
                    val pArr = c.optJSONArray("params") ?: JSONArray()
                    val params = mutableListOf<String>()
                    for (k in 0 until pArr.length()) params.add(pArr.getString(k))
                    slot.codes.add(MiniCode(type = c.optString("type", "none"), params = params))
                }
                while (slot.codes.size < 8) slot.codes.add(MiniCode())
                project.slots.add(slot)
            }
            return project
        }
    }
}
