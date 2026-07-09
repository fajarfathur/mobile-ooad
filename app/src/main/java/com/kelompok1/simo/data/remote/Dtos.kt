package com.kelompok1.simo.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** DTO = bentuk data mentah dari tabel Supabase (nama kolom snake_case). */

@Serializable
data class RoleDto(
    val id: String,
    val nama: String,
    val deskripsi: String? = null
)

@Serializable
data class AppUserDto(
    val id: String,
    @SerialName("nama_lengkap") val namaLengkap: String,
    val email: String,
    @SerialName("role_id") val roleId: String? = null,
    val unit: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class KontrakDto(
    val id: String,
    @SerialName("nomor_kontrak") val nomorKontrak: String,
    @SerialName("periode_mulai") val periodeMulai: String,
    @SerialName("periode_selesai") val periodeSelesai: String,
    @SerialName("nilai_kontrak") val nilaiKontrak: Double = 0.0,
    @SerialName("ruang_lingkup") val ruangLingkup: String? = null,
    @SerialName("jumlah_pegawai") val jumlahPegawai: Int = 0,
    val status: String = "draft",
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("ratified_at") val ratifiedAt: String? = null
)

@Serializable
data class TagihanDto(
    val id: String,
    @SerialName("nomor_tagihan") val nomorTagihan: String,
    @SerialName("total_tagihan") val totalTagihan: Double = 0.0,
    val status: String = "draft",
    @SerialName("tanggal_terbit") val tanggalTerbit: String? = null,
    @SerialName("kontrak_id") val kontrakId: String? = null,
    @SerialName("biaya_id") val biayaId: String? = null,
    @SerialName("catatan_djka") val catatanDjka: String? = null,
    @SerialName("verified_by") val verifiedBy: String? = null,
    @SerialName("verified_at") val verifiedAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class BiayaDto(
    val id: String,
    @SerialName("kontrak_id") val kontrakId: String? = null,
    val periode: String,
    @SerialName("total_biaya") val totalBiaya: Double = 0.0,
    val status: String = "draft",
    val parameter: JsonObject? = null,
    @SerialName("calculated_by") val calculatedBy: String? = null,
    @SerialName("calculated_at") val calculatedAt: String? = null
)

@Serializable
data class AsetDto(
    val id: String,
    @SerialName("kode_aset") val kodeAset: String,
    val jenis: String? = null,
    val lokasi: String? = null,
    val nilai: Double = 0.0,
    val kondisi: String? = null,
    val status: String = "aktif",
    @SerialName("kontrak_id") val kontrakId: String? = null
)

@Serializable
data class KaryawanDto(
    val id: String,
    val nip: String,
    val nama: String,
    val jabatan: String? = null,
    val unit: String? = null,
    val level: String? = null,
    val kategori: String = "umum",
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class PelatihanDto(
    val id: String,
    @SerialName("karyawan_id") val karyawanId: String,
    @SerialName("nama_pelatihan") val namaPelatihan: String? = null,
    val sertifikasi: String? = null,
    @SerialName("tanggal_terbit") val tanggalTerbit: String? = null,
    @SerialName("tanggal_kedaluwarsa") val tanggalKedaluwarsa: String? = null,
    val kategori: String = "umum",
    @SerialName("is_valid") val isValid: Boolean = true
)

@Serializable
data class PresensiDto(
    val id: String,
    @SerialName("karyawan_id") val karyawanId: String,
    val periode: String,
    val tanggal: String,
    @SerialName("status_kehadiran") val statusKehadiran: String? = null,
    val lokasi: String? = null
)

@Serializable
data class GajiDto(
    val id: String,
    @SerialName("karyawan_id") val karyawanId: String,
    val periode: String,
    @SerialName("gaji_pokok") val gajiPokok: Double = 0.0,
    val tunjangan: Double = 0.0,
    @SerialName("total_pendapatan") val totalPendapatan: Double = 0.0
)

@Serializable
data class BiayaDetailDto(
    val id: String,
    @SerialName("biaya_id") val biayaId: String,
    @SerialName("karyawan_id") val karyawanId: String? = null,
    val level: String? = null,
    val kategori: String = "umum",
    @SerialName("hari_hadir") val hariHadir: Int = 0,
    @SerialName("biaya_sdm") val biayaSdm: Double = 0.0
)

@Serializable
data class SyncLogDto(
    val id: String,
    val sumber: String,
    val status: String? = null,
    @SerialName("jumlah_record") val jumlahRecord: Int = 0,
    val pesan: String? = null,
    @SerialName("synced_at") val syncedAt: String? = null
)

@Serializable
data class AuditLogDto(
    val id: String,
    val entitas: String? = null,
    val aksi: String? = null,
    val detail: JsonObject? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AppUserWriteDto(
    val id: String,
    @SerialName("nama_lengkap") val namaLengkap: String,
    val email: String,
    @SerialName("role_id") val roleId: String,
    val unit: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class AppUserUpdateDto(
    @SerialName("role_id") val roleId: String,
    val unit: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class KontrakWriteDto(
    @SerialName("nomor_kontrak") val nomorKontrak: String,
    @SerialName("periode_mulai") val periodeMulai: String,
    @SerialName("periode_selesai") val periodeSelesai: String,
    @SerialName("nilai_kontrak") val nilaiKontrak: Double,
    @SerialName("ruang_lingkup") val ruangLingkup: String,
    @SerialName("jumlah_pegawai") val jumlahPegawai: Int,
    val status: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("ratified_at") val ratifiedAt: String? = null
)

@Serializable
data class AsetWriteDto(
    @SerialName("kode_aset") val kodeAset: String,
    val jenis: String,
    val lokasi: String,
    val nilai: Double,
    val kondisi: String,
    val status: String,
    @SerialName("kontrak_id") val kontrakId: String? = null
)

@Serializable
data class BiayaWriteDto(
    @SerialName("kontrak_id") val kontrakId: String,
    val periode: String,
    @SerialName("total_biaya") val totalBiaya: Double,
    val parameter: JsonObject,
    val status: String,
    @SerialName("calculated_by") val calculatedBy: String,
    @SerialName("calculated_at") val calculatedAt: String
)

@Serializable
data class BiayaDetailWriteDto(
    @SerialName("biaya_id") val biayaId: String,
    @SerialName("karyawan_id") val karyawanId: String,
    val level: String? = null,
    val kategori: String,
    @SerialName("hari_hadir") val hariHadir: Int,
    @SerialName("biaya_sdm") val biayaSdm: Double
)

@Serializable
data class TagihanWriteDto(
    @SerialName("nomor_tagihan") val nomorTagihan: String,
    @SerialName("kontrak_id") val kontrakId: String,
    @SerialName("biaya_id") val biayaId: String,
    @SerialName("total_tagihan") val totalTagihan: Double,
    val status: String,
    @SerialName("tanggal_terbit") val tanggalTerbit: String,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class TagihanUpdateDto(
    val status: String,
    @SerialName("catatan_djka") val catatanDjka: String? = null,
    @SerialName("verified_by") val verifiedBy: String? = null,
    @SerialName("verified_at") val verifiedAt: String? = null
)

@Serializable
data class SyncLogWriteDto(
    val sumber: String,
    val status: String,
    @SerialName("jumlah_record") val jumlahRecord: Int,
    val pesan: String
)

@Serializable
data class AuditLogWriteDto(
    @SerialName("user_id") val userId: String,
    val entitas: String,
    val aksi: String,
    val detail: JsonObject
)
