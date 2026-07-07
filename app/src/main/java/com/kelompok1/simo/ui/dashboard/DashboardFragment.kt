package com.kelompok1.simo.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kelompok1.simo.R
import com.kelompok1.simo.databinding.FragmentDashboardBinding
import com.kelompok1.simo.databinding.ViewStatCardBinding
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
        binding.rvAktivitas.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down)

        styleCard(binding.cardKontrak, R.drawable.ic_contract, R.drawable.bg_circle_blue, R.color.accent_blue, "Kontrak Aktif")
        styleCard(binding.cardTagihan, R.drawable.ic_billing, R.drawable.bg_circle_amber, R.color.accent_amber, "Tagihan Menunggu")
        styleCard(binding.cardBiaya, R.drawable.ic_cost, R.drawable.bg_circle_green, R.color.accent_green, "Total Biaya Final")
        styleCard(binding.cardSync, R.drawable.ic_sync, R.drawable.bg_circle_purple, R.color.accent_purple, "Sinkronisasi")
        binding.cardBiaya.tvValue.textSize = 16f
        binding.cardSync.tvValue.textSize = 14f

        val nama = viewModel.session.namaLengkap ?: "Pengguna"
        val role = when (viewModel.session.roleName) {
            "cost_accounting" -> "Cost Accounting"
            "it_support" -> "IT Support"
            "djka" -> "Otoritas DJKA"
            else -> "-"
        }
        binding.tvGreeting.text = nama
        binding.tvRole.text = role

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        animateCards()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { render(it) }
            }
        }
    }

    private fun styleCard(
        c: ViewStatCardBinding, iconRes: Int, circleBg: Int, tintColor: Int, label: String
    ) {
        c.statIcon.setImageResource(iconRes)
        c.statIconBg.setBackgroundResource(circleBg)
        c.statIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor))
        c.tvLabel.text = label
    }

    private fun animateCards() {
        val cards = listOf(
            binding.greetingCard, binding.cardKontrak.root, binding.cardTagihan.root,
            binding.cardBiaya.root, binding.cardSync.root
        )
        cards.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 50f
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(i * 90L).setDuration(450).start()
        }
    }

    private fun render(state: DashboardUi) {
        binding.swipeRefresh.isRefreshing = false
        binding.progress.visibility = if (state is DashboardUi.Loading) View.VISIBLE else View.GONE

        when (state) {
            is DashboardUi.Ready -> {
                val d = state.data
                binding.cardKontrak.tvValue.text = d.kontrakAktif.toString()
                binding.cardTagihan.tvValue.text = d.tagihanMenunggu.toString()
                binding.cardBiaya.tvValue.text = Rupiah.format(d.totalBiaya)
                binding.cardBiaya.tvValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
                binding.cardSync.tvValue.text = d.statusSync
                adapter.submit(d.aktivitas)
                binding.rvAktivitas.scheduleLayoutAnimation()
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
