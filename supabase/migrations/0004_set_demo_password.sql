-- ============================================================
-- SIMO — Samakan password akun demo menjadi: demo123
-- ============================================================

update auth.users
set encrypted_password = extensions.crypt('demo123', extensions.gen_salt('bf'))
where email in (
  'ca@kai.id',
  'djka@kai.id',
  'sap@kai.id',
  'costacc@demo.com',
  'djka@demo.com',
  'sap@demo.com',
  'itsupport@demo.com'
);
