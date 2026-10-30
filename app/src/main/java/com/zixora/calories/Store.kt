package com.zixora.calories

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {
    private const val PREFS = "calorie_prefs"

    fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun loadProfile(ctx: Context): Profile {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Profile(
            isMale = p.getBoolean("male", true),
            age = p.getInt("age", 29),
            heightCm = p.getInt("height", 170),
            weightKg = p.getFloat("weight", 65f).toDouble(),
            activity = p.getInt("activity", 1),
            goal = p.getInt("goal", 1)
        )
    }

    fun saveProfile(ctx: Context, p: Profile) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("male", p.isMale)
            .putInt("age", p.age)
            .putInt("height", p.heightCm)
            .putFloat("weight", p.weightKg.toFloat())
            .putInt("activity", p.activity)
            .putInt("goal", p.goal)
            .apply()
    }

    fun hasProfile(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("age")

    fun loadEntries(ctx: Context, date: String): MutableList<Entry> =
        loadAllEntries(ctx).filter { it.date == date }.toMutableList()

    /** ရက်စွဲအလိုက် သိမ်းထားတဲ့ မှတ်တမ်းအားလုံး (ရက်အလိုက် ပြန်ကြည့်ဖို့) */
    fun loadAllEntries(ctx: Context): MutableList<Entry> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("entries", "[]") ?: "[]"
        val out = mutableListOf<Entry>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o: JSONObject = arr.getJSONObject(i)
            out.add(
                Entry(
                    o.getLong("id"), o.getString("date"),
                    o.getString("name"), o.getString("detail"),
                    o.getInt("kcal")
                )
            )
        }
        return out
    }

    /** မှတ်တမ်း ရှိတဲ့ ရက်စွဲတွေ — အသစ်ဆုံး အရင် (yyyy-MM-dd ဆိုတော့ စာလုံးစဉ်အတိုင်း စီရင် ရက်စဉ်အတိုင်း ဖြစ်တယ်) */
    fun datesWithEntries(ctx: Context): List<String> =
        loadAllEntries(ctx).map { it.date }.distinct().sortedDescending()

    /** ခဏခဏ စားတဲ့ အစားအစာ နာမည်တွေ — အကြိမ်အများဆုံး အရင်၊ limit ခုအထိ */
    fun frequentFoodNames(ctx: Context, limit: Int): List<String> {
        val counts = LinkedHashMap<String, Int>()
        for (e in loadAllEntries(ctx)) {
            counts[e.name] = (counts[e.name] ?: 0) + 1
        }
        return counts.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    // ---- တစ်ရက်ရဲ့ ပန်းတိုင် မှတ်တမ်း (ရှေ့ရက်တွေကို ပြန်ကြည့်တဲ့အခါ အဲဒီနေ့က ပန်းတိုင်နဲ့ ယှဉ်ပြဖို့) ----
    // အဟောင်းဒေတာမှာ snapshot မရှိရင် လက်ရှိပန်းတိုင်ကို ပြမယ် — အဟောင်း key တွေကို လုံးဝ မထိဘူး။
    fun snapshotTarget(ctx: Context, date: String, target: Int) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString("target_snapshots", "{}") ?: "{}"
        val obj = JSONObject(raw)
        obj.put(date, target)
        prefs.edit().putString("target_snapshots", obj.toString()).apply()
    }

    fun loadTargetSnapshot(ctx: Context, date: String): Int? {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("target_snapshots", "{}") ?: "{}"
        val obj = JSONObject(raw)
        return if (obj.has(date)) obj.getInt(date) else null
    }

    // ---- ဝိတ်မှတ်တမ်း (ရက်စွဲအလိုက်၊ profile ရဲ့ ဝိတ်နဲ့ သီးခြား — profile ကို မထိဘူး) ----
    fun loadWeights(ctx: Context): MutableList<WeightRecord> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("weights", "[]") ?: "[]"
        val out = mutableListOf<WeightRecord>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o: JSONObject = arr.getJSONObject(i)
            out.add(WeightRecord(o.getString("date"), o.getDouble("weight")))
        }
        return out
    }

    fun saveWeight(ctx: Context, record: WeightRecord) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = loadWeights(ctx)
        val next = old.filter { it.date != record.date }.toMutableList()
        next.add(record)
        next.sortBy { it.date }
        val arr = JSONArray()
        for (r in next) {
            val o = JSONObject()
            o.put("date", r.date); o.put("weight", r.weight)
            arr.put(o)
        }
        prefs.edit().putString("weights", arr.toString()).apply()
    }

    fun addEntry(ctx: Context, e: Entry) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString("entries", "[]") ?: "[]")
        val o = JSONObject()
        o.put("id", e.id); o.put("date", e.date)
        o.put("name", e.name); o.put("detail", e.detail); o.put("kcal", e.kcal)
        arr.put(o)
        prefs.edit().putString("entries", arr.toString()).apply()
        // ဒီနေ့ရဲ့ ပန်းတိုင်ကို အဲဒီနေ့အတွက်ပါ သိမ်းထားမယ် (နောက်မှ ပြန်ကြည့်တဲ့အခါ သုံးဖို့)
        val target = if (hasProfile(ctx)) Calc.target(loadProfile(ctx)) else 2000
        snapshotTarget(ctx, e.date, target)
    }

    fun deleteEntry(ctx: Context, id: Long) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = JSONArray(prefs.getString("entries", "[]") ?: "[]")
        val next = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.getJSONObject(i)
            if (o.getLong("id") != id) next.put(o)
        }
        prefs.edit().putString("entries", next.toString()).apply()
    }

    // ---- ကိုယ့်အစားအစာ စာရင်း (အမြဲတမ်း၊ ပြန်သုံးလို့ရတယ်) ----
    fun loadCustomFoods(ctx: Context): MutableList<CustomFood> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("custom_foods", "[]") ?: "[]"
        val out = mutableListOf<CustomFood>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(CustomFood(o.getLong("id"), o.getString("name"), o.getInt("mode"), o.getDouble("kcal")))
        }
        return out
    }

    fun saveCustomFood(ctx: Context, f: CustomFood) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString("custom_foods", "[]") ?: "[]")
        // တူတဲ့ id ရှိရင် ပြင်တာ၊ မရှိရင် အသစ် ထည့်တာ
        val next = JSONArray()
        var replaced = false
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getLong("id") == f.id) {
                val n = JSONObject()
                n.put("id", f.id); n.put("name", f.name); n.put("mode", f.mode); n.put("kcal", f.kcal)
                next.put(n); replaced = true
            } else next.put(o)
        }
        if (!replaced) {
            val n = JSONObject()
            n.put("id", f.id); n.put("name", f.name); n.put("mode", f.mode); n.put("kcal", f.kcal)
            next.put(n)
        }
        prefs.edit().putString("custom_foods", next.toString()).apply()
    }

    fun deleteCustomFood(ctx: Context, id: Long) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = JSONArray(prefs.getString("custom_foods", "[]") ?: "[]")
        val next = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.getJSONObject(i)
            if (o.getLong("id") != id) next.put(o)
        }
        prefs.edit().putString("custom_foods", next.toString()).apply()
    }
}
