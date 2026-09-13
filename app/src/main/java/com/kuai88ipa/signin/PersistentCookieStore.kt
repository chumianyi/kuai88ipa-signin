package com.kuai88ipa.signin

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie

/**
 * 持久化Cookie管理器，使用SharedPreferences保存登录会话
 */
class PersistentCookieStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cookie_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_COOKIES = "cookies_json"
    }

    /**
     * 获取指定域名的所有cookie（用于请求）
     */
    fun getCookiesForDomain(host: String): List<Cookie> {
        val all = getAllCookies()
        return all.filter { cookie ->
            cookie.matchesHost(host)
        }
    }

    /**
     * 保存响应中的cookie
     */
    fun saveCookiesForDomain(host: String, cookies: List<Cookie>) {
        val all = getAllCookies().toMutableList()
        // 移除同名旧cookie
        for (newCookie in cookies) {
            all.removeAll { it.name == newCookie.name && it.domain == newCookie.domain }
            all.add(newCookie)
        }
        saveAllCookies(all)
    }

    fun hasCookies(): Boolean {
        return getAllCookies().isNotEmpty()
    }

    fun removeAll() {
        prefs.edit().remove(KEY_COOKIES).apply()
    }

    private fun getAllCookies(): List<Cookie> {
        val json = prefs.getString(KEY_COOKIES, null) ?: return emptyList()
        val result = mutableListOf<Cookie>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val cookie = Cookie.Builder()
                    .name(obj.getString("name"))
                    .value(obj.getString("value"))
                    .domain(obj.getString("domain"))
                    .path(obj.optString("path", "/"))
                    .also {
                        if (obj.optBoolean("secure", false)) it.secure()
                        if (obj.optLong("expiresAt", 0L) > 0L) it.expiresAt(obj.getLong("expiresAt"))
                        if (obj.optBoolean("httpOnly", false)) it.httpOnly()
                    }
                    .build()
                result.add(cookie)
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun saveAllCookies(cookies: List<Cookie>) {
        val arr = org.json.JSONArray()
        for (c in cookies) {
            val obj = org.json.JSONObject()
            obj.put("name", c.name)
            obj.put("value", c.value)
            obj.put("domain", c.domain)
            obj.put("path", c.path)
            obj.put("expiresAt", c.expiresAt)
            obj.put("secure", c.secure)
            obj.put("httpOnly", c.httpOnly)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_COOKIES, arr.toString()).apply()
    }

    /**
     * Cookie匹配host（支持子域名）
     */
    private fun Cookie.matchesHost(host: String): Boolean {
        val cookieDomain = this.domain.removePrefix(".").removePrefix("https://").removePrefix("http://")
        return host == cookieDomain || host.endsWith(".$cookieDomain") || host.endsWith(cookieDomain)
    }
}
