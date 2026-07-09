-- ============================================================
-- SIMO — Skema awal (enum, tabel, relasi) + RLS role-aware
-- Prototipe akademik. Data dummy.
-- ============================================================

-- ================= ENUMS =================
create type kontrak_status as enum ('draft','disepakati','diratifikasi','revisi','selesai');
create type aset_status as enum ('aktif','nonaktif','perbaikan');
create type biaya_status as enum ('draft','final');
create type tagihan_status as enum ('draft','terkirim','terverifikasi','perlu_revisi','dibayar');
create type kategori_kompetensi as enum ('ahli_bersertifikat','umum');
create type sync_source as enum ('pelatihan','presensi','penggajian','sap_master');

-- ============ RBAC & USER ============
create table roles (
  id uuid primary key default gen_random_uuid(),
  nama text unique not null,
  deskripsi text,
  permissions jsonb default '{}'::jsonb,
  created_at timestamptz default now()
);
create table app_users (
  id uuid primary key references auth.users(id) on delete cascade,
  nama_lengkap text not null,
  email text unique not null,
  role_id uuid references roles(id),
  unit text,
  is_active boolean default true,
  created_at timestamptz default now()
);

-- ====== MASTER & DATA SUMBER (SDM) ======
create table data_karyawan (
  id uuid primary key default gen_random_uuid(),
  nip text unique not null,
  nama text not null,
  jabatan text, unit text, level text,
  kategori kategori_kompetensi default 'umum',
  is_active boolean default true,
  last_synced_at timestamptz,
  created_at timestamptz default now()
);
create table pelatihan_kompetensi (
  id uuid primary key default gen_random_uuid(),
  karyawan_id uuid references data_karyawan(id) on delete cascade,
  nama_pelatihan text, sertifikasi text,
  tanggal_terbit date, tanggal_kedaluwarsa date,
  kategori kategori_kompetensi default 'umum',
  is_valid boolean default true
);
create table presensi (
  id uuid primary key default gen_random_uuid(),
  karyawan_id uuid references data_karyawan(id) on delete cascade,
  periode text not null,
  tanggal date not null,
  status_kehadiran text,
  lokasi text
);
create table komponen_gaji (
  id uuid primary key default gen_random_uuid(),
  karyawan_id uuid references data_karyawan(id) on delete cascade,
  periode text not null,
  gaji_pokok numeric(15,2) default 0,
  tunjangan numeric(15,2) default 0,
  total_pendapatan numeric(15,2) default 0
);

-- ============ KONTRAK & ASET ============
create table kontrak_imo (
  id uuid primary key default gen_random_uuid(),
  nomor_kontrak text unique not null,
  periode_mulai date not null, periode_selesai date not null,
  nilai_kontrak numeric(18,2) default 0,
  ruang_lingkup text, jumlah_pegawai int default 0,
  status kontrak_status default 'draft',
  created_by uuid references app_users(id),
  ratified_at timestamptz,
  created_at timestamptz default now(), updated_at timestamptz default now()
);
create table aset_infrastruktur (
  id uuid primary key default gen_random_uuid(),
  kode_aset text unique not null,
  jenis text, lokasi text,
  nilai numeric(18,2) default 0, kondisi text,
  status aset_status default 'aktif',
  kontrak_id uuid references kontrak_imo(id)
);

-- ======== KALKULASI BIAYA ========
create table biaya_pemeliharaan (
  id uuid primary key default gen_random_uuid(),
  kontrak_id uuid references kontrak_imo(id) on delete cascade,
  periode text not null,
  total_biaya numeric(18,2) default 0,
  parameter jsonb default '{}'::jsonb,
  status biaya_status default 'draft',
  calculated_by uuid references app_users(id),
  calculated_at timestamptz,
  created_at timestamptz default now()
);
create table biaya_detail (
  id uuid primary key default gen_random_uuid(),
  biaya_id uuid references biaya_pemeliharaan(id) on delete cascade,
  karyawan_id uuid references data_karyawan(id),
  level text, kategori kategori_kompetensi,
  hari_hadir int default 0,
  biaya_sdm numeric(18,2) default 0
);

