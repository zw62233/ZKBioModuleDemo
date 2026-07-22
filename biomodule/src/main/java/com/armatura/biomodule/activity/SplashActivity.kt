package com.armatura.biomodule.activity

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import android.window.OnBackInvokedDispatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imageView = findViewById<ImageView>(R.id.logo)
        val scaleXAnimator = ObjectAnimator.ofFloat(
            imageView,
            "scaleX", 1.0f, 1.5f
        )
        scaleXAnimator.duration = 5000
        scaleXAnimator.repeatCount = ValueAnimator.INFINITE
        scaleXAnimator.repeatMode = ValueAnimator.REVERSE
        val scaleYAnimator = ObjectAnimator.ofFloat(
            imageView,
            "scaleY", 1.0f, 1.5f
        )
        scaleYAnimator.duration = 5000
        scaleYAnimator.repeatCount = ValueAnimator.INFINITE
        scaleYAnimator.repeatMode = ValueAnimator.REVERSE
        scaleXAnimator.start()
        scaleYAnimator.start()

        Handler(Looper.getMainLooper()).postDelayed({
            scaleXAnimator.cancel()
            scaleYAnimator.cancel()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }
}