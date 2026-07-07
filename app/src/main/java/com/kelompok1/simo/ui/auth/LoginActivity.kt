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

    private fun render(state: LoginState) {
        val loading = state is LoginState.Loading
        binding.progress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnMasuk.isEnabled = !loading

        when (state) {
            is LoginState.Success -> {
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            is LoginState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(R.color.error))
                    .setTextColor(getColor(R.color.white))
                    .show()
                viewModel.reset()
            }
            else -> Unit
        }
    }
}
