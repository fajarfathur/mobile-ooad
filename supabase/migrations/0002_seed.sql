-- ============================================================
-- SIMO — Seed data dummy (prototipe akademik)
-- UUID auth user diisi dari akun demo yang sudah dibuat.
-- ============================================================

-- Roles
insert into roles (id, nama, deskripsi) values
  ('11111111-1111-1111-1111-111111111111','cost_accounting','Aktor utama: kontrak IMO, kalkulasi biaya, billing, dan hak akses'),
  ('22222222-2222-2222-2222-222222222222','sap_erp','Integrasi data master, sinkronisasi, dan kelola aset infrastruktur'),
  ('33333333-3333-3333-3333-333333333333','djka','Verifikasi tagihan dan monitoring status billing')
on conflict (id) do nothing;

-- App users (tautkan ke auth.users)
insert into app_users (id, nama_lengkap, email, role_id, unit, is_active) values
  ('33a76041-4ee2-439d-b448-5474ada53ccd','Rina Kartika','costacc@demo.com','11111111-1111-1111-1111-111111111111','Cost Accounting', true),
  ('fc316cb0-895b-4fd1-a557-23dc43163013','Budi Santoso','sap@demo.com','22222222-2222-2222-2222-222222222222','SAP ERP', true),
  ('604ded59-4dec-4e49-8ee7-99f32ffa966b','Agus Wijaya','djka@demo.com','33333333-3333-3333-3333-333333333333','Otoritas DJKA', true)
on conflict (id) do nothing;

-- Data karyawan (20)
insert into data_karyawan (nip, nama, jabatan, unit, level, kategori) values
  ('K001','Ahmad Fauzi','Teknisi Rel','Track','Ahli','ahli_bersertifikat'),
  ('K002','Siti Nurhaliza','Teknisi Sinyal','Signalling','Ahli','ahli_bersertifikat'),
  ('K003','Dedi Kurniawan','Teknisi Jembatan','Bridge','Ahli','ahli_bersertifikat'),
  ('K004','Rangga Pratama','Teknisi Rel','Track','Madya','umum'),
  ('K005','Maya Sari','Teknisi Persinyalan','Signalling','Madya','umum'),
  ('K006','Bagus Setiawan','Teknisi Stasiun','Station','Madya','umum'),
  ('K007','Indah Permata','Teknisi Rel','Track','Junior','umum'),
  ('K008','Fajar Ramadhan','Teknisi Jembatan','Bridge','Junior','umum'),
  ('K009','Nita Rahmawati','Teknisi Sinyal','Signalling','Junior','umum'),
  ('K010','Yoga Saputra','Teknisi Rel','Track','Madya','umum'),
  ('K011','Lestari Wulandari','Teknisi Stasiun','Station','Ahli','ahli_bersertifikat'),
  ('K012','Hendra Gunawan','Teknisi Persinyalan','Signalling','Madya','umum'),
  ('K013','Rizky Aditya','Teknisi Rel','Track','Junior','umum'),
  ('K014','Putri Ayu','Teknisi Jembatan','Bridge','Madya','umum'),
  ('K015','Andi Wijaya','Teknisi Sinyal','Signalling','Ahli','ahli_bersertifikat'),
  ('K016','Dewi Anggraini','Teknisi Stasiun','Station','Junior','umum'),
  ('K017','Bayu Nugroho','Teknisi Rel','Track','Madya','umum'),
  ('K018','Sinta Melati','Teknisi Persinyalan','Signalling','Junior','umum'),
  ('K019','Reza Fahlevi','Teknisi Jembatan','Bridge','Ahli','ahli_bersertifikat'),
  ('K020','Kartika Sari','Teknisi Rel','Track','Junior','umum')
on conflict (nip) do nothing;

-- Pelatihan/sertifikasi: yang Ahli punya sertifikat valid; K019 sertifikatnya kedaluwarsa (untuk demo downgrade)
insert into pelatihan_kompetensi (karyawan_id, nama_pelatihan, sertifikasi, tanggal_terbit, tanggal_kedaluwarsa, kategori, is_valid)
select id,
  'Sertifikasi Kompetensi ' || level,
  'CERT-' || nip,
  date '2024-01-15',
  case when nip = 'K019' then date '2025-12-31' else date '2027-12-31' end,
  'ahli_bersertifikat',
  case when nip = 'K019' then false else true end
from data_karyawan where kategori = 'ahli_bersertifikat';

-- Presensi + komponen gaji untuk 2 periode
do $$
declare k record; p text; d int; bln int;
begin
  foreach p in array array['2026-05','2026-06'] loop
    bln := split_part(p,'-',2)::int;
    for k in select id, level from data_karyawan loop
      insert into komponen_gaji(karyawan_id, periode, gaji_pokok, tunjangan, total_pendapatan)
      values (k.id, p,
        case k.level when 'Ahli' then 8000000 when 'Madya' then 6000000 else 4500000 end,
        case k.level when 'Ahli' then 3000000 when 'Madya' then 2000000 else 1200000 end,
        case k.level when 'Ahli' then 11000000 when 'Madya' then 8000000 else 5700000 end);
      for d in 1..22 loop
        insert into presensi(karyawan_id, periode, tanggal, status_kehadiran, lokasi)
        values (k.id, p, make_date(2026, bln, d),
          case when d <= 20 then 'hadir' when d = 21 then 'izin' else 'alpha' end,
          'Wilayah Operasi 1');
      end loop;
    end loop;
  end loop;
