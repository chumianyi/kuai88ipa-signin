package com.kuai88ipa.signin

import android.content.Context
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 88ipa API客户端
 */
class ApiClient private constructor(context: Context) {

    private val cookieStore = PersistentCookieStore(context.applicationContext)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore.getCookiesForDomain(url.host)
            }

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore.saveCookiesForDomain(url.host, cookies)
            }
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        @Volatile
        private var INSTANCE: ApiClient? = null

        fun getInstance(context: Context): ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiClient(context).also { INSTANCE = it }
            }
        }

        private const val BASE_URL = "https://www.88ipa.com"
    }

    private fun buildRequest(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "$BASE_URL/user/login.html")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36 Chrome/128.0.0.0 Mobile Safari/537.36"
            )
    }

    /**
     * 登录
     */
    fun login(userMail: String, userPass: String, callback: (Boolean, String) -> Unit) {
        val formBody = FormBody.Builder()
            .add("userMail", userMail)
            .add("userPass", userPass)
            .build()

        val request = buildRequest("$BASE_URL/user/api/login")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络错误：${e.message ?: "请检查网络连接"}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                try {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    val message = json.optString("message", "")
                    if (status == "success") {
                        callback(true, message.ifEmpty { "登录成功" })
                    } else {
                        callback(false, message.ifEmpty { "登录失败" })
                    }
                } catch (e: Exception) {
                    callback(false, "解析响应失败：${e.message}")
                }
            }
        })
    }

    /**
     * 签到
     */
    fun signIn(callback: (Boolean, String) -> Unit) {
        val request = buildRequest("$BASE_URL/user/api/sign_in")
            .post(ByteArray(0).toRequestBody())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络错误：${e.message ?: "请检查网络连接"}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                // 未登录时返回HTML登录页
                if (body.trimStart().startsWith("<")) {
                    callback(false, "__NOT_LOGGED_IN__")
                    return
                }
                try {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    val message = json.optString("message", "")
                    if (status == "success") {
                        callback(true, message.ifEmpty { "签到成功" })
                    } else {
                        callback(false, message.ifEmpty { "签到失败" })
                    }
                } catch (e: Exception) {
                    callback(false, "解析响应失败：${e.message}")
                }
            }
        })
    }

    /**
     * 是否已登录（有cookie）
     */
    fun isLoggedIn(): Boolean {
        return cookieStore.hasCookies()
    }

    /**
     * 退出登录
     */
    fun logout() {
        cookieStore.removeAll()
    }
}
