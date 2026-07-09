package com.kelompok1.simo.data.repository

import com.kelompok1.simo.data.remote.AuditLogWriteDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject

suspend fun SupabaseClient.insertAuditLog(
    userId: String?,
    entitas: String,
    aksi: String,
    detail: JsonObject
) {
    if (userId.isNullOrBlank()) return
    runCatching {
        from("audit_log").insert(AuditLogWriteDto(userId, entitas, aksi, detail))
    }
}
