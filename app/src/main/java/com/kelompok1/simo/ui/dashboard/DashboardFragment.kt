package com.kelompok1.simo.ui.dashboard

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kelompok1.simo.R
import com.kelompok1.simo.databinding.FragmentDashboardBinding
import com.kelompok1.simo.ui.biaya.BiayaFragment
import com.kelompok1.simo.ui.billing.BillingFragment
import com.kelompok1.simo.ui.kontrak.KontrakFragment
import com.kelompok1.simo.ui.main.MainActivity
import com.kelompok1.simo.ui.widget.QuarterlyChartView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lottie
        try { binding.lottieHero.playAnimation() } catch (_: Exception) {}

        // Greeting info
        binding.tvGreeting.text = viewModel.session.namaLengkap ?: "Pengguna"
        binding.tvRole.text = when (viewModel.session.roleName) {
            "cost_accounting" -> "Cost Accounting"
            "djka"            -> "Otoritas DJKA"
            "sap_erp"         -> "SAP ERP"
            else              -> "Pengguna"
        }

        // RecyclerView aktivitas
        val adapter = AktivitasAdapter()
        binding.rvAktivitas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAktivitas.isNestedScrollingEnabled = false
        binding.rvAktivitas.adapter = adapter

        // ===== Click pada stat cards =====
        binding.cardKontrak.setOnClickListener { navigateTo(KontrakFragment(), "Kontrak IMO") }
        binding.cardBiaya.setOnClickListener   { navigateTo(BiayaFragment(), "Kalkulasi Biaya") }
        binding.cardTagihan.setOnClickListener { navigateTo(BillingFragment(), "Billing & Tagihan") }

        // Quick actions
        binding.qaKontrak.setOnClickListener  { navigateTo(KontrakFragment(), "Kontrak IMO") }
        binding.qaBiaya.setOnClickListener    { navigateTo(BiayaFragment(), "Kalkulasi Biaya") }
        binding.qaBilling.setOnClickListener  { navigateTo(BillingFragment(), "Billing & Tagihan") }

        // Lihat semua
        binding.tvLihatSemua.setOnClickListener { navigateTo(AktivitasFragment(), "Semua Aktivitas") }

        // ===== Swipe refresh =====
        binding.swipeRefresh.setColorSchemeColors(
            resources.getColor(R.color.primary, null)
        )
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.load()
            binding.swipeRefresh.isRefreshing = false
        }

        // ===== Animasi entrance — staggered =====
        animateEntrance()

        // ===== Animasi lottie float =====
        try { startFloatAnimation(binding.lottieHero) } catch (_: Exception) {}

        // ===== Chart placeholder =====
        setupChart(null)

        // ===== Observe data =====
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ui.collect { state ->
                when (state) {
                    is DashboardUi.Loading -> {
                        binding.progress.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                    }
                    is DashboardUi.Ready -> {
                        binding.progress.visibility = View.GONE
                        val d = state.data
                        animateCounter(0, d.kontrakAktif)   { binding.tvKontrakValue.text = it.toString() }
                        binding.tvBiayaValue.text = formatBiaya(d.totalBiaya)
                        animateCounter(0, d.tagihanMenunggu) { binding.tvTagihanValue.text = it.toString() }
                        adapter.submit(d.aktivitas.take(5))
                        binding.tvEmpty.visibility = if (d.aktivitas.isEmpty()) View.VISIBLE else View.GONE
                        setupChart(d.totalBiaya)
                        // Animate chart in
                        binding.chartView.animate().alpha(1f).setDuration(600).start()
                    }
                    is DashboardUi.Error -> {
                        binding.progress.visibility = View.GONE
                        binding.tvEmpty.text = "⚠ ${state.message}"
                        binding.tvEmpty.visibility = View.VISIBLE
                        setupChart(null)
                    }
                }
            }
        }
    }

    // ================================================================
    // ANIMASI
    // ================================================================

    private fun animateEntrance() {
        // Hero header fade + scale
        binding.lottieHero.apply {
            alpha = 0f; scaleX = 0.8f; scaleY = 0.8f
            animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(500).setStartDelay(100)
                .setInterpolator(OvershootInterpolator(1.2f)).start()
        }

        // Greeting slide in
        binding.tvGreeting.apply {
            translationX = -24f; alpha = 0f
            animate().translationX(0f).alpha(1f)
                .setDuration(400).setStartDelay(150)
                .setInterpolator(DecelerateInterpolator()).start()
        }
        binding.tvRole.apply {
            translationX = -20f; alpha = 0f
            animate().translationX(0f).alpha(1f)
                .setDuration(400).setStartDelay(220)
                .setInterpolator(DecelerateInterpolator()).start()
        }

        // Stat cards staggered bounce entrance
        listOf(binding.cardKontrak, binding.cardBiaya, binding.cardTagihan)
            .forEachIndexed { i, card ->
                card.alpha = 0f
                card.translationY = 40f
                card.scaleX = 0.9f
                card.scaleY = 0.9f
                card.animate()
                    .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(300L + i * 90L)
                    .setDuration(450)
                    .setInterpolator(OvershootInterpolator(1.1f))
                    .start()
            }

        // Chart card slide in
        // (akan di-trigger setelah data masuk, tapi init dulu)
        binding.chartView.alpha = 0f
    }

    private fun startFloatAnimation(v: View) {
        ObjectAnimator.ofFloat(v, "translationY", 0f, -8f, 0f).apply {
            duration = 3200
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 600
        }.start()
    }

    private fun animateCounter(from: Int, to: Int, update: (Int) -> Unit) {
        ValueAnimator.ofInt(from, to).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            addUpdateListener { update(it.animatedValue as Int) }
        }.start()
    }

    // ================================================================
    // CHART
    // ================================================================

    private fun setupChart(realBiaya: Double?) {
        val base = realBiaya?.div(1_000_000)?.toFloat()?.coerceAtLeast(100f) ?: 380f
        val entries = listOf(
            QuarterlyChartView.BarEntry("Q1", base * 0.70f, formatM(base * 0.70f), R.color.chart_q1),
            QuarterlyChartView.BarEntry("Q2", base * 0.85f, formatM(base * 0.85f), R.color.chart_q2),
            QuarterlyChartView.BarEntry("Q3", base * 0.62f, formatM(base * 0.62f), R.color.chart_q3),
            QuarterlyChartView.BarEntry("Q4", base * 1.00f, formatM(base * 1.00f), R.color.chart_q4),
        )
        binding.chartView.setData(entries, animate = true)
        binding.chartView.onBarTapped = { entry, _ -> showChartDetail(entry, entries) }
    }

    private fun showChartDetail(
        selected: QuarterlyChartView.BarEntry,
        all: List<QuarterlyChartView.BarEntry>
    ) {
        val dialog = BottomSheetDialog(requireContext(), R.style.Widget_SIMO_BottomSheet)
        val v = View.inflate(requireContext(), R.layout.bottom_sheet_chart_detail, null)
        dialog.setContentView(v)
        v.findViewById<android.widget.TextView>(R.id.tvDetailPeriode)?.text =
            "${selected.label} — 2024"
        v.findViewById<android.widget.TextView>(R.id.tvDetailNilai)?.text =
            "Rp ${selected.valueFmt}"
        val pct = (selected.value / all.maxOf { it.value } * 100).toInt()
        v.findViewById<android.widget.TextView>(R.id.tvDetailStatus)?.text =
            if (pct >= 90) "Tertinggi ▲" else if (pct >= 60) "Normal ●" else "Terendah ▼"
        v.findViewById<android.widget.TextView>(R.id.tvDetailPersen)?.text =
            "$pct% dari kuartal tertinggi"
        dialog.show()
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun formatBiaya(v: Double) = when {
        v >= 1_000_000_000 -> "Rp ${String.format("%.1f", v / 1_000_000_000)}M"
        v >= 1_000_000     -> "Rp ${(v / 1_000_000).toInt()}Jt"
        else               -> "Rp ${v.toInt()}"
    }

    private fun formatM(v: Float) = when {
        v >= 1000 -> "${String.format("%.1f", v / 1000)}M"
        else      -> "${v.toInt()}Jt"
    }

    private fun navigateTo(fragment: Fragment, title: String) {
        (requireActivity() as? MainActivity)?.showFragment(fragment, title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { binding.lottieHero.cancelAnimation() } catch (_: Exception) {}
        _binding = null
    }
}
