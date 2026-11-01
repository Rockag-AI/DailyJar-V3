package com.rudra.dailyjar

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class Exp(val id: Long, val date: String, val amt: Double, val note: String)
class Month(val sp: DoubleArray, val limit: Long, val today: Int, val start: Int, val days: Int, val firstOffset: Int)
class Calc(val limit: Long, val spentToday: Double, val left: Double, val jar: Double)

fun inr(n: Double): String {
    var s = Math.round(Math.abs(n)).toString()
    if (s.length > 3) {
        val last = s.takeLast(3)
        var rest = s.dropLast(3)
        val sb = StringBuilder()
        while (rest.length > 2) { sb.insert(0, "," + rest.takeLast(2)); rest = rest.dropLast(2) }
        s = rest + sb + "," + last
    }
    return "₹" + s
}

object Store {
    fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences("jar_" + (Auth.current(c) ?: "_none"), Context.MODE_PRIVATE)

    fun isReady(c: Context): Boolean = Auth.current(c) != null && prefs(c).getBoolean("setup", false)

    fun finishSetup(c: Context) {
        prefs(c).edit().putBoolean("setup", true).putString("start", today()).apply()
    }

    fun pool(c: Context): Long {
        val s = prefs(c)
        return if (s.getString("mode", "earning") == "earning")
            s.getLong("salary", 0) - s.getLong("fixed", 0) else s.getLong("budget", 0)
    }

    fun today(): String {
        val c = Calendar.getInstance()
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun all(c: Context): List<Exp> {
        val out = ArrayList<Exp>()
        try {
            val a = JSONArray(prefs(c).getString("exp", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(Exp(o.getLong("id"), o.getString("date"), o.getDouble("amt"), o.getString("note")))
            }
        } catch (e: Exception) { }
        return out
    }

    private fun write(c: Context, list: List<Exp>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("id", it.id).put("date", it.date).put("amt", it.amt).put("note", it.note)) }
        prefs(c).edit().putString("exp", a.toString()).apply()
    }

    fun add(c: Context, amt: Double, note: String) {
        write(c, all(c) + Exp(System.currentTimeMillis(), today(), amt, note))
    }

    // Used by the SMS receiver: ignores the same message arriving twice within a minute.
    fun addIfNew(c: Context, amt: Double, note: String, body: String): Boolean {
        val p = prefs(c)
        val now = System.currentTimeMillis()
        if (p.getInt("lastHash", 0) == body.hashCode() && now - p.getLong("lastAt", 0) < 60000) return false
        p.edit().putInt("lastHash", body.hashCode()).putLong("lastAt", now).apply()
        add(c, amt, note)
        return true
    }

    fun remove(c: Context, id: Long) = write(c, all(c).filter { it.id != id })

    fun clear(c: Context) {
        write(c, emptyList())
        prefs(c).edit().putString("start", today()).apply()
    }

    fun setSettings(c: Context, mode: String, salary: Long, fixed: Long, budget: Long, opening: Long) {
        prefs(c).edit().putString("mode", mode).putLong("salary", salary).putLong("fixed", fixed)
            .putLong("budget", budget).putLong("opening", opening).apply()
    }

    fun month(c: Context): Month {
        val s = prefs(c)
        if (!s.contains("start")) s.edit().putString("start", today()).apply()
        val now = Calendar.getInstance()
        val prefix = String.format("%04d-%02d-", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
        val t = now.get(Calendar.DAY_OF_MONTH)
        val n = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val limit = Math.max(0L, Math.round(pool(c).toDouble() / n))
        val sp = DoubleArray(n + 1)
        for (e in all(c)) if (e.date.startsWith(prefix)) sp[e.date.substring(8).toInt()] += e.amt
        val start = s.getString("start", today()) ?: today()
        val sd = if (start.startsWith(prefix)) start.substring(8).toInt() else 1
        val first = (now.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        return Month(sp, limit, t, sd, n, (first.get(Calendar.DAY_OF_WEEK) + 5) % 7)
    }

    fun calc(c: Context): Calc {
        val m = month(c)
        var jar = prefs(c).getLong("opening", 0).toDouble()
        for (d in m.start until m.today) jar += m.limit - m.sp[d]
        jar += Math.min(0.0, m.limit - m.sp[m.today])
        return Calc(m.limit, m.sp[m.today], m.limit - m.sp[m.today], jar)
    }
}
