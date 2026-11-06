# Verifikasi Task 10.3

## Implementasi

- Backup memakai pemilih file Android; pengguna dapat memilih Drive atau penyedia berkas lain tanpa login OAuth khusus.
- File `.arusbackup` mengenkripsi ZIP dengan AES-GCM dan kunci turunan PBKDF2-HMAC-SHA256. ZIP berisi database SQLCipher, passphrase database yang dilindungi enkripsi backup, manifest skema, serta semua lampiran.
- Restore mengekstrak ke staging privat, menjalankan migrasi Room, memeriksa seluruh berkas lampiran, meminta konfirmasi overwrite, lalu menukar database dan folder lampiran.
- Password salah, file rusak, dan skema lebih baru ditolak sebelum database lokal diganti. Kegagalan saat swap memulihkan database serta passphrase sebelumnya.
- Alur Pulihkan backup tersedia saat onboarding dan di menu Backup & Restore. Backup otomatis tidak termasuk task 10.3.

## Verifikasi

- Tes unit memeriksa enkripsi terautentikasi, round-trip, dan penolakan password salah.
- Tes instrumentasi memeriksa restore benar, password salah, skema lebih baru, dan pemulihan lampiran.
- Tes onboarding memastikan jalur Pulihkan backup tersedia.
- Suite penuh lulus: unit dan 93 tes instrumentasi AVD.
