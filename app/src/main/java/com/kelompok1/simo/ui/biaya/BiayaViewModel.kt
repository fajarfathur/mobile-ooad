package com.kelompok1.simo.ui.biaya

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok1.simo.data.repository.BiayaFormSnapshot
import com.kelompok1.simo.data.repository.BiayaRepository
import com.kelompok1.simo.data.repository.BiayaResult
import com.kelompok1.simo.data.repository.KontrakOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BiayaUiState(
    val loading: Boolean = true,
    val periods: List<String> = emptyList(),
    val contracts: List<KontrakOption> = emptyList(),
    val selectedPeriod: String? = null,
    val selectedContractId: String? = null,
    val filterSummary: String = "Memuat filter sertifikasi kecakapan…",
    val result: BiayaResult? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class BiayaViewModel @Inject constructor(
    private val repository: BiayaRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(BiayaUiState())
    val ui: StateFlow<BiayaUiState> = _ui

    init { loadForm() }

    fun loadForm() {
        _ui.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.loadForm().fold(
                onSuccess = { snapshot ->
                    _ui.value = fromSnapshot(snapshot)
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal memuat form biaya.") }
                }
            )
        }
    }

    fun preview(periode: String, bidang: String, level: String) {
        _ui.update { it.copy(loading = true, error = null, selectedPeriod = periode) }
        viewModelScope.launch {
            repository.preview(periode, bidang, level).fold(
                onSuccess = { preview ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            filterSummary = preview.summaryText,
                            selectedPeriod = periode
                        )
                    }
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal membaca preview filter.") }
                }
            )
        }
    }

    fun hitungBiaya(kontrakId: String, periode: String, bidang: String, level: String) {
        _ui.update {
            it.copy(
                loading = true,
                error = null,
                selectedContractId = kontrakId,
                selectedPeriod = periode,
                message = null
            )
        }
        viewModelScope.launch {
            repository.hitungBiaya(kontrakId, periode, bidang, level).fold(
                onSuccess = { result ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            result = result,
                            message = "Kalkulasi biaya berhasil disimpan ke Supabase."
                        )
                    }
                },
                onFailure = { err ->
                    _ui.update { it.copy(loading = false, error = err.message ?: "Gagal menghitung biaya.") }
                }
            )
        }
    }

    private fun fromSnapshot(snapshot: BiayaFormSnapshot) = BiayaUiState(
        loading = false,
        periods = snapshot.periods,
        contracts = snapshot.contracts,
        selectedPeriod = snapshot.defaultPeriod,
        selectedContractId = snapshot.defaultContractId
    )
}
