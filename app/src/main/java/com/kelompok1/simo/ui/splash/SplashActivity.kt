package com.kelompok1.simo.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kelompok1.simo.R
import com.kelompok1.simo.data.repository.AuthRepository
import com.kelompok1.simo.databinding.ActivitySplashBinding
import com.kelompok1.simo.ui.auth.LoginActivity
import com.kelompok1.simo.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateEntrance()

        lifecycleScope.launch {
            delay(1500)
            val loggedIn = runCatching { authRepository.hasActiveSession() }.getOrDefault(false)
            val next = if (loggedIn) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this@SplashActivity, next))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }

    private fun animateEntrance() {
        binding.logoCard.apply {
            alpha = 0f; scaleX = 0.6f; scaleY = 0.6f; rotation = -12f
            animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f)
                .setStartDelay(120).setDuration(650)
                .setInterpolator(OvershootInterpolator(1.4f)).start()
        }
        listOf(binding.tvLogo, binding.tvTagline).forEachIndexed { i, v ->
            v.alpha = 0f; v.translationY = 40f
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(420L + i * 120).setDuration(500).start()
        }
    }
}
