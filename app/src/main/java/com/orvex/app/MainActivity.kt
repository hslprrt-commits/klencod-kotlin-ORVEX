
package com.orvex.app

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray

class MainActivity : Activity() {

    private val bg = Color.rgb(7, 7, 13)
    private val surface = Color.rgb(17, 15, 27)
    private val purple = Color.rgb(151, 45, 255)
    private val muted = Color.rgb(165, 155, 190)
    private val white = Color.WHITE

    private lateinit var content: LinearLayout

    private val preferences by lazy {
        getSharedPreferences("orvex_data", Context.MODE_PRIVATE)
    }

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
            if (stroke != 0) setStroke(dp(1), stroke)
        }
    }

    private fun gradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(43, 12, 76),
                Color.rgb(15, 10, 29),
                Color.rgb(9, 8, 17)
            )
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), purple)
        }
    }

    private fun text(
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
            if (bold) typeface = Typeface.DEFAULT_BOLD
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility = 0
        }

        showHome()
    }

    // =====================================
    // MAIN LAYOUT
    // =====================================

    private fun createPage(
        title: String,
        subtitle: String
    ): LinearLayout {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(25))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = text("‹", 34f, purple, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showHome() }
        }

        header.addView(back, LinearLayout.LayoutParams(dp(45), dp(48)))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        titleBox.addView(text(title, 20f, white, true))
        titleBox.addView(text(subtitle, 11f, muted))

        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))

        content.addView(header)
        scroll.addView(content)

        root.addView(scroll, LinearLayout.LayoutParams(0, 0, 1f))
        root.addView(createNavigation())

        return root
    }

    private fun createNavigation(): LinearLayout {

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(7), dp(4), dp(7))
            setBackgroundColor(surface)
        }

        val items = listOf(
            "⌂" to "الرئيسية",
            "▦" to "الأدوات",
            "▣" to "التطبيقات",
            "⚙" to "الإعدادات",
            "ⓘ" to "حول"
        )

        items.forEachIndexed { index, item ->

            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            val active = index == 0

            box.addView(
                text(
                    item.first,
                    22f,
                    if (active) purple else muted,
                    true
                ).apply { gravity = Gravity.CENTER }
            )

            box.addView(
                text(
                    item.second,
                    10f,
                    if (active) purple else muted
                ).apply { gravity = Gravity.CENTER }
            )

            box.setOnClickListener {
                when (index) {
                    0 -> showHome()
                    1 -> showTools()
                    2 -> showApplications()
                    3 -> showSettings()
                    4 -> showAbout()
                }
            }

            nav.addView(
                box,
                LinearLayout.LayoutParams(0, dp(53), 1f)
            )
        }

        return nav
    }

    private fun showPage(
        title: String,
        subtitle: String
    ) {
        setContentView(createPage(title, subtitle))
    }

    private fun addSection(
        title: String,
        subtitle: String
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        box.addView(text(title, 18f, white, true))
        box.addView(text(subtitle, 11f, muted))

        content.addView(box, params(top = 24))
    }

    private fun addCard(
        title: String,
        description: String,
        icon: String,
        action: () -> Unit
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(surface, 17, 0xFF51217E.toInt())
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        card.addView(text("$icon  $title", 16f, white, true))
        card.addView(text(description, 11f, muted))

        content.addView(card, params(top = 10))
    }

    // =====================================
    // HOME
    // =====================================

    private fun showHome() {

        showPage(
            "ORVEX",
            "SMART CONTROL CENTER • VERSION 1.1"
        )

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = gradient()
        }

        hero.addView(text("مرحباً بك في ORVEX", 23f, white, true))
        hero.addView(
            text("مركز التحكم الذكي لهاتفك", 14f, muted),
            params(top = 8)
        )
        hero.addView(
            text("كل ما تحتاجه في مكان واحد", 12f, purple, true),
            params(top = 15)
        )

        content.addView(hero, params(top = 20))

        addSection("حالة الجهاز", "إحصائيات الهاتف الحالية")

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(surface, 17, 0xFF44216A.toInt())
            setPadding(dp(5), dp(15), dp(5), dp(15))
        }

        addStat(stats, "🔋", getBattery(), "البطارية")
        addStat(stats, "💾", getStorageUsed(), "التخزين")
        addStat(stats, "▣", getMemoryAvailable(), "RAM")
        addStat(stats, "🤖", "Android ${Build.VERSION.RELEASE}", "النظام")

        content.addView(stats, params(top = 12))

        addSection("مركز الأدوات", "الخدمات الرئيسية")

        addCard(
            "معلومات الجهاز",
            "تفاصيل الهاتف والذاكرة والتخزين والشاشة",
            "📊"
        ) { showDeviceInfo() }

        addCard(
            "ORVEX AI",
            "واجهة المساعد الذكي",
            "🤖"
        ) { showAI() }

        addCard(
            "مدير الملفات",
            "فتح الملفات والمستندات",
            "📁"
        ) { openFiles() }

        addCard(
            "المهام والتنبيهات",
            "إضافة المهام وحفظها وإدارتها",
            "📋"
        ) { showTasks() }

        addCard(
            "الأدوات",
            "الحاسبة ومشاركة النص وفحص التخزين",
            "🛠"
        ) { showTools() }
    }

    private fun addStat(
        parent: LinearLayout,
        icon: String,
        value: String,
        label: String
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        box.addView(
            text(icon, 21f, purple, true).apply {
                gravity = Gravity.CENTER
            }
        )

        box.addView(
            text(value, 11f, white, true).apply {
                gravity = Gravity.CENTER
                maxLines = 1
            }
        )

        box.addView(
            text(label, 10f, muted).apply {
                gravity = Gravity.CENTER
            }
        )

        parent.addView(
            box,
            LinearLayout.LayoutParams(0, dp(75), 1f)
        )
    }

    // =====================================
    // BATTERY
    // =====================================

    private fun getBattery(): String {
        return try {
            val intent = registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val level = intent?.getIntExtra(
                BatteryManager.EXTRA_LEVEL, -1
            ) ?: -1

            val scale = intent?.getIntExtra(
                BatteryManager.EXTRA_SCALE, -1
            ) ?: -1

            if (level >= 0 && scale > 0) {
                "${level * 100 / scale}%"
            } else {
                "غير متاح"
            }
        } catch (e: Exception) {
            "غير متاح"
        }
    }

    private fun getBatteryStatus(): String {
        return try {
            val intent = registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            when (
                intent?.getIntExtra(
                    BatteryManager.EXTRA_STATUS, -1
                )
            ) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "قيد الشحن"
                BatteryManager.BATTERY_STATUS_FULL -> "ممتلئة"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "تعمل على البطارية"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "غير قيد الشحن"
                else -> "غير معروف"
            }
        } catch (e: Exception) {
            "غير متاح"
        }
    }

    // =====================================
    // STORAGE
    // =====================================

    private fun storageData(): Triple<Long, Long, Long> {
        val stat = StatFs(Environment.getDataDirectory().path)

        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = total - free

        return Triple(total, used, free)
    }

    private fun formatGB(bytes: Long): String {
        return String.format(
            "%.2f GB",
            bytes.toDouble() / 1_073_741_824.0
        )
    }

    private fun getStorageUsed(): String {
        return try {
            formatGB(storageData().second)
        } catch (e: Exception) {
            "غير متاح"
        }
    }

    // =====================================
    // MEMORY
    // =====================================

    private fun memoryData(): ActivityManager.MemoryInfo {
        val manager = getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager

        return ActivityManager.MemoryInfo().also {
            manager.getMemoryInfo(it)
        }
    }

    private fun getMemoryAvailable(): String {
        return try {
            formatGB(memoryData().availMem)
        } catch (e: Exception) {
            "غير متاح"
        }
    }

    // =====================================
    // DEVICE INFORMATION
    // =====================================

    private fun showDeviceInfo() {

        showPage(
            "معلومات الجهاز",
            "تفاصيل الهاتف الحالية"
        )

        addSection("معلومات عامة", "هوية الجهاز والنظام")

        addInfo("الشركة المصنعة", Build.MANUFACTURER)
        addInfo("طراز الجهاز", Build.MODEL)
        addInfo("اسم الجهاز", Build.DEVICE)
        addInfo("إصدار Android", Build.VERSION.RELEASE)
        addInfo("مستوى SDK", Build.VERSION.SDK_INT.toString())
        addInfo("اسم النظام", Build.DISPLAY)

        addSection("الذاكرة", "حالة RAM")

        try {
            val memory = memoryData()

            addInfo("إجمالي RAM", formatGB(memory.totalMem))
            addInfo("الذاكرة المتاحة", formatGB(memory.availMem))
            addInfo(
                "الذاكرة المستخدمة تقريبياً",
                formatGB(memory.totalMem - memory.availMem)
            )
        } catch (e: Exception) {
            addInfo("RAM", "غير متاح")
        }

        addSection("التخزين", "مساحة قسم بيانات التطبيق")

        try {
            val storage = storageData()

            addInfo("المساحة الكلية", formatGB(storage.first))
            addInfo("المساحة المستخدمة", formatGB(storage.second))
            addInfo("المساحة المتاحة", formatGB(storage.third))
        } catch (e: Exception) {
            addInfo("التخزين", "غير متاح")
        }

        addSection("البطارية", "حالة الطاقة")

        addInfo("مستوى البطارية", getBattery())
        addInfo("حالة الشحن", getBatteryStatus())

        addSection("الشاشة", "معلومات العرض")

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        addInfo(
            "الدقة",
            "${metrics.widthPixels} × ${metrics.heightPixels}"
        )

        addInfo(
            "كثافة الشاشة",
            "${metrics.densityDpi} DPI"
        )

        addCard(
            "تحديث المعلومات",
            "إعادة قراءة بيانات الجهاز",
            "↻"
        ) {
            showDeviceInfo()
        }
    }

    private fun addInfo(label: String, value: String) {

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(surface, 13)
        }

        row.addView(text(label, 11f, muted))
        row.addView(
            text(value, 14f, white, true),
            params(top = 5)
        )

        content.addView(row, params(top = 8))
    }

    // =====================================
    // AI
    // =====================================

    private fun showAI() {

        showPage(
            "ORVEX AI",
            "المساعد الذكي"
        )

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = gradient()
        }

        panel.addView(text("ORVEX AI", 23f, white, true))

        panel.addView(
            text(
                "المساعد الذكي قيد الربط بخدمة AI فعلية.",
                13f,
                muted
            ),
            params(top = 12)
        )

        panel.addView(
            text(
                "لن يتم عرض إجابات وهمية أو الادعاء بأن الذكاء الاصطناعي متصل.",
                12f,
                Color.rgb(205, 160, 255)
            ),
            params(top = 10)
        )

        content.addView(panel, params(top = 20))

        addSection("الميزات المخطط لها", "قدرات المساعد")

        addInfo("البرمجة", "كتابة الأكواد وشرحها")
        addInfo("تحليل الأخطاء", "قراءة الأخطاء واقتراح حلول")
        addInfo("معلومات الجهاز", "شرح بيانات الهاتف")
        addInfo("المحادثة", "الإجابة عن الأسئلة")
    }

    // =====================================
    // FILE MANAGER
    // =====================================

    private fun openFiles() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }

            startActivityForResult(intent, 100)
        } catch (e: Exception) {
            toast("تعذر فتح مدير الملفات")
        }
    }

    // =====================================
    // TOOLS
    // =====================================

    private fun showTools() {

        showPage("الأدوات", "أدوات ORVEX")

        addCard("معلومات الجهاز", "عرض بيانات الهاتف", "📊") {
            showDeviceInfo()
        }

        addCard("الحاسبة", "فتح حاسبة الهاتف", "🧮") {
            try {
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_APP_CALCULATOR)

                startActivity(intent)
            } catch (e: Exception) {
                toast("لا توجد حاسبة متاحة")
            }
        }

        addCard("فحص التخزين", "عرض المساحة المتاحة والمستخدمة", "💾") {
            showStorageInfo()
        }

        addCard("مشاركة ORVEX", "مشاركة معلومات التطبيق", "↗") {
            shareText()
        }
    }

    private fun showStorageInfo() {
        try {
            val storage = storageData()

            AlertDialog.Builder(this)
                .setTitle("معلومات التخزين")
                .setMessage(
                    "المساحة الكلية: ${formatGB(storage.first)}\n\n" +
                    "المستخدمة: ${formatGB(storage.second)}\n\n" +
                    "المتاحة: ${formatGB(storage.third)}"
                )
                .setPositiveButton("إغلاق", null)
                .show()
        } catch (e: Exception) {
            toast("تعذر قراءة معلومات التخزين")
        }
    }

    // =====================================
    // TASKS
    // =====================================

    private fun loadTasks(): MutableList<String> {
        val result = mutableListOf<String>()

        return try {
            val json = JSONArray(
                preferences.getString("tasks", "[]") ?: "[]"
            )

            for (i in 0 until json.length()) {
                result.add(json.getString(i))
            }

            result
        } catch (e: Exception) {
            result
        }
    }

    private fun saveTasks(tasks: List<String>) {
        val json = JSONArray()

        tasks.forEach { json.put(it) }

        preferences.edit()
            .putString("tasks", json.toString())
            .apply()
    }

    private fun showTasks() {

        showPage(
            "المهام",
            "إدارة المهام المحفوظة"
        )

        addCard(
            "إضافة مهمة",
            "إنشاء مهمة جديدة وحفظها",
            "+"
        ) {
            addTask()
        }

        val tasks = loadTasks()

        addSection(
            "قائمة المهام",
            "${tasks.size} مهمة محفوظة"
        )

        if (tasks.isEmpty()) {
            addInfo("لا توجد مهام", "أضف مهمتك الأولى")
        } else {
            tasks.forEachIndexed { index, task ->
                addCard(
                    task,
                    "اضغط لحذف هذه المهمة",
                    "📌"
                ) {
                    AlertDialog.Builder(this)
                        .setTitle("حذف المهمة")
                        .setMessage("هل تريد حذف هذه المهمة؟")
                        .setPositiveButton("حذف") { _, _ ->
                            val updated = loadTasks()
                            if (index in updated.indices) {
                                updated.removeAt(index)
                                saveTasks(updated)
                                showTasks()
                            }
                        }
                        .setNegativeButton("إلغاء", null)
                        .show()
                }
            }
        }
    }

    private fun addTask() {

        val input = EditText(this).apply {
            hint = "اكتب المهمة هنا"
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(surface)
        }

        AlertDialog.Builder(this)
            .setTitle("مهمة جديدة")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->

                val value = input.text.toString().trim()

                if (value.isNotEmpty()) {
                    val tasks = loadTasks()
                    tasks.add(value)
                    saveTasks(tasks)
                    showTasks()
                    toast("تم حفظ المهمة")
                } else {
                    toast("اكتب اسم المهمة أولاً")
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    // =====================================
    // APPLICATIONS
    // =====================================

    private fun showApplications() {

        showPage(
            "التطبيقات",
            "إدارة التطبيقات"
        )

        addInfo(
            "إدارة التطبيقات",
            "هذا القسم يحتاج تطويراً إضافياً."
        )
    }

    // =====================================
    // SETTINGS
    // =====================================

    private fun showSettings() {

        showPage(
            "الإعدادات",
            "إعدادات ORVEX"
        )

        addCard(
            "معلومات الجهاز",
            "عرض بيانات الهاتف",
            "📊"
        ) {
            showDeviceInfo()
        }

        addCard(
            "الأدوات",
            "فتح أدوات التطبيق",
            "🛠"
        ) {
            showTools()
        }

        addCard(
            "المهام",
            "إدارة المهام المحفوظة",
            "📋"
        ) {
            showTasks()
        }

        addCard(
            "حول ORVEX",
            "معلومات الإصدار",
            "ⓘ"
        ) {
            showAbout()
        }
    }

    // =====================================
    // ABOUT
    // =====================================

    private fun showAbout() {

        showPage(
            "حول ORVEX",
            "معلومات التطبيق"
        )

        addCard(
            "ORVEX Smart Control Center",
            "الإصدار 1.1\nتم التطوير باستخدام Kotlin",
            "O/X"
        ) {}

        addInfo("الإصدار", "1.1")
        addInfo("المنصة", "Android")
        addInfo("لغة البرمجة", "Kotlin")
    }

    // =====================================
    // SHARE
    // =====================================

    private fun shareText() {

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "ORVEX Smart Control Center - Version 1.1"
            )
        }

        startActivity(
            Intent.createChooser(intent, "مشاركة ORVEX")
        )
    }

    // =====================================
    // TOAST
    // =====================================

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
