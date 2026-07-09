package com.kelompok1.simo.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.kelompok1.simo.R
import com.kelompok1.simo.databinding.ActivityLoginBinding
import com.kelompok1.simo.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateEntrance()

        // Tap demo pill → auto-fill dan langsung login
        binding.demoCA.setOnClickListener { fillDemo("cost.accounting@simo.dev") }
        binding.demoDJKA.setOnClickListener { fillDemo("otoritas.djka@simo.dev") }
        binding.demoSAP.setOnClickListener { fillDemo("sap.erp@simo.dev") }

        binding.btnMasuk.setOnClickListener {
            viewModel.login(
                binding.etEmail.text?.toString().orEmpty(),
                binding.etPassword.text?.toString().orEmpty()
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun animateEntrance() {
        binding.hero.alpha = 0f
        binding.hero.animate().alpha(1f).setDuration(400).start()
        listOf(binding.formCard, binding.demoCard).forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 60f
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(180L + i * 120).setDuration(480).start()
        }
    }

    private fun render(state: LoginState) {
        val loading = state is LoginState.Loading
        binding.progress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnMasuk.isEnabled = !loading
        binding.btnMasuk.text = if (loading) "Memproses…" else "Masuk"

        when (state) {
            is LoginState.Success -> {
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            is LoginState.Error -> {
                val msg = when {
                    state.message.contains("credentials", true) ||
                    state.message.contains("password", true)    -> "❌ Email atau password salah. Pastikan menggunakan: demo123"
                    state.message.contains("email", true)       -> "❌ Format email tidak valid"
                    state.message.contains("network", true) ||
                    state.message.contains("connect", true)     -> "❌ Tidak ada koneksi internet"
                    else -> "❌ ${state.message}"
                }
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(R.color.error))
                    .setTextColor(getColor(R.color.white))
                    .show()
                viewModel.reset()
            }
            else -> Unit
        }
    }

    private fun fillDemo(email: String) {
        binding.etEmail.setText(email)
        binding.etPassword.setText("demo123")
        // Langsung login
        viewModel.login(email, "demo123")
    }
}
