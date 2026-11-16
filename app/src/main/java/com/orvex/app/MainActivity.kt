
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
import android.view.Gravity
import android.widget.*
 
class MainActivity : Activity() {

    // =====================================
    // ORVEX THEME
    // =====================================

    private val bg = Color.rgb(7, 7, 13)
    private val surface = Color.rgb(17, 15, 27)
    private val purple = Color.rgb(151, 45, 255)
    private val muted = Color.rgb(165, 155, 190)
    private val white = Color.WHITE

    private lateinit var content: LinearLayout

    // =====================================
    // FEATURE MODEL
    // =====================================

    private data class FeatureCard(
        val icon: String,
        val title: String,
        val description: String,
        val action: () -> Unit
    )

    // =====================================
    // DP
    // =====================================

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // =====================================
    // BACKGROUND
    // =====================================

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
                Color.rgb(43, 12, 76),
                Color.rgb(15, 10, 29),
                Color.rgb(9, 8, 17)
            )
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), purple)
        }
    }

    // =====================================
    // TEXT
    // =====================================

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

    // =====================================
    // SECTION
    // =====================================

    private fun section(
        title: String,
        subtitle: String
    ): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                text(title, 19f, white, true)
            )

            addView(
                text(subtitle, 12f, muted)
            )
        }
    }

    // =====================================
    // CREATE ACTIVITY
    // =====================================

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
    // HOME
    // =====================================

    private fun showHome() {

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
            setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(20)
            )
        }

        // HEADER

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val menu = text("☰", 27f, white, true).apply {
            gravity = Gravity.CENTER
        }

        header.addView(
            menu,
            LinearLayout.LayoutParams(dp(45), dp(50))
        )

        val branding = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val logoRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
        }

        logoRow.addView(
            text("ORVE", 27f, white, true)
        )

        logoRow.addView(
            text("X", 27f, purple, true)
        )

        branding.addView(logoRow)

        branding.addView(
            text("SMART CONTROL CENTER", 9f, muted)
        )

        header.addView(
            branding,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val settings = text("⚙", 25f, purple, true).apply {
            gravity = Gravity.CENTER
        }

        header.addView(
            settings,
            LinearLayout.LayoutParams(dp(45), dp(50))
        )

        settings.setOnClickListener {
            showSettings()
        }

        menu.setOnClickListener {
            showSettings()
        }

        content.addView(header)

        // HERO

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(20),
                dp(22),
                dp(20),
                dp(22)
            )
            background = gradient()
        }

        hero.addView(
            text(
                "مرحباً بك في ORVEX",
                23f,
                white,
                true
            )
        )

        val description = text(
            "مركز التحكم الذكي لهاتفك",
            14f,
            muted
        )

        val descParams = LinearLayout.LayoutParams(-1, -2)
        descParams.topMargin = dp(8)

        hero.addView(description, descParams)

        val slogan = text(
            "كل ما تحتاجه في مكان واحد",
            13f,
            Color.rgb(205, 160, 255),
            true
        )

        val sloganParams = LinearLayout.LayoutParams(-1, -2)
        sloganParams.topMargin = dp(18)

        hero.addView(slogan, sloganParams)

        val heroParams = LinearLayout.LayoutParams(-1, -2)
        heroParams.topMargin = dp(25)

        content.addView(hero, heroParams)

        // DEVICE INFORMATION

        addSectionTitle(
            "معلومات الجهاز",
            "إحصائيات هاتفك الحالية"
        )

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(
                surface,
                18,
                0xFF44216A.toInt()
            )
            setPadding(
                dp(5),
                dp(15),
                dp(5),
                dp(15)
            )
        }

        addStat(stats, "🔋", getBattery(), "البطارية")
        addStat(stats, "💾", getStorage(), "التخزين")
        addStat(stats, "▣", getMemory(), "الذاكرة")
        addStat(
            stats,
            "🤖",
            "Android ${Build.VERSION.RELEASE}",
            "الإصدار"
        )

        val statsParams = LinearLayout.LayoutParams(-1, -2)
        statsParams.topMargin = dp(12)

        content.addView(stats, statsParams)

        // FEATURES

        addSectionTitle(
            "مركز الأدوات",
            "اختر الخدمة التي تريد استخدامها"
        )

        val features = listOf(

            FeatureCard(
                "🤖",
                "المساعد الذكي",
                "اسأل واحصل على إجابات"
            ) {
                showAI()
            },

            FeatureCard(
                "📁",
                "مدير الملفات",
                "تصفح ملفات هاتفك"
            ) {
                openFiles()
            },

            FeatureCard(
                "🛠",
                "الأدوات الذكية",
                "أدوات مفيدة ومتنوعة"
            ) {
                showTools()
            },

            FeatureCard(
                "📋",
                "المهام والتنبيهات",
                "نظم مهامك وتذكيراتك"
            ) {
                showTasks()
            },

            FeatureCard(
                "🎨",
                "التخصيص",
                "تخصيص تجربة التطبيق"
            ) {
                showSettings()
            },

            FeatureCard(
                "📊",
                "معلومات الهاتف",
                "تفاصيل الجهاز"
            ) {
                showDeviceInfo()
            }
        )

        for (i in features.indices step 2) {

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            addFeature(row, features[i])

            if (i + 1 < features.size) {
                addFeature(row, features[i + 1])
            }

            val params = LinearLayout.LayoutParams(
                -1,
                dp(112)
            )

            params.topMargin = dp(12)

            content.addView(row, params)
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(0, 0, 1f)
        )

        // BOTTOM NAVIGATION

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(
                dp(5),
                dp(8),
                dp(5),
                dp(8)
            )
            setBackgroundColor(surface)
        }

        val navItems = listOf(
            "⌂" to "الرئيسية",
            "▦" to "الأدوات",
            "▣" to "التطبيقات",
            "⚙" to "الإعدادات",
            "ⓘ" to "حول"
        )

        navItems.forEachIndexed { index, item ->

            val itemView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            val selected = index == 0

            val icon = text(
                item.first,
                23f,
                if (selected) purple else muted,
                true
            ).apply {
                gravity = Gravity.CENTER
            }

            val label = text(
                item.second,
                10f,
                if (selected) purple else muted
            ).apply {
                gravity = Gravity.CENTER
            }

            itemView.addView(icon)
            itemView.addView(label)

            itemView.setOnClickListener {
                when (index) {
                    0 -> showHome()
                    1 -> showTools()
                    2 -> showApplications()
                    3 -> showSettings()
                    4 -> showAbout()
                }
            }

            nav.addView(
                itemView,
                LinearLayout.LayoutParams(
                    0,
                    dp(55),
                    1f
                )
            )
        }

        root.addView(nav)

        setContentView(root)
    }

    // =====================================
    // SECTION TITLE
    // =====================================

    private fun addSectionTitle(
        title: String,
        subtitle: String
    ) {

        val view = section(title, subtitle)

        val params = LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(25)

        content.addView(view, params)
    }

    // =====================================
    // DEVICE STATS
    // =====================================

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
            text(icon, 22f, purple, true).apply {
                gravity = Gravity.CENTER
            }
        )

        box.addView(
            text(value, 12f, white, true).apply {
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
            LinearLayout.LayoutParams(
                0,
                dp(85),
                1f
            )
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
                BatteryManager.EXTRA_LEVEL,
                -1
            ) ?: -1

            val scale = intent?.getIntExtra(
                BatteryManager.EXTRA_SCALE,
                -1
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

    // =====================================
    // STORAGE
    // =====================================

    private fun getStorage(): String {

        return try {

            val stat = StatFs(
                Environment.getDataDirectory().path
            )

            val used = stat.totalBytes - stat.availableBytes

            "${used / 1_073_741_824L} GB"

        } catch (e: Exception) {
            "غير متاح"
        }
    }

    // =====================================
    // MEMORY
    // =====================================

    private fun getMemory(): String {

        return try {

            val manager = getSystemService(
                Context.ACTIVITY_SERVICE
            ) as ActivityManager

            val info = ActivityManager.MemoryInfo()

            manager.getMemoryInfo(info)

            "${info.availMem / 1_073_741_824L} GB"

        } catch (e: Exception) {
            "غير متاح"
        }
    }

    // =====================================
    // FEATURE CARD
    // =====================================

    private fun addFeature(
        row: LinearLayout,
        feature: FeatureCard
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(
                surface,
                18,
                0xFF51217E.toInt()
            )
            setPadding(
                dp(7),
                dp(8),
                dp(7),
                dp(8)
            )
            isClickable = true
            isFocusable = true
        }

        card.addView(
            text(
                feature.icon,
                27f,
                purple,
                true
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        card.addView(
            text(
                feature.title,
                14f,
                white,
                true
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        card.addView(
            text(
                feature.description,
                10f,
                muted
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        card.setOnClickListener {
            feature.action.invoke()
        }

        val params = LinearLayout.LayoutParams(
            0,
            -1,
            1f
        )

        params.setMargins(
            dp(5),
            0,
            dp(5),
            0
        )

        row.addView(card, params)
    }

    // =====================================
    // AI
    // =====================================

    private fun showAI() {

        AlertDialog.Builder(this)
            .setTitle("ORVEX AI")
            .setMessage(
                "المساعد الذكي\n\n" +
                "هذه واجهة أولية للمساعد.\n\n" +
                "يلزم ربط خدمة ذكاء اصطناعي فعلية " +
                "حتى يتمكن من معالجة الأسئلة والإجابة عنها."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    // =====================================
    // FILE MANAGER
    // =====================================

    private fun openFiles() {

        try {

            val intent = Intent(
                Intent.ACTION_OPEN_DOCUMENT
            ).apply {

                addCategory(
                    Intent.CATEGORY_OPENABLE
                )

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

        val tools = arrayOf(
            "حاسبة",
            "معلومات الجهاز",
            "مشاركة نص",
            "فحص التخزين"
        )

        AlertDialog.Builder(this)
            .setTitle("الأدوات الذكية")
            .setItems(tools) { _, index ->

                when (index) {

                    0 -> {

                        val intent = Intent(
                            Intent.ACTION_MAIN
                        ).addCategory(
                            Intent.CATEGORY_APP_CALCULATOR
                        )

                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            toast("لا توجد حاسبة متاحة")
                        }
                    }

                    1 -> showDeviceInfo()

                    2 -> shareText()

                    3 -> showStorageInfo()
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    // =====================================
    // TASKS
    // =====================================

    private fun showTasks() {

        AlertDialog.Builder(this)
            .setTitle("المهام والتنبيهات")
            .setMessage(
                "قسم المهام قيد التطوير.\n\n" +
                "سيتم إضافة حفظ المهام والتنبيهات " +
                "في مرحلة التطوير القادمة."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    // =====================================
    // DEVICE INFO
    // =====================================

    private fun showDeviceInfo() {

        val manager = getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager

        val memory = ActivityManager.MemoryInfo()

        manager.getMemoryInfo(memory)

        val info = """
            اسم الجهاز: ${Build.MODEL}

            الشركة: ${Build.MANUFACTURER}

            إصدار Android: ${Build.VERSION.RELEASE}

            SDK: ${Build.VERSION.SDK_INT}

            الذاكرة الكلية: ${memory.totalMem / 1_073_741_824L} GB

            الذاكرة المتاحة: ${memory.availMem / 1_073_741_824L} GB

            البطارية: ${getBattery()}

            التخزين المستخدم: ${getStorage()}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("معلومات الجهاز")
            .setMessage(info)
            .setPositiveButton("إغلاق", null)
            .show()
    }

    // =====================================
    // STORAGE INFO
    // =====================================

    private fun showStorageInfo() {

        try {

            val stat = StatFs(
                Environment.getDataDirectory().path
            )

            val total = stat.totalBytes / 1_073_741_824L
            val free = stat.availableBytes / 1_073_741_824L
            val used = total - free

            AlertDialog.Builder(this)
                .setTitle("التخزين")
                .setMessage(
                    "المساحة الكلية: $total GB\n\n" +
                    "المساحة المستخدمة: $used GB\n\n" +
                    "المساحة المتاحة: $free GB"
                )
                .setPositiveButton("حسناً", null)
                .show()

        } catch (e: Exception) {

            toast("تعذر قراءة معلومات التخزين")
        }
    }

    // =====================================
    // SHARE
    // =====================================

    private fun shareText() {

        val intent = Intent(
            Intent.ACTION_SEND
        ).apply {

            type = "text/plain"

            putExtra(
                Intent.EXTRA_TEXT,
                "ORVEX - Smart Control Center"
            )
        }

        startActivity(
            Intent.createChooser(
                intent,
                "مشاركة ORVEX"
            )
        )
    }

    // =====================================
    // APPLICATIONS
    // =====================================

    private fun showApplications() {

        AlertDialog.Builder(this)
            .setTitle("التطبيقات")
            .setMessage(
                "قسم إدارة التطبيقات قيد التطوير."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    // =====================================
    // SETTINGS
    // =====================================

    private fun showSettings() {

        AlertDialog.Builder(this)
            .setTitle("إعدادات ORVEX")
            .setItems(
                arrayOf(
                    "معلومات الجهاز",
                    "الأدوات",
                    "حول التطبيق"
                )
            ) { _, index ->

                when (index) {

                    0 -> showDeviceInfo()

                    1 -> showTools()

                    2 -> showAbout()
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    // =====================================
    // ABOUT
    // =====================================

    private fun showAbout() {

        AlertDialog.Builder(this)
            .setTitle("ORVEX")
            .setMessage(
                "ORVEX Smart Control Center\n\n" +
                "Version 1.0\n\n" +
                "Developed with Kotlin\n" +
                "Android Application"
            )
            .setPositiveButton("حسناً", null)
            .show()
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
