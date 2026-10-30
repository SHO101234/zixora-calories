package com.zixora.calories

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** ဝိတ်မှတ်တမ်း — ရိုးရှင်း Canvas ဂရပ်နဲ့ (library အသစ် လုံးဝ မသုံးဘူး)။
 *  မှတ်တမ်းက profile ရဲ့ ဝိတ်နဲ့ သီးခြား — profile ကို မထိဘူး။ ဒီနေ့မှာ ရှိပြီးသား
 *  မှတ်တမ်းကို ပြန်သိမ်းရင် အသစ်က အဟောင်းကို အစားထိုးမယ် (မှားသိမ်းတာ ပြင်လို့ရအောင်)။ */
class WeightActivity : AppCompatActivity() {
    private lateinit var txtSummary: TextView
    private lateinit var graph: WeightGraphView
    private lateinit var listContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight)

        txtSummary = findViewById(R.id.weightSummary)
        graph = findViewById(R.id.weightGraph)
        listContainer = findViewById(R.id.weightListContainer)
        val input: EditText = findViewById(R.id.etWeightRecord)

        findViewById<Button>(R.id.btnSaveWeight).setOnClickListener {
            val w = input.text.toString().toDoubleOrNull()
            if (w == null || w <= 0 || w > 500) {
                Toast.makeText(this, "ကိုယ်အလေးချိန်ကို သေချာ ထည့်ပါ (ကီလိုဂရမ်)", Toast.LENGTH_SHORT).show()
            } else {
                Store.saveWeight(this, WeightRecord(Store.today(), w))
                input.setText("")
                Toast.makeText(this, "သိမ်းပြီးပြီ", Toast.LENGTH_SHORT).show()
                refresh()
            }
        }
        refresh()
    }

    private fun refresh() {
        val weights = Store.loadWeights(this) // ရက်စဉ်အတိုင်း စီပြီးသား (အဟောင်း -> အသစ်)
        graph.setData(weights)
        val inflater = LayoutInflater.from(this)
        listContainer.removeAllViews()
        if (weights.isEmpty()) {
            txtSummary.text = "ဝိတ်မှတ်တမ်း မရှိသေးဘူး — အပေါ်မှာ ဒီနေ့ဝိတ်ကို ထည့်ပြီး သိမ်းလိုက်ပါ။"
            return
        }
        val first = weights.first()
        val last = weights.last()
        val change = last.weight - first.weight
        val changeText = when {
            change > 0 -> "+${fmt(change)} kg တက်လာတယ်"
            change < 0 -> "${fmt(change)} kg ကျသွားတယ်"
            else -> "မပြောင်းသေးဘူး"
        }
        val min = weights.minOf { it.weight }
        val max = weights.maxOf { it.weight }
        txtSummary.text = "ပထမဆုံး: ${fmt(first.weight)} kg (${first.date})\n" +
            "နောက်ဆုံး: ${fmt(last.weight)} kg (${last.date}) • $changeText\n" +
            "အနည်းဆုံး: ${fmt(min)} kg • အများဆုံး: ${fmt(max)} kg"
        // စာရင်း: အသစ်ဆုံး အရင်
        for (r in weights.asReversed()) {
            val row = inflater.inflate(R.layout.item_entry, listContainer, false)
            row.findViewById<TextView>(R.id.entryName).text = r.date
            row.findViewById<TextView>(R.id.entryDetail).text = ""
            row.findViewById<TextView>(R.id.entryKcal).text = "${fmt(r.weight)} kg"
            row.findViewById<Button>(R.id.entryDelete).visibility = View.GONE
            listContainer.addView(row)
        }
    }

    private fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
}

/** ရိုးရှင်း မျဉ်းဂရပ် — Android Canvas သက်သက်။ */
class WeightGraphView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : View(ctx, attrs) {
    private var points: List<WeightRecord> = emptyList()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00897B"); strokeWidth = 5f; style = Paint.Style.STROKE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0"); style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BBDEFB"); strokeWidth = 2f; style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#54707F"); textSize = 32f
    }

    fun setData(data: List<WeightRecord>) {
        points = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) {
            canvas.drawText(
                "ဂရပ်ဆွဲဖို့ မှတ်တမ်း ၂ ခု လိုတယ်",
                24f, height / 2f, textPaint
            )
            return
        }
        val padL = 130f; val padR = 24f; val padT = 32f; val padB = 48f
        val w = width - padL - padR
        val h = height - padT - padB
        if (w <= 0 || h <= 0) return
        var min = points.minOf { it.weight }
        var max = points.maxOf { it.weight }
        if (max - min < 1.0) { // အကွာအဝေး အရမ်းနည်းရင် ဂရပ်ပြားမသွားအောင်
            val mid = (max + min) / 2.0
            min = mid - 0.5; max = mid + 0.5
        }
        // အနည်းဆုံး / အများဆုံး မျဉ်းနဲ့ စာသား
        canvas.drawLine(padL, padT, padL + w, padT, gridPaint)
        canvas.drawLine(padL, padT + h, padL + w, padT + h, gridPaint)
        canvas.drawText(fmtShort(max), 16f, padT + 12f, textPaint)
        canvas.drawText(fmtShort(min), 16f, padT + h + 12f, textPaint)
        val n = points.size
        var prevX = 0f; var prevY = 0f
        for (i in points.indices) {
            val x = if (n == 1) padL + w / 2f else padL + w * i / (n - 1)
            val frac = ((points[i].weight - min) / (max - min)).toFloat()
            val y = padT + h * (1f - frac)
            canvas.drawCircle(x, y, 10f, dotPaint)
            if (i > 0) canvas.drawLine(prevX, prevY, x, y, linePaint)
            prevX = x; prevY = y
        }
        // ပထမဆုံး / နောက်ဆုံး ရက်စွဲ
        canvas.drawText(points.first().date, padL, height - 12f, textPaint)
        val lastLabel = points.last().date
        canvas.drawText(lastLabel, padL + w - textPaint.measureText(lastLabel), height - 12f, textPaint)
    }

    private fun fmtShort(v: Double): String =
        if (v % 1.0 == 0.0) "${v.toInt()} kg" else String.format("%.1f kg", v)
}
