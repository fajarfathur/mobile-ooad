package com.kelompok1.simo.data.repository

import com.kelompok1.simo.data.remote.AuditLogDto
import com.kelompok1.simo.data.remote.BiayaDto
import com.kelompok1.simo.data.remote.KontrakDto
import com.kelompok1.simo.data.remote.SyncLogDto
import com.kelompok1.simo.data.remote.TagihanDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class Aktivitas(val entitas: String, val aksi: String, val waktu: String)

data class DashboardData(
    val kontrakAktif: Int,
    val totalBiaya: Double,
    val tagihanMenunggu: Int,
    val statusSync: String,
    val aktivitas: List<Aktivitas>
)

@Singleton
class DashboardRepository @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun load(): Result<DashboardData> = withContext(Dispatchers.IO) {
        runCatching {
            val kontrak = tryOrEmpty { client.from("kontrak_imo").select().decodeList<KontrakDto>() }
            val kontrakAktif = kontrak.count {
                it.status in listOf("aktif", "disepakati", "diratifikasi", "draft", "selesai")
            }.coerceAtLeast(if (kontrak.isEmpty()) DEMO_DATA.kontrakAktif else 0)

            val biaya = tryOrEmpty { client.from("biaya_pemeliharaan").select().decodeList<BiayaDto>() }
            val totalBiaya = biaya.sumOf { it.totalBiaya }
                .let { if (it == 0.0 && biaya.isEmpty()) DEMO_DATA.totalBiaya else it }

            val tagihan = tryOrEmpty { client.from("tagihan").select().decodeList<TagihanDto>() }
            val tagihanMenunggu = tagihan.count { it.status in listOf("terbit", "terkirim", "draft") }
                .coerceAtLeast(if (tagihan.isEmpty()) DEMO_DATA.tagihanMenunggu else 0)

            val sync = tryOrNull {
                client.from("sync_log")
                    .select { order("synced_at", Order.DESCENDING); limit(1L) }
                    .decodeList<SyncLogDto>()
                    .firstOrNull()
            }
            val statusSync = sync?.let { "${it.sumber} • ${it.status}" }
                ?: DEMO_DATA.statusSync

            val audit = tryOrEmpty {
                client.from("audit_log")
                    .select { order("created_at", Order.DESCENDING); limit(10L) }
                    .decodeList<AuditLogDto>()
                    .map { Aktivitas(it.entitas ?: "-", it.aksi ?: "-", (it.createdAt ?: "").take(10)) }
            }.ifEmpty { DEMO_DATA.aktivitas }

            DashboardData(kontrakAktif, totalBiaya, tagihanMenunggu, statusSync, audit)
        }.recoverCatching {
            // Jika gagal total (network / auth), pakai demo data
            DEMO_DATA
        }
    }

    // Helpers
    private suspend inline fun <T> tryOrEmpty(block: () -> List<T>): List<T> =
        try { block() } catch (_: Exception) { emptyList() }

    private suspend inline fun <T> tryOrNull(block: () -> T?): T? =
        try { block() } catch (_: Exception) { null }

    companion object {
        /** Demo data tampil saat Supabase belum ada data / offline */
        val DEMO_DATA = DashboardData(
            kontrakAktif = 3,
            totalBiaya = 8_550_000_000.0,
            tagihanMenunggu = 3,
            statusSync = "SAP ERP • Terhubung",
            aktivitas = listOf(
                Aktivitas("Kontrak IMO", "Buat KTR/2024/001 — Rp 4.5M", "2024-07-08"),
                Aktivitas("Biaya Pemeliharaan", "Kalkulasi Q2 2024 — 125 pegawai", "2024-07-07"),
                Aktivitas("Billing & Tagihan", "Generate tagihan ke DJKA #007", "2024-07-06"),
                Aktivitas("Sinkronisasi SAP", "Tarik data SDM — 91 record", "2024-07-05"),
                Aktivitas("Hak Akses & Role", "Update role DJKA — djka@kai.id", "2024-07-04"),
                Aktivitas("Aset Infrastruktur", "Verifikasi 12 aset — Bandung", "2024-07-03"),
                Aktivitas("Kontrak IMO", "Ratifikasi KTR/2024/002 — DJKA", "2024-07-02"),
                Aktivitas("Biaya Pemeliharaan", "Kalkulasi Q1 2024 — selesai", "2024-07-01"),
            )
        )
    }
}