end $$;

-- Kontrak IMO
insert into kontrak_imo (id, nomor_kontrak, periode_mulai, periode_selesai, nilai_kontrak, ruang_lingkup, jumlah_pegawai, status, created_by, ratified_at) values
  ('a0000000-0000-0000-0000-000000000001','IMO-2026-001','2026-01-01','2026-12-31',2500000000,
   'Pemeliharaan jalur rel, persinyalan, dan jembatan Wilayah Operasi 1', 20, 'diratifikasi',
   '33a76041-4ee2-439d-b448-5474ada53ccd', now()),
  ('a0000000-0000-0000-0000-000000000002','IMO-2026-002','2026-06-01','2026-12-31',1200000000,
   'Pemeliharaan stasiun dan fasilitas pendukung Wilayah Operasi 2', 12, 'draft',
   '33a76041-4ee2-439d-b448-5474ada53ccd', null)
on conflict (id) do nothing;

-- Aset infrastruktur (untuk kontrak 1)
insert into aset_infrastruktur (kode_aset, jenis, lokasi, nilai, kondisi, status, kontrak_id) values
  ('AST-REL-001','Rel','KM 12+300 s/d KM 18+500', 850000000,'Baik','aktif','a0000000-0000-0000-0000-000000000001'),
  ('AST-JBT-001','Jembatan','KM 15+200 (Sungai Ciliwung)', 620000000,'Sedang','perbaikan','a0000000-0000-0000-0000-000000000001'),
  ('AST-SIN-001','Sinyal','Stasiun A - Stasiun B', 430000000,'Baik','aktif','a0000000-0000-0000-0000-000000000001'),
  ('AST-STA-001','Stasiun','Stasiun Sentral A', 600000000,'Baik','aktif','a0000000-0000-0000-0000-000000000001')
on conflict (kode_aset) do nothing;

-- Biaya pemeliharaan final (kontrak 1, periode 2026-06)
insert into biaya_pemeliharaan (id, kontrak_id, periode, total_biaya, parameter, status, calculated_by, calculated_at) values
  ('b0000000-0000-0000-0000-000000000001','a0000000-0000-0000-0000-000000000001','2026-06',
   198000000, '{"hanya_ahli": false, "min_hari_hadir": 15}'::jsonb, 'final',
   '33a76041-4ee2-439d-b448-5474ada53ccd', now())
on conflict (id) do nothing;

-- Tagihan (status terkirim → muncul di "menunggu verifikasi" DJKA)
insert into tagihan (id, nomor_tagihan, kontrak_id, biaya_id, total_tagihan, status, tanggal_terbit, created_by) values
  ('c0000000-0000-0000-0000-000000000001','TAG-2026-0601','a0000000-0000-0000-0000-000000000001',
   'b0000000-0000-0000-0000-000000000001', 198000000, 'terkirim', '2026-07-01',
   '33a76041-4ee2-439d-b448-5474ada53ccd')
on conflict (id) do nothing;

-- Dokumen pendukung standar untuk tagihan di atas
insert into dokumen_pendukung (tagihan_id, nama_dokumen, jenis_verifikasi, status) values
  ('c0000000-0000-0000-0000-000000000001','KAK','administrasi','ada'),
  ('c0000000-0000-0000-0000-000000000001','RAB','administrasi','ada'),
  ('c0000000-0000-0000-0000-000000000001','HPS','administrasi','ada'),
  ('c0000000-0000-0000-0000-000000000001','Spesifikasi Teknis','administrasi','ada'),
  ('c0000000-0000-0000-0000-000000000001','Gambar Teknis','administrasi','ada'),
  ('c0000000-0000-0000-0000-000000000001','Sertifikat Petugas','lapangan','ada'),
  ('c0000000-0000-0000-0000-000000000001','Laporan Hasil Pekerjaan','lapangan','ada'),
  ('c0000000-0000-0000-0000-000000000001','Laporan Gangguan KA','lapangan','tidak_ada'),
  ('c0000000-0000-0000-0000-000000000001','Laporan TQI Triwulan','lapangan','ada'),
  ('c0000000-0000-0000-0000-000000000001','Daftar Hadir','administrasi','ada'),
  ('c0000000-0000-0000-0000-000000000001','Dokumentasi Foto','lapangan','tidak_ada');

-- Sync log awal
insert into sync_log (sumber, status, jumlah_record, pesan) values
  ('sap_master','sukses',20,'Sinkronisasi master SAP berhasil'),
  ('presensi','sukses',880,'Sinkronisasi presensi 2 periode'),
  ('penggajian','sukses',40,'Sinkronisasi komponen gaji'),
  ('pelatihan','sukses',6,'Sinkronisasi sertifikasi kompetensi');

-- Audit log contoh
insert into audit_log (user_id, entitas, aksi, detail) values
  ('33a76041-4ee2-439d-b448-5474ada53ccd','kontrak_imo','ratifikasi','{"nomor":"IMO-2026-001"}'::jsonb),
  ('33a76041-4ee2-439d-b448-5474ada53ccd','tagihan','terbit','{"nomor":"TAG-2026-0601"}'::jsonb);
