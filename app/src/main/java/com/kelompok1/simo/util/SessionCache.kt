package com.kelompok1.simo.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Cache ringan profil user aktif (untuk UI & routing), disimpan di SharedPreferences. */
@Singleton
class SessionCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("simo_session", Context.MODE_PRIVATE)

    var userId: String?
        get() = prefs.getString("user_id", null)
        set(v) = prefs.edit().putString("user_id", v).apply()

    var namaLengkap: String?
        get() = prefs.getString("nama", null)
        set(v) = prefs.edit().putString("nama", v).apply()

    var email: String?
        get() = prefs.getString("email", null)
        set(v) = prefs.edit().putString("email", v).apply()

    /** cost_accounting | it_support | djka */
    var roleName: String?
        get() = prefs.getString("role", null)
        set(v) = prefs.edit().putString("role", v).apply()

    var unit: String?
        get() = prefs.getString("unit", null)
        set(v) = prefs.edit().putString("unit", v).apply()

    fun save(userId: String, nama: String, email: String, role: String, unit: String?) {
        prefs.edit()
            .putString("user_id", userId)
            .putString("nama", nama)
            .putString("email", email)
            .putString("role", role)
            .putString("unit", unit)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()
}
