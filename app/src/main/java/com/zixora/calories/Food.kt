package com.zixora.calories

import kotlin.math.roundToInt

/** တစ်ပွဲ / တစ်ခွက် / တစ်ခု ပုံမှန် အရွယ်အစား အတွက် ခန့်မှန်းခြေ ကယ်လိုရီ။
 *  အလေးချိန် (weightGrams) ပါရင် ပုံမှန် ပွဲ အရွယ်အစား ခန့်မှန်းခြေ ဖြစ်သည်။ */
data class BuiltInFood(
    val name: String,
    val unit: String,
    val kcalPerUnit: Int,
    val weightNote: String = ""
)

data class Entry(
    val id: Long,
    val date: String, // yyyy-MM-dd
    val name: String,
    val detail: String, // e.g. "၂ ခွက်" or "၂၅၀ ဂရမ်"
    val kcal: Int
)

data class Profile(
    val isMale: Boolean = true,
    val age: Int = 29,
    val heightCm: Int = 170,
    val weightKg: Double = 65.0,
    val activity: Int = 1, // 0..3
    val goal: Int = 1 // 0 lose, 1 maintain, 2 gain
)

object Calc {
    val ACTIVITY_LABELS = listOf(
        "သိပ်မလှုပ်ရှားဘူး (ထိုင်ပြီးအလုပ်လုပ်တယ်)",
        "အနည်းငယ် လှုပ်ရှားတယ် (တစ်ပတ် ၁–၃ ရက်)",
        "အလယ်အလတ် လှုပ်ရှားတယ် (တစ်ပတ် ၃–၅ ရက်)",
        "များများ လှုပ်ရှားတယ် (တစ်ပတ် ၆–၇ ရက်)"
    )
    val ACTIVITY_FACTORS = doubleArrayOf(1.2, 1.375, 1.55, 1.725)
    val GOAL_LABELS = listOf("ဝိတ်လျော့မယ်", "ဝိတ်ထိန်းမယ်", "ဝိတ်တင်မယ်")

    /** Mifflin-St Jeor — အာဟာရပညာရှင်တွေ သုံးတဲ့ ဆေးဘက်ဆိုင်ရာ စံပြ formula */
    fun bmr(p: Profile): Double {
        val base = 10.0 * p.weightKg + 6.25 * p.heightCm - 5.0 * p.age
        return if (p.isMale) base + 5.0 else base - 161.0
    }

    fun tdee(p: Profile): Double = bmr(p) * ACTIVITY_FACTORS[p.activity.coerceIn(0, 3)]

    fun target(p: Profile): Int {
        val t = tdee(p)
        return when (p.goal) {
            0 -> (t - 500).roundToInt()
            2 -> (t + 300).roundToInt()
            else -> t.roundToInt()
        }
    }

    /** ဂရမ်နဲ့ ထည့်တာ: ၁၀၀ ဂရမ်မှာ ကယ်လိုရီ X ပါရင် */
    fun kcalFromGrams(grams: Double, kcalPer100g: Double): Int =
        (grams * kcalPer100g / 100.0).roundToInt()
}

/** သုံးစွဲသူ ကိုယ်တိုင် ထည့်ထားတဲ့ အစားအစာ — အမြဲတမ်း သိမ်းထားပြီး ပြန်သုံးလို့ရတယ်။
 *  mode 0 = တစ်ပွဲစာ ကယ်လိုရီ အတိုင်း၊ mode 1 = ၁၀၀ ဂရမ်မှာ ကယ်လိုရီ အတိုင်း */
data class CustomFood(
    val id: Long,
    val name: String,
    val mode: Int,
    val kcal: Double // mode 0: per portion; mode 1: per 100g
)

