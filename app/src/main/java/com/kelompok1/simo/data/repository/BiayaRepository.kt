package com.kelompok1.simo.data.repository

import com.kelompok1.simo.data.remote.BiayaDetailWriteDto
import com.kelompok1.simo.data.remote.BiayaDto
import com.kelompok1.simo.data.remote.BiayaWriteDto
import com.kelompok1.simo.data.remote.GajiDto
import com.kelompok1.simo.data.remote.KaryawanDto
import com.kelompok1.simo.data.remote.KontrakDto
import com.kelompok1.simo.data.remote.PelatihanDto
import com.kelompok1.simo.data.remote.PresensiDto
import com.kelompok1.simo.util.SessionCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class KontrakOption(
    val id: String,
    val nomor: String
)

data class BiayaFormSnapshot(
    val periods: List<String>,
    val contracts: List<KontrakOption>,
    val defaultPeriod: String?,
    val defaultContractId: String?
)

data class BiayaPreview(
    val summaryText: String,
    val matchedCount: Int,
    val totalActive: Int
)

data class BiayaResult(
    val biayaId: String,
    val contractNumber: String,
    val periode: String,
    val matchedCount: Int,
    val biayaSdm: Double,
    val biayaMaterial: Double,
    val biayaOperasional: Double,
    val totalBiaya: Double
)

private data class PegawaiKalkulasi(
    val karyawan: KaryawanDto,
    val totalPendapatan: Double,
    val hariHadir: Int,
    val bidangLabel: String,
    val kategori: String
)

