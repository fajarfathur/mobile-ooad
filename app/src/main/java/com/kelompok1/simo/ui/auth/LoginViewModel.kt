package com.kelompok1.simo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok1.simo.data.repository.AuthRepository
import com.kelompok1.simo.data.repository.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data class Success(val profile: UserProfile) : LoginState
    data class Error(val message: String) : LoginState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Email dan password wajib diisi.")
            return
        }
        _state.value = LoginState.Loading
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            _state.value = result.fold(
                onSuccess = { LoginState.Success(it) },
                onFailure = { LoginState.Error(mapError(it)) }
            )
        }
    }

    fun reset() { _state.value = LoginState.Idle }

    private fun mapError(t: Throwable): String {
        val msg = t.message ?: "Terjadi kesalahan."
        return when {
            msg.contains("Invalid login", true) ||
                msg.contains("credentials", true) -> "Email atau password salah."
            msg.contains("Hubungi Administrator", true) -> msg
            msg.contains("belum terdaftar", true) -> msg
            msg.contains("network", true) ||
                msg.contains("host", true) ||
                msg.contains("timeout", true) -> "Gagal terhubung. Cek koneksi internet."
            else -> msg
        }
    }
}
