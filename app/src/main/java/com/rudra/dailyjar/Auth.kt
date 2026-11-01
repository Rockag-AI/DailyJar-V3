package com.rudra.dailyjar

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

/** Local accounts: every username gets its own private data on this phone. */
object Auth {
    private fun acc(c: Context) = c.getSharedPreferences("accounts", Context.MODE_PRIVATE)
    fun current(c: Context): String? = acc(c).getString("session", null)
    private fun clean(n: String) = n.trim().lowercase()
    private fun hash(salt: String, pass: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pass).toByteArray(Charsets.UTF_8))
            .joinToString("") { String.format("%02x", it) }

    /** Returns an error message, or null on success. */
    fun register(c: Context, rawName: String, pass: String): String? {
        val n = clean(rawName)
        if (!Regex("[a-z0-9_.]{3,20}").matches(n)) return "Username needs 3 to 20 letters, numbers, dots or underscores."
        if (pass.length < 4) return "Password needs at least 4 characters."
        val a = acc(c)
        if (a.contains("hash:$n")) return "That username already exists on this phone. Sign in instead."
        val salt = UUID.randomUUID().toString()
        a.edit().putString("salt:$n", salt).putString("hash:$n", hash(salt, pass)).putString("session", n).apply()
        return null
    }

    fun login(c: Context, rawName: String, pass: String): String? {
        val n = clean(rawName)
        val a = acc(c)
        val salt = a.getString("salt:$n", null) ?: return "No account with that username."
        if (hash(salt, pass) != a.getString("hash:$n", "")) return "Wrong password."
        a.edit().putString("session", n).apply()
        return null
    }

    fun logout(c: Context) { acc(c).edit().remove("session").apply() }
}
