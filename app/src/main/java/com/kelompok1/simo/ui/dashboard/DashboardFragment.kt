package com.kelompok1.simo.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kelompok1.simo.databinding.FragmentDashboardBinding
import com.kelompok1.simo.util.Rupiah
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private val adapter = AktivitasAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAktivitas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAktivitas.adapter = adapter

        val nama = viewModel.session.namaLengkap ?: "Pengguna"
        val role = when (viewModel.session.roleName) {
            "cost_accounting" -> "Cost Accounting"
            "it_support" -> "IT Support"
            "djka" -> "Otoritas DJKA"
            else -> "-"
        }
        binding.tvGreeting.text = "Halo, $nama"
        binding.tvRole.text = role

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { render(it) }
            }
        }
    }

    private fun render(state: DashboardUi) {
        binding.swipeRefresh.isRefreshing = false
        binding.progress.visibility = if (state is DashboardUi.Loading) View.VISIBLE else View.GONE

        when (state) {
            is DashboardUi.Ready -> {
                val d = state.data
                binding.tvKontrakAktif.text = d.kontrakAktif.toString()
                binding.tvTotalBiaya.text = Rupiah.format(d.totalBiaya)
                binding.tvTagihanMenunggu.text = d.tagihanMenunggu.toString()
                binding.tvStatusSync.text = d.statusSync
                adapter.submit(d.aktivitas)
                binding.tvEmpty.visibility = if (d.aktivitas.isEmpty()) View.VISIBLE else View.GONE
            }
            is DashboardUi.Error -> {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Gagal memuat: ${state.message}"
            }
            DashboardUi.Loading -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
