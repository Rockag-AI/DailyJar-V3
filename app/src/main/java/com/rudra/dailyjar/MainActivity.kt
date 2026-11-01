package com.rudra.dailyjar

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val hd = Handler(Looper.getMainLooper())
    private var mainReady = false

    private val ink = Pal.ink; private val mute = Pal.mute; private val teal = Pal.teal
    private val tealBg = Pal.tealBg; private val amber = Pal.amber; private val amberBg = Pal.amberBg
    private val white = Pal.white

    private lateinit var greet: TextView
    private lateinit var left: TextView
    private lateinit var sub: TextView
    private lateinit var ring: Ring
    private lateinit var jarCard: LinearLayout
    private lateinit var jarLabel: TextView
    private lateinit var jarAmt: TextView
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var cal: LinearLayout
    private lateinit var amt: EditText
    private lateinit var note: EditText
    private lateinit var modeSw: Switch
    private lateinit var salary: EditText
    private lateinit var fixed: EditText
    private lateinit var budget: EditText
    private lateinit var opening: EditText
    private lateinit var earnBox: LinearLayout
    private lateinit var budBox: LinearLayout

    override fun onCreate(b: Bundle?) { super.onCreate(b); route() }
    override fun onResume() { super.onResume(); if (mainReady) { Scheduler.schedule(this); render() } }
    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) { if (mainReady) render() }

    private fun route() {
        if (Auth.current(this) == null) showLogin()
        else if (!Store.prefs(this).getBoolean("setup", false)) showSetup()
        else showMain()
    }

    private fun bars(dark: Boolean) {
        window.statusBarColor = if (dark) Pal.dbg else Pal.bg
        window.navigationBarColor = if (dark) Pal.dbg else Pal.bg
        window.decorView.systemUiVisibility = if (dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    private fun scroller(dark: Boolean) = ScrollView(this).apply {
        setBackgroundColor(if (dark) Pal.dbg else Pal.bg); isFillViewport = true
    }

    private fun lbl(t: String) = tv(t, 13f, false, Pal.dmute).apply { setPadding(dp(2), dp(14), 0, 0) }

    // ------------------------------------------------------------ Login
    private fun showLogin() {
        mainReady = false; bars(true)
        var creating = false
        val sv = scroller(true)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(56), dp(24), dp(32))
        }
        sv.addView(root)
        val logo = Ring(this, Pal.mint, Pal.mint, Pal.mintBg, true).apply { layoutParams = LinearLayout.LayoutParams(dp(104), dp(104)) }
        root.addView(logo)
        root.addView(tv("Daily Jar", 30f, true, Pal.dink).apply { setPadding(0, dp(16), 0, 0) })
        root.addView(tv("Spend less than today’s limit. Save the rest.", 15f, false, Pal.dmute).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(12)) })

        val panel = card(Pal.dcard)
        val tabs = LinearLayout(this)
        val tabIn = tv("Sign in", 16f, true, Pal.dink).apply { setPadding(0, 0, dp(22), dp(6)) }
        val tabUp = tv("Create account", 16f, true, Pal.dmute).apply { setPadding(0, 0, 0, dp(6)) }
        tabs.addView(tabIn); tabs.addView(tabUp); panel.addView(tabs)
        val user = field("Username", 0, true)
        val pass = field("Password", 2, true)
        val confirm = field("Repeat password", 2, true).apply { visibility = View.GONE }
        val err = tv("", 13f, false, 0xFFFCA5A5.toInt()).apply { setPadding(dp(2), dp(8), 0, 0) }
        panel.addView(user); panel.addView(pass); panel.addView(confirm); panel.addView(err)
        val go = btn("Sign in", Pal.mint, Pal.dbg) {
            val u = user.text.toString(); val p = pass.text.toString()
            val e = if (creating) {
                if (p != confirm.text.toString()) "Passwords don’t match." else Auth.register(this, u, p)
            } else Auth.login(this, u, p)
            if (e != null) err.text = e else route()
        }
        panel.addView(go)
        fun style() {
            tabIn.setTextColor(if (!creating) Pal.dink else Pal.dmute)
            tabUp.setTextColor(if (creating) Pal.dink else Pal.dmute)
            confirm.visibility = if (creating) View.VISIBLE else View.GONE
            go.text = if (creating) "Create account" else "Sign in"
            err.text = ""
        }
        tabIn.setOnClickListener { creating = false; style() }
        tabUp.setOnClickListener { creating = true; style() }
        root.addView(panel)
        root.addView(tv("Accounts and data stay on this phone.", 12f, false, Pal.dmute).apply { setPadding(0, dp(14), 0, 0) })
        setContentView(sv)

        // One entrance: the ring draws itself, then the sign-in panel rises.
        panel.alpha = 0f; panel.translationY = dp(28).toFloat()
        hd.postDelayed({ logo.set(1f, false) }, 150)
        panel.animate().alpha(1f).translationY(0f).setStartDelay(450).setDuration(500).start()
    }

    // ------------------------------------------------------------ First-time setup
    private fun showSetup() {
        mainReady = false; bars(true)
        var earning = true
        val sv = scroller(true)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(56), dp(24), dp(32)) }
        sv.addView(root)
        root.addView(tv("Hi, " + (Auth.current(this) ?: ""), 15f, false, Pal.dmute))
        root.addView(tv("Set your monthly money", 28f, true, Pal.dink).apply { setPadding(0, dp(6), 0, dp(6)) })
        root.addView(tv("We’ll turn it into a limit for every day.", 15f, false, Pal.dmute))

        val seg = LinearLayout(this).apply { setPadding(0, dp(20), 0, 0) }
        val segA = tv("I earn a salary", 15f, true, Pal.dbg).apply { gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(12)); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        val segB = tv("Fixed budget", 15f, true, Pal.dmute).apply { gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(12)); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        seg.addView(segA); seg.addView(segB); root.addView(seg)

        val amtLabel = lbl("Monthly salary (₹)")
        val amtF = field("e.g. 50000", 1, true)
        val fixLabel = lbl("Fixed bills such as rent and EMIs (₹)")
        val fixF = field("0 if none", 1, true)
        val savLabel = lbl("Savings you already have (₹, optional)")
        val savF = field("0 if none", 1, true)
        val err = tv("", 13f, false, 0xFFFCA5A5.toInt()).apply { setPadding(dp(2), dp(8), 0, 0) }
        root.addView(amtLabel); root.addView(amtF); root.addView(fixLabel); root.addView(fixF)
        root.addView(savLabel); root.addView(savF); root.addView(err)

        fun style() {
            segA.background = if (earning) shape(Pal.mint, 12) else shape(Pal.dcard, 12)
            segB.background = if (!earning) shape(Pal.mint, 12) else shape(Pal.dcard, 12)
            segA.setTextColor(if (earning) Pal.dbg else Pal.dmute)
            segB.setTextColor(if (!earning) Pal.dbg else Pal.dmute)
            amtLabel.text = if (earning) "Monthly salary (₹)" else "Total you want to spend this month (₹)"
            fixLabel.visibility = if (earning) View.VISIBLE else View.GONE
            fixF.visibility = if (earning) View.VISIBLE else View.GONE
            err.text = ""
        }
        segA.setOnClickListener { earning = true; style() }
        segB.setOnClickListener { earning = false; style() }
        style()

        root.addView(btn("Show my daily limit", Pal.mint, Pal.dbg) {
            val amount = amtF.text.toString().toDoubleOrNull()?.toLong() ?: 0L
            val fix = if (earning) (fixF.text.toString().toDoubleOrNull()?.toLong() ?: 0L) else 0L
            val sav = savF.text.toString().toDoubleOrNull()?.toLong() ?: 0L
            if (amount <= 0) { err.text = "Enter an amount above zero."; return@btn }
            if (fix >= amount) { err.text = "Fixed bills must be less than the amount."; return@btn }
            Store.setSettings(this, if (earning) "earning" else "budget",
                if (earning) amount else 0L, fix, if (earning) 0L else amount, sav)
            Store.finishSetup(this)
            showReveal()
        })
        root.addView(tv("Sign out", 14f, false, Pal.dmute).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(18), 0, dp(6))
            setOnClickListener { Auth.logout(this@MainActivity); route() }
        })
        setContentView(sv)
    }

    // ------------------------------------------------------------ Reveal animation
    private fun showReveal() {
        mainReady = false; bars(true)
        val m = Store.month(this)
        val pool = Store.pool(this).toDouble()
        val limit = m.limit.toDouble()
        val sv = scroller(true)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(48), dp(20), dp(32))
        }
        sv.addView(root)
        val top = tv("Your money this month", 15f, false, Pal.dmute).apply { alpha = 0f }
        root.addView(top)
        val box = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(dp(230), dp(230)).apply { topMargin = dp(16) } }
        val rg = Ring(this, Pal.mint, Pal.mint, Pal.mintBg, true)
        box.addView(rg, FrameLayout.LayoutParams(-1, -1))
        val centre = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        val big = tv(inr(0.0), 34f, true, Pal.dink)
        val cap = tv("a month", 14f, false, Pal.dmute)
        centre.addView(big); centre.addView(cap)
        box.addView(centre, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        root.addView(box)
        val split = tv("÷ " + m.days + " days", 18f, true, Pal.mint).apply { alpha = 0f; setPadding(0, dp(12), 0, dp(12)) }
        root.addView(split)

        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val cells = ArrayList<TextView>()
        val rows = (m.firstOffset + m.days + 6) / 7
        for (r in 0 until rows) {
            val row = LinearLayout(this)
            for (k in 0 until 7) {
                val d = r * 7 + k - m.firstOffset + 1
                if (d < 1 || d > m.days) row.addView(cell("", 0, Pal.dmute, 0, 40))
                else {
                    val cc = cell(short(m.limit), Pal.mintBg, Pal.mint, 0, 40, 11f).apply { alpha = 0f; scaleX = 0.6f; scaleY = 0.6f }
                    cells.add(cc); row.addView(cc)
                }
            }
            grid.addView(row)
        }
        root.addView(grid)
        val morning = tv("Good morning. Today’s limit is " + inr(limit) + ".", 17f, true, Pal.dink)
            .apply { alpha = 0f; gravity = Gravity.CENTER; setPadding(0, dp(18), 0, 0) }
        root.addView(morning)
        val start = btn("Start tracking", Pal.mint, Pal.dbg) { showMain() }.apply { alpha = 0f; isEnabled = false }
        root.addView(start)
        setContentView(sv)

        // 1. The monthly amount counts up inside a ring that draws itself.
        top.animate().alpha(1f).setDuration(400).start()
        rg.set(1f, false)
        countTo(big, 0.0, pool, 1300)
        // 2. It splits across the days of the month.
        hd.postDelayed({ split.animate().alpha(1f).setDuration(400).start() }, 1800)
        // 3. The number drops to the daily limit.
        hd.postDelayed({
            top.text = "Your daily limit"; cap.text = "every day"
            countTo(big, pool, limit, 900)
            big.scaleX = 1.18f; big.scaleY = 1.18f
            big.animate().scaleX(1f).scaleY(1f).setStartDelay(800).setDuration(300).start()
        }, 2500)
        // 4. The month fills in, day by day.
        hd.postDelayed({
            cells.forEachIndexed { i, v ->
                v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(i * 25L).setDuration(260).start()
            }
        }, 3600)
        // 5. The morning message and the start button arrive.
        hd.postDelayed({
            morning.animate().alpha(1f).setDuration(500).start()
            start.isEnabled = true
            start.animate().alpha(1f).setStartDelay(250).setDuration(400).start()
            sv.post { sv.fullScroll(View.FOCUS_DOWN) }
        }, 4900)
    }

    // ------------------------------------------------------------ Main screen
    private fun showMain() {
        bars(false)
        val sv = scroller(false)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(36), dp(16), dp(40)) }
        sv.addView(root); setContentView(sv)

        val hero = card()
        val heroRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val heroText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        greet = tv("", 14f, false, mute); heroText.addView(greet)
        left = tv("", 44f, true).apply { letterSpacing = -0.02f }; heroText.addView(left)
        sub = tv("", 14f, false, mute); heroText.addView(sub)
        ring = Ring(this, teal, amber, tealBg).apply { layoutParams = LinearLayout.LayoutParams(dp(92), dp(92)) }
        heroRow.addView(heroText); heroRow.addView(ring); hero.addView(heroRow)
        root.addView(hero)

        jarCard = card(tealBg)
        val jarRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        jarLabel = tv("", 15f).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        jarAmt = tv("", 24f, true, teal)
        jarRow.addView(jarLabel); jarRow.addView(jarAmt); jarCard.addView(jarRow)
        root.addView(jarCard)
        status = tv("", 13f, false, mute).apply { setPadding(dp(4), dp(10), dp(4), 0) }
        root.addView(status)

        val addCard = card()
        addCard.addView(tv("Add an expense", 17f, true))
        amt = field("Amount in ₹"); note = field("What for?", 0)
        addCard.addView(amt); addCard.addView(note)
        addCard.addView(btn("Add expense", ink, white) {
            val a = amt.text.toString().toDoubleOrNull()
            if (a == null || a <= 0) Toast.makeText(this, "Enter an amount above zero", Toast.LENGTH_SHORT).show()
            else {
                Store.add(this, a, note.text.toString().trim().ifEmpty { "Expense" })
                amt.setText(""); note.setText(""); render()
            }
        })
        root.addView(addCard)

        val listCard = card()
        listCard.addView(tv("Today’s spending", 17f, true))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listCard.addView(list); root.addView(listCard)

        val calCard = card()
        calCard.addView(tv("This month", 17f, true))
        cal = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        calCard.addView(cal)
        calCard.addView(tv("Green: within the limit. Orange: over the limit, so savings paid the difference.", 12f, false, mute).apply { setPadding(0, dp(8), 0, 0) })
        root.addView(calCard)

        val setCard = card()
        val setHead = tv("Settings  ▾", 17f, true).apply { setPadding(0, dp(4), 0, dp(4)) }
        setCard.addView(setHead)
        val setBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        setBody.addView(tv("Signed in as " + (Auth.current(this) ?: ""), 14f, false, mute).apply { setPadding(0, dp(8), 0, 0) })
        modeSw = Switch(this).apply { text = "I’m not earning (use a monthly budget)"; setPadding(0, dp(10), 0, dp(4)); textSize = 15f }
        setBody.addView(modeSw)
        salary = field("Monthly salary (₹)"); fixed = field("Fixed bills: rent, EMIs (₹)")
        earnBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(salary); addView(fixed) }
        budget = field("Total to spend this month (₹)")
        budBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(budget) }
        opening = field("Savings already in the jar (₹)")
        setBody.addView(earnBox); setBody.addView(budBox); setBody.addView(opening)
        modeSw.setOnCheckedChangeListener { _, on ->
            earnBox.visibility = if (on) View.GONE else View.VISIBLE
            budBox.visibility = if (on) View.VISIBLE else View.GONE
        }
        val p = Store.prefs(this)
        modeSw.isChecked = p.getString("mode", "earning") == "budget"
        salary.setText(p.getLong("salary", 0).toString()); fixed.setText(p.getLong("fixed", 0).toString())
        budget.setText(p.getLong("budget", 0).toString()); opening.setText(p.getLong("opening", 0).toString())
        earnBox.visibility = if (modeSw.isChecked) View.GONE else View.VISIBLE
        budBox.visibility = if (modeSw.isChecked) View.VISIBLE else View.GONE
        setBody.addView(btn("Save settings", teal, white) {
            fun n(e: EditText) = e.text.toString().toDoubleOrNull()?.toLong() ?: 0L
            Store.setSettings(this, if (modeSw.isChecked) "budget" else "earning", n(salary), n(fixed), n(budget), n(opening))
            render(); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        })
        setBody.addView(btn("Clear all expenses", amberBg, amber) { Store.clear(this); render() })
        setBody.addView(btn("Sign out", 0xFFF6F8F7.toInt(), ink) { Auth.logout(this); route() })
        setCard.addView(setBody)
        setHead.setOnClickListener {
            val open = setBody.visibility == View.GONE
            setBody.visibility = if (open) View.VISIBLE else View.GONE
            setHead.text = if (open) "Settings  ▴" else "Settings  ▾"
        }
        root.addView(setCard)

        val need = ArrayList<String>()
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 1)

        mainReady = true
        Scheduler.schedule(this)
        render()
        hero.alpha = 0f; hero.translationY = dp(24).toFloat()
        hero.animate().alpha(1f).translationY(0f).setDuration(450).start()
    }

    private fun render() {
        val c = Store.calc(this)
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        greet.text = (if (h < 12) "Good morning" else if (h < 17) "Good afternoon" else "Good evening") +
            ". Today’s limit is " + inr(c.limit.toDouble())
        left.text = (if (c.left < 0) "-" else "") + inr(c.left)
        left.setTextColor(if (c.left < 0) amber else ink)
        sub.text = if (c.left < 0) "over the limit. Savings pay the difference." else "left today"
        ring.set(if (c.limit > 0) Math.max(0.0, c.left / c.limit).toFloat() else 0f, c.left < 0)

        val debt = c.jar < 0
        jarCard.background = shape(if (debt) amberBg else tealBg, 22)
        jarLabel.text = if (debt) "Savings jar is in debt" else "Savings jar"
        jarAmt.text = (if (debt) "-" else "") + inr(c.jar)
        jarAmt.setTextColor(if (debt) amber else teal)

        val sms = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        status.text = if (sms) "●  Bank SMS tracking is on. Debits update this screen."
        else "●  SMS tracking is off. Allow the SMS permission in the app’s settings."
        status.setTextColor(if (sms) teal else amber)

        list.removeAllViews()
        val today = Store.all(this).filter { it.date == Store.today() }.reversed()
        if (today.isEmpty()) list.addView(tv("Nothing spent yet today. Payments from bank SMS appear here.", 14f, false, mute))
        today.forEach { e ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
            row.addView(tv(e.note, 16f).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
            row.addView(tv("-" + inr(e.amt), 16f, true))
            row.addView(tv("✕", 16f, false, mute).apply {
                setPadding(dp(14), dp(4), dp(4), dp(4)); contentDescription = "Remove " + e.note
                setOnClickListener { Store.remove(context, e.id); render() }
            })
            list.addView(row)
        }

        val m = Store.month(this)
        cal.removeAllViews()
        val head = LinearLayout(this)
        arrayOf("M", "T", "W", "T", "F", "S", "S").forEach { head.addView(cell(it, 0, mute)) }
        cal.addView(head)
        val rows = (m.firstOffset + m.days + 6) / 7
        for (r in 0 until rows) {
            val row = LinearLayout(this)
            for (k in 0 until 7) {
                val d = r * 7 + k - m.firstOffset + 1
                if (d < 1 || d > m.days) { row.addView(cell("", 0, mute)); continue }
                var fill = 0
                if (d >= m.start && d <= m.today) {
                    if (m.sp[d] > m.limit) fill = amberBg
                    else if (d < m.today || m.sp[d] > 0) fill = tealBg
                }
                row.addView(cell(d.toString(), fill, if (fill == 0) mute else ink, if (d == m.today) ink else 0, 38, 13f))
            }
            cal.addView(row)
        }
    }
}
