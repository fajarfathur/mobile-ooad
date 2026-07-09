package com.kelompok1.simo.ui.sinkronisasi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kelompok1.simo.databinding.FragmentSinkronisasiBinding

/**
 * Modul Integrasi SAP ERP SDM (4.7) & Modul-modul integrasi lainnya
 * Menampilkan status sinkronisasi data dari SAP ERP, Presensi, Pelatihan, Penggajian
 * Aktor: SAP ERP (primer), Cost Accounting (monitoring)
 */
class SinkronisasiFragment : Fragment() {

    private var _binding: FragmentSinkronisasiBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSinkronisasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSyncStatus()
        setupButtons()
    }

    private fun loadSyncStatus() {
        val syncItems = listOf(
            listOf("SAP ERP SDM",    "Berhasil",  "2026-07-08 06:00", "1.247 record"),
            listOf("Sistem Presensi","Berhasil",  "2026-07-08 05:45", "842 record"),
            listOf("Sistem Pelatihan","Berhasil", "2026-07-07 23:00", "319 record"),
            listOf("Sistem Penggajian","Gagal",   "2026-07-07 22:00", "Koneksi timeout"),
        )
        binding.tvLogSync.text = syncItems.joinToString("\n\n") { (sumber, status, waktu, info) ->
            val icon = if (status == "Berhasil") "✅" else "❌"
            "$icon $sumber\n  Status: $status | $waktu\n  $info"
        }
    }

    private fun setupButtons() {
        binding.btnSinkronisasi.setOnClickListener {
            binding.tvLogSync.text = "🔄 Memulai sinkronisasi manual...\n\n" +
                "✅ SAP ERP SDM — OK (1.247 record)\n\n" +
                "✅ Sistem Presensi — OK (842 record)\n\n" +
                "✅ Sistem Pelatihan — OK (319 record)\n\n" +
                "✅ Sistem Penggajian — OK (756 record)\n\n" +
                "Sinkronisasi selesai: ${java.time.LocalDateTime.now().toString().take(16)}"
            binding.cardStatusSync.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
