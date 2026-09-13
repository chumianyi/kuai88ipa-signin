package com.kuai88ipa.signin

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import java.io.File

class DetailActivity : AppCompatActivity() {

    private lateinit var ivIcon: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvDescription: TextView
    private lateinit var screenshotContainer: LinearLayout
    private lateinit var scrollContent: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnDownload: Button
    private lateinit var btnInstall: Button

    private var appId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        ivIcon = findViewById(R.id.ivIcon)
        tvName = findViewById(R.id.tvName)
        tvInfo = findViewById(R.id.tvInfo)
        tvDescription = findViewById(R.id.tvDescription)
        screenshotContainer = findViewById(R.id.screenshotContainer)
        scrollContent = findViewById(R.id.scrollContent)
        progressBar = findViewById(R.id.progressBar)
        btnDownload = findViewById(R.id.btnDownload)
        btnInstall = findViewById(R.id.btnInstall)

        appId = intent.getStringExtra("app_id") ?: ""
        val appName = intent.getStringExtra("app_name") ?: "应用详情"
        title = appName

        btnDownload.setOnClickListener { downloadIpa() }
        btnInstall.setOnClickListener { installIpa() }

        loadDetail()
    }

    private fun loadDetail() {
        progressBar.visibility = View.VISIBLE
        scrollContent.visibility = View.GONE

        ApiClient.getInstance(this).fetchAppDetail(appId) { success, detail, error ->
            runOnUiThread {
                progressBar.visibility = View.GONE
                if (success && detail != null) {
                    scrollContent.visibility = View.VISIBLE
                    tvName.text = detail.name
                    val info = buildString {
                        if (detail.version.isNotEmpty()) append("v${detail.version}  ")
                        if (detail.size.isNotEmpty()) append(detail.size)
                        if (detail.iosVersion.isNotEmpty()) append("  ·  ${detail.iosVersion}")
                    }
                    tvInfo.text = info
                    tvDescription.text = detail.description

                    Glide.with(this).load(detail.iconUrl).placeholder(R.drawable.ic_logo).into(ivIcon)

                    // 加载截图
                    screenshotContainer.removeAllViews()
                    detail.screenshots.forEach { screenshotUrl ->
                        val iv = ImageView(this).apply {
                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                marginEnd = 16
                                height = 360
                            }
                            layoutParams = params
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            adjustViewBounds = true
                        }
                        Glide.with(this).load(screenshotUrl).into(iv)
                        screenshotContainer.addView(iv)
                    }
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun downloadIpa() {
        btnDownload.isEnabled = false
        btnDownload.text = "获取链接中..."

        ApiClient.getInstance(this).getDownloadLink(appId) { success, link, error ->
            runOnUiThread {
                btnDownload.isEnabled = true
                btnDownload.text = "下载IPA文件"
                when {
                    error == "__NOT_LOGGED_IN__" -> {
                        Toast.makeText(this, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show()
                    }
                    success && link.isNotEmpty() -> {
                        startDownload(link)
                    }
                    else -> {
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun installIpa() {
        btnInstall.isEnabled = false
        btnInstall.text = "获取安装链接中..."

        ApiClient.getInstance(this).getInstallPlist(appId) { success, plistLink, error ->
            runOnUiThread {
                btnInstall.isEnabled = true
                btnInstall.text = "在线安装"
                when {
                    error == "__NOT_LOGGED_IN__" -> {
                        Toast.makeText(this, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show()
                    }
                    success && plistLink.isNotEmpty() -> {
                        // iOS在线安装通过itms-services协议，Android上无法直接安装IPA
                        // 这里提示用户在iOS设备上打开
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(plistLink))
                        try {
                            startActivity(intent)
                            Toast.makeText(this, "已请求安装，请在iOS设备上确认", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "无法调起安装，请使用iOS设备", Toast.LENGTH_LONG).show()
                        }
                    }
                    else -> {
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun startDownload(url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("下载IPA")
                .setDescription("正在下载 ${tvName.text}.ipa")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "${tvName.text}.ipa")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "开始下载IPA文件...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "下载失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
