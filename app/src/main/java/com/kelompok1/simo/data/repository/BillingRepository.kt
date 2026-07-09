package com.kelompok1.simo.data.repository

import com.kelompok1.simo.data.remote.BiayaDto
import com.kelompok1.simo.data.remote.KontrakDto
import com.kelompok1.simo.data.remote.TagihanDto
import com.kelompok1.simo.data.remote.TagihanUpdateDto
import com.kelompok1.simo.data.remote.TagihanWriteDto
import com.kelompok1.simo.util.SessionCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class TagihanItem(
    val id: String,
    val nomor: String,
    val kontrakNomor: String,
    val totalTagihan: Double,
    val status: String,
    val tanggalTerbit: String?,
    val catatanDjka: String?
)

data class BillingSnapshot(
    val items: List<TagihanItem>,
    val totalDiverifikasi: Int,
    val totalTerbit: Int,
    val totalDraft: Int
)

@Singleton
class BillingRepository @Inject constructor(
    private val client: SupabaseClient,
    private val session: SessionCache
) {

    suspend fun load(): Result<BillingSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val kontrakMap = client.from("kontrak_imo")
                .select()
                .decodeList<KontrakDto>()
                .associateBy { it.id }

            val items = client.from("tagihan")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<TagihanDto>()
                .map { dto ->
                    TagihanItem(
                        id = dto.id,
                        nomor = dto.nomorTagihan,
                        kontrakNomor = dto.kontrakId?.let { kontrakMap[it]?.nomorKontrak } ?: "-",
                        totalTagihan = dto.totalTagihan,
                        status = dto.status,
                        tanggalTerbit = dto.tanggalTerbit,
                        catatanDjka = dto.catatanDjka
                    )
                }

            BillingSnapshot(
                items = items,
                totalDiverifikasi = items.count { it.status == "terverifikasi" },
                totalTerbit = items.count { it.status == "terkirim" },
                totalDraft = items.count { it.status == "draft" || it.status == "perlu_revisi" }
            )
        }
    }

    suspend fun generateBilling(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireCostAccounting()

            val tagihan = client.from("tagihan").select().decodeList<TagihanDto>()
            val biayaFinal = client.from("biaya_pemeliharaan")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<BiayaDto>()
                .filter { it.status == "final" }

            val targetBiaya = biayaFinal.firstOrNull { biaya ->
                tagihan.none { it.biayaId == biaya.id }
            } ?: error("Belum ada hasil kalkulasi final yang siap ditagihkan.")

            val kontrak = client.from("kontrak_imo")
                .select { filter { eq("id", targetBiaya.kontrakId ?: "") } }
                .decodeSingle<KontrakDto>()

            val now = LocalDate.now()
            val nomor = "TAG-${now.year}-${now.monthValue.toString().padStart(2, '0')}${now.dayOfMonth.toString().padStart(2, '0')}-${(tagihan.size + 1).toString().padStart(3, '0')}"

            client.from("tagihan").insert(
                TagihanWriteDto(
                    nomorTagihan = nomor,
                    kontrakId = targetBiaya.kontrakId ?: error("Kontrak tagihan tidak tersedia."),
                    biayaId = targetBiaya.id,
                    totalTagihan = targetBiaya.totalBiaya,
                    status = "terkirim",
                    tanggalTerbit = now.toString(),
                    createdBy = session.userId ?: error("Sesi pengguna tidak ditemukan.")
                )
            )

            client.insertAuditLog(
                userId = session.userId,
                entitas = "tagihan",
                aksi = "terbit",
                detail = buildJsonObject {
                    put("nomor_tagihan", nomor)
                    put("kontrak_id", kontrak.id)
                    put("biaya_id", targetBiaya.id)
                }
            )

            "Tagihan $nomor berhasil diterbitkan untuk kontrak ${kontrak.nomorKontrak}."
        }
    }

    suspend fun verifikasiTagihan(tagihanId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireDjka()
            updateTagihan(
                tagihanId = tagihanId,
                status = "terverifikasi",
                catatan = "Tagihan diverifikasi DJKA.",
                aksi = "verifikasi"
            )
            "Tagihan berhasil diverifikasi oleh DJKA."
        }
    }

    suspend fun revisiTagihan(tagihanId: String, catatan: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireDjka()
            updateTagihan(
                tagihanId = tagihanId,
                status = "perlu_revisi",
                catatan = catatan.ifBlank { "Perlu revisi rincian billing." },
                aksi = "revisi"
            )
            "Tagihan berhasil dikembalikan untuk revisi."
        }
    }

    private suspend fun updateTagihan(
        tagihanId: String,
        status: String,
        catatan: String,
        aksi: String
    ) {
        client.from("tagihan").update(
            TagihanUpdateDto(
                status = status,
                catatanDjka = catatan,
                verifiedBy = session.userId,
                verifiedAt = OffsetDateTime.now().toString()
            )
        ) {
            filter { eq("id", tagihanId) }
        }

        client.insertAuditLog(
            userId = session.userId,
            entitas = "tagihan",
            aksi = aksi,
            detail = buildJsonObject {
                put("tagihan_id", tagihanId)
                put("status", status)
            }
        )
    }

    private fun requireCostAccounting() {
        check(session.roleName == "cost_accounting") {
            "Hanya Cost Accounting yang dapat generate billing."
        }
    }

    private fun requireDjka() {
        check(session.roleName == "djka") {
            "Hanya Otoritas DJKA yang dapat memverifikasi tagihan."
        }
    }
}
