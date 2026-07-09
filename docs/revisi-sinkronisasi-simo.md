# Sinkronisasi Revisi OOAD SIMO

Dokumen ini merangkum penyesuaian aplikasi Android SIMO terhadap revisi laporan OOAD yang menjadi acuan implementasi.

## Ringkasan revisi yang disinkronkan

- Aktor autentikasi/manajemen akses final hanya `Cost Accounting`, `Otoritas DJKA`, dan `SAP ERP`.
- Aktor `IT Support` dihapus dari implementasi role aktif aplikasi.
- Hak akses `Mengelola Hak Akses & Role` dipindahkan ke `Cost Accounting`.
- Hak akses `Mengelola Data Aset Infrastruktur` dipindahkan ke `SAP ERP`.
- Use case filter biaya memakai istilah `Filtering pegawai & sertifikasi kecakapan sesuai bidang`.
- Aplikasi tetap bersifat prototipe akademik berbasis hasil wawancara, bukan replika 1:1 sistem internal PT KAI.

## Mapping aplikasi ke revisi laporan

### Modul 4.3 — Autentikasi dan Manajemen Akses

- Login tetap memakai Supabase Auth sebagai pengganti SSO akademik.
- Pengelolaan `roles` dan `app_users` sekarang dijalankan oleh akun `cost_accounting`.
- Tampilan Hak Akses sudah live ke database dan dapat:
  - membaca user dan role dari Supabase,
  - membuat akun login baru,
  - mengubah role, unit, dan status aktif/nonaktif.

### Modul 4.8 — Kontrak IMO dan Aset Infrastruktur

- Kontrak IMO sekarang membaca data nyata dari tabel `kontrak_imo`.
- Penambahan kontrak draft dari aplikasi menulis langsung ke Supabase.
- Modul aset membaca data nyata dari `aset_infrastruktur`.
- Tombol sinkronisasi aset menulis ke `sync_log`.
- Penambahan aset dibatasi untuk aktor `sap_erp`.

### Modul 4.9 — Kalkulasi Biaya Pemeliharaan

- Form kalkulasi membaca kontrak dan periode nyata dari Supabase.
- Filtering pegawai menggunakan:
  - level pegawai,
  - sertifikasi valid,
  - pemetaan bidang kerja yang sesuai dengan revisi laporan.
- Hasil kalkulasi disimpan ke `biaya_pemeliharaan` dan `biaya_detail`.
- Status hasil dibuat `final` agar langsung bisa dipakai modul billing.

### Modul 4.10 — Billing dan Verifikasi Tagihan ke DJKA

- Generate billing mengambil hasil biaya final yang belum ditagihkan.
- Verifikasi dan revisi tagihan dijalankan oleh aktor `djka`.
- Status tagihan tersimpan live ke tabel `tagihan`.

## Catatan implementasi sequence diagram

Setiap alur utama aplikasi sudah mengikuti pola empat komponen yang diminta dosen:

- Aktor: role pengguna aktif (`cost_accounting`, `djka`, `sap_erp`)
- View: `Activity` / `Fragment` / dialog input
- Controller: `ViewModel` + `Repository`
- Model: tabel Supabase/Postgres

### Matriks sequence per modul (Aktor-Model-View-Controller)

| Modul | Aktor | Model (database) | View | Controller |
|---|---|---|---|---|
| 4.3 Autentikasi & Hak Akses | Cost Accounting / DJKA / SAP ERP | `roles`, `app_users`, `audit_log` | `LoginActivity`, `HakAksesFragment` | `LoginViewModel`, `HakAksesViewModel`, `AuthRepository`, `AccessRepository` |
| 4.8 Kontrak IMO & Aset | Cost Accounting / SAP ERP | `kontrak_imo`, `aset_infrastruktur`, `sync_log`, `audit_log` | `KontrakFragment`, `AsetFragment` | `KontrakViewModel`, `AsetViewModel`, `KontrakRepository`, `AsetRepository` |
| 4.9 Kalkulasi Biaya | Cost Accounting | `data_karyawan`, `pelatihan_kompetensi`, `presensi`, `komponen_gaji`, `biaya_pemeliharaan`, `biaya_detail` | `BiayaFragment` | `BiayaViewModel`, `BiayaRepository` |
| 4.10 Billing & Verifikasi | Cost Accounting / DJKA | `tagihan`, `dokumen_pendukung`, `biaya_pemeliharaan`, `audit_log` | `BillingFragment` | `BillingViewModel`, `BillingRepository` |

## Dampak ke backend Supabase

- Migrasi `0003_revisi_ooad_roles.sql` menyelaraskan role aktif dan RLS dengan revisi laporan.
- `audit_log` dipakai untuk mencatat aksi utama lintas modul agar dashboard dan halaman aktivitas menampilkan log yang konsisten.
- Seed lokal diperbarui agar proyek baru langsung memakai tiga aktor final SIMO.
