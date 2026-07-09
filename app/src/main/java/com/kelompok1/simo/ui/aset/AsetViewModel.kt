package com.kelompok1.simo.ui.aset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok1.simo.data.repository.AsetItem
import com.kelompok1.simo.data.repository.AsetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AsetUiState(
    val loading: Boolean = true,
    val items: List<AsetItem> = emptyList(),
    val totalAktif: Int = 0,
    val totalPerbaikan: Int = 0,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class AsetViewModel @Inject constructor(
    private val repository: AsetRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AsetUiState())
    val ui: StateFlow<AsetUiState> = _ui

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.load().fold(
                onSuccess = { snapshot ->
                    _ui.value = AsetUiState(
                        loading = false,
                        items = snapshot.items,
                        totalAktif = snapshot.totalAktif,
                        totalPerbaikan = snapshot.totalPerbaikan,
                        message = _ui.value.message
                    )
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal memuat aset.") }
                }
            )
        }
    }

    fun tambahAset(
        kodeAset: String,
        jenis: String,
        lokasi: String,
        nilai: Double,
        kondisi: String,
        kontrakId: String?
    ) {
        _ui.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.tambahAset(kodeAset, jenis, lokasi, nilai, kondisi, kontrakId).fold(
                onSuccess = { msg ->
                    _ui.update { it.copy(message = msg) }
                    load()
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal menambah aset.") }
                }
            )
        }
    }

    fun sinkronisasiSap() {
        _ui.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.sinkronisasiSap().fold(
                onSuccess = { msg ->
                    _ui.update { it.copy(message = msg) }
                    load()
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal sinkronisasi SAP.") }
                }
            )
        }
    }
}
