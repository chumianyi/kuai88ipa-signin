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

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore.getCookiesForDomain(url.host)
            }

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore.saveCookiesForDomain(url.host, cookies)
            }
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
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

        const val CATEGORY_HOME = "/"
        const val CATEGORY_GAME = "/game.html"
        const val CATEGORY_SOFT = "/soft.html"
    }

    private fun buildRequest(url: String, referer: String = "$BASE_URL/user/login.html"): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("Referer", referer)
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
            .header("X-Requested-With", "XMLHttpRequest")
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
        val request = buildRequest("$BASE_URL/user/api/sign_in", "$BASE_URL/user/dashboard.html")
            .header("X-Requested-With", "XMLHttpRequest")
            .post(ByteArray(0).toRequestBody())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络错误：${e.message ?: "请检查网络连接"}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
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
     * 获取HTML页面（同步，需在子线程调用）
     */
    fun fetchHtml(path: String): String? {
        return try {
            val request = buildRequest("$BASE_URL$path").build()
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 搜索应用
     */
    fun searchApps(keyword: String, callback: (Boolean, List<AppInfo>, String) -> Unit) {
        val encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
        Thread {
            val html = fetchHtml("/search.html?keyword=$encoded")
            if (html != null) {
                val apps = HtmlParser.parseAppList(html)
                callback(true, apps, "")
            } else {
                callback(false, emptyList(), "网络请求失败")
            }
        }.start()
    }

    /**
     * 获取应用列表（首页/分类）
     */
    fun fetchAppList(path: String, callback: (Boolean, List<AppInfo>, String) -> Unit) {
        Thread {
            val html = fetchHtml(path)
            if (html != null) {
                val apps = HtmlParser.parseAppList(html)
                callback(true, apps, "")
            } else {
                callback(false, emptyList(), "网络请求失败")
            }
        }.start()
    }

    /**
     * 获取应用详情
     */
    fun fetchAppDetail(appId: String, callback: (Boolean, AppDetail?, String) -> Unit) {
        Thread {
            val html = fetchHtml("/application/$appId.html")
            if (html != null) {
                val detail = HtmlParser.parseAppDetail(html, appId)
                if (detail != null) {
                    callback(true, detail, "")
                } else {
                    callback(false, null, "解析详情失败")
                }
            } else {
                callback(false, null, "网络请求失败")
            }
        }.start()
    }

    /**
     * 获取IPA下载链接
     */
    fun getDownloadLink(appId: String, callback: (Boolean, String, String) -> Unit) {
        val formBody = FormBody.Builder()
            .add("appId", appId)
            .build()

        val request = buildRequest("$BASE_URL/user/api/down_the_file", "$BASE_URL/select_download_method/$appId.html")
            .header("X-Requested-With", "XMLHttpRequest")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "", "网络错误：${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (body.trimStart().startsWith("<")) {
                    callback(false, "", "__NOT_LOGGED_IN__")
                    return
                }
                try {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    if (status == "success") {
                        val link = json.getJSONObject("data").optString("ipa_download_link", "")
                        callback(true, link, "")
                    } else {
                        callback(false, "", json.optString("message", "获取下载链接失败"))
                    }
                } catch (e: Exception) {
                    callback(false, "", "解析失败：${e.message}")
                }
            }
        })
    }

    /**
     * 在线安装IPA（获取plist链接）
     */
    fun getInstallPlist(appId: String, callback: (Boolean, String, String) -> Unit) {
        val formBody = FormBody.Builder()
            .add("appId", appId)
            .build()

        val request = buildRequest("$BASE_URL/user/api/install_the_ipa", "$BASE_URL/select_download_method/$appId.html")
            .header("X-Requested-With", "XMLHttpRequest")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "", "网络错误：${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (body.trimStart().startsWith("<")) {
                    callback(false, "", "__NOT_LOGGED_IN__")
                    return
                }
                try {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    if (status == "success") {
                        val link = json.getJSONObject("data").optString("plist_link", "")
                        callback(true, link, "")
                    } else {
                        callback(false, "", json.optString("message", "获取安装链接失败"))
                    }
                } catch (e: Exception) {
                    callback(false, "", "解析失败：${e.message}")
                }
            }
        })
    }

    fun isLoggedIn(): Boolean {
        return cookieStore.hasCookies()
    }

    fun logout() {
        cookieStore.removeAll()
    }
}
