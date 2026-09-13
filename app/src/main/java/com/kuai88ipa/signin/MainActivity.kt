package com.kuai88ipa.signin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnSignIn: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvWelcome = findViewById(R.id.tvWelcome)
        tvEmail = findViewById(R.id.tvEmail)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)
        tvResult = findViewById(R.id.tvResult)

        // 显示用户邮箱
        val email = SessionManager.getSavedEmail(this).ifEmpty { "已登录" }
        tvEmail.text = email

        btnSignIn.setOnClickListener {
            doSignIn()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirm()
        }
    }

    private fun doSignIn() {
        setSignInLoading(true)
        tvResult.visibility = View.GONE

        ApiClient.getInstance(this).signIn { success, message ->
            runOnUiThread {
                setSignInLoading(false)
                when {
                    message == "__NOT_LOGGED_IN__" -> {
                        // 未登录，跳回登录页
                        Toast.makeText(this, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show()
                        ApiClient.getInstance(this).logout()
                        goToLogin()
                    }
                    success -> {
                        showResult(true, message)
                    }
                    else -> {
                        showResult(false, message)
                    }
                }
            }
        }
    }

    private fun setSignInLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSignIn.isEnabled = !loading
        btnSignIn.text = if (loading) "签到中..." else "一键签到"
    }

    private fun showResult(success: Boolean, message: String) {
        tvResult.visibility = View.VISIBLE
        if (success) {
            tvResult.text = "✅ $message"
            tvResult.setTextColor(0xFF2E7D32.toInt())
        } else {
            tvResult.text = "ℹ️ $message"
            tvResult.setTextColor(0xFFF57C00.toInt())
        }
    }

    private fun showLogoutConfirm() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("退出") { _, _ ->
                ApiClient.getInstance(this).logout()
                SessionManager.clearCredentials(this)
                goToLogin()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // 主页禁止返回键直接退出（可选：直接退出应用）
        finishAffinity()
    }
}
