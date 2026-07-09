package com.kelompok1.simo.ui.kontrak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok1.simo.data.repository.KontrakItem
import com.kelompok1.simo.data.repository.KontrakRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class KontrakUiState(
    val loading: Boolean = true,
    val items: List<KontrakItem> = emptyList(),
    val totalAktif: Int = 0,
    val totalDraft: Int = 0,
    val totalSelesai: Int = 0,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class KontrakViewModel @Inject constructor(
    private val repository: KontrakRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(KontrakUiState())
    val ui: StateFlow<KontrakUiState> = _ui

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.load().fold(
                onSuccess = { snapshot ->
                    _ui.value = KontrakUiState(
                        loading = false,
                        items = snapshot.items,
                        totalAktif = snapshot.totalAktif,
                        totalDraft = snapshot.totalDraft,
                        totalSelesai = snapshot.totalSelesai,
                        message = _ui.value.message
                    )
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal memuat kontrak.") }
                }
            )
        }
    }

    fun tambahKontrak(
        nomorKontrak: String,
        ruangLingkup: String,
        nilaiKontrak: Double,
        periodeMulai: LocalDate,
        periodeSelesai: LocalDate
    ) {
        _ui.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.tambahKontrak(nomorKontrak, ruangLingkup, nilaiKontrak, periodeMulai, periodeSelesai).fold(
                onSuccess = { msg ->
                    _ui.update { it.copy(message = msg) }
                    load()
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal menambah kontrak.") }
                }
            )
        }
    }
}
