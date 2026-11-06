# Catatan Rilis Arus 0.1.0

## Cakupan

- Android `minSdk 26`, application ID `com.illustravibes.arus`.
- Transaksi offline, dompet, anggaran/laporan/rutin, utang-piutang, group order, pemindai OCR, backup/restore terenkripsi, ekspor, pengaturan, dan rekonsiliasi.
- Room schema terbaru: 21. Upgrade 19→20 menambah hitung pecahan tunai; upgrade 20→21 menambah hasil cek mutasi.
- Backup memakai file AES-GCM melalui Storage Access Framework. Pengguna memilih penyedia file dan wajib menyimpan kata sandi.

## Upgrade

1. Buat backup terenkripsi ke lokasi aman sebelum memasang pembaruan.
2. Jangan menghapus data aplikasi atau mengubah nama database.
3. Pasang APK 0.1.0 di atas versi sebelumnya. Room menjalankan migrasi terdaftar saat startup.
4. Pastikan aplikasi membuka beranda, riwayat transaksi, dompet, dan checkpoint.
5. Uji restore backup hanya setelah memastikan file dan kata sandi tersedia.

## Rollback aman

- Jangan memasang APK lama di atas schema 21. APK lama tidak memiliki migrasi turun dan dapat menolak membuka database.
- Jangan menghapus data aplikasi, uninstall, atau memakai destructive migration untuk mengatasi kegagalan startup.
- Hentikan distribusi versi bermasalah dan rilis hotfix yang mempertahankan schema 21 atau memigrasikan ke versi lebih baru.
- Bila perlu pulih dari backup, jalankan APK yang memahami versi skema backup; lakukan restore eksplisit dan verifikasi data sebelum melanjutkan.
- Jaga database serta file backup asli tetap utuh sampai hotfix atau restore terverifikasi.

## Bukti upgrade

- Fixture schema 20 berhasil dimigrasikan ke schema 21 oleh `DatabaseMigrationTest.migration20To21CreatesReconciliationItems`; data fixture tetap ada.
- Clean build dan suite penuh harus lulus sebelum distribusi: `./gradlew clean :app:testDebugUnitTest :app:connectedDebugAndroidTest`.
