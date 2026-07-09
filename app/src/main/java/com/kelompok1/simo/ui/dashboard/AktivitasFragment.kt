package com.kelompok1.simo.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kelompok1.simo.databinding.FragmentAktivitasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * AktivitasFragment — Tampilan lengkap semua aktivitas/log
 * Dipanggil saat user tap "Lihat semua →" di Dashboard
 */
@AndroidEntryPoint
class AktivitasFragment : Fragment() {

    private var _b: FragmentAktivitasBinding? = null
    private val binding get() = _b!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentAktivitasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AktivitasAdapter()
        binding.rvSemuaAktivitas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSemuaAktivitas.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ui.collect { state ->
                when (state) {
                    is DashboardUi.Loading -> {
                        binding.progressAktivitas.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                    }
                    is DashboardUi.Ready -> {
                        binding.progressAktivitas.visibility = View.GONE
                        val list = state.data.aktivitas
                        adapter.submit(list)
                        binding.tvAktivitasCount.text = "${list.size} aktivitas tercatat"
                        binding.layoutEmpty.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                    is DashboardUi.Error -> {
                        binding.progressAktivitas.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