object Foods {
    // ကယ်လိုရီတွေဟာ ထုတ်ပြန်ထားတဲ့ စံ ခန့်မှန်းခြေတွေပါ။ ဆိုင်နဲ့ ချက်နည်းပေါ်မူတည်ပြီး ကွာနိုင်တယ်။
    val ALL: List<BuiltInFood> = listOf(
        // လက်ဖက်ရည်ဆိုင် / လမ်းဘေး — ပုံမှန်ပွဲ၊ ချိန်စရာ မလိုဘူး
        BuiltInFood("လက်ဖက်ရည်", "၁ ခွက်", 150, "ပုံမှန်ခွက်၊ သကြား+နို့ဆီ ပါပြီးသား"),
        BuiltInFood("နန်းကြီးသုပ်", "၁ ပွဲ", 550, "ပုံမှန်ပွဲ"),
        BuiltInFood("အီကြာကွေး", "၁ ချောင်း", 120, "ပုံမှန်အရွယ် ၁ ချောင်း"),
        BuiltInFood("ပလာတာ", "၁ ချပ်", 300, "ပုံမှန်အရွယ် ၁ ချပ်"),
        BuiltInFood("စမူဆာ", "၁ ခု", 150, "ပုံမှန်အရွယ် ၁ ခု"),
        BuiltInFood("ပဲပြုတ်", "၁ ပွဲ", 200, "ပုံမှန်ပွဲ"),
        BuiltInFood("ရခိုင်မုန့်တီ", "၁ ပွဲ", 450, "ပုံမှန်ပွဲ"),
        BuiltInFood("အုန်းနို့ခေါက်ဆွဲ", "၁ ပွဲ", 600, "ပုံမှန်ပွဲ"),
        // မြန်မာ အဓိက အစားအစာများ
        BuiltInFood("မုန့်ဟင်းခါး", "၁ ပွဲ", 450, "ပုံမှန်ပွဲ ~၄၀၀ ဂရမ်"),
        BuiltInFood("ရှမ်းခေါက်ဆွဲ", "၁ ပွဲ", 500, "ပုံမှန်ပွဲ"),
        BuiltInFood("ထမင်း + ကြက်သားဟင်း", "၁ ပွဲ", 650, "ထမင်း ၁ ပွဲ + ဟင်း"),
        BuiltInFood("ထမင်း + ဝက်သားဟင်း", "၁ ပွဲ", 700, "ထမင်း ၁ ပွဲ + ဟင်း"),
        BuiltInFood("ထမင်း + ငါးဟင်း", "၁ ပွဲ", 550, "ထမင်း ၁ ပွဲ + ဟင်း"),
        BuiltInFood("ထမင်း + အသီးအရွက်ဟင်း", "၁ ပွဲ", 450, "ထမင်း ၁ ပွဲ + ဟင်း"),
        BuiltInFood("ထမင်းကြော်", "၁ ပွဲ", 650, "ပုံမှန်ပွဲ"),
        BuiltInFood("ခေါက်ဆွဲပြုတ်", "၁ ပွဲ", 400, "ပုံမှန်ပွဲ"),
        BuiltInFood("ကြက်ဥပြုတ်", "၁ လုံး", 78, "အလယ်အလတ် ၁ လုံး ~၅၀ ဂရမ်"),
        BuiltInFood("ပေါင်မုန့်", "၁ ချပ်", 80, "ပုံမှန်အချပ်"),
        // သစ်သီး / အချိုရည် / မုန့်
        BuiltInFood("ငှက်ပျောသီး", "၁ လုံး", 105, "အလယ်အလတ် ၁ လုံး"),
        BuiltInFood("ပန်းသီး", "၁ လုံး", 95, "အလယ်အလတ် ၁ လုံး"),
        BuiltInFood("သရက်သီး", "၁ လုံး", 150, "အလယ်အလတ် ၁ လုံး"),
        BuiltInFood("ကော်ဖီ (နို့နဲ့)", "၁ ခွက်", 80, "ပုံမှန်ခွက်"),
        BuiltInFood("လက်ဖက်ခြောက် (သကြားမပါ)", "၁ ခွက်", 5, "ပုံမှန်ခွက်"),
        BuiltInFood("အချိုရည် (ဗူး)", "၁ ဗူး", 140, "၃၃၀ မီလီ ၁ ဗူး"),
        BuiltInFood("သစ်သီးဖျော်ရည်", "၁ ခွက်", 120, "ပုံမှန်ခွက်"),
        BuiltInFood("ဒိုးနတ်", "၁ ခု", 300, "ပုံမှန်အရွယ် ၁ ခု"),
        BuiltInFood("ကိတ်", "၁ တုံး", 250, "ပုံမှန်အတုံး"),
        BuiltInFood("ဘီစကစ်", "၁ ထုပ်", 180, "ပုံမှန်ထုပ်"),
        BuiltInFood("အာလူးကြော် (ထုပ်)", "၁ ထုပ်", 150, "ပုံမှန်ထုပ်")
    )
}
