package com.kelompok1.simo.ui.billing

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.kelompok1.simo.databinding.FragmentBillingBinding
import com.kelompok1.simo.util.Rupiah
import com.kelompok1.simo.util.SessionCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BillingFragment : Fragment() {

    @Inject lateinit var session: SessionCache

    private var _binding: FragmentBillingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BillingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupByRole(session.roleName ?: "cost_accounting")

        binding.btnGenerate.setOnClickListener { viewModel.generateBilling() }
        binding.btnVerifikasi.setOnClickListener { chooseTagihanForVerification() }
        binding.btnRevisi.setOnClickListener { chooseTagihanForRevision() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ui.collect(::render)
        }
    }

    private fun setupByRole(role: String) {
        when (role) {
            "djka" -> {
                binding.btnGenerate.isVisible = false
                binding.sectionGenerate.isVisible = false
                binding.tvSubtitle.text = "Otoritas DJKA — UC-25: Verifikasi Tagihan"
            }
            "cost_accounting" -> {
                binding.btnVerifikasi.isVisible = false
                binding.btnRevisi.isVisible = false
                binding.tvSubtitle.text = "Cost Accounting — UC-24: Generate Billing ke DJKA"
            }
            else -> {
                binding.btnGenerate.isVisible = false
                binding.sectionGenerate.isVisible = false
                binding.btnVerifikasi.isVisible = false
                binding.btnRevisi.isVisible = false
            }
        }
    }

    private fun render(state: BillingUiState) {
        binding.tvVerifiedCount.text = state.totalDiverifikasi.toString()
        binding.tvSentCount.text = state.totalTerbit.toString()
        binding.tvDraftCount.text = state.totalDraft.toString()

        binding.tvTagihanList.text = when {
            state.loading && state.items.isEmpty() -> "Memuat data dari Supabase…"
            state.items.isEmpty() -> "Belum ada tagihan billing yang tercatat."
            else -> state.items.joinToString("\n\n") { item ->
                val badge = when (item.status) {
                    "terverifikasi" -> "✅"
                    "terkirim" -> "📤"
                    "perlu_revisi" -> "⚠"
                    else -> "📝"
                }
                "$badge ${item.nomor}\n  ${item.kontrakNomor}\n  ${Rupiah.format(item.totalTagihan)}\n  Status: ${item.status.uppercase()}" +
                    if (item.catatanDjka.isNullOrBlank()) "" else "\n  Catatan DJKA: ${item.catatanDjka}"
            }
        }

        val status = state.error ?: state.message
        binding.cardStatus.isVisible = status != null
        binding.tvStatusBilling.text = status.orEmpty()
    }

    private fun chooseTagihanForVerification() {
        val candidates = viewModel.ui.value.items.filter { it.status == "terkirim" }
        if (candidates.isEmpty()) {
            binding.cardStatus.isVisible = true
            binding.tvStatusBilling.text = "Tidak ada tagihan berstatus TERKIRIM yang siap diverifikasi."
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Tagihan")
            .setItems(candidates.map { it.nomor }.toTypedArray()) { _, which ->
                viewModel.verifikasi(candidates[which].id)
            }
            .show()
    }

    private fun chooseTagihanForRevision() {
        val candidates = viewModel.ui.value.items.filter { it.status == "terkirim" }
        if (candidates.isEmpty()) {
            binding.cardStatus.isVisible = true
            binding.tvStatusBilling.text = "Tidak ada tagihan berstatus TERKIRIM yang bisa dikembalikan."
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Tagihan")
            .setItems(candidates.map { it.nomor }.toTypedArray()) { _, which ->
                val noteInput = EditText(requireContext()).apply {
                    hint = "Catatan revisi DJKA"
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Catatan Revisi")
                    .setView(noteInput)
                    .setNegativeButton("Batal", null)
                    .setPositiveButton("Kirim") { _, _ ->
                        viewModel.revisi(candidates[which].id, noteInput.text.toString())
                    }
                    .show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
