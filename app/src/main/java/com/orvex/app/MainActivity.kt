
package com.orvex.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    // =====================================
    // ORVEX 1.0 - PROGRAMMING ENVIRONMENT
    // =====================================

    private val bg = Color.rgb(7, 7, 13)
    private val surface = Color.rgb(18, 16, 29)
    private val surface2 = Color.rgb(29, 23, 44)
    private val white = Color.WHITE
    private val muted = Color.rgb(165, 155, 190)

    private val accent = Color.rgb(160, 75, 255)
    private val accent2 = Color.rgb(95, 35, 170)

    private lateinit var mainRoot: FrameLayout
    private lateinit var content: LinearLayout

    private var currentPage = "home"
    private var drawerOpen = false

    private val preferences by lazy {
        getSharedPreferences("orvex_data", Context.MODE_PRIVATE)
    }

    private val pages = listOf(
        "home" to "الرئيسية",
        "learn" to "تعلم البرمجة",
        "editor" to "محرر الأكواد",
        "playground" to "مختبر البرمجة",
        "challenges" to "التحديات",
        "quizzes" to "الاختبارات",
        "projects" to "مشاريعي",
        "achievements" to "الإنجازات",
        "settings" to "الإعدادات",
        "about" to "حول ORVEX"
    )

    private val languages = listOf(
        "Python",
        "Kotlin",
        "Java",
        "JavaScript",
        "C++"
    )

    // =====================================
    // HELPERS
    // =====================================

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(
        color: Int,
        radius: Int = 18,
        stroke: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()

            if (stroke != 0) {
                setStroke(dp(1), stroke)
            }
        }
    }

    private fun gradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(76, 27, 125),
                Color.rgb(35, 20, 57),
                Color.rgb(15, 12, 24)
            )
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), accent)
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun params(
        width: Int = -1,
        height: Int = -2,
        top: Int = 0
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(width, height).apply {
            topMargin = dp(top)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun section(title: String, subtitle: String = "") {
        content.addView(
            makeText(title, 18f, white, true),
            params(top = 22)
        )

        if (subtitle.isNotEmpty()) {
            content.addView(
                makeText(subtitle, 11f, muted),
                params(top = 4)
            )
        }
    }

    private fun infoCard(title: String, description: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = rounded(surface, 15)
        }

        box.addView(makeText(title, 13f, accent, true))

        box.addView(
            makeText(description, 11f, muted),
            params(top = 5)
        )

        content.addView(box, params(top = 9))
    }

    private fun card(
        title: String,
        description: String,
        icon: String,
        action: () -> Unit
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(14), dp(13), dp(14))
            background = rounded(surface, 16, Color.rgb(55, 38, 78))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        val iconView = TextView(this).apply {
            text = icon
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = rounded(surface2, 12)
        }

        box.addView(iconView, LinearLayout.LayoutParams(dp(45), dp(45)))

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        labels.addView(makeText(title, 14f, white, true))
        labels.addView(
            makeText(description, 10f, muted),
            params(top = 4)
        )

        box.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(makeText("‹", 25f, accent, true))

        content.addView(box, params(top = 10))
    }

    // =====================================
    // ACTIVITY
    // =====================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        showHome()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (drawerOpen) {
            closeDrawer()
        } else if (currentPage != "home") {
            showHome()
        } else {
            super.onBackPressed()
        }
    }

    // =====================================
    // PAGE SYSTEM
    // =====================================

    private fun createPage(
        title: String,
        subtitle: String
    ) {
        mainRoot = FrameLayout(this).apply {
            setBackgroundColor(bg)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(17), dp(13), dp(17), dp(13))
            background = rounded(surface, 0)
        }

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 27f
            setTextColor(accent)
            gravity = Gravity.CENTER
            isClickable = true
            setOnClickListener { openDrawer() }
        }

        header.addView(menuButton, LinearLayout.LayoutParams(dp(45), dp(48)))

        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        titles.addView(makeText(title, 17f, white, true))
        titles.addView(
            makeText(subtitle, 9f, muted),
            params(top = 2)
        )

        header.addView(
            titles,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        header.addView(makeText("ORVEX", 13f, accent, true))

        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(15), dp(17), dp(28))
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        mainRoot.addView(root)
        setContentView(mainRoot)
    }

    private fun openDrawer() {
        if (drawerOpen) return
        drawerOpen = true

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0x99000000.toInt())
            tag = "drawer_overlay"
            setOnClickListener { closeDrawer() }
        }

        val drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(24), dp(17), dp(18))
            background = rounded(surface, 0)
            tag = "drawer_panel"
        }

        drawer.addView(
            makeText("ORVEX", 26f, accent, true),
            params(top = 10)
        )

        drawer.addView(
            makeText("بيئة التطوير البرمجية", 11f, muted),
            params(top = 5)
        )

        val line = View(this).apply {
            setBackgroundColor(Color.rgb(65, 42, 91))
        }

        drawer.addView(line, params(height = dp(1), top = 17))

        val menuScroll = ScrollView(this)

        val menuItems = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        pages.forEach { page ->
            val active = currentPage == page.first

            val item = TextView(this).apply {
                text = (if (active) "◆  " else "◇  ") + page.second
                textSize = 14f
                setTextColor(if (active) accent else white)
                setPadding(dp(13), dp(15), dp(13), dp(15))
                background = if (active) rounded(surface2, 12) else null
                isClickable = true
                setOnClickListener {
                    closeDrawer()
                    navigate(page.first)
                }
            }

            menuItems.addView(item, params(top = 5))
        }

        menuScroll.addView(menuItems)

        drawer.addView(
            menuScroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        drawer.addView(
            makeText("ORVEX 1.0", 10f, muted),
            params(top = 12)
        )

        overlay.addView(
            drawer,
            FrameLayout.LayoutParams(
                dp(285),
                -1,
                Gravity.END
            )
        )

        mainRoot.addView(
            overlay,
            FrameLayout.LayoutParams(-1, -1)
        )
    }

    private fun closeDrawer() {
        val overlay = mainRoot.findViewWithTag<View>("drawer_overlay")
        if (overlay != null) {
            mainRoot.removeView(overlay)
        }
        drawerOpen = false
    }

    private fun navigate(page: String) {
        when (page) {
            "home" -> showHome()
            "learn" -> showLearn()
            "editor" -> showEditor()
            "playground" -> showPlayground()
            "challenges" -> showChallenges()
            "quizzes" -> showQuizzes()
            "projects" -> showProjects()
            "achievements" -> showAchievements()
            "settings" -> showSettings()
            "about" -> showAbout()
        }
    }

    // =====================================
    // HOME
    // =====================================

    private fun showHome() {
        currentPage = "home"
        createPage("الرئيسية", "PROGRAMMING ENVIRONMENT")

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(23), dp(20), dp(23))
            background = gradient()
        }

        hero.addView(makeText("WELCOME TO", 11f, accent, true))
        hero.addView(
            makeText("ORVEX", 31f, white, true),
            params(top = 7)
        )
        hero.addView(
            makeText("بيئة التطوير البرمجية الخاصة بك", 13f, white),
            params(top = 6)
        )
        hero.addView(
            makeText(
                "تعلّم، اكتب الأكواد، أنشئ مشاريعك وطوّر مهاراتك.",
                11f,
                muted
            ),
            params(top = 10)
        )

        content.addView(hero, params(top = 5))

        section("مساحة العمل", "ابدأ العمل على مشروعك البرمجي")

        card("محرر الأكواد", "اكتب واحفظ الأكواد البرمجية", "</>") {
            showEditor()
        }

        card("مشاريعي", "إدارة المشاريع والملفات المحفوظة", "▣") {
            showProjects()
        }

        card("مختبر البرمجة", "مساحة تجارب وكتابة الأكواد", "⌘") {
            showPlayground()
        }

        section("التعلّم والتطوير", "طوّر مستواك البرمجي")

        card("تعلم البرمجة", "دروس ومفاهيم برمجية", "▤") {
            showLearn()
        }

        card("التحديات", "اختبر مهاراتك البرمجية", "⚡") {
            showChallenges()
        }

        card("الاختبارات", "أسئلة لقياس معرفتك", "✓") {
            showQuizzes()
        }

        card("الإنجازات", "تابع تقدمك ومستواك", "★") {
            showAchievements()
        }

        infoCard(
            "حالة بيئة التطوير",
            "الإصدار 1.0 — محرر الأكواد المحلي جاهز للكتابة والحفظ. محركات التشغيل ستضاف تدريجياً."
        )
    }

    // =====================================
    // LEARNING CENTER
    // =====================================

    private fun showLearn() {
        currentPage = "learn"
        createPage("تعلم البرمجة", "LEARNING CENTER")

        section("اختر لغة البرمجة", "ابدأ من الأساسيات")

        languages.forEach { language ->
            card(
                language,
                "دروس ومفاهيم وتمارين تعليمية",
                "</>"
            ) {
                showLesson(language)
            }
        }

        section("مسارات تعليمية")

        card("أساسيات البرمجة", "المتغيرات والشروط والحلقات", "01") {
            showLesson("أساسيات البرمجة")
        }

        card("هياكل البيانات", "القوائم والمصفوفات والخرائط", "02") {
            showLesson("هياكل البيانات")
        }

        card("حل المشكلات", "تعلم التفكير المنطقي", "03") {
            showLesson("حل المشكلات")
        }
    }

    private fun showLesson(language: String) {
        currentPage = "lesson"
        createPage(language, "LEARNING PATH")

        section("مقدمة")

        infoCard(
            "ما هي $language؟",
            "لغة برمجة تستخدم لكتابة تعليمات يفهمها الحاسوب. في هذا المسار ستتعرف على المفاهيم الأساسية ثم تنتقل إلى التطبيقات العملية."
        )

        section("المفاهيم الأساسية")

        val lessons = listOf(
            "مقدمة إلى اللغة",
            "المتغيرات وأنواع البيانات",
            "العمليات الحسابية",
            "الشروط واتخاذ القرار",
            "الحلقات التكرارية",
            "الدوال",
            "التطبيق العملي"
        )

        lessons.forEachIndexed { index, title ->
            card(
                "الدرس ${index + 1}: $title",
                "محتوى تعليمي تمهيدي",
                "${index + 1}"
            ) {
                AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(
                        "هذا الدرس ضمن مسار $language.\n\n" +
                            "سيتم توسيع المحتوى والأمثلة والتمارين في تحديثات التعلم القادمة."
                    )
                    .setPositiveButton("حسناً", null)
                    .show()
            }
        }

        card("فتح محرر الأكواد", "تجربة كتابة الأكواد", "</>") {
            showEditor()
        }
    }

    // =====================================
    // CODE EDITOR
    // =====================================

    private fun showEditor() {
        currentPage = "editor"
        createPage("محرر الأكواد", "CODE EDITOR")

        val savedCode = preferences.getString("editor_code", "") ?: ""

        val languagesArray = languages.toTypedArray()

        var selectedLanguage =
            preferences.getString("editor_language", "Python") ?: "Python"

        section("لغة البرمجة", "اختر لغة العمل")

        val spinner = Spinner(this)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languagesArray
        )

        spinner.adapter = adapter

        val selectedIndex = languages.indexOf(selectedLanguage)
        spinner.setSelection(if (selectedIndex >= 0) selectedIndex else 0)

        content.addView(spinner, params(top = 10))

        section("محرر الكود", "اكتب الكود البرمجي هنا")

        val codeEditor = EditText(this).apply {
            setText(savedCode)
            setTextColor(Color.rgb(220, 205, 255))
            setHintTextColor(muted)
            hint = "// اكتب الكود هنا"
            textSize = 13f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.START
            textDirection = View.TEXT_DIRECTION_LTR
            setPadding(dp(13), dp(15), dp(13), dp(15))
            minLines = 14
            maxLines = 100
            background = rounded(Color.rgb(12, 11, 19), 14, accent)
            isVerticalScrollBarEnabled = true
        }

        content.addView(
            codeEditor,
            params(height = dp(310), top = 10)
        )

        card("حفظ الكود", "حفظ الكود محلياً على الجهاز", "↓") {
            selectedLanguage = spinner.selectedItem.toString()

            preferences.edit()
                .putString("editor_code", codeEditor.text.toString())
                .putString("editor_language", selectedLanguage)
                .apply()

            toast("تم حفظ الكود")
        }

        card("فتح مشاريعي", "عرض المشاريع المحفوظة", "▣") {
            saveCurrentCode(
                codeEditor.text.toString(),
                spinner.selectedItem.toString(),
                false
            )
            showProjects()
        }

        card("معلومات التشغيل", "حالة محرك التنفيذ", "▶") {
            AlertDialog.Builder(this)
                .setTitle("تشغيل الكود")
                .setMessage(
                    "محرك تشغيل ${spinner.selectedItem} غير مدمج في هذه المرحلة.\n\n" +
                        "المحرر يدعم الكتابة والحفظ حالياً. لن يتم عرض نتائج تشغيل وهمية."
                )
                .setPositiveButton("حسناً", null)
                .show()
        }

        infoCard(
            "ملاحظة",
            "هذا محرر نصوص برمجية أولي. تلوين Syntax Highlighting والتنفيذ والتصحيح الفعلي ستتم إضافتها في مراحل مستقلة."
        )
    }

    // =====================================
    // PROJECT STORAGE
    // =====================================

    private fun loadProjects(): JSONArray {
        return try {
            JSONArray(
                preferences.getString("projects", "[]") ?: "[]"
            )
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveCurrentCode(
        code: String,
        language: String,
        showMessage: Boolean = true
    ) {
        val input = EditText(this).apply {
            hint = "اسم المشروع"
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(surface)
        }

        AlertDialog.Builder(this)
            .setTitle("حفظ المشروع")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val name = input.text.toString().trim()

                if (name.isEmpty()) {
                    toast("اكتب اسم المشروع")
                    return@setPositiveButton
                }

                val projects = loadProjects()

                val item = JSONObject().apply {
                    put("name", name)
                    put("language", language)
                    put("code", code)
                    put("updated", System.currentTimeMillis())
                }

                projects.put(item)

                preferences.edit()
                    .putString("projects", projects.toString())
                    .apply()

                if (showMessage) toast("تم حفظ المشروع")
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showProjects() {
        currentPage = "projects"
        createPage("مشاريعي", "PROJECT MANAGER")

        val projects = loadProjects()

        section("المشاريع المحفوظة", "${projects.length()} مشروع")

        card("مشروع جديد", "إنشاء مشروع برمجي جديد", "+") {
            showEditor()
        }

        if (projects.length() == 0) {
            infoCard(
                "لا توجد مشاريع",
                "افتح محرر الأكواد واحفظ أول مشروع لك."
            )
        }

        for (i in projects.length() - 1 downTo 0) {
            val project = projects.optJSONObject(i) ?: continue
            val name = project.optString("name", "مشروع")
            val language = project.optString("language", "غير معروف")

            card(name, language, "</>") {
                showProjectOptions(i)
            }
        }
    }

    private fun showProjectOptions(index: Int) {
        val projects = loadProjects()
        val project = projects.optJSONObject(index) ?: return

        AlertDialog.Builder(this)
            .setTitle(project.optString("name"))
            .setItems(arrayOf("فتح المشروع", "حذف المشروع")) { _, choice ->
                when (choice) {
                    0 -> {
                        preferences.edit()
                            .putString(
                                "editor_code",
                                project.optString("code")
                            )
                            .putString(
                                "editor_language",
                                project.optString("language")
                            )
                            .apply()

                        showEditor()
                    }

                    1 -> {
                        AlertDialog.Builder(this)
                            .setTitle("حذف المشروع")
                            .setMessage("هل تريد حذف هذا المشروع؟")
                            .setPositiveButton("حذف") { _, _ ->
                                val updated = loadProjects()
                                if (index in 0 until updated.length()) {
                                    updated.remove(index)
                                    preferences.edit()
                                        .putString("projects", updated.toString())
                                        .apply()
                                }
                                showProjects()
                            }
                            .setNegativeButton("إلغاء", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    // =====================================
    // PLAYGROUND
    // =====================================

    private fun showPlayground() {
        currentPage = "playground"
        createPage("مختبر البرمجة", "PROGRAMMING PLAYGROUND")

        section("مساحة التجارب")

        infoCard(
            "مختبر ORVEX",
            "استخدم هذه المساحة لكتابة أفكارك وتجربة الأكواد دون الحاجة إلى إنشاء مشروع جديد."
        )

        card("فتح المحرر", "الانتقال إلى محرر الأكواد", "</>") {
            showEditor()
        }

        card("عرض اللغات", "اللغات المستهدفة في ORVEX", "⌘") {
            AlertDialog.Builder(this)
                .setTitle("لغات البرمجة")
                .setItems(languages.toTypedArray(), null)
                .setPositiveButton("إغلاق", null)
                .show()
        }

        infoCard(
            "حالة المختبر",
            "الكتابة والحفظ متاحان من المحرر. بيئات التنفيذ المعزولة ستضاف لاحقاً."
        )
    }

    // =====================================
    // CHALLENGES
    // =====================================

    private fun showChallenges() {
        currentPage = "challenges"
        createPage("التحديات", "CODING CHALLENGES")

        section("تحديات البرمجة", "اختر تحدياً وجرّب حلّه")

        val challenges = listOf(
            "اطبع رسالة ترحيب",
            "احسب مجموع عددين",
            "اكتشف العدد الزوجي",
            "اعكس نصاً",
            "احسب أكبر رقم"
        )

        challenges.forEachIndexed { index, challenge ->
            card(
                "التحدي ${index + 1}",
                challenge,
                "⚡"
            ) {
                showChallenge(challenge)
            }
        }
    }

    private fun showChallenge(title: String) {
        currentPage = "challenge"
        createPage(title, "CHALLENGE DETAILS")

        section(title)

        infoCard(
            "المطلوب",
            when (title) {
                "اطبع رسالة ترحيب" ->
                    "اكتب برنامجاً يعرض رسالة ترحيب على الشاشة."
                "احسب مجموع عددين" ->
                    "اكتب برنامجاً يستقبل عددين ويحسب مجموعهما."
                "اكتشف العدد الزوجي" ->
                    "اكتب برنامجاً يحدد هل العدد زوجي أم فردي."
                "اعكس نصاً" ->
                    "اكتب برنامجاً يعرض النص بترتيب معكوس."
                else ->
                    "اكتب برنامجاً يجد أكبر قيمة ضمن مجموعة أعداد."
            }
        )

        card("ابدأ الحل", "افتح محرر الأكواد", "</>") {
            showEditor()
        }
    }

    // =====================================
    // QUIZZES
    // =====================================

    private fun showQuizzes() {
        currentPage = "quizzes"
        createPage("الاختبارات", "PROGRAMMING QUIZZES")

        section("اختبار أساسيات البرمجة")

        val questions = listOf(
            Triple(
                "ما وظيفة المتغير؟",
                listOf("تخزين البيانات", "إغلاق البرنامج", "حذف النظام"),
                0
            ),
            Triple(
                "ما وظيفة الحلقة التكرارية؟",
                listOf("تكرار التعليمات", "حذف الملفات", "تغيير الشاشة"),
                0
            ),
            Triple(
                "ما وظيفة الشرط؟",
                listOf("اتخاذ قرار", "تشغيل الصوت", "تغيير اللون"),
                0
            )
        )

        card("بدء الاختبار", "3 أسئلة في أساسيات البرمجة", "✓") {
            startQuiz(questions)
        }
    }

    private fun startQuiz(
        questions: List<Triple<String, List<String>, Int>>
    ) {
        var index = 0
        var score = 0

        fun nextQuestion() {
            if (index >= questions.size) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("انتهى الاختبار")
                    .setMessage("نتيجتك: $score من ${questions.size}")
                    .setPositiveButton("إنهاء") { _, _ ->
                        if (score == questions.size) {
                            preferences.edit()
                                .putBoolean("quiz_completed", true)
                                .apply()
                        }
                        showQuizzes()
                    }
                    .show()
                return
            }

            val question = questions[index]

            AlertDialog.Builder(this@MainActivity)
                .setTitle("السؤال ${index + 1}")
                .setMessage(question.first)
                .setSingleChoiceItems(
                    question.second.toTypedArray(),
                    -1
                ) { dialog, selected ->
                    if (selected == question.third) score++
                    index++
                    dialog.dismiss()
                    nextQuestion()
                }
                .setNegativeButton("إلغاء") { _, _ ->
                    showQuizzes()
                }
                .setCancelable(false)
                .show()
        }

        nextQuestion()
    }

    // =====================================
    // ACHIEVEMENTS
    // =====================================

    private fun showAchievements() {
        currentPage = "achievements"
        createPage("الإنجازات", "YOUR PROGRESS")

        val projects = loadProjects().length()
        val quizDone = preferences.getBoolean("quiz_completed", false)

        section("إحصائياتك")

        infoCard("المشاريع", "$projects مشروع محفوظ")

        infoCard(
            "الاختبارات",
            if (quizDone) "أكملت اختباراً بنجاح كامل" else "لم تكمل اختباراً بعد"
        )

        section("الإنجازات")

        card(
            "المبرمج المبتدئ",
            "ابدأ رحلتك البرمجية",
            "★"
        ) {
            toast("واصل التعلم لتحقيق الإنجازات")
        }

        card(
            "صانع المشاريع",
            "احفظ أول مشروع برمجي",
            "◆"
        ) {
            toast(
                if (projects > 0) "تم فتح الإنجاز" else "احفظ مشروعاً أولاً"
            )
        }

        card(
            "متقن الأساسيات",
            "أكمل الاختبار بالنتيجة الكاملة",
            "✓"
        ) {
            toast(
                if (quizDone) "تم تحقيق الإنجاز" else "أكمل الاختبار أولاً"
            )
        }
    }

    // =====================================
    // SETTINGS
    // =====================================

    private fun showSettings() {
        currentPage = "settings"
        createPage("الإعدادات", "ORVEX SETTINGS")

        section("إعدادات التطبيق")

        infoCard("المظهر", "أسود وبنفسجي كهربائي")

        card("مسح الكود المحفوظ", "حذف النص الموجود في المحرر", "×") {
            AlertDialog.Builder(this)
                .setTitle("مسح المحرر")
                .setMessage("هل تريد حذف الكود المحفوظ؟")
                .setPositiveButton("حذف") { _, _ ->
                    preferences.edit()
                        .remove("editor_code")
                        .apply()
                    toast("تم حذف الكود المحفوظ")
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        card("حول ORVEX", "معلومات التطبيق والإصدار", "ⓘ") {
            showAbout()
        }
    }

    // =====================================
    // ABOUT
    // =====================================

    private fun showAbout() {
        currentPage = "about"
        createPage("حول ORVEX", "APPLICATION INFORMATION")

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(23), dp(20), dp(23))
            background = gradient()
        }

        panel.addView(makeText("ORVEX", 32f, white, true))

        panel.addView(
            makeText("PROGRAMMING ENVIRONMENT", 11f, accent, true),
            params(top = 8)
        )

        panel.addView(
            makeText("بيئة تعليم وتطوير البرمجيات", 13f, muted),
            params(top = 8)
        )

        content.addView(panel, params(top = 8))

        section("معلومات الإصدار")

        infoCard("اسم التطبيق", "ORVEX")
        infoCard("الإصدار", "1.0")
        infoCard("Package", packageName)
        infoCard("لغة التطوير", "Kotlin")
        infoCard("المنصة", "Android")

        section("معلومات تقنية")

        infoCard(
            "محرر الأكواد",
            "كتابة وحفظ النصوص البرمجية محلياً."
        )

        infoCard(
            "محركات التشغيل",
            "لم يتم دمج محركات التنفيذ بعد."
        )

        infoCard(
            "الخصوصية",
            "المشاريع والكود محفوظة محلياً باستخدام SharedPreferences."
        )
    }
}
