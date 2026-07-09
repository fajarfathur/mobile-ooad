package com.kelompok1.simo.ui.hakakses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok1.simo.data.repository.AccessRepository
import com.kelompok1.simo.data.repository.AccessRole
import com.kelompok1.simo.data.repository.AccessUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HakAksesUiState(
    val loading: Boolean = true,
    val roles: List<AccessRole> = emptyList(),
    val users: List<AccessUser> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class HakAksesViewModel @Inject constructor(
    private val repository: AccessRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HakAksesUiState())
    val ui: StateFlow<HakAksesUiState> = _ui

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.load().fold(
                onSuccess = { snapshot ->
                    _ui.value = HakAksesUiState(
                        loading = false,
                        roles = snapshot.roles,
                        users = snapshot.users,
                        message = _ui.value.message
                    )
                },
                onFailure = { err ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            error = err.message ?: "Gagal memuat hak akses."
                        )
                    }
                }
            )
        }
    }

    fun createUser(
        nama: String,
        email: String,
        password: String,
        roleId: String,
        unit: String?
    ) {
        _ui.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.createUser(nama, email, password, roleId, unit).fold(
                onSuccess = { msg ->
                    _ui.update { it.copy(message = msg) }
                    load()
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal membuat akun.") }
                }
            )
        }
    }

    fun updateUser(user: AccessUser, roleId: String, isActive: Boolean, unit: String?) {
        _ui.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.updateUser(user, roleId, isActive, unit).fold(
                onSuccess = { msg ->
                    _ui.update { it.copy(message = msg) }
                    load()
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal menyimpan perubahan.") }
                }
            )
        }
    }
}
