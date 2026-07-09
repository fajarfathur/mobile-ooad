package com.kelompok1.simo.ui.kontrak

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
import com.kelompok1.simo.databinding.FragmentKontrakBinding
import com.kelompok1.simo.util.Rupiah
import com.kelompok1.simo.util.SessionCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class KontrakFragment : Fragment() {

    @Inject lateinit var session: SessionCache

    private var _binding: FragmentKontrakBinding? = null
    private val binding get() = _binding!!
    private val viewModel: KontrakViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKontrakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val canManage = session.roleName == "cost_accounting"
        binding.btnTambahKontrak.isVisible = canManage
        binding.btnTambahKontrak.setOnClickListener { showTambahKontrakDialog() }

        if (!canManage) {
            binding.cardStatusKontrak.isVisible = true
            binding.tvStatusKontrak.text = "Mode baca saja. Penambahan kontrak IMO berada pada aktor Cost Accounting."
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ui.collect(::render)
        }
    }

    private fun render(state: KontrakUiState) {
        binding.tvAktifCount.text = state.totalAktif.toString()
        binding.tvDraftCount.text = state.totalDraft.toString()
        binding.tvSelesaiCount.text = state.totalSelesai.toString()

        binding.tvKontrakList.text = when {
            state.loading && state.items.isEmpty() -> "Memuat data dari Supabase…"
            state.items.isEmpty() -> "Belum ada kontrak IMO yang tercatat."
            else -> state.items.joinToString("\n\n") { item ->
                "• ${item.nomor}\n  ${item.nama}\n  Periode: ${item.periodeMulai} s/d ${item.periodeSelesai}\n  Nilai: ${Rupiah.format(item.nilaiKontrak)}\n  Pegawai: ${item.jumlahPegawai} | Status: ${item.status.uppercase()}"
            }
        }

        val status = state.error ?: state.message
        binding.cardStatusKontrak.isVisible = status != null || !binding.btnTambahKontrak.isVisible
        if (status != null) {
            binding.tvStatusKontrak.text = status
        }
    }

    private fun showTambahKontrakDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etNomor = EditText(requireContext()).apply { hint = "Nomor kontrak" }
        val etLingkup = EditText(requireContext()).apply { hint = "Ruang lingkup" }
        val etNilai = EditText(requireContext()).apply {
            hint = "Nilai kontrak"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etMulai = EditText(requireContext()).apply { hint = "Periode mulai (YYYY-MM-DD)" }
        val etSelesai = EditText(requireContext()).apply { hint = "Periode selesai (YYYY-MM-DD)" }

        listOf(etNomor, etLingkup, etNilai, etMulai, etSelesai).forEach { container.addView(it) }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Kontrak IMO")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                viewModel.tambahKontrak(
                    nomorKontrak = etNomor.text.toString(),
                    ruangLingkup = etLingkup.text.toString(),
                    nilaiKontrak = etNilai.text.toString().toDoubleOrNull() ?: 0.0,
                    periodeMulai = LocalDate.parse(etMulai.text.toString()),
                    periodeSelesai = LocalDate.parse(etSelesai.text.toString())
                )
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
