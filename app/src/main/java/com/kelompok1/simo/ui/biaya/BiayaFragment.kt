package com.kelompok1.simo.ui.biaya

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.kelompok1.simo.databinding.FragmentBiayaBinding
import com.kelompok1.simo.util.Rupiah
import com.kelompok1.simo.util.SessionCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BiayaFragment : Fragment() {

    @Inject lateinit var session: SessionCache

    private var _binding: FragmentBiayaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BiayaViewModel by viewModels()

    private val bidangOptions = listOf(
        "Semua Bidang",
        "Jalur & Jembatan",
        "Sinyal & Telekomunikasi",
        "Listrik",
        "Bangunan"
    )
    private val levelOptions = listOf("Semua Level", "Junior", "Madya", "Ahli")
    private var initializedStaticAdapters = false
    private var initializedDynamicAdapters = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBiayaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val canCalculate = session.roleName == "cost_accounting"
        binding.btnHitung.isVisible = canCalculate
        binding.btnEkspor.isVisible = canCalculate

        setupStaticAdapters()
        setupListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ui.collect(::render)
        }
    }

    private fun setupStaticAdapters() {
        if (initializedStaticAdapters) return
        initializedStaticAdapters = true
        binding.spinnerBidang.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            bidangOptions
        )
        binding.spinnerLevel.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            levelOptions
        )
    }

    private fun setupListeners() {
        val previewListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                triggerPreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.spinnerKontrak.onItemSelectedListener = previewListener
        binding.spinnerPeriode.onItemSelectedListener = previewListener
        binding.spinnerBidang.onItemSelectedListener = previewListener
        binding.spinnerLevel.onItemSelectedListener = previewListener

        binding.btnHitung.setOnClickListener {
            val kontrak = currentContractId() ?: return@setOnClickListener
            val periode = currentPeriode() ?: return@setOnClickListener
            viewModel.hitungBiaya(kontrak, periode, currentBidang(), currentLevel())
        }

        binding.btnEkspor.setOnClickListener {
            val result = viewModel.ui.value.result
            if (result == null) {
                showResultText("Belum ada hasil kalkulasi yang bisa diekspor.")
                return@setOnClickListener
            }
            showResultText(
                buildString {
                    append(formatResult(result))
                    append("\n\nEkspor aktif: ringkasan final sudah tersimpan di Supabase dan siap dijadikan lampiran billing.")
                }
            )
        }
    }

    private fun render(state: BiayaUiState) {
        if (!initializedDynamicAdapters && state.periods.isNotEmpty()) {
            initializedDynamicAdapters = true
            binding.spinnerPeriode.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                state.periods
            )
            binding.spinnerKontrak.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                state.contracts.map { it.nomor }
            )
        }

        binding.tvHasilFilter.text = state.filterSummary

        state.result?.let {
            showResultText(formatResult(it))
        }

        state.error?.let { showResultText("⚠ $it") }
        if (state.message != null && state.result == null) {
            showResultText(state.message)
        }
    }

    private fun triggerPreview() {
        val periode = currentPeriode() ?: return
        viewModel.preview(periode, currentBidang(), currentLevel())
    }

    private fun currentPeriode(): String? =
        binding.spinnerPeriode.selectedItem?.toString()

    private fun currentContractId(): String? {
        val idx = binding.spinnerKontrak.selectedItemPosition
        val contracts = viewModel.ui.value.contracts
        return contracts.getOrNull(idx)?.id
    }

    private fun currentBidang(): String =
        binding.spinnerBidang.selectedItem?.toString() ?: bidangOptions.first()

    private fun currentLevel(): String =
        binding.spinnerLevel.selectedItem?.toString() ?: levelOptions.first()

    private fun formatResult(result: com.kelompok1.simo.data.repository.BiayaResult): String =
        """
        REKAP BIAYA PEMELIHARAAN
        ────────────────────────
        Periode       : ${result.periode}
        Kontrak       : ${result.contractNumber}
        Pegawai Lolos : ${result.matchedCount}

        Komponen Biaya:
        • Biaya SDM
          → ${Rupiah.format(result.biayaSdm)}
        • Biaya Material
          → ${Rupiah.format(result.biayaMaterial)}
        • Biaya Operasional
          → ${Rupiah.format(result.biayaOperasional)}

        ────────────────────────
        TOTAL BIAYA FINAL
        ${Rupiah.format(result.totalBiaya)}
        ────────────────────────
        Status: FINAL — siap untuk billing
        Ref Biaya: ${result.biayaId}
        """.trimIndent()

    private fun showResultText(text: String) {
        binding.cardHasil.isVisible = true
        binding.tvHasilKalkulasi.isVisible = true
        binding.tvHasilKalkulasi.text = text
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
