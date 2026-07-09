package com.kelompok1.simo.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok1.simo.data.repository.BillingRepository
import com.kelompok1.simo.data.repository.TagihanItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillingUiState(
    val loading: Boolean = true,
    val items: List<TagihanItem> = emptyList(),
    val totalDiverifikasi: Int = 0,
    val totalTerbit: Int = 0,
    val totalDraft: Int = 0,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val repository: BillingRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(BillingUiState())
    val ui: StateFlow<BillingUiState> = _ui

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.load().fold(
                onSuccess = { snapshot ->
                    _ui.value = BillingUiState(
                        loading = false,
                        items = snapshot.items,
                        totalDiverifikasi = snapshot.totalDiverifikasi,
                        totalTerbit = snapshot.totalTerbit,
                        totalDraft = snapshot.totalDraft,
                        message = _ui.value.message
                    )
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal memuat tagihan.") }
                }
            )
        }
    }

    fun generateBilling() {
        _ui.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.generateBilling().fold(
                onSuccess = { msg ->
                    _ui.update { it.copy(message = msg) }
                    load()
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal generate billing.") }
                }
            )
        }
    }

    fun verifikasi(tagihanId: String) {
        _ui.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.verifikasiTagihan(tagihanId).fold(
                onSuccess = { msg ->
                    _ui.update { it.copy(message = msg) }
                    load()
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal verifikasi tagihan.") }
                }
            )
        }
    }

    fun revisi(tagihanId: String, catatan: String) {
        _ui.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.revisiTagihan(tagihanId, catatan).fold(
                onSuccess = { msg ->
                    _ui.update { it.copy(message = msg) }
                    load()
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal mengembalikan tagihan.") }
                }
            )
        }
    }
}
