
package com.orvex.app

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {

    // =====================================
    // ORVEX THEME
    // =====================================

    private val bg = Color.rgb(7, 7, 13)
    private val surface = Color.rgb(18, 16, 29)
    private val surface2 = Color.rgb(27, 22, 42)
    private val purple = Color.rgb(160, 75, 255)
    private val purpleDark = Color.rgb(91, 35, 150)
    private val white = Color.WHITE
    private val muted = Color.rgb(165, 155, 190)
    private val green = Color.rgb(70, 220, 155)

    private lateinit var content: LinearLayout

    private var currentPage = "home"

    private val preferences by lazy {
        getSharedPreferences("orvex_data", Context.MODE_PRIVATE)
    }

    private data class TaskItem(
        var title: String,
        var done: Boolean = false
    )

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

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
                Color.rgb(65, 24, 105),
                Color.rgb(27, 17, 45),
                Color.rgb(13, 11, 22)
            )
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), purpleDark)
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

    private fun divider(): View {
        return View(this).apply {
            setBackgroundColor(Color.rgb(48, 39, 65))
        }
    }

    // =====================================
    // ACTIVITY
    // =====================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        showHome()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (currentPage != "home") {
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
        subtitle: String,
        selected: String
    ): LinearLayout {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(25))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = text("‹", 35f, purple, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showHome() }
        }

        header.addView(back, LinearLayout.LayoutParams(dp(42), dp(48)))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        titleBox.addView(text(title, 21f, white, true))
        titleBox.addView(text(subtitle, 11f, muted))

        header.addView(
            titleBox,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        content.addView(header)
        content.addView(divider(), params(height = dp(1), top = 12))

        scroll.addView(content)

        // FIX: ScrollView must occupy the full available width.
        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        root.addView(createNavigation(selected))

        return root
    }

    private fun createNavigation(selected: String): LinearLayout {

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(6), dp(4), dp(6))
            background = rounded(surface, 0)
        }

        val items = listOf(
            Triple("⌂", "الرئيسية", "home"),
            Triple("▦", "الأدوات", "tools"),
            Triple("▣", "التطبيقات", "apps"),
            Triple("⚙", "الإعدادات", "settings"),
            Triple("ⓘ", "حول", "about")
        )

        items.forEach { item ->

            val active = selected == item.third
            val color = if (active) purple else muted

            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(4), dp(2), dp(4))
                isClickable = true
                isFocusable = true
            }

            box.addView(
                text(item.first, 22f, color, true).apply {
                    gravity = Gravity.CENTER
                }
            )

            box.addView(
                text(item.second, 9f, color, active).apply {
                    gravity = Gravity.CENTER
                }
            )

            box.setOnClickListener {
                when (item.third) {
                    "home" -> showHome()
                    "tools" -> showTools()
                    "apps" -> showApplications()
                    "settings" -> showSettings()
                    "about" -> showAbout()
                }
            }

            nav.addView(
                box,
                LinearLayout.LayoutParams(0, dp(54), 1f)
            )
        }

        return nav
    }

    private fun showPage(
        title: String,
        subtitle: String,
        selected: String = "home"
    ) {
        setContentView(createPage(title, subtitle, selected))
    }

    // =====================================
    // COMPONENTS
    // =====================================

    private fun addSection(
        title: String,
        subtitle: String
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        box.addView(text(title, 17f, white, true))
        box.addView(text(subtitle, 11f, muted))

        content.addView(box, params(top = 22))
    }

    private fun addCard(
        title: String,
        description: String,
        icon: String,
        action: () -> Unit
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(surface, 17, Color.rgb(52, 37, 75))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        val iconBox = TextView(this).apply {
            text = icon
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(purple)
            background = rounded(surface2, 13)
        }

        card.addView(iconBox, LinearLayout.LayoutParams(dp(48), dp(48)))

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        labels.addView(text(title, 14f, white, true))
        labels.addView(text(description, 10f, muted))

        card.addView(
            labels,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        card.addView(text("›", 25f, purple, true))

        content.addView(card, params(top = 10))
    }

    private fun addInfo(label: String, value: String) {

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(surface, 13)
        }

        row.addView(text(label, 10f, muted))
        row.addView(
            text(value, 14f, white, true),
            params(top = 4)
        )

        content.addView(row, params(top = 8))
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
            setPadding(dp(3), dp(8), dp(3), dp(8))
        }

        box.addView(
            text(icon, 20f, purple, true).apply {
                gravity = Gravity.CENTER
            }
        )

        box.addView(
            text(value, 10f, white, true).apply {
                gravity = Gravity.CENTER
                maxLines = 1
            }
        )

        box.addView(
            text(label, 9f, muted).apply {
                gravity = Gravity.CENTER
            }
        )

        parent.addView(
            box,
            LinearLayout.LayoutParams(0, dp(75), 1f)
        )
    }

    // =====================================
    // HOME DASHBOARD
    // =====================================

    private fun showHome() {

        currentPage = "home"

        showPage(
            "ORVEX",
            "SMART CONTROL CENTER • VERSION 1.1",
            "home"
        )

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(19), dp(21), dp(19), dp(21))
            background = gradient()
        }

        hero.addView(
            text("O / X", 15f, purple, true)
        )

        hero.addView(
            text("مرحباً بك في ORVEX", 22f, white, true),
            params(top = 8)
        )

        hero.addView(
            text(
                "مركز التحكم الذكي لهاتفك",
                12f,
                muted
            ),
            params(top = 5)
        )

        hero.addView(
            text(
                "تحكم • راقب • نظّم",
                11f,
                Color.rgb(205, 165, 255),
                true
            ),
            params(top = 13)
        )

        content.addView(hero, params(top = 17))

        addSection(
            "حالة الجهاز",
            "قراءة مباشرة للبيانات المتاحة"
        )

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(surface, 17, purpleDark)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        addStat(stats, "🔋", getBattery(), "البطارية")
        addStat(stats, "💾", getStorageUsed(), "المستخدم")
        addStat(stats, "▣", getMemoryAvailable(), "RAM متاح")
        addStat(stats, "OS", Build.VERSION.RELEASE, "Android")

        content.addView(stats, params(top = 12))

        addCard(
            "تحديث البيانات",
            "إعادة قراءة حالة الجهاز",
            "↻"
        ) {
            showHome()
            toast("تم تحديث البيانات")
        }

        addSection(
            "مركز التحكم",
            "الأقسام الرئيسية في ORVEX"
        )

        addCard(
            "معلومات الجهاز",
            "تفاصيل النظام والذاكرة والتخزين والبطارية",
            "▣"
        ) {
            showDeviceInfo()
        }

        addCard(
            "ORVEX AI",
            "واجهة المساعد الذكي",
            "✦"
        ) {
            showAI()
        }

        addCard(
            "مدير الملفات",
            "اختيار وفتح الملفات من الهاتف",
            "▤"
        ) {
            openFiles()
        }

        addCard(
            "إدارة المهام",
            "إضافة وتعديل وإنجاز المهام",
            "✓"
        ) {
            showTasks()
        }

        addCard(
            "الأدوات",
            "مجموعة أدوات الهاتف",
            "⚒"
        ) {
            showTools()
        }
    }

    // =====================================
    // BATTERY
    // =====================================

    private fun batteryIntent() = registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )

    private fun getBattery(): String {
        return try {
            val intent = batteryIntent()
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
            when (
                batteryIntent()?.getIntExtra(
                    BatteryManager.EXTRA_STATUS, -1
                )
            ) {
                BatteryManager.BATTERY_STATUS_CHARGING ->
                    "قيد الشحن"

                BatteryManager.BATTERY_STATUS_FULL ->
                    "ممتلئة"

                BatteryManager.BATTERY_STATUS_DISCHARGING ->
                    "تعمل على البطارية"

                BatteryManager.BATTERY_STATUS_NOT_CHARGING ->
                    "غير قيد الشحن"

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
        val used = (total - free).coerceAtLeast(0L)

        return Triple(total, used, free)
    }

    private fun formatGB(bytes: Long): String {
        return String.format(
            Locale.US,
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
    // RAM
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

        currentPage = "device"

        showPage(
            "معلومات الجهاز",
            "تفاصيل الهاتف والنظام",
            "home"
        )

        addSection("معلومات عامة", "هوية الجهاز")

        addInfo("الشركة المصنعة", Build.MANUFACTURER)
        addInfo("طراز الجهاز", Build.MODEL)
        addInfo("اسم الجهاز", Build.DEVICE)
        addInfo("إصدار Android", Build.VERSION.RELEASE)
        addInfo("مستوى SDK", Build.VERSION.SDK_INT.toString())
        addInfo("إصدار النظام", Build.DISPLAY)

        addSection("الذاكرة", "حالة RAM")

        try {
            val memory = memoryData()

            addInfo("إجمالي RAM", formatGB(memory.totalMem))
            addInfo("المتاح", formatGB(memory.availMem))
            addInfo(
                "المستخدم تقريبياً",
                formatGB(
                    (memory.totalMem - memory.availMem)
                        .coerceAtLeast(0L)
                )
            )
        } catch (e: Exception) {
            addInfo("RAM", "غير متاح")
        }

        addSection("التخزين", "مساحة قسم بيانات الجهاز")

        try {
            val storage = storageData()

            addInfo("الإجمالي", formatGB(storage.first))
            addInfo("المستخدم", formatGB(storage.second))
            addInfo("المتاح", formatGB(storage.third))
        } catch (e: Exception) {
            addInfo("التخزين", "غير متاح")
        }

        addSection("البطارية", "حالة الطاقة")

        addInfo("مستوى البطارية", getBattery())
        addInfo("حالة الشحن", getBatteryStatus())

        addSection("الشاشة", "معلومات العرض")

        try {
            val metrics = resources.displayMetrics

            addInfo(
                "الدقة",
                "${metrics.widthPixels} × ${metrics.heightPixels}"
            )

            addInfo(
                "كثافة الشاشة",
                "${metrics.densityDpi} DPI"
            )
        } catch (e: Exception) {
            addInfo("الشاشة", "غير متاح")
        }

        addCard(
            "تحديث المعلومات",
            "إعادة قراءة بيانات الجهاز",
            "↻"
        ) {
            showDeviceInfo()
        }
    }

    // =====================================
    // ORVEX AI
    // =====================================

    private fun showAI() {

        currentPage = "ai"

        showPage(
            "ORVEX AI",
            "المساعد الذكي",
            "home"
        )

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(19), dp(18), dp(19))
            background = gradient()
        }

        panel.addView(text("✦ ORVEX AI", 23f, white, true))

        panel.addView(
            text(
                "واجهة المساعد الذكي",
                14f,
                Color.rgb(210, 180, 255),
                true
            ),
            params(top = 9)
        )

        panel.addView(
            text(
                "حالة الاتصال: غير متصل",
                12f,
                muted
            ),
            params(top = 12)
        )

        panel.addView(
            text(
                "لم يتم ربط نموذج ذكاء اصطناعي بعد. " +
                    "لن يتم إنشاء إجابات وهمية.",
                12f,
                white
            ),
            params(top = 8)
        )

        content.addView(panel, params(top = 18))

        addSection(
            "قدرات ORVEX AI المخطط لها",
            "سيتم تفعيلها بعد ربط الخادم"
        )

        addInfo("البرمجة", "كتابة الأكواد وشرحها")
        addInfo("تحليل الأخطاء", "اقتراح حلول للمشاكل البرمجية")
        addInfo("المحادثة", "الإجابة عن الأسئلة")
        addInfo("تحليل المعلومات", "تنظيم البيانات والاستنتاج")

        addCard(
            "حالة الاتصال",
            "عرض معلومات ربط خدمة الذكاء الاصطناعي",
            "◎"
        ) {
            AlertDialog.Builder(this)
                .setTitle("ORVEX AI")
                .setMessage(
                    "الخدمة غير مربوطة حالياً.\n\n" +
                        "عند إضافة الاتصال، يجب حفظ مفاتيح API " +
                        "في الخادم وليس داخل تطبيق Android."
                )
                .setPositiveButton("حسناً", null)
                .show()
        }
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

    @Deprecated("Using activity result callback for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            val uri = data?.data

            if (uri != null) {
                toast("تم اختيار الملف")
            }
        }
    }

    // =====================================
    // TOOLS
    // =====================================

    private fun showTools() {

        currentPage = "tools"

        showPage(
            "الأدوات",
            "أدوات ORVEX",
            "tools"
        )

        addSection(
            "أدوات الجهاز",
            "الوصول السريع للخدمات"
        )

        addCard(
            "معلومات الجهاز",
            "عرض تفاصيل الهاتف",
            "▣"
        ) {
            showDeviceInfo()
        }

        addCard(
            "الحاسبة",
            "فتح حاسبة الهاتف",
            "＋"
        ) {
            try {
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_APP_CALCULATOR)

                startActivity(intent)
            } catch (e: Exception) {
                toast("لا توجد حاسبة متاحة")
            }
        }

        addCard(
            "فحص التخزين",
            "عرض المساحة المستخدمة والمتاحة",
            "▤"
        ) {
            showStorageInfo()
        }

        addCard(
            "مشاركة ORVEX",
            "مشاركة معلومات التطبيق",
            "↗"
        ) {
            shareText()
        }

        addCard(
            "إدارة المهام",
            "فتح قائمة المهام",
            "✓"
        ) {
            showTasks()
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
    // TASK STORAGE
    // =====================================

    private fun loadTasks(): MutableList<TaskItem> {

        val result = mutableListOf<TaskItem>()

        return try {
            val json = JSONArray(
                preferences.getString("tasks", "[]") ?: "[]"
            )

            for (i in 0 until json.length()) {

                val item = json.get(i)

                if (item is String) {
                    result.add(TaskItem(item, false))
                } else if (item is JSONObject) {
                    result.add(
                        TaskItem(
                            item.optString("title", ""),
                            item.optBoolean("done", false)
                        )
                    )
                }
            }

            result
        } catch (e: Exception) {
            result
        }
    }

    private fun saveTasks(tasks: List<TaskItem>) {

        val json = JSONArray()

        tasks.forEach { task ->
            val item = JSONObject()
            item.put("title", task.title)
            item.put("done", task.done)
            json.put(item)
        }

        preferences.edit()
            .putString("tasks", json.toString())
            .apply()
    }

    // =====================================
    // TASKS PAGE
    // =====================================

    private fun showTasks() {

        currentPage = "tasks"

        showPage(
            "المهام",
            "إدارة وتنظيم مهامك",
            "home"
        )

        val tasks = loadTasks()
        val completed = tasks.count { it.done }
        val pending = tasks.size - completed

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = gradient()
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(15), dp(8), dp(15))
        }

        addStat(summary, "▤", tasks.size.toString(), "الكل")
        addStat(summary, "✓", completed.toString(), "منجزة")
        addStat(summary, "◷", pending.toString(), "متبقية")

        content.addView(summary, params(top = 17))

        addCard(
            "إضافة مهمة جديدة",
            "إنشاء مهمة وحفظها على الجهاز",
            "+"
        ) {
            editTask()
        }

        addSection(
            "قائمة المهام",
            "${tasks.size} مهمة محفوظة"
        )

        if (tasks.isEmpty()) {

            addInfo(
                "لا توجد مهام حالياً",
                "اضغط على إضافة مهمة لإنشاء أول مهمة."
            )

        } else {

            tasks.forEachIndexed { index, task ->

                val status = if (task.done) {
                    "مكتملة ✓"
                } else {
                    "قيد التنفيذ"
                }

                addCard(
                    task.title,
                    "$status • اضغط لإدارة المهمة",
                    if (task.done) "✓" else "○"
                ) {
                    showTaskOptions(index)
                }
            }
        }
    }

    private fun editTask(index: Int = -1) {

        val tasks = loadTasks()
        val existing = if (index in tasks.indices) {
            tasks[index]
        } else {
            null
        }

        val input = EditText(this).apply {
            hint = "اكتب عنوان المهمة"
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(surface)
            setSingleLine(false)

            if (existing != null) {
                setText(existing.title)
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(
                if (existing == null) "مهمة جديدة"
                else "تعديل المهمة"
            )
            .setView(container)
            .setPositiveButton("حفظ") { _, _ ->

                val value = input.text.toString().trim()

                if (value.isEmpty()) {
                    toast("اكتب عنوان المهمة أولاً")
                    return@setPositiveButton
                }

                val updated = loadTasks()

                if (index in updated.indices) {
                    updated[index].title = value
                } else {
                    updated.add(TaskItem(value))
                }

                saveTasks(updated)
                showTasks()
                toast("تم حفظ المهمة")
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showTaskOptions(index: Int) {

        val tasks = loadTasks()

        if (index !in tasks.indices) {
            showTasks()
            return
        }

        val task = tasks[index]

        val options = arrayOf(
            if (task.done) "إلغاء الإنجاز" else "تحديد كمنجزة",
            "تعديل المهمة",
            "حذف المهمة"
        )

        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setItems(options) { _, selected ->

                when (selected) {

                    0 -> {
                        val updated = loadTasks()

                        if (index in updated.indices) {
                            updated[index].done = !updated[index].done
                            saveTasks(updated)
                            showTasks()
                        }
                    }

                    1 -> editTask(index)

                    2 -> confirmDeleteTask(index)
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun confirmDeleteTask(index: Int) {

        AlertDialog.Builder(this)
            .setTitle("حذف المهمة")
            .setMessage("هل تريد حذف هذه المهمة نهائياً؟")
            .setPositiveButton("حذف") { _, _ ->

                val updated = loadTasks()

                if (index in updated.indices) {
                    updated.removeAt(index)
                    saveTasks(updated)
                    showTasks()
                    toast("تم حذف المهمة")
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    // =====================================
    // APPLICATIONS
    // =====================================

    private fun showApplications() {

        currentPage = "apps"

        showPage(
            "التطبيقات",
            "التطبيقات القابلة للتشغيل",
            "apps"
        )

        addSection(
            "تطبيقات الهاتف",
            "التطبيقات التي يمكن للنظام إظهارها"
        )

        addCard(
            "إعدادات التطبيقات",
            "فتح إعدادات إدارة التطبيقات في Android",
            "⚙"
        ) {
            try {
                startActivity(
                    Intent(
                        android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
                    )
                )
            } catch (e: Exception) {
                toast("تعذر فتح إعدادات التطبيقات")
            }
        }

        val apps = getLaunchableApps()

        addSection(
            "التطبيقات المتاحة",
            "${apps.size} تطبيق"
        )

        if (apps.isEmpty()) {

            addInfo(
                "لا توجد تطبيقات ظاهرة",
                "قد يمنع إصدار Android عرض بعض التطبيقات."
            )

        } else {

            apps.forEach { app ->

                val label = try {
                    app.loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    app.activityInfo.packageName
                }

                val packageName = app.activityInfo.packageName

                addCard(
                    label,
                    packageName,
                    "▣"
                ) {
                    try {
                        val launchIntent =
                            packageManager.getLaunchIntentForPackage(
                                packageName
                            )

                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        } else {
                            toast("لا يمكن تشغيل هذا التطبيق")
                        }
                    } catch (e: Exception) {
                        toast("تعذر تشغيل التطبيق")
                    }
                }
            }
        }
    }

    private fun getLaunchableApps(): List<ResolveInfo> {

        return try {

            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
                .sortedBy {
                    it.loadLabel(packageManager).toString()
                        .lowercase(Locale.getDefault())
                }

        } catch (e: Exception) {
            emptyList()
        }
    }

    // =====================================
    // SETTINGS
    // =====================================

    private fun showSettings() {

        currentPage = "settings"

        showPage(
            "الإعدادات",
            "إعدادات ORVEX",
            "settings"
        )

        addSection(
            "إدارة التطبيق",
            "الوصول إلى أقسام ORVEX"
        )

        addCard(
            "معلومات الجهاز",
            "تفاصيل الهاتف",
            "▣"
        ) {
            showDeviceInfo()
        }

        addCard(
            "المهام",
            "إدارة المهام المحفوظة",
            "✓"
        ) {
            showTasks()
        }

        addCard(
            "الأدوات",
            "أدوات ORVEX",
            "⚒"
        ) {
            showTools()
        }

        addCard(
            "حول ORVEX",
            "معلومات الإصدار والتطبيق",
            "ⓘ"
        ) {
            showAbout()
        }

        addSection(
            "معلومات النظام",
            "إعدادات Android"
        )

        addCard(
            "إعدادات الهاتف",
            "فتح إعدادات النظام",
            "⚙"
        ) {
            try {
                startActivity(
                    Intent(android.provider.Settings.ACTION_SETTINGS)
                )
            } catch (e: Exception) {
                toast("تعذر فتح إعدادات الهاتف")
            }
        }
    }

    // =====================================
    // ABOUT
    // =====================================

    private fun showAbout() {

        currentPage = "about"

        showPage(
            "حول ORVEX",
            "معلومات التطبيق",
            "about"
        )

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(25), dp(20), dp(25))
            background = gradient()
        }

        hero.addView(
            text("O / X", 38f, purple, true).apply {
                gravity = Gravity.CENTER
            }
        )

        hero.addView(
            text("ORVEX", 25f, white, true).apply {
                gravity = Gravity.CENTER
            },
            params(top = 8)
        )

        hero.addView(
            text(
                "SMART CONTROL CENTER",
                10f,
                muted,
                true
            ).apply {
                gravity = Gravity.CENTER
            },
            params(top = 5)
        )

        content.addView(hero, params(top = 20))

        addSection(
            "معلومات الإصدار",
            "بيانات المشروع"
        )

        addInfo("اسم التطبيق", "ORVEX")
        addInfo("الإصدار", "1.1")
        addInfo("المنصة", "Android")
        addInfo("لغة البرمجة", "Kotlin")
        addInfo("الحزمة", "com.orvex.app")

        addSection(
            "مبدأ ORVEX",
            "تطوير واضح وميزات حقيقية"
        )

        addInfo(
            "الخصوصية",
            "لا يتم إرسال بيانات المهام إلى خادم خارجي."
        )

        addInfo(
            "الذكاء الاصطناعي",
            "غير متصل حالياً، بانتظار ربط خدمة فعلية."
        )
    }

    // =====================================
    // SHARE
    // =====================================

    private fun shareText() {

        try {
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

        } catch (e: Exception) {
            toast("تعذرت المشاركة")
        }
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