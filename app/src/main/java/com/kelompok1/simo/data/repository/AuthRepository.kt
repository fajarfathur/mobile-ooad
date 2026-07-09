package com.kelompok1.simo.data.repository

import com.kelompok1.simo.data.remote.AppUserDto
import com.kelompok1.simo.data.remote.RoleDto
import com.kelompok1.simo.util.SessionCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val userId: String,
    val nama: String,
    val email: String,
    val role: String,
    val unit: String?
)

@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient,
    private val session: SessionCache
) {
    /**
     * Login ke Supabase Auth.
     * Jika `app_users` belum ada record (RLS / DB belum di-seed),
     * fallback ke role berdasarkan email agar demo tetap bisa jalan.
     */
    suspend fun login(email: String, password: String): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Auth Supabase
                client.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                val uid = client.auth.currentUserOrNull()?.id
                    ?: error("Sesi tidak valid setelah login.")

                // 2. Ambil profil dari app_users — fallback jika tidak ada
                val user: AppUserDto? = runCatching {
                    client.from("app_users")
                        .select { filter { eq("id", uid) } }
                        .decodeSingleOrNull<AppUserDto>()
                }.getOrNull()

                // 3. Tentukan role — dari DB atau fallback email-based
                val roleName: String
                val namaLengkap: String
                val unit: String?

                if (user != null) {
                    if (!user.isActive) error("Akun nonaktif. Hubungi Administrator.")
                    roleName = user.roleId?.let { rid ->
                        runCatching {
                            client.from("roles")
                                .select { filter { eq("id", rid) } }
                                .decodeSingleOrNull<RoleDto>()?.nama
                        }.getOrNull()
                    } ?: roleByEmail(email)
                    namaLengkap = user.namaLengkap
                    unit = user.unit
                } else {
                    // Fallback: user ada di Supabase Auth tapi belum di app_users
                    roleName    = roleByEmail(email)
                    namaLengkap = namaByEmail(email)
                    unit        = unitByRole(roleName)
                }

                session.save(uid, namaLengkap, email.trim(), roleName, unit)
                UserProfile(uid, namaLengkap, email.trim(), roleName, unit)
            }
        }

    suspend fun hasActiveSession(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            client.auth.awaitInitialization()
            client.auth.currentSessionOrNull() != null && session.roleName != null
        }.getOrDefault(false)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { client.auth.signOut() }
        session.clear()
    }

    // ===== Helpers fallback =====
    private fun roleByEmail(email: String): String {
        val e = email.lowercase()
        return when {
            e.contains("djka")            -> "djka"
            e.contains("sap")             -> "sap_erp"
            e.contains("ca") ||
            e.contains("cost") ||
            e.contains("accounting")      -> "cost_accounting"
            else                          -> "cost_accounting"
        }
    }

    private fun namaByEmail(email: String): String {
        val e = email.lowercase()
        return when {
            e.contains("djka")            -> "Admin DJKA"
            e.contains("sap")             -> "Admin SAP ERP"
            e.contains("ca") ||
            e.contains("cost") ||
            e.contains("accounting")      -> "Admin Cost Accounting"
            else -> email.substringBefore("@")
                .replace(".", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    private fun unitByRole(role: String): String = when (role) {
        "djka"            -> "Direktorat Jenderal Kereta Api"
        "sap_erp"         -> "SAP ERP / PT Telkomsigma"
        "cost_accounting" -> "Divisi Akuntansi Biaya"
        else              -> "PT Kereta Api Indonesia"
    }
}
