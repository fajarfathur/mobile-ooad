package com.kelompok1.simo.data.repository

import com.kelompok1.simo.data.remote.KaryawanDto
import com.kelompok1.simo.data.remote.KontrakDto
import com.kelompok1.simo.data.remote.KontrakWriteDto
import com.kelompok1.simo.util.SessionCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class KontrakItem(
    val id: String,
    val nomor: String,
    val nama: String,
    val status: String,
    val jumlahPegawai: Int,
    val nilaiKontrak: Double,
    val periodeMulai: String,
    val periodeSelesai: String
)

data class KontrakSnapshot(
    val items: List<KontrakItem>,
    val totalAktif: Int,
    val totalDraft: Int,
    val totalSelesai: Int
)

@Singleton
class KontrakRepository @Inject constructor(
    private val client: SupabaseClient,
    private val session: SessionCache
) {

    suspend fun load(): Result<KontrakSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val items = client.from("kontrak_imo")
                .select { order("periode_mulai", Order.DESCENDING) }
                .decodeList<KontrakDto>()
                .map { dto ->
                    KontrakItem(
                        id = dto.id,
                        nomor = dto.nomorKontrak,
                        nama = dto.ruangLingkup ?: "Kontrak IMO",
                        status = dto.status,
                        jumlahPegawai = dto.jumlahPegawai,
                        nilaiKontrak = dto.nilaiKontrak,
                        periodeMulai = dto.periodeMulai,
                        periodeSelesai = dto.periodeSelesai
                    )
                }

            KontrakSnapshot(
                items = items,
                totalAktif = items.count { it.status in setOf("disepakati", "diratifikasi") },
                totalDraft = items.count { it.status == "draft" || it.status == "revisi" },
                totalSelesai = items.count { it.status == "selesai" }
            )
        }
    }

    suspend fun tambahKontrak(
        nomorKontrak: String,
        ruangLingkup: String,
        nilaiKontrak: Double,
        periodeMulai: LocalDate,
        periodeSelesai: LocalDate
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireCostAccounting()

            val jumlahPegawai = client.from("data_karyawan")
                .select()
                .decodeList<KaryawanDto>()
                .count { it.isActive }

            client.from("kontrak_imo").insert(
                KontrakWriteDto(
                    nomorKontrak = nomorKontrak.trim(),
                    periodeMulai = periodeMulai.toString(),
                    periodeSelesai = periodeSelesai.toString(),
                    nilaiKontrak = nilaiKontrak,
                    ruangLingkup = ruangLingkup.trim(),
                    jumlahPegawai = jumlahPegawai,
                    status = "draft",
                    createdBy = session.userId ?: error("Sesi pengguna tidak ditemukan."),
                    ratifiedAt = null
                )
            )

            client.insertAuditLog(
                userId = session.userId,
                entitas = "kontrak_imo",
                aksi = "insert_draft",
                detail = buildJsonObject {
                    put("nomor_kontrak", nomorKontrak.trim())
                    put("jumlah_pegawai", jumlahPegawai)
                }
            )

            "Kontrak IMO baru berhasil disimpan sebagai draft."
        }
    }

    private fun requireCostAccounting() {
        check(session.roleName == "cost_accounting") {
            "Hanya Cost Accounting yang dapat menambah kontrak IMO."
        }
    }
}
