package com.kelompok1.simo.data.repository

import com.kelompok1.simo.BuildConfig
import com.kelompok1.simo.data.remote.AppUserDto
import com.kelompok1.simo.data.remote.AppUserUpdateDto
import com.kelompok1.simo.data.remote.AppUserWriteDto
import com.kelompok1.simo.data.remote.RoleDto
import com.kelompok1.simo.util.SessionCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class AccessRole(
    val id: String,
    val nama: String,
    val deskripsi: String?
) {
    val label: String
        get() = when (nama) {
            "cost_accounting" -> "Cost Accounting"
            "djka" -> "Otoritas DJKA"
            "sap_erp" -> "SAP ERP"
            else -> nama
        }
}

data class AccessUser(
    val id: String,
    val nama: String,
    val email: String,
    val roleId: String,
    val roleNama: String,
    val unit: String?,
    val isActive: Boolean
)

data class AccessSnapshot(
    val roles: List<AccessRole>,
    val users: List<AccessUser>
)

@Singleton
class AccessRepository @Inject constructor(
    private val client: SupabaseClient,
    private val session: SessionCache
) {

    suspend fun load(): Result<AccessSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val roles = client.from("roles")
                .select { order("nama", Order.ASCENDING) }
                .decodeList<RoleDto>()
                .map { AccessRole(it.id, it.nama, it.deskripsi) }

            val roleMap = roles.associateBy { it.id }
            val users = client.from("app_users")
                .select { order("nama_lengkap", Order.ASCENDING) }
                .decodeList<AppUserDto>()
                .mapNotNull { dto ->
                    val role = dto.roleId?.let(roleMap::get) ?: return@mapNotNull null
                    AccessUser(
                        id = dto.id,
                        nama = dto.namaLengkap,
                        email = dto.email,
                        roleId = role.id,
                        roleNama = role.nama,
                        unit = dto.unit,
                        isActive = dto.isActive
                    )
                }

            AccessSnapshot(roles, users)
        }
    }

    suspend fun createUser(
        nama: String,
        email: String,
        password: String,
        roleId: String,
        unit: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireCostAccounting()

            val secondary = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
            }

            val userInfo = secondary.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }

            val userId = userInfo?.id ?: error("Gagal mendapatkan ID user dari proses registrasi Supabase.")
            client.from("app_users").insert(
                AppUserWriteDto(
                    id = userId,
                    namaLengkap = nama.trim(),
                    email = email.trim(),
                    roleId = roleId,
                    unit = unit?.trim().takeUnless { it.isNullOrBlank() },
                    isActive = true
                )
            )

            client.insertAuditLog(
                userId = session.userId,
                entitas = "app_users",
                aksi = "create_user",
                detail = buildJsonObject {
                    put("target_user_id", userId)
                    put("nama_lengkap", nama.trim())
                    put("role_id", roleId)
                }
            )

            runCatching { secondary.auth.signOut() }
            "Akun baru berhasil dibuat dan diberi hak akses."
        }
    }

    suspend fun updateUser(
        user: AccessUser,
        roleId: String,
        isActive: Boolean,
        unit: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireCostAccounting()

            client.from("app_users").update(
                AppUserUpdateDto(
                    roleId = roleId,
                    unit = unit?.trim().takeUnless { it.isNullOrBlank() },
                    isActive = isActive
                )
            ) {
                filter { eq("id", user.id) }
            }

            client.insertAuditLog(
                userId = session.userId,
                entitas = "app_users",
                aksi = "update_role_status",
                detail = buildJsonObject {
                    put("target_user_id", user.id)
                    put("new_role_id", roleId)
                    put("is_active", isActive)
                }
            )

            "Perubahan hak akses berhasil disimpan."
        }
    }

    private fun requireCostAccounting() {
        check(session.roleName == "cost_accounting") {
            "Hanya Cost Accounting yang dapat mengelola user & role."
        }
    }
}