-- ======== BILLING & VERIFIKASI DJKA ========
create table tagihan (
  id uuid primary key default gen_random_uuid(),
  nomor_tagihan text unique not null,
  kontrak_id uuid references kontrak_imo(id),
  biaya_id uuid references biaya_pemeliharaan(id),
  total_tagihan numeric(18,2) default 0,
  status tagihan_status default 'draft',
  tanggal_terbit date, catatan_djka text,
  verified_by uuid references app_users(id), verified_at timestamptz,
  created_by uuid references app_users(id),
  created_at timestamptz default now()
);
create table dokumen_pendukung (
  id uuid primary key default gen_random_uuid(),
  tagihan_id uuid references tagihan(id) on delete cascade,
  nama_dokumen text,
  jenis_verifikasi text,
  status text default 'tidak_ada',
  file_url text, keterangan text
);

-- ======== INTEGRASI & AUDIT ========
create table sync_log (
  id uuid primary key default gen_random_uuid(),
  sumber sync_source not null, status text,
  jumlah_record int default 0, pesan text,
  synced_at timestamptz default now()
);
create table audit_log (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references app_users(id),
  entitas text, aksi text, detail jsonb,
  created_at timestamptz default now()
);

-- ============================================================
-- Helper: nama role user yang sedang login (security definer
-- agar tidak kena RLS app_users/roles saat dievaluasi policy).
-- ============================================================
create or replace function public.current_role_name()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select r.nama
  from app_users u
  join roles r on r.id = u.role_id
  where u.id = auth.uid()
$$;

-- ============================================================
-- RLS
-- ============================================================
alter table roles enable row level security;
alter table app_users enable row level security;
alter table data_karyawan enable row level security;
alter table pelatihan_kompetensi enable row level security;
alter table presensi enable row level security;
alter table komponen_gaji enable row level security;
alter table kontrak_imo enable row level security;
alter table aset_infrastruktur enable row level security;
alter table biaya_pemeliharaan enable row level security;
alter table biaya_detail enable row level security;
alter table tagihan enable row level security;
alter table dokumen_pendukung enable row level security;
alter table sync_log enable row level security;
alter table audit_log enable row level security;

-- roles: semua user login boleh baca; cost_accounting kelola penuh
create policy roles_select on roles for select to authenticated using (true);
create policy roles_all_cost on roles for all to authenticated
  using (public.current_role_name() = 'cost_accounting')
  with check (public.current_role_name() = 'cost_accounting');

-- app_users: user login boleh baca (untuk nama pembuat/aktivitas); cost_accounting kelola penuh
create policy users_select on app_users for select to authenticated using (true);
create policy users_all_cost on app_users for all to authenticated
  using (public.current_role_name() = 'cost_accounting')
  with check (public.current_role_name() = 'cost_accounting');

-- Data operasional: semua user login boleh SELECT; tulis hanya cost_accounting
do $$
declare t text;
begin
  foreach t in array array[
    'data_karyawan','pelatihan_kompetensi','presensi','komponen_gaji',
    'kontrak_imo','aset_infrastruktur','biaya_pemeliharaan','biaya_detail',
    'dokumen_pendukung','sync_log','audit_log'
  ]
  loop
    execute format('create policy %I_select on %I for select to authenticated using (true);', t, t);
    execute format($f$create policy %I_write on %I for all to authenticated
      using (public.current_role_name() = 'cost_accounting')
      with check (public.current_role_name() = 'cost_accounting');$f$, t, t);
  end loop;
end $$;

-- tagihan: SELECT semua; INSERT/DELETE cost_accounting; UPDATE cost_accounting ATAU djka (verifikasi)
create policy tagihan_select on tagihan for select to authenticated using (true);
create policy tagihan_insert on tagihan for insert to authenticated
  with check (public.current_role_name() = 'cost_accounting');
create policy tagihan_delete on tagihan for delete to authenticated
  using (public.current_role_name() = 'cost_accounting');
create policy tagihan_update on tagihan for update to authenticated
  using (public.current_role_name() in ('cost_accounting','djka'))
  with check (public.current_role_name() in ('cost_accounting','djka'));
