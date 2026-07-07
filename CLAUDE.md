# CLAUDE.md — Aturan & Konteks Proyek SIMO

## Tentang proyek
Aplikasi Android Native (Kotlin) "SIMO — Sistem Infrastructure Maintenance & Operation".
Prototipe akademik OOAD + UAS Pemrograman Mobile, reimplementasi sistem internal PT KAI
berdasarkan laporan kelompok. Semua data dummy, tanpa logo/kredensial/aset asli KAI,
label "Prototipe Akademik" di aplikasi. Bahasa UI: Indonesia.

## Batasan WAJIB dari dosen (tidak boleh dilanggar)
- Android Native Kotlin. DILARANG Flutter/React Native/Compose sebagai pengganti.
- WAJIB benar-benar memakai: Activity, Fragment, RecyclerView + Adapter, Intent (+putExtra), ViewBinding, XML layout.
- Aplikasi TIDAK BOLEH force close. Fitur inti harus berjalan.
- Deliverable akhir di repo: source code, APK release di /apk, laporan OOAD di /docs, README.md lengkap.

## Arsitektur
MVVM + Repository + Hilt (DI) + Coroutines/Flow. Package com.kelompok1.simo.
Lapisan: data(remote/repository/mapper) - domain(model/repository/usecase) - ui - di - util.

## Backend
Supabase (Postgres cloud): Auth (email/password), Postgrest, Storage, RLS.
SDK supabase-kt (BOM: auth-kt, postgrest-kt, storage-kt) + ktor-client-android + kotlinx-serialization.
SUPABASE_URL & SUPABASE_ANON_KEY disimpan di local.properties -> BuildConfig. JANGAN commit rahasia.

## Role & hak akses
- cost_accounting: aktor utama, akses hampir semua modul operasional.
- it_support: kelola user & role (RBAC) saja.
- djka: lihat & verifikasi tagihan saja.

## Desain
Warna: primary #1B3A5B, secondary #E67E22, background #F5F7FA, success #2E7D32, warning #F9A825, error #C62828.
Material3, Card 12dp, tombol 8dp, status pakai Chip berwarna, angka format Rupiah, tiap list punya empty & loading state.

## Aturan Git/commit
- Commit pakai identitas Git pemilik repo (nama & email GitHub sendiri).
- Trailer co-author dimatikan (includeCoAuthoredBy=false) — ini preferensi format commit, bukan untuk menyembunyikan apa pun.
- Conventional Commits, Bahasa Indonesia singkat. Commit bertahap per fitur agar riwayat mudah dibaca.
- Tanggal commit = tanggal pengerjaan sebenarnya. TIDAK memalsukan tanggal/linimasa.
- Kalau kampus mensyaratkan disklosur penggunaan alat bantu/AI, cantumkan di README secara jujur.

## Cara kerja
- Kerjakan hanya fase yang diminta. Setelah selesai satu fase, berhenti dan tunggu instruksi berikutnya.
- Selalu jaga aplikasi tetap bisa di-build dan tidak crash.
