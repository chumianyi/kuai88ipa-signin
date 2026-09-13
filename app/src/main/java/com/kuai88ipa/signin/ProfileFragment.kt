package com.kuai88ipa.signin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    private lateinit var tvEmail: TextView
    private lateinit var btnSignIn: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResult: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvEmail = view.findViewById(R.id.tvEmail)
        btnSignIn = view.findViewById(R.id.btnSignIn)
        btnLogout = view.findViewById(R.id.btnLogout)
        progressBar = view.findViewById(R.id.progressBar)
        tvResult = view.findViewById(R.id.tvResult)

        val email = SessionManager.getSavedEmail(requireContext()).ifEmpty { "已登录" }
        tvEmail.text = email

        btnSignIn.setOnClickListener { doSignIn() }
        btnLogout.setOnClickListener { showLogoutConfirm() }
    }

    private fun doSignIn() {
        btnSignIn.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvResult.visibility = View.GONE

        ApiClient.getInstance(requireContext()).signIn { success, message ->
            activity?.runOnUiThread {
                btnSignIn.isEnabled = true
                progressBar.visibility = View.GONE
                when {
                    message == "__NOT_LOGGED_IN__" -> {
                        Toast.makeText(context, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show()
                        ApiClient.getInstance(requireContext()).logout()
                        goToLogin()
                    }
                    success -> {
                        tvResult.visibility = View.VISIBLE
                        tvResult.text = "✅ $message"
                        tvResult.setTextColor(0xFF2E7D32.toInt())
                    }
                    else -> {
                        tvResult.visibility = View.VISIBLE
                        tvResult.text = "ℹ️ $message"
                        tvResult.setTextColor(0xFFF57C00.toInt())
                    }
                }
            }
        }
    }

    private fun showLogoutConfirm() {
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("退出") { _, _ ->
                ApiClient.getInstance(requireContext()).logout()
                SessionManager.clearCredentials(requireContext())
                goToLogin()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}
