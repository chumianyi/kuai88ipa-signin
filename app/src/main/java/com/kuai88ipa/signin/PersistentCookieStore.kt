package com.kuai88ipa.signin

import android.content.Context
import android.content.SharedPreferences
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI

/**
 * 持久化Cookie管理器，使用SharedPreferences保存登录会话
 */
class PersistentCookieStore(context: Context) : CookieStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cookie_prefs", Context.MODE_PRIVATE)
    private val cookies = mutableMapOf<String, MutableList<HttpCookie>>()

    init {
        // 从SharedPreferences加载所有cookie
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("cookie_") && value is String) {
                try {
                    val cookieStr = value
                    val cookiesForUri = cookies.getOrPut(key.removePrefix("cookie_")) { mutableListOf() }
                    cookieStr.split(";").forEach { pair ->
                        val trimmed = pair.trim()
                        if (trimmed.isNotEmpty()) {
                            val eq = trimmed.indexOf('=')
                            if (eq > 0) {
                                val name = trimmed.substring(0, eq).trim()
                                val valueStr = trimmed.substring(eq + 1).trim()
                                if (name.isNotEmpty()) {
                                    cookiesForUri.add(HttpCookie(name, valueStr))
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun add(uri: URI?, cookie: HttpCookie?) {
        if (cookie == null) return
        val domain = cookie.domain ?: uri?.host ?: "default"
        val list = cookies.getOrPut(domain) { mutableListOf() }
        // 移除同名旧cookie
        list.removeAll { it.name == cookie.name }
        list.add(cookie)
        save()
    }

    override fun get(uri: URI?): List<HttpCookie> {
        val host = uri?.host ?: return emptyList()
        val result = mutableListOf<HttpCookie>()
        cookies.forEach { (domain, list) ->
            if (host.endsWith(domain.removePrefix(".")) || domain == host) {
                result.addAll(list)
            }
        }
        return result
    }

    override fun getCookies(): List<HttpCookie> {
        val result = mutableListOf<HttpCookie>()
        cookies.values.forEach { result.addAll(it) }
        return result
    }

    override fun getURIs(): MutableList<URI> {
        return cookies.keys.mapNotNull { runCatching { URI("https://$it") }.getOrNull() }.toMutableList()
    }

    override fun remove(uri: URI?, cookie: HttpCookie?): Boolean {
        if (cookie == null) return false
        val domain = cookie.domain ?: uri?.host ?: "default"
        val removed = cookies[domain]?.removeAll { it.name == cookie.name } ?: false
        if (removed) save()
        return removed
    }

    override fun removeAll(): Boolean {
        cookies.clear()
        prefs.edit().clear().apply()
        return true
    }

    private fun save() {
        val editor = prefs.edit().clear()
        cookies.forEach { (domain, list) ->
            val cookieStr = list.joinToString(";") { "${it.name}=${it.value}" }
            editor.putString("cookie_$domain", cookieStr)
        }
        editor.apply()
    }

    fun hasCookies(): Boolean {
        return getCookies().isNotEmpty()
    }
}
