package com.zixora.calories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** ရှေ့ရက်တွေရဲ့ စားမှတ်တမ်း — ကြည့်ရုံပဲ၊ ပြင်လို့ မရဘူး (v6)။
 *  ရက်စွဲတွေကို မှတ်တမ်း ရှိတဲ့ ရက်တွေအတိုင်းပဲ လှည့်ကြည့်တယ် (ဗလာ ရက်ကို မရောက်ဘူး)။
 *  စာရင်းတွေကို RecyclerView အစား ရိုးရှင်း LinearLayout နဲ့ ပြတယ် (ScrollView ထဲမှာ အပြည့်မြင်ရအောင်)။ */
class HistoryActivity : AppCompatActivity() {
    private lateinit var txtDate: TextView
    private lateinit var txtTotal: TextView
    private lateinit var txtTarget: TextView
    private lateinit var txtEmpty: TextView
    private lateinit var entryContainer: LinearLayout
    private lateinit var dateContainer: LinearLayout
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private var dates: List<String> = emptyList()
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        txtDate = findViewById(R.id.histDate)
        txtTotal = findViewById(R.id.histTotal)
        txtTarget = findViewById(R.id.histTarget)
        txtEmpty = findViewById(R.id.histEmpty)
        entryContainer = findViewById(R.id.histEntryContainer)
        dateContainer = findViewById(R.id.histDateContainer)
        btnPrev = findViewById(R.id.btnHistPrev)
        btnNext = findViewById(R.id.btnHistNext)

        btnPrev.setOnClickListener {
            if (index < dates.size - 1) { index++; showDate() }
        }
        btnNext.setOnClickListener {
            if (index > 0) { index--; showDate() }
        }

        dates = Store.datesWithEntries(this)
        showDate()
    }

    private fun showDate() {
        entryContainer.removeAllViews()
        dateContainer.removeAllViews()
        if (dates.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            txtDate.text = "—"
            txtTotal.text = ""
            txtTarget.text = ""
            btnPrev.isEnabled = false
            btnNext.isEnabled = false
            return
        }
        txtEmpty.visibility = View.GONE
        val date = dates[index]
        txtDate.text = date
        val entries = Store.loadEntries(this, date)
        val total = entries.sumOf { it.kcal }
        txtTotal.text = "စုစုပေါင်း စားပြီး: $total kcal"
        val snapshot = Store.loadTargetSnapshot(this, date)
        txtTarget.text = if (snapshot != null) {
            "ပန်းတိုင် (အဲဒီနေ့က): $snapshot kcal"
        } else {
            // အဟောင်းဒေတာမှာ snapshot မရှိဘူး — လက်ရှိပန်းတိုင်နဲ့ ယှဉ်ပြမယ်၊ အဲဒါလို့ ရှင်းရှင်း ရေးထားတယ်။
            val current = if (Store.hasProfile(this)) Calc.target(Store.loadProfile(this)) else 2000
            "ပန်းတိုင် (လက်ရှိတွက်ချက်): $current kcal"
        }
        val inflater = LayoutInflater.from(this)
        for (e in entries) {
            val row = inflater.inflate(R.layout.item_entry, entryContainer, false)
            row.findViewById<TextView>(R.id.entryName).text = e.name
            row.findViewById<TextView>(R.id.entryDetail).text = e.detail
            row.findViewById<TextView>(R.id.entryKcal).text = "${e.kcal} kcal"
            row.findViewById<Button>(R.id.entryDelete).visibility = View.GONE
            entryContainer.addView(row)
        }
        // အဟောင်း -> အသစ်: index ကြီးလေ ရက်ဟောင်းလေ။ Prev = ပိုဟောင်းတဲ့ ရက်၊ Next = ပိုသစ်တဲ့ ရက်။
        btnPrev.isEnabled = index < dates.size - 1
        btnNext.isEnabled = index > 0
        for ((pos, d) in dates.withIndex()) {
            val row = inflater.inflate(R.layout.item_entry, dateContainer, false)
            row.findViewById<TextView>(R.id.entryName).text = d
            row.findViewById<TextView>(R.id.entryDetail).text =
                if (pos == index) "◀ ကြည့်နေတဲ့ ရက်" else ""
            row.findViewById<TextView>(R.id.entryKcal).text = ""
            row.findViewById<Button>(R.id.entryDelete).visibility = View.GONE
            row.setOnClickListener { index = pos; showDate() }
            dateContainer.addView(row)
        }
    }
}
