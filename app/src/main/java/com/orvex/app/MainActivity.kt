
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
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    // =====================================
    // ORVEX 1.3
    // =====================================

    private val bg = Color.rgb(7, 7, 13)
    private val surface = Color.rgb(18, 16, 29)
    private val surface2 = Color.rgb(27, 22, 42)
    private val white = Color.WHITE
    private val muted = Color.rgb(165, 155, 190)

    private var accent = Color.rgb(160, 75, 255)

    private lateinit var content: LinearLayout
    private var currentPage = "home"
    private var drawerOverlay: FrameLayout? = null
    private var drawerOpen = false

    private val preferences by lazy {
        getSharedPreferences("orvex_data", Context.MODE_PRIVATE)
    }

    private data class TaskItem(
        var title: String,
        var done: Boolean = false
    )

    private data class Diagnostic(
        val title: String,
        val details: String,
        val severity: Int
    )

    // =====================================
    // BASIC HELPERS
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
            if (stroke != 0) setStroke(dp(1), stroke)
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
            setStroke(dp(1), accent)
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

    private fun divider(): View =
        View(this).apply {
            setBackgroundColor(Color.rgb(48, 39, 65))
        }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun safeStart(intent: Intent, error: String) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            toast(error)
        }
    }

    private fun formatGB(bytes: Long): String {
        return String.format(
            Locale.US,
            "%.2f GB",
            bytes.toDouble() / 1_073_741_824.0
        )
    }

    private fun percentage(used: Long, total: Long): Int {
        if (total <= 0L) return 0
        return ((used.toDouble() / total.toDouble()) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    // =====================================
    // ACTIVITY
    // =====================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accent = preferences.getInt(
            "accent_color",
            Color.rgb(160, 75, 255)
        )

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
        if (drawerOpen) {
            closeDrawer()
            return
        }

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
        subtitle: String
    ): FrameLayout {

        val root = FrameLayout(this).apply {
            setBackgroundColor(bg)
        }

        val page = LinearLayout(this).apply {
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
            setPadding(dp(18), dp(16), dp(18), dp(28))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        titleBox.addView(text(title, 23f, white, true))
        titleBox.addView(
            text(subtitle, 10f, muted),
            params(top = 3)
        )

        header.addView(
            titleBox,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = rounded(surface2, 13)
            isClickable = true
            isFocusable = true
            setOnClickListener { openDrawer() }
        }

        header.addView(
            menuButton,
            LinearLayout.LayoutParams(dp(46), dp(44))
        )

        content.addView(header)
        content.addView(divider(), params(height = dp(1), top = 13))

        scroll.addView(content)

        page.addView(
            scroll,
            LinearLayout.LayoutParams(-1, -1)
        )

        root.addView(
            page,
            FrameLayout.LayoutParams(-1, -1)
        )

        drawerOverlay = null
        drawerOpen = false

        return root
    }

    private fun showPage(
        title: String,
        subtitle: String
    ) {
        setContentView(createPage(title, subtitle))
    }

    // =====================================
    // RIGHT DRAWER
    // =====================================

    private fun openDrawer() {
        if (drawerOpen) return

        val root = window.decorView.findViewById<FrameLayout>(
            android.R.id.content
        )

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val dim = View(this).apply {
            setBackgroundColor(Color.argb(175, 0, 0, 0))
            setOnClickListener { closeDrawer() }
        }

        overlay.addView(
            dim,
            FrameLayout.LayoutParams(-1, -1)
        )

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(25), dp(17), dp(20))
            background = rounded(surface, 0, accent)
        }

        val panelParams = FrameLayout.LayoutParams(
            dp(290),
            -1,
            Gravity.END
        )

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        heading.addView(text("ORVEX", 25f, white, true))
        heading.addView(
            text("OS CONTROL CENTER • 1.3", 10f, accent, true),
            params(top = 4)
        )

        panel.addView(heading)
        panel.addView(divider(), params(height = dp(1), top = 18))

        val menuScroll = ScrollView(this)
        val menuItems = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val items = listOf(
            Triple("⌂", "الرئيسية", "home"),
            Triple("◷", "مراقبة النظام", "monitor"),
            Triple("◇", "تشخيص الجهاز", "diagnostics"),
            Triple("✦", "ORVEX Intelligence", "intelligence"),
            Triple("▣", "مركز الأداء", "performance"),
            Triple("▦", "التطبيقات", "apps"),
            Triple("▤", "الملفات", "files"),
            Triple("✓", "إدارة المهام", "tasks"),
            Triple("⚒", "الأدوات", "tools"),
            Triple("≡", "سجل النشاط", "logs"),
            Triple("⚙", "الإعدادات", "settings"),
            Triple("ⓘ", "حول ORVEX", "about")
        )

        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(13), dp(12), dp(13))
                background = rounded(
                    if (currentPage == item.third) surface2 else surface,
                    12
                )
                isClickable = true
                isFocusable = true
            }

            row.addView(
                text(item.first, 20f, accent, true),
                LinearLayout.LayoutParams(dp(35), -2)
            )

            row.addView(
                text(
                    item.second,
                    13f,
                    if (currentPage == item.third) accent else white,
                    currentPage == item.third
                ),
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            row.setOnClickListener {
                closeDrawer()
                when (item.third) {
                    "home" -> showHome()
                    "monitor" -> showMonitor()
                    "diagnostics" -> showDiagnostics()
                    "intelligence" -> showIntelligence()
                    "performance" -> showPerformance()
                    "apps" -> showApplications()
                    "files" -> openFiles()
                    "tasks" -> showTasks()
                    "tools" -> showTools()
                    "logs" -> showActivityLog()
                    "settings" -> showSettings()
                    "about" -> showAbout()
                }
            }

            menuItems.addView(row, params(top = 7))
        }

        menuScroll.addView(menuItems)

        panel.addView(
            menuScroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        panel.addView(divider(), params(height = dp(1), top = 10))
        panel.addView(
            text("ORVEX 1.3 • SYSTEM MANAGEMENT", 9f, muted),
            params(top = 12)
        )

        overlay.addView(panel, panelParams)
        root.addView(
            overlay,
            FrameLayout.LayoutParams(-1, -1)
        )

        drawerOverlay = overlay
        drawerOpen = true
    }

    private fun closeDrawer() {
        if (!drawerOpen) return

        val root = window.decorView.findViewById<FrameLayout>(
            android.R.id.content
        )

        drawerOverlay?.let { root.removeView(it) }

        drawerOverlay = null
        drawerOpen = false
    }

    // =====================================
    // COMPONENTS
    // =====================================

    private fun addSection(title: String, subtitle: String) {
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
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = rounded(surface2, 13)
        }

        card.addView(iconBox, LinearLayout.LayoutParams(dp(46), dp(46)))

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        labels.addView(text(title, 14f, white, true))
        labels.addView(text(description, 10f, muted))

        card.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(text("›", 25f, accent, true))

        content.addView(card, params(top = 10))
    }

    private fun addInfo(label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(surface, 13)
        }

        row.addView(text(label, 10f, muted))
        row.addView(text(value, 14f, white, true), params(top = 4))

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
            text(icon, 17f, accent, true).apply {
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

        parent.addView(box, LinearLayout.LayoutParams(0, dp(75), 1f))
    }

    private fun addProgress(
        title: String,
        value: Int,
        details: String
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            background = rounded(surface, 13)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        top.addView(text(title, 12f, white, true),
            LinearLayout.LayoutParams(0, -2, 1f))

        top.addView(text("$value%", 12f, accent, true))

        box.addView(top)

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            this.progress = value.coerceIn(0, 100)
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList =
                android.content.res.ColorStateList.valueOf(surface2)
        }

        box.addView(
            progress,
            params(height = dp(7), top = 9)
        )

        box.addView(text(details, 10f, muted), params(top = 7))

        content.addView(box, params(top = 9))
    }

    // =====================================
    // ACTIVITY LOG
    // =====================================

    private fun logActivity(message: String) {
        try {
            val logs = JSONArray(
                preferences.getString("activity_log", "[]") ?: "[]"
            )

            val item = JSONObject().apply {
                put("message", message)
                put(
                    "time",
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                )
            }

            logs.put(item)

            val limited = JSONArray()
            val start = (logs.length() - 100).coerceAtLeast(0)

            for (i in start until logs.length()) {
                limited.put(logs.getJSONObject(i))
            }

            preferences.edit()
                .putString("activity_log", limited.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun loadLogs(): JSONArray {
        return try {
            JSONArray(
                preferences.getString("activity_log", "[]") ?: "[]"
            )
        } catch (_: Exception) {
            JSONArray()
        }
    }

    // =====================================
    // BATTERY
    // =====================================

    private fun batteryIntent() = registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )

    private fun batteryLevel(): Int {
        return try {
            val intent = batteryIntent()
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level >= 0 && scale > 0) level * 100 / scale else -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun getBattery(): String {
        val level = batteryLevel()
        return if (level >= 0) "$level%" else "غير متاح"
    }

    private fun getBatteryStatus(): String {
        return try {
            when (
                batteryIntent()?.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    -1
                )
            ) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "قيد الشحن"
                BatteryManager.BATTERY_STATUS_FULL -> "ممتلئة"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "تعمل على البطارية"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "غير قيد الشحن"
                else -> "غير معروف"
            }
        } catch (_: Exception) {
            "غير متاح"
        }
    }

    private fun batteryTemperature(): String {
        return try {
            val temp = batteryIntent()?.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                -1
            ) ?: -1

            if (temp >= 0) {
                String.format(Locale.US, "%.1f °C", temp / 10.0)
            } else {
                "غير متاح"
            }
        } catch (_: Exception) {
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

    private fun getStorageUsed(): String {
        return try {
            formatGB(storageData().second)
        } catch (_: Exception) {
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
        } catch (_: Exception) {
            "غير متاح"
        }
    }

    private fun processMemoryMB(): Long {
        return try {
            Debug.getPss().toLong() / 1024L
        } catch (_: Exception) {
            0L
        }
    }

    // =====================================
    // HOME
    // =====================================

    private fun showHome() {
        currentPage = "home"
        showPage("ORVEX", "OS EVOLUTION • VERSION 1.3")

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(19), dp(21), dp(19), dp(21))
            background = gradient()
        }

        hero.addView(text("ORVEX OS", 14f, accent, true))
        hero.addView(
            text("مركز إدارة الهاتف", 23f, white, true),
            params(top = 8)
        )
        hero.addView(
            text(
                "راقب جهازك وشخّص حالته ونظّم أدواتك من مكان واحد.",
                12f,
                muted
            ),
            params(top = 6)
        )
        hero.addView(
            text("SYSTEM CONTROL CENTER", 10f, accent, true),
            params(top = 14)
        )

        content.addView(hero, params(top = 17))

        addSection("حالة الجهاز", "قراءات Android المتاحة حالياً")

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(surface, 17, accent)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        addStat(stats, "BAT", getBattery(), "البطارية")
        addStat(stats, "RAM", getMemoryAvailable(), "RAM متاح")
        addStat(stats, "ROM", getStorageUsed(), "المستخدم")
        addStat(stats, "OS", Build.VERSION.RELEASE ?: "?", "Android")

        content.addView(stats, params(top = 12))

        addCard("مراقبة النظام", "RAM والبطارية والتخزين", "◷") {
            showMonitor()
        }

        addCard("تشخيص الجهاز", "تحليل القراءات والتنبيهات", "◇") {
            showDiagnostics()
        }

        addCard("ORVEX Intelligence", "تحليل محلي وتوصيات مبنية على القراءات", "✦") {
            showIntelligence()
        }

        addCard("مركز الأداء", "معلومات الذاكرة واستخدام عملية ORVEX", "▣") {
            showPerformance()
        }

        addCard("معلومات الجهاز", "تفاصيل الهاتف وإصدار النظام", "▤") {
            showDeviceInfo()
        }

        addCard("مركز الأمان", "مراجعة الأذونات وإعدادات الحماية", "◇") {
            showSecurity()
        }

        addCard("مدير التطبيقات", "البحث عن التطبيقات القابلة للتشغيل", "▦") {
            showApplications()
        }

        addCard("مدير الملفات", "اختيار ملف من الهاتف", "▤") {
            openFiles()
        }

        addCard("إدارة المهام", "تنظيم مهامك وحفظها", "✓") {
            showTasks()
        }

        addCard("الأدوات", "أدوات النظام والمشاركة", "⚒") {
            showTools()
        }

        addCard("سجل النشاط", "العمليات المسجلة داخل ORVEX", "≡") {
            showActivityLog()
        }
    }

    // =====================================
    // SYSTEM MONITOR
    // =====================================

    private fun showMonitor() {
        currentPage = "monitor"
        showPage("مراقبة النظام", "SYSTEM MONITOR • VERSION 1.3")

        addSection("مراقبة الذاكرة", "قراءة RAM المتاحة للنظام")

        try {
            val memory = memoryData()
            val used = (memory.totalMem - memory.availMem).coerceAtLeast(0L)
            val percent = percentage(used, memory.totalMem)

            addProgress(
                "استخدام RAM التقريبي",
                percent,
                "${formatGB(used)} مستخدم من ${formatGB(memory.totalMem)}"
            )

            addInfo("إجمالي RAM", formatGB(memory.totalMem))
            addInfo("RAM المستخدم تقريبياً", formatGB(used))
            addInfo("RAM المتاح", formatGB(memory.availMem))
            addInfo("حد انخفاض الذاكرة للنظام", formatGB(memory.threshold))
            addInfo(
                "حالة الذاكرة",
                if (memory.lowMemory) "النظام يشير إلى انخفاض الذاكرة"
                else "لا توجد إشارة انخفاض ذاكرة من Android"
            )
        } catch (_: Exception) {
            addInfo("RAM", "تعذر قراءة البيانات")
        }

        addSection("مراقبة التخزين", "قسم بيانات الجهاز")

        try {
            val storage = storageData()
            val percent = percentage(storage.second, storage.first)

            addProgress(
                "استخدام التخزين",
                percent,
                "${formatGB(storage.second)} مستخدم من ${formatGB(storage.first)}"
            )

            addInfo("المساحة الكلية", formatGB(storage.first))
            addInfo("المساحة المستخدمة", formatGB(storage.second))
            addInfo("المساحة المتاحة", formatGB(storage.third))
        } catch (_: Exception) {
            addInfo("التخزين", "تعذر قراءة البيانات")
        }

        addSection("مراقبة البطارية", "حالة الطاقة")

        addInfo("مستوى البطارية", getBattery())
        addInfo("حالة الشحن", getBatteryStatus())
        addInfo("حرارة البطارية", batteryTemperature())

        addCard("تحديث القراءات", "إعادة قراءة معلومات النظام", "↻") {
            showMonitor()
        }

        addInfo(
            "حدود القياس",
            "القراءات تؤخذ عند فتح الصفحة أو تحديثها، وليست مراقبة مستمرة في الخلفية."
        )
    }

    // =====================================
    // DIAGNOSTICS
    // =====================================

    private fun collectDiagnostics(): List<Diagnostic> {
        val results = mutableListOf<Diagnostic>()

        try {
            val memory = memoryData()
            val ramPercent = percentage(
                (memory.totalMem - memory.availMem).coerceAtLeast(0L),
                memory.totalMem
            )

            when {
                memory.lowMemory -> results.add(
                    Diagnostic(
                        "انخفاض الذاكرة",
                        "Android أبلغ أن الذاكرة المتاحة منخفضة.",
                        2
                    )
                )

                ramPercent >= 90 -> results.add(
                    Diagnostic(
                        "استخدام RAM مرتفع",
                        "الاستخدام التقريبي للذاكرة وصل إلى $ramPercent%.",
                        1
                    )
                )

                else -> results.add(
                    Diagnostic(
                        "الذاكرة",
                        "لا توجد إشارة انخفاض ذاكرة من Android. الاستخدام التقريبي $ramPercent%.",
                        0
                    )
                )
            }
        } catch (_: Exception) {
            results.add(
                Diagnostic("الذاكرة", "تعذر قراءة معلومات RAM.", 1)
            )
        }

        try {
            val storage = storageData()
            val storagePercent = percentage(storage.second, storage.first)

            when {
                storagePercent >= 95 -> results.add(
                    Diagnostic(
                        "التخزين شبه ممتلئ",
                        "المساحة المستخدمة $storagePercent%. المساحة المتبقية قليلة.",
                        2
                    )
                )

                storagePercent >= 85 -> results.add(
                    Diagnostic(
                        "مساحة التخزين مرتفعة",
                        "المستخدم من التخزين $storagePercent%.",
                        1
                    )
                )

                else -> results.add(
                    Diagnostic(
                        "التخزين",
                        "المساحة المتاحة تبدو كافية وفق القراءة الحالية.",
                        0
                    )
                )
            }
        } catch (_: Exception) {
            results.add(
                Diagnostic("التخزين", "تعذر قراءة معلومات التخزين.", 1)
            )
        }

        val battery = batteryLevel()

        when {
            battery < 0 -> results.add(
                Diagnostic("البطارية", "تعذر قراءة مستوى البطارية.", 1)
            )

            battery <= 10 -> results.add(
                Diagnostic(
                    "البطارية منخفضة جداً",
                    "مستوى البطارية $battery%. يُنصح بتوصيل الشاحن عند الحاجة.",
                    2
                )
            )

            battery <= 20 -> results.add(
                Diagnostic(
                    "البطارية منخفضة",
                    "مستوى البطارية $battery%.",
                    1
                )
            )

            else -> results.add(
                Diagnostic(
                    "البطارية",
                    "مستوى البطارية $battery%.",
                    0
                )
            )
        }

        return results
    }

    private fun severityLabel(severity: Int): String {
        return when (severity) {
            2 -> "تنبيه مهم"
            1 -> "ملاحظة"
            else -> "طبيعي"
        }
    }

    private fun severityColor(severity: Int): Int {
        return when (severity) {
            2 -> Color.rgb(245, 75, 85)
            1 -> Color.rgb(245, 175, 65)
            else -> Color.rgb(65, 200, 130)
        }
    }

    private fun showDiagnostics() {
        currentPage = "diagnostics"
        showPage("تشخيص الجهاز", "DEVICE DIAGNOSTICS")

        val results = collectDiagnostics()
        val important = results.count { it.severity == 2 }
        val notices = results.count { it.severity == 1 }

        addSection("نتيجة الفحص", "تحليل القراءات المتاحة للتطبيق")

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = gradient()
        }

        summary.addView(
            text(
                if (important > 0) "توجد تنبيهات تحتاج الانتباه"
                else if (notices > 0) "توجد ملاحظات على الجهاز"
                else "القراءات ضمن النطاق المعتاد",
                16f,
                white,
                true
            )
        )

        summary.addView(
            text(
                "تنبيهات مهمة: $important    ملاحظات: $notices",
                11f,
                muted
            ),
            params(top = 8)
        )

        content.addView(summary, params(top = 12))

        results.forEach { result ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(13), dp(14), dp(13))
                background = rounded(
                    surface,
                    14,
                    severityColor(result.severity)
                )
            }

            card.addView(
                text(
                    "${result.title} • ${severityLabel(result.severity)}",
                    13f,
                    severityColor(result.severity),
                    true
                )
            )

            card.addView(
                text(result.details, 11f, muted),
                params(top = 7)
            )

            content.addView(card, params(top = 9))
        }

        addCard("إعادة الفحص", "قراءة أحدث بيانات الجهاز", "↻") {
            showDiagnostics()
        }

        addInfo(
            "نطاق التشخيص",
            "هذا فحص إرشادي يعتمد على RAM والتخزين والبطارية فقط. لا يفحص الفيروسات ولا يحلل جميع التطبيقات أو مكونات الهاتف."
        )
    }

    // =====================================
    // ORVEX INTELLIGENCE
    // =====================================

    private fun intelligenceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        try {
            val memory = memoryData()
            val used = (memory.totalMem - memory.availMem).coerceAtLeast(0L)
            val percent = percentage(used, memory.totalMem)

            if (memory.lowMemory || percent >= 90) {
                recommendations.add(
                    "الذاكرة مرتفعة الاستخدام. راجع التطبيقات المفتوحة وأغلق ما لا تحتاجه يدوياً."
                )
            } else {
                recommendations.add(
                    "قراءة RAM لا تشير حالياً إلى ضغط شديد وفق المعلومات التي يوفرها Android."
                )
            }
        } catch (_: Exception) {
            recommendations.add("تعذر تحليل الذاكرة حالياً.")
        }

        try {
            val storage = storageData()
            val percent = percentage(storage.second, storage.first)

            if (percent >= 90) {
                recommendations.add(
                    "التخزين مرتفع الاستخدام. راجع الملفات الكبيرة واحذف ما لم تعد تحتاجه."
                )
            } else {
                recommendations.add(
                    "مساحة التخزين ليست ضمن مستوى التنبيه المرتفع في القراءة الحالية."
                )
            }
        } catch (_: Exception) {
            recommendations.add("تعذر تحليل التخزين حالياً.")
        }

        val battery = batteryLevel()

        when {
            battery < 0 -> recommendations.add(
                "تعذر الحصول على مستوى البطارية."
            )

            battery <= 20 -> recommendations.add(
                "البطارية منخفضة. يفضّل توصيل الشاحن عند الحاجة."
            )

            else -> recommendations.add(
                "مستوى البطارية الحالي $battery%."
            )
        }

        return recommendations
    }

    private fun showIntelligence() {
        currentPage = "intelligence"
        showPage("ORVEX Intelligence", "LOCAL DEVICE ANALYSIS")

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(19), dp(18), dp(19))
            background = gradient()
        }

        panel.addView(text("ORVEX INTELLIGENCE", 18f, white, true))
        panel.addView(
            text("تحليل محلي لبيانات الجهاز", 13f, accent, true),
            params(top = 8)
        )
        panel.addView(
            text(
                "هذه الميزة تستخدم قواعد تحليل محلية ولا تتصل بنموذج ذكاء اصطناعي خارجي.",
                11f,
                muted
            ),
            params(top = 9)
        )

        content.addView(panel, params(top = 17))

        addSection("التوصيات", "مقترحات بناءً على القراءات الحالية")

        intelligenceRecommendations().forEachIndexed { index, recommendation ->
            addInfo("تحليل ${index + 1}", recommendation)
        }

        addCard(
            "تحديث التحليل",
            "إعادة قراءة بيانات الجهاز وتوليد التوصيات",
            "↻"
        ) {
            showIntelligence()
        }

        addInfo(
            "الشفافية",
            "لا يوجد نموذج AI متصل، ولا يتم إرسال بيانات الجهاز إلى خادم خارجي."
        )
    }

    // =====================================
    // PERFORMANCE CENTER
    // =====================================

    private fun showPerformance() {
        currentPage = "performance"
        showPage("مركز الأداء", "PERFORMANCE CENTER")

        addSection("استخدام الذاكرة", "قراءات النظام وعملية ORVEX")

        try {
            val memory = memoryData()
            val used = (memory.totalMem - memory.availMem).coerceAtLeast(0L)
            val percent = percentage(used, memory.totalMem)

            addProgress(
                "استخدام RAM التقريبي للنظام",
                percent,
                "${formatGB(used)} من ${formatGB(memory.totalMem)}"
            )

            addInfo("RAM المتاح", formatGB(memory.availMem))
            addInfo("ذاكرة عملية ORVEX التقريبية", "${processMemoryMB()} MB")
            addInfo("حالة الذاكرة", if (memory.lowMemory) "منخفضة حسب Android" else "طبيعية حسب Android")
        } catch (_: Exception) {
            addInfo("الذاكرة", "تعذر قراءة البيانات")
        }

        addSection("التخزين", "حالة مساحة البيانات")

        try {
            val storage = storageData()
            addProgress(
                "استخدام التخزين",
                percentage(storage.second, storage.first),
                "${formatGB(storage.second)} مستخدم"
            )
            addInfo("المتاح", formatGB(storage.third))
        } catch (_: Exception) {
            addInfo("التخزين", "غير متاح")
        }

        addSection("معلومات الطاقة", "حالة البطارية")

        addInfo("البطارية", getBattery())
        addInfo("الشحن", getBatteryStatus())
        addInfo("حرارة البطارية", batteryTemperature())

        addCard("تحديث الأداء", "تحديث القراءات الحالية", "↻") {
            showPerformance()
        }

        addInfo(
            "ملاحظة تقنية",
            "لا يملك التطبيق صلاحية قياس FPS لجميع التطبيقات أو تغيير تردد المعالج أو فرض إغلاق التطبيقات الأخرى. هذه الصفحة تعرض فقط البيانات المتاحة للتطبيق."
        )
    }

    // =====================================
    // DEVICE INFORMATION
    // =====================================

    private fun showDeviceInfo() {
        currentPage = "device"
        showPage("معلومات الجهاز", "DEVICE INFORMATION")

        addSection("معلومات عامة", "هوية الجهاز")

        addInfo("الشركة المصنعة", Build.MANUFACTURER ?: "غير معروف")
        addInfo("طراز الجهاز", Build.MODEL ?: "غير معروف")
        addInfo("اسم الجهاز", Build.DEVICE ?: "غير معروف")
        addInfo("إصدار Android", Build.VERSION.RELEASE ?: "غير معروف")
        addInfo("مستوى SDK", Build.VERSION.SDK_INT.toString())
        addInfo("إصدار النظام", Build.DISPLAY ?: "غير معروف")

        addSection("الذاكرة", "RAM")

        try {
            val memory = memoryData()
            addInfo("إجمالي RAM", formatGB(memory.totalMem))
            addInfo("المتاح", formatGB(memory.availMem))
            addInfo(
                "المستخدم تقريبياً",
                formatGB((memory.totalMem - memory.availMem).coerceAtLeast(0L))
            )
        } catch (_: Exception) {
            addInfo("RAM", "غير متاح")
        }

        addSection("التخزين", "مساحة قسم بيانات الجهاز")

        try {
            val storage = storageData()
            addInfo("الإجمالي", formatGB(storage.first))
            addInfo("المستخدم", formatGB(storage.second))
            addInfo("المتاح", formatGB(storage.third))
        } catch (_: Exception) {
            addInfo("التخزين", "غير متاح")
        }

        addSection("البطارية", "حالة الطاقة")
        addInfo("مستوى البطارية", getBattery())
        addInfo("حالة الشحن", getBatteryStatus())
        addInfo("حرارة البطارية", batteryTemperature())

        addSection("الشاشة", "معلومات العرض")

        try {
            val metrics = resources.displayMetrics
            addInfo(
                "الدقة",
                "${metrics.widthPixels} × ${metrics.heightPixels}"
            )
            addInfo("كثافة الشاشة", "${metrics.densityDpi} DPI")
        } catch (_: Exception) {
            addInfo("الشاشة", "غير متاح")
        }

        addCard("تحديث المعلومات", "إعادة قراءة بيانات الجهاز", "↻") {
            showDeviceInfo()
        }
    }

    // =====================================
    // SECURITY CENTER
    // =====================================

    private fun showSecurity() {
        currentPage = "security"
        showPage("مركز الأمان", "SECURITY CENTER")

        addSection("حالة الحماية", "معلومات الأذونات والخصوصية")

        addInfo("حالة ORVEX", "يعمل ضمن صلاحيات Android العادية")
        addInfo("صلاحيات التطبيق", "لا يطلب التطبيق صلاحيات تخزين عامة أو تحكماً كاملاً بالنظام.")
        addInfo("خصوصية المهام", "المهام محفوظة محلياً على الجهاز.")
        addInfo("الاتصال الخارجي", "لا يوجد اتصال بخادم ذكاء اصطناعي.")

        addSection("إعدادات الحماية", "اختصارات Android")

        addCard("أذونات ORVEX", "عرض إعدادات التطبيق", "◇") {
            safeStart(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                ),
                "تعذر فتح إعدادات الأذونات"
            )
        }

        addCard("إعدادات الأمان", "فتح صفحة الأمان في Android", "⚙") {
            safeStart(
                Intent(Settings.ACTION_SECURITY_SETTINGS),
                "تعذر فتح إعدادات الأمان"
            )
        }

        addCard("إعدادات التطبيقات", "إدارة التطبيقات من النظام", "▣") {
            safeStart(
                Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),
                "تعذر فتح إعدادات التطبيقات"
            )
        }

        addInfo(
            "حدود الفحص",
            "لا يستطيع ORVEX فحص جميع التطبيقات أو تغيير أذوناتها بصمت. إدارة الأذونات النهائية تبقى ضمن Android."
        )
    }

    // =====================================
    // FILE CENTER
    // =====================================

    private fun openFiles() {
        currentPage = "files"

        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }

            startActivityForResult(intent, 100)
        } catch (_: Exception) {
            toast("تعذر فتح مدير الملفات")
        }
    }

    @Deprecated("Using activity result callback for compatibility")
    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            try {
                val flags = data.flags and
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                contentResolver.takePersistableUriPermission(uri, flags)

                preferences.edit()
                    .putString("last_file_uri", uri.toString())
                    .apply()

                logActivity("تم اختيار ملف وحفظ صلاحية الوصول")
                toast("تم اختيار الملف")
            } catch (_: Exception) {
                preferences.edit()
                    .putString("last_file_uri", uri.toString())
                    .apply()

                logActivity("تم اختيار ملف")
                toast("تم اختيار الملف")
            }
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
                    result.add(TaskItem(item))
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
        } catch (_: Exception) {
            result
        }
    }

    private fun saveTasks(tasks: List<TaskItem>) {
        val json = JSONArray()

        tasks.forEach { task ->
            json.put(
                JSONObject().apply {
                    put("title", task.title)
                    put("done", task.done)
                }
            )
        }

        preferences.edit()
            .putString("tasks", json.toString())
            .apply()
    }

    // =====================================
    // TASK CENTER
    // =====================================

    private fun showTasks() {
        currentPage = "tasks"
        showPage("المهام", "TASK CENTER")

        val tasks = loadTasks()
        val completed = tasks.count { it.done }
        val pending = tasks.size - completed

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = gradient()
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(15), dp(8), dp(15))
        }

        addStat(summary, "ALL", tasks.size.toString(), "الكل")
        addStat(summary, "OK", completed.toString(), "منجزة")
        addStat(summary, "WAIT", pending.toString(), "متبقية")

        content.addView(summary, params(top = 17))

        addCard("إضافة مهمة جديدة", "إنشاء مهمة وحفظها على الجهاز", "+") {
            editTask()
        }

        addSection("قائمة المهام", "${tasks.size} مهمة محفوظة")

        if (tasks.isEmpty()) {
            addInfo("لا توجد مهام حالياً", "اضغط على إضافة مهمة لإنشاء أول مهمة.")
        } else {
            tasks.forEachIndexed { index, task ->
                addCard(
                    task.title,
                    if (task.done) "مكتملة • اضغط لإدارة المهمة"
                    else "قيد التنفيذ • اضغط لإدارة المهمة",
                    if (task.done) "✓" else "○"
                ) {
                    showTaskOptions(index)
                }
            }
        }
    }

    private fun editTask(index: Int = -1) {
        val tasks = loadTasks()
        val existing = tasks.getOrNull(index)

        val input = EditText(this).apply {
            hint = "اكتب عنوان المهمة"
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(surface)
            setText(existing?.title ?: "")
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(input)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "مهمة جديدة" else "تعديل المهمة")
            .setView(container)
            .setPositiveButton("حفظ", null)
            .setNegativeButton("إلغاء", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text.toString().trim()

                if (value.isEmpty()) {
                    input.error = "اكتب عنوان المهمة"
                    return@setOnClickListener
                }

                val updated = loadTasks()

                if (index in updated.indices) {
                    updated[index].title = value
                } else {
                    updated.add(TaskItem(value))
                }

                saveTasks(updated)
                logActivity(if (existing == null) "إضافة مهمة" else "تعديل مهمة")

                dialog.dismiss()
                showTasks()
                toast("تم حفظ المهمة")
            }
        }

        dialog.show()
    }

    private fun showTaskOptions(index: Int) {
        val tasks = loadTasks()
        val task = tasks.getOrNull(index) ?: return

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
                            logActivity("تغيير حالة مهمة")
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
                    logActivity("حذف مهمة")
                    showTasks()
                    toast("تم حذف المهمة")
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    // =====================================
    // APPLICATION MANAGER
    // =====================================

    private fun showApplications() {
        currentPage = "apps"
        showPage("التطبيقات", "APPLICATION MANAGER")

        addSection("إدارة التطبيقات", "إعدادات النظام والبحث")

        addCard("إعدادات التطبيقات", "فتح إدارة التطبيقات في Android", "⚙") {
            safeStart(
                Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),
                "تعذر فتح إعدادات التطبيقات"
            )
        }

        val search = EditText(this).apply {
            hint = "ابحث عن تطبيق..."
            setTextColor(white)
            setHintTextColor(muted)
            singleLine = true
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(surface, 13)
        }

        content.addView(search, params(top = 14))

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(listCon