package com.zixora.calories

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EntryAdapter(
    private var items: List<Entry>,
    private val onDelete: (Entry) -> Unit
) : RecyclerView.Adapter<EntryAdapter.Holder>() {
    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.entryName)
        val detail: TextView = v.findViewById(R.id.entryDetail)
        val kcal: TextView = v.findViewById(R.id.entryKcal)
        val del: Button = v.findViewById(R.id.entryDelete)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): Holder =
        Holder(LayoutInflater.from(p.context).inflate(R.layout.item_entry, p, false))

    override fun onBindViewHolder(h: Holder, pos: Int) {
        val e = items[pos]
        h.name.text = e.name
        h.detail.text = e.detail
        h.kcal.text = "${e.kcal} kcal"
        h.del.setOnClickListener { onDelete(e) }
    }

    override fun getItemCount() = items.size
    fun update(n: List<Entry>) { items = n; notifyDataSetChanged() }
}

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: EntryAdapter
    private lateinit var txtEaten: TextView
    private lateinit var txtTarget: TextView
    private lateinit var txtRemain: TextView
    private lateinit var txtSummary: TextView
    private lateinit var progress: android.widget.ProgressBar
    // Banner animation (animators/Handler only, no libraries).
    // Started in onResume, cancelled in onPause so nothing animates off-screen.
    private var lanternAnimatorL: ObjectAnimator? = null
    private var lanternAnimatorR: ObjectAnimator? = null
    private val lightsHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lightsPhase = false
    private val lightsRunnable = object : Runnable {
        override fun run() {
            lightsPhase = !lightsPhase
            for (rowId in intArrayOf(R.id.lightsTopRow, R.id.lightsBottomRow)) {
                val row: android.widget.LinearLayout = findViewById(rowId)
                for (i in 0 until row.childCount) {
                    // Even dots and odd dots alternate: one set bright, the other dim.
                    row.getChildAt(i).alpha = if ((i % 2 == 0) == lightsPhase) 1f else 0.35f
                }
            }
            lightsHandler.postDelayed(this, 600)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtEaten = findViewById(R.id.txtEaten)
        txtTarget = findViewById(R.id.txtTarget)
        txtRemain = findViewById(R.id.txtRemain)
        txtSummary = findViewById(R.id.txtSummary)
        progress = findViewById(R.id.progress)
        val list: RecyclerView = findViewById(R.id.entryList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = EntryAdapter(emptyList()) { e ->
            Store.deleteEntry(this, e.id); refresh()
        }
        list.adapter = adapter

        findViewById<Button>(R.id.btnAddPreset).setOnClickListener { showFoodPicker() }
        findViewById<Button>(R.id.btnAddCustom).setOnClickListener { showCustomDialog(null) }
        findViewById<Button>(R.id.btnMyFoods).setOnClickListener { showCustomList() }
        findViewById<Button>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<Button>(R.id.btnGuide).setOnClickListener {
            startActivity(Intent(this, GuideActivity::class.java))
        }
        refresh()
    }

    override fun onResume() { super.onResume(); refresh(); startBannerAnimation() }

    override fun onPause() { stopBannerAnimation(); super.onPause() }

    private fun startBannerAnimation() {
        stopBannerAnimation()
        val left: android.widget.ImageView = findViewById(R.id.lanternLeft)
        val right: android.widget.ImageView = findViewById(R.id.lanternRight)
        // Lantern glow pulse: alpha 0.75 -> 1.0, gentle reverse loop.
        lanternAnimatorL = ObjectAnimator.ofFloat(left, "alpha", 0.75f, 1f).apply {
            duration = 1800; repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE; start()
        }
        lanternAnimatorR = ObjectAnimator.ofFloat(right, "alpha", 0.75f, 1f).apply {
            duration = 1800; startDelay = 900; repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE; start()
        }
        // String lights: even/odd bulbs chase (alternate bright/dim every 600ms).
        lightsHandler.post(lightsRunnable)
    }

    private fun stopBannerAnimation() {
        lightsHandler.removeCallbacks(lightsRunnable)
        lanternAnimatorL?.cancel(); lanternAnimatorL = null
        lanternAnimatorR?.cancel(); lanternAnimatorR = null
    }

    private fun refresh() {
        val date = Store.today()
        val entries = Store.loadEntries(this, date)
        val eaten = entries.sumOf { it.kcal }
        val target = if (Store.hasProfile(this)) Calc.target(Store.loadProfile(this)) else 2000
        val remain = target - eaten
        txtEaten.text = "$eaten kcal"
        txtTarget.text = "$target kcal"
        txtRemain.text = if (remain >= 0) "ကျန် $remain kcal" else "ပိုသွားတယ် ${-remain} kcal"
        val safeTarget = target.coerceAtLeast(1)
        progress.max = safeTarget
        progress.progress = eaten.coerceIn(0, safeTarget)
        txtSummary.text = if (Store.hasProfile(this)) {
            val p = Store.loadProfile(this)
            "ပန်းတိုင်: ${Calc.GOAL_LABELS[p.goal]}  •  BMR ${Calc.bmr(p).toInt()}  •  TDEE ${Calc.tdee(p).toInt()}"
        } else {
            "ကိုယ်ရေးအချက်အလက် မထည့်ရသေးဘူး — ပန်းတိုင် တွက်ဖို့ အရင် ထည့်ပါ။"
        }
        adapter.update(entries)
    }

    private fun addEntry(name: String, detail: String, kcal: Int) {
        Store.addEntry(this, Entry(System.currentTimeMillis(), Store.today(), name, detail, kcal))
        refresh()
    }

    // ---- စာရင်းထဲက ရွေးတာ: built-in + ကိုယ့်အစားအစာ ----
    private fun showFoodPicker() {
        val view = layoutInflater.inflate(R.layout.dialog_food_picker, null)
        val list: RecyclerView = view.findViewById(R.id.pickerList)
        list.layoutManager = LinearLayoutManager(this)
        val customs = Store.loadCustomFoods(this)
        // ရိုးရှင်းတဲ့ စာရင်း: built-in တွေ အရင်၊ ပြီးမှ ကိုယ့်အစားအစာတွေ
        val rows = mutableListOf<String>()
        Foods.ALL.forEach { rows.add("B|${it.name}") }
        customs.forEach { rows.add("C|${it.id}") }
        val ad = object : RecyclerView.Adapter<PickerHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, t: Int) =
                PickerHolder(LayoutInflater.from(p.context).inflate(R.layout.item_entry, p, false))
            override fun onBindViewHolder(h: PickerHolder, pos: Int) {
                val row = rows[pos]
                if (row.startsWith("B|")) {
                    val f = Foods.ALL.first { it.name == row.substring(2) }
                    h.name.text = f.name
                    h.detail.text = "${f.unit} • ${f.kcalPerUnit} kcal" +
                        (if (f.weightNote.isNotEmpty()) " • ${f.weightNote}" else "")
                    h.kcal.text = "${f.kcalPerUnit} kcal"
                    h.del.visibility = View.GONE
                    h.itemView.setOnClickListener { showCountDialog(f.name, f.unit, f.kcalPerUnit) }
                } else {
                    val c = customs.first { it.id == row.substring(2).toLong() }
                    h.name.text = "${c.name} (ကိုယ့်စာရင်း)"
                    h.detail.text = if (c.mode == 0) "၁ ပွဲစာ ${c.kcal.toInt()} kcal" else "၁၀၀ ဂရမ် ${c.kcal.toInt()} kcal"
                    h.kcal.text = ""
                    h.del.visibility = View.GONE
                    h.itemView.setOnClickListener {
                        if (c.mode == 0) showCountDialog(c.name, "ပွဲ", c.kcal.toInt())
                        else showGramsDialog(c.name, c.kcal)
                    }
                }
            }
            override fun getItemCount() = rows.size
        }
        list.adapter = ad
        val dlg = AlertDialog.Builder(this)
            .setTitle("အစားအစာ ရွေးပါ")
            .setView(view)
            .setNegativeButton("ပိတ်မယ်", null)
            .create()
        view.findViewById<Button>(R.id.btnNewFood).setOnClickListener {
            dlg.dismiss(); showCustomDialog(null)
        }
        dlg.show()
    }

    private class PickerHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.entryName)
        val detail: TextView = v.findViewById(R.id.entryDetail)
        val kcal: TextView = v.findViewById(R.id.entryKcal)
        val del: Button = v.findViewById(R.id.entryDelete)
    }

    private fun showCountDialog(name: String, unit: String, kcalPerUnit: Int) {
        val view = layoutInflater.inflate(R.layout.dialog_count, null)
        view.findViewById<TextView>(R.id.countFoodName).text = "$name ($unit)"
        val input: EditText = view.findViewById(R.id.countInput)
        AlertDialog.Builder(this)
            .setTitle("အရေအတွက် ထည့်ပါ")
            .setView(view)
            .setPositiveButton("ထည့်မယ်") { _, _ ->
                val count = input.text.toString().toDoubleOrNull() ?: 1.0
                if (count <= 0) {
                    Toast.makeText(this, "အရေအတွက်ကို ၀ ထက် ကြီးအောင် ထည့်ပါ", Toast.LENGTH_SHORT).show()
                } else {
                    addEntry(name, "$count $unit", (kcalPerUnit * count).toInt())
                }
            }
            .setNegativeButton("မထည့်တော့ဘူး", null)
            .show()
    }

    private fun showGramsDialog(name: String, kcalPer100g: Double) {
        val view = layoutInflater.inflate(R.layout.dialog_count, null)
        view.findViewById<TextView>(R.id.countFoodName).text = "$name (ဂရမ်နဲ့)"
        val input: EditText = view.findViewById(R.id.countInput)
        input.hint = "ဂရမ် ထည့်ပါ (ဥပမာ ၂၅၀)"
        AlertDialog.Builder(this)
            .setTitle("အလေးချိန် ထည့်ပါ")
            .setView(view)
            .setPositiveButton("ထည့်မယ်") { _, _ ->
                val grams = input.text.toString().toDoubleOrNull() ?: 0.0
                if (grams <= 0) {
                    Toast.makeText(this, "ဂရမ်ကို ၀ ထက် ကြီးအောင် ထည့်ပါ", Toast.LENGTH_SHORT).show()
                } else {
                    addEntry(name, "$grams ဂရမ်", Calc.kcalFromGrams(grams, kcalPer100g))
                }
            }
            .setNegativeButton("မထည့်တော့ဘူး", null)
            .show()
    }

    // ---- အစားအစာအသစ်: သိမ်းပြီး ဒီနေ့ထဲ တစ်ခါတည်း ထည့်မယ် ----
    private fun showCustomDialog(existing: CustomFood?) {
        val view = layoutInflater.inflate(R.layout.dialog_custom, null)
        val nameIn: EditText = view.findViewById(R.id.customName)
        val kcalIn: EditText = view.findViewById(R.id.customKcal)
        val amtIn: EditText = view.findViewById(R.id.customAmount)
        val modeGroup: RadioGroup = view.findViewById(R.id.customMode)
        existing?.let {
            nameIn.setText(it.name)
            kcalIn.setText(it.kcal.toInt().toString())
            modeGroup.check(if (it.mode == 0) R.id.modePreset else R.id.modeGrams)
        }
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "အစားအစာအသစ် ထည့်မယ်" else "ကိုယ့်အစားအစာ ပြင်မယ်")
            .setView(view)
            .setPositiveButton("သိမ်းပြီး ဒီနေ့ထည့်မယ်") { _, _ ->
                val name = nameIn.text.toString().trim()
                val kcal = kcalIn.text.toString().toDoubleOrNull()
                val amt = amtIn.text.toString().toDoubleOrNull() ?: 1.0
                if (name.isEmpty() || kcal == null || kcal <= 0 || amt <= 0) {
                    Toast.makeText(this, "နာမည်၊ ကယ်လိုရီ၊ ပမာဏကို သေချာ ထည့်ပါ", Toast.LENGTH_SHORT).show()
                } else {
                    val mode = if (modeGroup.checkedRadioButtonId == R.id.modeGrams) 1 else 0
                    val food = CustomFood(existing?.id ?: System.currentTimeMillis(), name, mode, kcal)
                    Store.saveCustomFood(this, food)
                    if (mode == 0) addEntry(name, "$amt ပွဲ (ကိုယ့်စာရင်း)", (kcal * amt).toInt())
                    else addEntry(name, "$amt ဂရမ် (ကိုယ့်စာရင်း)", Calc.kcalFromGrams(amt, kcal))
                    Toast.makeText(this, "သိမ်းပြီးပြီ — နောက်ရက်တွေလည်း ပြန်သုံးလို့ရတယ်", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("သိမ်းရုံပဲ") { _, _ ->
                val name = nameIn.text.toString().trim()
                val kcal = kcalIn.text.toString().toDoubleOrNull()
                if (name.isEmpty() || kcal == null || kcal <= 0) {
                    Toast.makeText(this, "နာမည်၊ ကယ်လိုရီကို သေချာ ထည့်ပါ", Toast.LENGTH_SHORT).show()
                } else {
                    val mode = if (modeGroup.checkedRadioButtonId == R.id.modeGrams) 1 else 0
                    Store.saveCustomFood(this, CustomFood(existing?.id ?: System.currentTimeMillis(), name, mode, kcal))
                    Toast.makeText(this, "ကိုယ့်စာရင်းထဲ သိမ်းပြီးပြီ", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("မလုပ်တော့ဘူး", null)
            .show()
    }

    // ---- ကိုယ့်စာရင်း: ထည့် / ပြင် / ဖျက် ----
    private fun showCustomList() {
        val view = layoutInflater.inflate(R.layout.dialog_food_picker, null)
        val list: RecyclerView = view.findViewById(R.id.pickerList)
        list.layoutManager = LinearLayoutManager(this)
        lateinit var dlg: AlertDialog
        val ad = object : RecyclerView.Adapter<PickerHolder>() {
            var items: List<CustomFood> = Store.loadCustomFoods(this@MainActivity)
            override fun onCreateViewHolder(p: ViewGroup, t: Int) =
                PickerHolder(LayoutInflater.from(p.context).inflate(R.layout.item_entry, p, false))
            override fun onBindViewHolder(h: PickerHolder, pos: Int) {
                val c = items[pos]
                h.name.text = c.name
                h.detail.text = if (c.mode == 0) "၁ ပွဲစာ ${c.kcal.toInt()} kcal" else "၁၀၀ ဂရမ် ${c.kcal.toInt()} kcal"
                h.kcal.text = ""
                h.del.text = "ဖျက်မယ်"
                h.del.setOnClickListener {
                    Store.deleteCustomFood(this@MainActivity, c.id)
                    items = Store.loadCustomFoods(this@MainActivity)
                    notifyDataSetChanged()
                }
                h.itemView.setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(c.name)
                        .setItems(arrayOf("ဒီနေ့ထည့်မယ်", "ပြင်မယ်", "ဖျက်မယ်")) { _, which ->
                            when (which) {
                                0 -> {
                                    if (c.mode == 0) showCountDialog(c.name, "ပွဲ", c.kcal.toInt())
                                    else showGramsDialog(c.name, c.kcal)
                                }
                                1 -> {
                                    dlg.dismiss()
                                    showCustomDialog(c)
                                }
                                2 -> {
                                    Store.deleteCustomFood(this@MainActivity, c.id)
                                    items = Store.loadCustomFoods(this@MainActivity)
                                    notifyDataSetChanged()
                                }
                            }
                        }
                        .setNegativeButton("မလုပ်တော့ဘူး", null)
                        .show()
                }
            }
            override fun getItemCount() = items.size
        }
        list.adapter = ad
        dlg = AlertDialog.Builder(this)
            .setTitle("ကိုယ့်အစားအစာ စာရင်း")
            .setView(view)
            .setNegativeButton("ပိတ်မယ်", null)
            .create()
        view.findViewById<Button>(R.id.btnNewFood).text = "အသစ် ထည့်မယ်"
        view.findViewById<Button>(R.id.btnNewFood).setOnClickListener {
            dlg.dismiss(); showCustomDialog(null)
        }
        dlg.show()
        if (Store.loadCustomFoods(this).isEmpty()) {
            Toast.makeText(this, "ကိုယ့်စာရင်းထဲမှာ ဘာမှ မရှိသေးဘူး — အသစ် ထည့်လိုက်ပါ", Toast.LENGTH_LONG).show()
        }
    }
}
