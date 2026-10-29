package com.zixora.calories

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val spGender: Spinner = findViewById(R.id.spGender)
        val spActivity: Spinner = findViewById(R.id.spActivity)
        val spGoal: Spinner = findViewById(R.id.spGoal)
        spGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("ကျား", "မ"))
        spActivity.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Calc.ACTIVITY_LABELS)
        spGoal.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Calc.GOAL_LABELS)

        val etAge: EditText = findViewById(R.id.etAge)
        val etHeight: EditText = findViewById(R.id.etHeight)
        val etWeight: EditText = findViewById(R.id.etWeight)
        val txtResult: TextView = findViewById(R.id.txtResult)

        val cur = Store.loadProfile(this)
        spGender.setSelection(if (cur.isMale) 0 else 1)
        spActivity.setSelection(cur.activity)
        spGoal.setSelection(cur.goal)
        etAge.setText(cur.age.toString())
        etHeight.setText(cur.heightCm.toString())
        etWeight.setText(if (cur.weightKg % 1.0 == 0.0) cur.weightKg.toInt().toString() else cur.weightKg.toString())

        fun showResult(p: Profile) {
            val bmr = Calc.bmr(p).toInt()
            val tdee = Calc.tdee(p).toInt()
            val target = Calc.target(p)
            val warn = if (p.goal == 0 && ((p.isMale && target < 1500) || (!p.isMale && target < 1200))) {
                "\n\n⚠️ ပန်းတိုင် အရမ်း နည်းနေတယ်။ ဒီလောက် နည်းနည်းပဲ စားရင် ကျန်းမာရေး ထိခိုက်နိုင်တယ် — ဆရာဝန်နဲ့ တိုင်ပင်ပါ။"
            } else ""
            txtResult.text = "BMR: $bmr kcal\nTDEE: $tdee kcal\nတစ်ရက်ပန်းတိုင်: $target kcal\n\n" +
                "BMR ဆိုတာ ဘာမှမလုပ်ပဲ အသက်ရှူဖို့၊ နှလုံးခုန်ဖို့ တစ်နေ့လုံး လောင်တဲ့ ကယ်လိုရီပါ။\n" +
                "TDEE ဆိုတာ BMR ကို လှုပ်ရှားမှုနဲ့ မြှောက်ထားတာ — တစ်နေ့တာ တကယ် လောင်တဲ့ ပမာဏပါ။\n" +
                "ဝိတ်လျော့မယ်ဆိုရင် TDEE ထက် ၅၀၀ လျှော့တယ်။ ဝိတ်ထိန်းမယ်ဆိုရင် TDEE အတိုင်းပဲ။ ဝိတ်တင်မယ်ဆိုရင် TDEE ထက် ၃၀၀ ပေါင်းတယ်။$warn"
        }

        findViewById<Button>(R.id.btnCalc).setOnClickListener {
            val age = etAge.text.toString().toIntOrNull()
            val h = etHeight.text.toString().toIntOrNull()
            val w = etWeight.text.toString().toDoubleOrNull()
            if (age == null || h == null || w == null || age <= 0 || h <= 0 || w <= 0) {
                Toast.makeText(this, "အသက်၊ အရပ်၊ ကိုယ်အလေးချိန်ကို သေချာ ထည့်ပါ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val p = Profile(spGender.selectedItemPosition == 0, age, h, w, spActivity.selectedItemPosition, spGoal.selectedItemPosition)
            showResult(p)
        }
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            val age = etAge.text.toString().toIntOrNull()
            val h = etHeight.text.toString().toIntOrNull()
            val w = etWeight.text.toString().toDoubleOrNull()
            if (age == null || h == null || w == null || age <= 0 || h <= 0 || w <= 0) {
                Toast.makeText(this, "အသက်၊ အရပ်၊ ကိုယ်အလေးချိန်ကို သေချာ ထည့်ပါ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val p = Profile(spGender.selectedItemPosition == 0, age, h, w, spActivity.selectedItemPosition, spGoal.selectedItemPosition)
            Store.saveProfile(this, p)
            showResult(p)
            Toast.makeText(this, "သိမ်းပြီးပြီ", Toast.LENGTH_SHORT).show()
            finish()
        }
        if (Store.hasProfile(this)) showResult(cur)
    }
}
