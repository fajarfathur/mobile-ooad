package com.kelompok1.simo.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

        lifecycleScope.launch {
            delay(1500)
            val loggedIn = runCatching { authRepository.hasActiveSession() }.getOrDefault(false)
            val next = if (loggedIn) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this@SplashActivity, next))
            finish()
        }
    }
}
