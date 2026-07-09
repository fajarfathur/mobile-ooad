package com.kelompok1.simo.ui.aset

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.kelompok1.simo.databinding.FragmentAsetBinding
import com.kelompok1.simo.util.Rupiah
import com.kelompok1.simo.util.SessionCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AsetFragment : Fragment() {

    @Inject lateinit var session: SessionCache

    private var _binding: FragmentAsetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AsetViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAsetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val canManage = session.roleName == "sap_erp"
        binding.btnSinkronSap.isVisible = canManage
        binding.btnTambahAset.isVisible = canManage

        binding.btnSinkronSap.setOnClickListener { viewModel.sinkronisasiSap() }
        binding.btnTambahAset.setOnClickListener { showTambahAsetDialog() }

        if (!canManage) {
            binding.tvStatusSync.isVisible = true
            binding.tvStatusSync.text = "Mode baca saja. Revisi OOAD menetapkan pengelolaan aset ada pada aktor SAP ERP."
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ui.collect(::render)
        }
    }

    private fun render(state: AsetUiState) {
        binding.tvAsetList.text = when {
            state.loading && state.items.isEmpty() -> "Memuat data aset dari Supabase…"
            state.items.isEmpty() -> "Belum ada aset infrastruktur yang tercatat."
            else -> state.items.joinToString("\n\n") { item ->
                val icon = if (item.status == "aktif") "🟢" else "🟡"
                "$icon ${item.kodeAset}\n  ${item.namaTampil}\n  Kondisi: ${item.kondisi} | Status: ${item.status.uppercase()}\n  Nilai kontrak: ${item.kontrakLabel ?: "-"}"
            }
        }

        val status = state.error ?: state.message
        if (status != null) {
            binding.tvStatusSync.isVisible = true
            binding.tvStatusSync.text = "$status\nAktif: ${state.totalAktif} | Perbaikan: ${state.totalPerbaikan}"
        } else if (!binding.btnSinkronSap.isVisible) {
            binding.tvStatusSync.isVisible = true
        }
    }

    private fun showTambahAsetDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etKode = EditText(requireContext()).apply { hint = "Kode aset" }
        val etJenis = EditText(requireContext()).apply { hint = "Jenis aset" }
        val etLokasi = EditText(requireContext()).apply { hint = "Lokasi / deskripsi" }
        val etNilai = EditText(requireContext()).apply {
            hint = "Nilai aset"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etKondisi = EditText(requireContext()).apply { hint = "Kondisi (Baik / Perbaikan)" }

        listOf(etKode, etJenis, etLokasi, etNilai, etKondisi).forEach { container.addView(it) }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Aset Infrastruktur")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                viewModel.tambahAset(
                    kodeAset = etKode.text.toString(),
                    jenis = etJenis.text.toString(),
                    lokasi = etLokasi.text.toString(),
                    nilai = etNilai.text.toString().toDoubleOrNull() ?: 0.0,
                    kondisi = etKondisi.text.toString().ifBlank { "Baik" },
                    kontrakId = null
                )
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
