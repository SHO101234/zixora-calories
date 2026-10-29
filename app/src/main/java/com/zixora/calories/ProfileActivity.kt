package com.zixora.calories

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    // Raised-button selections (replaces the old Spinners; values map 1:1
    // onto the same Profile fields, so Calc/Store logic is untouched).
    private var genderMale = true
    private var activityIdx = 1
    private var goalIdx = 1

    private lateinit var btnGenderMale: Button
    private lateinit var btnGenderFemale: Button
    private lateinit var actButtons: List<Button>
    private lateinit var goalButtons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        btnGenderMale = findViewById(R.id.btnGenderMale)
        btnGenderFemale = findViewById(R.id.btnGenderFemale)
        actButtons = listOf(
            findViewById(R.id.btnAct0), findViewById(R.id.btnAct1),
            findViewById(R.id.btnAct2), findViewById(R.id.btnAct3)
        )
        goalButtons = listOf(
            findViewById(R.id.btnGoal0), findViewById(R.id.btnGoal1),
            findViewById(R.id.btnGoal2)
        )

        val etAge: EditText = findViewById(R.id.etAge)
        val etHeight: EditText = findViewById(R.id.etHeight)
        val etWeight: EditText = findViewById(R.id.etWeight)
        val txtResult: TextView = findViewById(R.id.txtResult)

        val cur = Store.loadProfile(this)
        genderMale = cur.isMale
        activityIdx = cur.activity.coerceIn(0, 3)
        goalIdx = cur.goal.coerceIn(0, 2)
        etAge.setText(cur.age.toString())
        etHeight.setText(cur.heightCm.toString())
        etWeight.setText(if (cur.weightKg % 1.0 == 0.0) cur.weightKg.toInt().toString() else cur.weightKg.toString())
        refreshChoiceStyles()

        btnGenderMale.setOnClickListener { genderMale = true; refreshChoiceStyles() }
        btnGenderFemale.setOnClickListener { genderMale = false; refreshChoiceStyles() }
        actButtons.forEachIndexed { i, b -> b.setOnClickListener { activityIdx = i; refreshChoiceStyles() } }
        goalButtons.forEachIndexed { i, b -> b.setOnClickListener { goalIdx = i; refreshChoiceStyles() } }

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

        fun readProfile(): Profile? {
            val age = etAge.text.toString().toIntOrNull()
            val h = etHeight.text.toString().toIntOrNull()
            val w = etWeight.text.toString().toDoubleOrNull()
            if (age == null || h == null || w == null || age <= 0 || h <= 0 || w <= 0) {
                Toast.makeText(this, "အသက်၊ အရပ်၊ ကိုယ်အလေးချိန်ကို သေချာ ထည့်ပါ", Toast.LENGTH_SHORT).show()
                return null
            }
            return Profile(genderMale, age, h, w, activityIdx, goalIdx)
        }

        findViewById<Button>(R.id.btnCalc).setOnClickListener {
            readProfile()?.let { showResult(it) }
        }
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            val p = readProfile() ?: return@setOnClickListener
            Store.saveProfile(this, p)
            showResult(p)
            Toast.makeText(this, "သိမ်းပြီးပြီ", Toast.LENGTH_SHORT).show()
            finish()
        }
        if (Store.hasProfile(this)) showResult(cur)
    }

    /** Selected button = orange fill + white text + higher elevation;
     *  unselected = pale cream fill + dark text. */
    private fun styleChoice(b: Button, selected: Boolean) {
        if (selected) {
            b.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E86A17"))
            b.setTextColor(Color.WHITE)
            b.elevation = 8f
        } else {
            b.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFF6E5"))
            b.setTextColor(Color.parseColor("#2B1A0E"))
            b.elevation = 4f
        }
    }

    private fun refreshChoiceStyles() {
        styleChoice(btnGenderMale, genderMale)
        styleChoice(btnGenderFemale, !genderMale)
        actButtons.forEachIndexed { i, b -> styleChoice(b, i == activityIdx) }
        goalButtons.forEachIndexed { i, b -> styleChoice(b, i == goalIdx) }
    }
}
