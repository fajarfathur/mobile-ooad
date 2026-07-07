package com.kelompok1.simo.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok1.simo.data.repository.DashboardData
import com.kelompok1.simo.data.repository.DashboardRepository
import com.kelompok1.simo.util.SessionCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUi {
    data object Loading : DashboardUi
    data class Ready(val data: DashboardData) : DashboardUi
    data class Error(val message: String) : DashboardUi
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    val session: SessionCache
) : ViewModel() {

    private val _ui = MutableStateFlow<DashboardUi>(DashboardUi.Loading)
    val ui: StateFlow<DashboardUi> = _ui

    init { load() }

    fun load() {
        _ui.value = DashboardUi.Loading
        viewModelScope.launch {
            _ui.value = repository.load().fold(
                onSuccess = { DashboardUi.Ready(it) },
                onFailure = { DashboardUi.Error(it.message ?: "Gagal memuat dashboard.") }
            )
        }
    }
}