@Singleton
class BiayaRepository @Inject constructor(
    private val client: SupabaseClient,
    private val session: SessionCache
) {

    suspend fun loadForm(): Result<BiayaFormSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val periods = client.from("komponen_gaji")
                .select { order("periode", Order.DESCENDING) }
                .decodeList<GajiDto>()
                .map { it.periode }
                .distinct()

            val contracts = client.from("kontrak_imo")
                .select { order("periode_mulai", Order.DESCENDING) }
                .decodeList<KontrakDto>()
                .filter { it.status != "selesai" }
                .map { KontrakOption(it.id, it.nomorKontrak) }

            BiayaFormSnapshot(
                periods = periods,
                contracts = contracts,
                defaultPeriod = periods.firstOrNull(),
                defaultContractId = contracts.firstOrNull()?.id
            )
        }
    }

    suspend fun preview(
        periode: String,
        bidang: String,
        level: String
    ): Result<BiayaPreview> = withContext(Dispatchers.IO) {
        runCatching {
            val matched = buildKalkulasiRows(periode, bidang, level)
            val totalActive = client.from("data_karyawan")
                .select()
                .decodeList<KaryawanDto>()
                .count { it.isActive }

            val summary = if (matched.isEmpty()) {
                "Tidak ada pegawai bersertifikasi kecakapan valid untuk filter ini."
            } else {
                "${matched.size} pegawai lolos filter sertifikasi kecakapan pada $bidang."
            }

            BiayaPreview(
                summaryText = "$summary Total pegawai aktif: $totalActive.",
                matchedCount = matched.size,
                totalActive = totalActive
            )
        }
    }

    suspend fun hitungBiaya(
        kontrakId: String,
        periode: String,
        bidang: String,
        level: String
    ): Result<BiayaResult> = withContext(Dispatchers.IO) {
        runCatching {
            requireCostAccounting()

            val kontrak = client.from("kontrak_imo")
                .select { filter { eq("id", kontrakId) } }
                .decodeSingle<KontrakDto>()

            val matched = buildKalkulasiRows(periode, bidang, level)
            check(matched.isNotEmpty()) {
                "Tidak ada pegawai yang memenuhi filter sertifikasi kecakapan untuk dihitung."
            }

            val biayaSdm = matched.sumOf { pegawai ->
                val faktorKehadiran = (pegawai.hariHadir.coerceAtMost(22)).toDouble() / 22.0
                pegawai.totalPendapatan * faktorKehadiran
            }
            val biayaMaterial = biayaSdm * 0.18
            val biayaOperasional = biayaSdm * 0.07
            val totalBiaya = biayaSdm + biayaMaterial + biayaOperasional
            val calculatedAt = OffsetDateTime.now().toString()

            client.from("biaya_pemeliharaan").insert(
                BiayaWriteDto(
                    kontrakId = kontrakId,
                    periode = periode,
                    totalBiaya = totalBiaya,
                    parameter = buildJsonObject {
                        put("bidang", bidang)
                        put("level", level)
                        put("pegawai_terfilter", matched.size)
                    },
                    status = "final",
                    calculatedBy = session.userId ?: error("Sesi pengguna tidak ditemukan."),
                    calculatedAt = calculatedAt
                )
            )

            val biaya = client.from("biaya_pemeliharaan")
                .select {
                    filter {
                        eq("kontrak_id", kontrakId)
                        eq("periode", periode)
                        eq("calculated_at", calculatedAt)
                    }
                }
                .decodeSingle<BiayaDto>()

            val detailRows = matched.map { pegawai ->
                BiayaDetailWriteDto(
                    biayaId = biaya.id,
                    karyawanId = pegawai.karyawan.id,
                    level = pegawai.karyawan.level,
                    kategori = pegawai.kategori,
                    hariHadir = pegawai.hariHadir,
                    biayaSdm = pegawai.totalPendapatan * (pegawai.hariHadir.coerceAtMost(22).toDouble() / 22.0)
                )
            }
            client.from("biaya_detail").insert(detailRows)

            client.insertAuditLog(
                userId = session.userId,
                entitas = "biaya_pemeliharaan",
                aksi = "hitung",
                detail = buildJsonObject {
                    put("biaya_id", biaya.id)
                    put("periode", periode)
                    put("pegawai_terfilter", matched.size)
                    put("total_biaya", totalBiaya)
                }
            )

            BiayaResult(
                biayaId = biaya.id,
                contractNumber = kontrak.nomorKontrak,
                periode = periode,
                matchedCount = matched.size,
                biayaSdm = biayaSdm,
                biayaMaterial = biayaMaterial,
                biayaOperasional = biayaOperasional,
                totalBiaya = totalBiaya
            )
        }
    }

    private suspend fun buildKalkulasiRows(
        periode: String,
        bidang: String,
        level: String
    ): List<PegawaiKalkulasi> {
        val karyawan = client.from("data_karyawan").select().decodeList<KaryawanDto>()
        val gaji = client.from("komponen_gaji")
            .select { filter { eq("periode", periode) } }
            .decodeList<GajiDto>()
            .associateBy { it.karyawanId }
        val presensi = client.from("presensi")
            .select { filter { eq("periode", periode) } }
            .decodeList<PresensiDto>()
            .groupBy { it.karyawanId }
        val pelatihan = client.from("pelatihan_kompetensi")
            .select()
            .decodeList<PelatihanDto>()
            .groupBy { it.karyawanId }

        return karyawan
            .asSequence()
            .filter { it.isActive }
            .filter { level == "Semua Level" || (it.level ?: "").equals(level, true) }
            .mapNotNull { pegawai ->
                val bidangLabel = bidangUntukPegawai(pegawai)
                val sertifikasiValid = pelatihan[pegawai.id].orEmpty().any { it.isValid }
                val sesuaiBidang = bidang == "Semua Bidang" || bidangLabel == bidang
                val gajiPeriode = gaji[pegawai.id] ?: return@mapNotNull null
                if (!sertifikasiValid || !sesuaiBidang) return@mapNotNull null

                val hariHadir = presensi[pegawai.id].orEmpty()
                    .count { it.statusKehadiran.equals("hadir", true) }

                PegawaiKalkulasi(
                    karyawan = pegawai,
                    totalPendapatan = gajiPeriode.totalPendapatan,
                    hariHadir = hariHadir,
                    bidangLabel = bidangLabel,
                    kategori = pegawai.kategori
                )
            }
            .toList()
    }

    private fun bidangUntukPegawai(pegawai: KaryawanDto): String {
        val unit = pegawai.unit.orEmpty().lowercase()
        return when {
            unit.contains("track") || unit.contains("bridge") -> "Jalur & Jembatan"
            unit.contains("signal") -> "Sinyal & Telekomunikasi"
            unit.contains("station") -> "Bangunan"
            unit.contains("electric") || unit.contains("listrik") -> "Listrik"
            else -> "Bidang Lain"
        }
    }

    private fun requireCostAccounting() {
        check(session.roleName == "cost_accounting") {
            "Hanya Cost Accounting yang dapat menghitung biaya pemeliharaan."
        }
    }
}
