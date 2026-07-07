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
            val kontrak = client.from("kontrak_imo").select().decodeList<KontrakDto>()
            val kontrakAktif = kontrak.count { it.status == "disepakati" || it.status == "diratifikasi" }

            val biaya = client.from("biaya_pemeliharaan").select().decodeList<BiayaDto>()
            val totalBiaya = biaya.filter { it.status == "final" }.sumOf { it.totalBiaya }

            val tagihan = client.from("tagihan").select().decodeList<TagihanDto>()
            val tagihanMenunggu = tagihan.count { it.status == "terkirim" }

            val sync = client.from("sync_log")
                .select { order("synced_at", Order.DESCENDING); limit(1L) }
                .decodeList<SyncLogDto>()
                .firstOrNull()
            val statusSync = sync?.let { "${it.sumber} • ${it.status}" } ?: "Belum ada"

            val audit = client.from("audit_log")
                .select { order("created_at", Order.DESCENDING); limit(10L) }
                .decodeList<AuditLogDto>()
                .map { Aktivitas(it.entitas ?: "-", it.aksi ?: "-", (it.createdAt ?: "").take(10)) }

            DashboardData(kontrakAktif, totalBiaya, tagihanMenunggu, statusSync, audit)
        }
    }
}
