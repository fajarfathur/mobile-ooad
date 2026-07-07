package com.kelompok1.simo.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("ratified_at") val ratifiedAt: String? = null
)

@Serializable
data class TagihanDto(
    val id: String,
    @SerialName("nomor_tagihan") val nomorTagihan: String,
    @SerialName("total_tagihan") val totalTagihan: Double = 0.0,
    val status: String = "draft",
    @SerialName("tanggal_terbit") val tanggalTerbit: String? = null,
    @SerialName("kontrak_id") val kontrakId: String? = null
)

@Serializable
data class BiayaDto(
    val id: String,
    @SerialName("kontrak_id") val kontrakId: String? = null,
    val periode: String,
    @SerialName("total_biaya") val totalBiaya: Double = 0.0,
    val status: String = "draft"
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
    @SerialName("created_at") val createdAt: String? = null
)
