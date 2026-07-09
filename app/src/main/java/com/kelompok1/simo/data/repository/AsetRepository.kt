package com.kelompok1.simo.data.repository

import com.kelompok1.simo.data.remote.AsetDto
import com.kelompok1.simo.data.remote.AsetWriteDto
import com.kelompok1.simo.data.remote.KontrakDto
import com.kelompok1.simo.data.remote.SyncLogWriteDto
import com.kelompok1.simo.util.SessionCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class AsetItem(
    val id: String,
    val kodeAset: String,
    val namaTampil: String,
    val jenis: String,
    val lokasi: String,
    val kondisi: String,
    val status: String,
    val kontrakLabel: String?
)

data class AsetSnapshot(
    val items: List<AsetItem>,
    val totalAktif: Int,
    val totalPerbaikan: Int
)

@Singleton
class AsetRepository @Inject constructor(
    private val client: SupabaseClient,
    private val session: SessionCache
) {

    suspend fun load(): Result<AsetSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val kontrakMap = client.from("kontrak_imo")
                .select()
                .decodeList<KontrakDto>()
                .associateBy { it.id }

            val items = client.from("aset_infrastruktur")
                .select { order("kode_aset", Order.ASCENDING) }
                .decodeList<AsetDto>()
                .map { dto ->
                    val jenis = dto.jenis ?: "Aset Infrastruktur"
                    val lokasi = dto.lokasi ?: "Lokasi belum diisi"
                    AsetItem(
                        id = dto.id,
                        kodeAset = dto.kodeAset,
                        namaTampil = "$jenis — $lokasi",
                        jenis = jenis,
                        lokasi = lokasi,
                        kondisi = dto.kondisi ?: "Belum ada kondisi",
                        status = dto.status,
                        kontrakLabel = dto.kontrakId?.let { kontrakMap[it]?.nomorKontrak }
                    )
                }

            AsetSnapshot(
                items = items,
                totalAktif = items.count { it.status == "aktif" },
                totalPerbaikan = items.count { it.status == "perbaikan" }
            )
        }
    }

    suspend fun tambahAset(
        kodeAset: String,
        jenis: String,
        lokasi: String,
        nilai: Double,
        kondisi: String,
        kontrakId: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireSapErp()

            client.from("aset_infrastruktur").insert(
                AsetWriteDto(
                    kodeAset = kodeAset.trim(),
                    jenis = jenis.trim(),
                    lokasi = lokasi.trim(),
                    nilai = nilai,
                    kondisi = kondisi.trim(),
                    status = if (kondisi.equals("Baik", true)) "aktif" else "perbaikan",
                    kontrakId = kontrakId
                )
            )

            client.insertAuditLog(
                userId = session.userId,
                entitas = "aset_infrastruktur",
                aksi = "insert",
                detail = buildJsonObject {
                    put("kode_aset", kodeAset.trim())
                    put("jenis", jenis.trim())
                }
            )

            "Aset infrastruktur baru berhasil ditambahkan."
        }
    }

    suspend fun sinkronisasiSap(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireSapErp()
            val assets = client.from("aset_infrastruktur").select().decodeList<AsetDto>()

            client.from("sync_log").insert(
                SyncLogWriteDto(
                    sumber = "sap_master",
                    status = "sukses",
                    jumlahRecord = assets.size,
                    pesan = "Sinkronisasi aset SAP ERP berhasil dijalankan dari aplikasi."
                )
            )

            client.insertAuditLog(
                userId = session.userId,
                entitas = "sync_log",
                aksi = "sinkronisasi_aset",
                detail = buildJsonObject {
                    put("sumber", "sap_master")
                    put("jumlah_record", assets.size)
                }
            )

            "Sinkronisasi SAP ERP tercatat dengan ${assets.size} aset pada log."
        }
    }

    private fun requireSapErp() {
        check(session.roleName == "sap_erp") {
            "Hanya SAP ERP yang dapat mengelola aset infrastruktur."
        }
    }
}
