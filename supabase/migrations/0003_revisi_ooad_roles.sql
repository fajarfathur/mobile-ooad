-- ============================================================
-- SIMO — Revisi OOAD: aktor final, RLS final, dan live modules
-- 2026-07-08
-- ============================================================

-- Samakan role dengan laporan revisi:
-- cost_accounting, djka, sap_erp
update roles
set nama = 'sap_erp',
    deskripsi = 'Integrasi data master, sinkronisasi, dan kelola aset infrastruktur'
where nama = 'it_support';

insert into roles (id, nama, deskripsi)
values (
  '22222222-2222-2222-2222-222222222222',
  'sap_erp',
  'Integrasi data master, sinkronisasi, dan kelola aset infrastruktur'
)
on conflict (id) do update
set nama = excluded.nama,
    deskripsi = excluded.deskripsi;

update roles
set deskripsi = 'Aktor utama: kontrak IMO, kalkulasi biaya, billing, dan hak akses'
where nama = 'cost_accounting';

update roles
set deskripsi = 'Verifikasi tagihan dan monitoring status billing'
where nama = 'djka';

update app_users
set unit = 'SAP ERP'
where role_id = (select id from roles where nama = 'sap_erp')
  and coalesce(unit, '') in ('IT Support', 'it_support', '');

delete from roles
where nama = 'it_support';

drop policy if exists roles_all_it on roles;
drop policy if exists users_all_it on app_users;
drop policy if exists data_karyawan_write on data_karyawan;
drop policy if exists pelatihan_kompetensi_write on pelatihan_kompetensi;
drop policy if exists presensi_write on presensi;
drop policy if exists komponen_gaji_write on komponen_gaji;
drop policy if exists kontrak_imo_write on kontrak_imo;
drop policy if exists aset_infrastruktur_write on aset_infrastruktur;
drop policy if exists biaya_pemeliharaan_write on biaya_pemeliharaan;
drop policy if exists biaya_detail_write on biaya_detail;
drop policy if exists dokumen_pendukung_write on dokumen_pendukung;
drop policy if exists sync_log_write on sync_log;
drop policy if exists audit_log_write on audit_log;

create policy roles_manage_cost_accounting on roles for all to authenticated
  using (public.current_role_name() = 'cost_accounting')
  with check (public.current_role_name() = 'cost_accounting');

create policy users_manage_cost_accounting on app_users for all to authenticated
  using (public.current_role_name() = 'cost_accounting')
  with check (public.current_role_name() = 'cost_accounting');

create policy data_karyawan_write_sap on data_karyawan for all to authenticated
  using (public.current_role_name() = 'sap_erp')
  with check (public.current_role_name() = 'sap_erp');

create policy pelatihan_kompetensi_write_sap on pelatihan_kompetensi for all to authenticated
  using (public.current_role_name() = 'sap_erp')
  with check (public.current_role_name() = 'sap_erp');

create policy presensi_write_sap on presensi for all to authenticated
  using (public.current_role_name() = 'sap_erp')
  with check (public.current_role_name() = 'sap_erp');

create policy komponen_gaji_write_sap on komponen_gaji for all to authenticated
  using (public.current_role_name() = 'sap_erp')
  with check (public.current_role_name() = 'sap_erp');

create policy aset_infrastruktur_write_sap on aset_infrastruktur for all to authenticated
  using (public.current_role_name() = 'sap_erp')
  with check (public.current_role_name() = 'sap_erp');

create policy sync_log_write_sap on sync_log for all to authenticated
  using (public.current_role_name() = 'sap_erp')
  with check (public.current_role_name() = 'sap_erp');

create policy kontrak_imo_write_cost on kontrak_imo for all to authenticated
  using (public.current_role_name() = 'cost_accounting')
  with check (public.current_role_name() = 'cost_accounting');

create policy biaya_pemeliharaan_write_cost on biaya_pemeliharaan for all to authenticated
  using (public.current_role_name() = 'cost_accounting')
  with check (public.current_role_name() = 'cost_accounting');

create policy biaya_detail_write_cost on biaya_detail for all to authenticated
  using (public.current_role_name() = 'cost_accounting')
  with check (public.current_role_name() = 'cost_accounting');

create policy dokumen_pendukung_write_cost on dokumen_pendukung for all to authenticated
  using (public.current_role_name() = 'cost_accounting')
  with check (public.current_role_name() = 'cost_accounting');

create policy audit_log_write_all_actors on audit_log for all to authenticated
  using (public.current_role_name() in ('cost_accounting', 'djka', 'sap_erp'))
  with check (public.current_role_name() in ('cost_accounting', 'djka', 'sap_erp'));
