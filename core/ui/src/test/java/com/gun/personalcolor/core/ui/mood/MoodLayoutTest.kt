package com.gun.personalcolor.core.ui.mood

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The mood scenes read A_layout.json / B_layout.json at runtime (feature/widget assets). Every layout the renderers
 * can ask for must exist, and every slot rect must lie inside the base (or, for mini/hero, inside its crop, since
 * those rects are relative to the crop).
 */
class MoodLayoutTest {
    private val dir = File("../../feature/widget/src/main/assets/mood")
    private fun load(name: String) = Json.parseToJsonElement(File(dir, name).readText()).jsonObject
    private fun JsonArray.f(i: Int) = this[i].jsonPrimitive.float

    private fun checkSlots(where: String, layouts: JsonObject, w: Float, h: Float, need: List<String>) {
        need.forEach { assertTrue("$where: layout $it missing", it in layouts) }
        for ((name, slots) in layouts) for ((role, slot) in slots.jsonObject) {
            val r = slot.jsonObject["rect"]!!.jsonArray
            val (x, y, rw, rh) = listOf(r.f(0), r.f(1), r.f(2), r.f(3))
            assertTrue("$where/$name/$role: empty rect $r", rw > 0 && rh > 0)
            assertTrue("$where/$name/$role: rect $r outside ${w}x$h", x >= 0 && y >= 0 && x + rw <= w && y + rh <= h)
        }
    }

    private val wardrobe = listOf("top_bottom", "dress", "outer_top_bottom", "outer_dress")

    @Test fun bedLayoutsFitBaseAndCrops() {
        val a = load("A_layout.json")
        val size = a["size"]!!.jsonArray
        checkSlots("A", a["layouts"]!!.jsonObject, size.f(0), size.f(1), wardrobe)
        for ((view, need) in listOf("mini" to listOf("top_bottom", "dress"), "hero" to wardrobe)) {
            val v = a[view]!!.jsonObject
            val crop = v["crop"]!!.jsonArray
            assertTrue("A/$view crop outside base", crop.f(0) >= 0 && crop.f(1) >= 0 &&
                crop.f(0) + crop.f(2) <= size.f(0) && crop.f(1) + crop.f(3) <= size.f(1))
            checkSlots("A/$view", v["layouts"]!!.jsonObject, crop.f(2), crop.f(3), need)
        }
    }

    @Test fun wallLayoutsFitBaseAndHangersExist() {
        val b = load("B_layout.json")
        val size = b["size"]!!.jsonArray
        val layouts = b["layouts"]!!.jsonObject
        checkSlots("B", layouts, size.f(0), size.f(1), wardrobe)
        val hangers = b["hangers"]!!.jsonObject
        for ((name, slots) in layouts) for ((role, slot) in slots.jsonObject) {
            val s = slot.jsonObject
            assertTrue("B/$name/$role: z missing", s["z"] != null)
            val hanger = s["hanger"]?.jsonPrimitive?.takeIf { it.isString }?.content ?: continue
            if (hanger !in setOf("hanger", "clip")) continue // only these are drawn
            val def = hangers[hanger]?.jsonObject
            assertTrue("B/$name/$role: hanger $hanger undefined", def != null)
            assertTrue("B/$name/$role: hook missing", s["hook"] is JsonArray)
            assertTrue("hanger file missing", File(dir, def!!["file"]!!.jsonPrimitive.content).isFile)
        }
    }

    @Test fun everySceneAssetExists() {
        val files = dir.list()!!.toSet()
        for (scene in listOf("A", "B")) for (w in listOf("sunny", "cloudy", "rain", "night")) {
            assertTrue("${scene}_${w}_base", "${scene}_${w}_base.webp" in files)
            for (p in 0..2) assertTrue("${scene}_${w}_light$p", "${scene}_${w}_light$p.webp" in files)
        }
        assertTrue("grain.webp" in files)
    }
}
