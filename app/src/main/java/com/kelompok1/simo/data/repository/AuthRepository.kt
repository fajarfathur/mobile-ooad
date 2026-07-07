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
    /** Login ke Supabase Auth, lalu ambil profil + role dari tabel. */
    suspend fun login(email: String, password: String): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                val uid = client.auth.currentUserOrNull()?.id
                    ?: error("Sesi tidak valid setelah login.")

                val user = client.from("app_users")
                    .select { filter { eq("id", uid) } }
                    .decodeSingleOrNull<AppUserDto>()
                    ?: error("Akun belum terdaftar di sistem. Hubungi IT Support.")

                if (!user.isActive) error("Akun nonaktif. Hubungi IT Support.")

                val roleName = user.roleId?.let { rid ->
                    client.from("roles")
                        .select { filter { eq("id", rid) } }
                        .decodeSingleOrNull<RoleDto>()?.nama
                } ?: error("Role belum diatur. Hubungi IT Support.")

                session.save(uid, user.namaLengkap, user.email, roleName, user.unit)
                UserProfile(uid, user.namaLengkap, user.email, roleName, user.unit)
            }
        }

    suspend fun hasActiveSession(): Boolean = withContext(Dispatchers.IO) {
        client.auth.awaitInitialization()
        client.auth.currentSessionOrNull() != null && session.roleName != null
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { client.auth.signOut() }
        session.clear()
    }
}
