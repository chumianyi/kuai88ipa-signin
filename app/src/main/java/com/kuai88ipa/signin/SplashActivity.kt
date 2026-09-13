package com.kuai88ipa.signin

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd

class SplashActivity : AppCompatActivity() {

    private lateinit var ivLogo: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvLoading: TextView
    private lateinit var gradientBackground: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        ivLogo = findViewById(R.id.ivLogo)
        tvAppName = findViewById(R.id.tvAppName)
        tvLoading = findViewById(R.id.tvLoading)
        gradientBackground = findViewById(R.id.gradientBackground)

        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        // Logo 缩放+淡入+旋转动画（Overshoot弹性效果）
        val logoScaleX = ObjectAnimator.ofFloat(ivLogo, "scaleX", 0f, 1f).apply {
            duration = 800
            interpolator = OvershootInterpolator(2.5f)
        }
        val logoScaleY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 0f, 1f).apply {
            duration = 800
            interpolator = OvershootInterpolator(2.5f)
        }
        val logoAlpha = ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f).apply {
            duration = 600
        }
        val logoRotation = ObjectAnimator.ofFloat(ivLogo, "rotation", -180f, 0f).apply {
            duration = 900
            interpolator = AccelerateDecelerateInterpolator()
        }

        val logoSet = AnimatorSet()
        logoSet.playTogether(logoScaleX, logoScaleY, logoAlpha, logoRotation)
        logoSet.start()

        // App名称淡入上移
        val nameAlpha = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f).apply {
            startDelay = 400
            duration = 600
        }
        val nameTranslationY = ObjectAnimator.ofFloat(tvAppName, "translationY", 60f, 0f).apply {
            startDelay = 400
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
        }
        val nameSet = AnimatorSet()
        nameSet.playTogether(nameAlpha, nameTranslationY)
        nameSet.start()

        // 加载文字呼吸闪烁
        val loadingAlpha = ObjectAnimator.ofFloat(tvLoading, "alpha", 0.3f, 1f, 0.3f).apply {
            startDelay = 800
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        loadingAlpha.start()

        // 背景渐变缓慢流动效果
        val bgAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { _ ->
                // 背景由drawable渐变实现，这里仅占位
            }
        }
        bgAnimator.start()

        // 总时长2.5秒后跳转
        ivLogo.postDelayed({
            navigateToNext()
        }, 2500)
    }

    private fun navigateToNext() {
        val apiClient = ApiClient.getInstance(this)
        val intent = if (apiClient.isLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
