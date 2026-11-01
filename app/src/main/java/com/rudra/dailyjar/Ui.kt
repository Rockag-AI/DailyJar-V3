package com.rudra.dailyjar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object Pal {
    val bg = 0xFFEEF1EF.toInt(); val white = 0xFFFFFFFF.toInt(); val ink = 0xFF14262B.toInt()
    val mute = 0xFF5F7377.toInt(); val line = 0xFFD5DDDB.toInt(); val teal = 0xFF0F766E.toInt()
    val tealBg = 0xFFCDEBE6.toInt(); val amber = 0xFFB45309.toInt(); val amberBg = 0xFFFCE3C0.toInt()
    val dbg = 0xFF0B1416.toInt(); val dcard = 0xFF152226.toInt(); val dink = 0xFFE8F1F0.toInt()
    val dmute = 0xFF8FA8AC.toInt(); val dline = 0xFF2A3D42.toInt()
    val mint = 0xFF2DD4BF.toInt(); val mintBg = 0xFF14403D.toInt()
}

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun Context.shape(fill: Int, r: Int, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
    setColor(fill); cornerRadius = dp(r).toFloat(); if (stroke != 0) setStroke(dp(1), stroke)
}

fun Context.tv(t: String, sp: Float = 16f, bold: Boolean = false, color: Int = Pal.ink): TextView = TextView(this).apply {
    text = t; textSize = sp; setTextColor(color)
    if (bold) setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
}

fun Context.card(fill: Int = Pal.white): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL; background = shape(fill, 22)
    setPadding(dp(18), dp(16), dp(18), dp(16))
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
}

/** kind: 0 = text, 1 = number, 2 = password */
fun Context.field(h: String, kind: Int = 1, dark: Boolean = false): EditText = EditText(this).apply {
    hint = h; setSingleLine(); textSize = 16f
    setTextColor(if (dark) Pal.dink else Pal.ink)
    setHintTextColor(if (dark) Pal.dmute else Pal.mute)
    background = if (dark) shape(Pal.dbg, 12, Pal.dline) else shape(0xFFF6F8F7.toInt(), 12, Pal.line)
    setPadding(dp(14), dp(12), dp(14), dp(12))
    inputType = when (kind) {
        1 -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        2 -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
}

fun Context.btn(t: String, fill: Int, fg: Int, go: () -> Unit): Button = Button(this).apply {
    text = t; isAllCaps = false; textSize = 15f; setTextColor(fg)
    background = shape(fill, 14); stateListAnimator = null; minimumHeight = dp(48)
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    setOnClickListener { go() }
}

fun Context.cell(text: String, fill: Int, fg: Int, ring: Int = 0, h: Int = 38, sp: Float = 12f): TextView = TextView(this).apply {
    this.text = text; gravity = Gravity.CENTER; textSize = sp; setTextColor(fg)
    layoutParams = LinearLayout.LayoutParams(0, dp(h), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
    if (fill != 0 || ring != 0) background = GradientDrawable().apply {
        setColor(if (fill == 0) Color.TRANSPARENT else fill)
        cornerRadius = dp(10).toFloat()
        if (ring != 0) setStroke(dp(2), ring)
    }
}

fun short(n: Long): String = if (n >= 1000) String.format("%.1fk", n / 1000.0).replace(".0k", "k") else n.toString()

fun countTo(t: TextView, from: Double, to: Double, ms: Long) {
    val a = ValueAnimator.ofFloat(0f, 1f)
    a.duration = ms
    a.addUpdateListener { t.text = inr(from + (to - from) * (it.animatedValue as Float)) }
    a.start()
}

/** Ring gauge. With glow = true it draws a soft halo (used on the dark intro screens). */
class Ring(c: Context, private val good: Int, private val bad: Int, private val track: Int, private val glow: Boolean = false) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private var frac = 0f
    private var over = false
    fun set(target: Float, isOver: Boolean) {
        over = isOver
        val a = ValueAnimator.ofFloat(frac, target)
        a.duration = 900
        a.addUpdateListener { frac = it.animatedValue as Float; invalidate() }
        a.start()
    }
    override fun onDraw(cv: Canvas) {
        val w = Math.min(width, height).toFloat()
        val s = w * 0.10f
        val inset = if (glow) s else s / 2
        val r = RectF(inset, inset, w - inset, w - inset)
        val sweep = 360f * (if (over) 1f else frac)
        val col = if (over) bad else good
        p.strokeWidth = s; p.color = track; p.alpha = 255; cv.drawArc(r, 0f, 360f, false, p)
        if (glow) { p.strokeWidth = s * 1.9f; p.color = col; p.alpha = 50; cv.drawArc(r, -90f, sweep, false, p) }
        p.strokeWidth = s; p.color = col; p.alpha = 255; cv.drawArc(r, -90f, sweep, false, p)
    }
}
