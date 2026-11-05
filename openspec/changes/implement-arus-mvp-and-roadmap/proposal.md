# Proposal

## Why

Arus telah memiliki ERD, alur pengguna, dan wireframe yang cukup rinci, tetapi belum memiliki implementasi aplikasi maupun kontrak OpenSpec yang dapat memandu pengembangan secara konsisten. Perubahan ini menetapkan perilaku produk dan urutan pembangunan bertahap, dimulai dari MVP pencatatan keuangan offline yang dapat dipercaya sebelum fitur fase berikutnya ditambahkan.

## What Changes

- Menetapkan perilaku MVP untuk onboarding, dompet, kategori, transaksi pemasukan/pengeluaran, transfer, beranda, dan rekonsiliasi saldo berdasarkan dokumen sumber dan wireframe.
- Menetapkan fondasi data lokal yang konsisten: nominal rupiah integer, saldo terhitung dari transaksi efektif, transaksi masa depan tidak mengubah saldo saat ini, operasi multi-baris atomik, serta migrasi Room berversi.
- Memindahkan fondasi split kategori transaksi ke MVP agar model data sesuai ERD; kategori split tetap menjadi satu kategori penuh untuk transaksi MVP.
- Membuat spesifikasi bertahap bagi anggaran/laporan/rutin (Fase 2), utang-piutang dan group order (Fase 3), serta OCR dan backup/export (Fase 4), tanpa memasukkan seluruh fitur tersebut ke MVP.
- Menyelaraskan wireframe S-xx dengan kriteria penerimaan; wireframe menjadi referensi tampilan, sedangkan aturan perilaku ditetapkan spesifikasi.
- Menyediakan tugas implementasi terurut dengan gerbang verifikasi per fase; repositori saat ini hanya berisi dokumen ERD, user flow, dan wireframe, tanpa kode aplikasi.

## Capabilities

### New Capabilities
- `financial-foundation`: Penyimpanan lokal, konfigurasi awal, migrasi, dan integritas data domain keuangan.
- `wallet-management`: Onboarding dan pengelolaan dompet, termasuk saldo terhitung dan arsip.
- `transaction-management`: Pencatatan, penayangan, pengubahan, penghapusan, dan transfer antar dompet.
- `reconciliation`: Pencocokan saldo dompet, resolusi selisih, serta checkpoint verifikasi.
- `financial-overview`: Beranda, saldo agregat, net worth MVP, dan navigasi ke rincian.
- `budgets-and-reports`: Anggaran, laporan, kategori split, dan transaksi rutin Fase 2.
- `debt-and-group-orders`: Kontak, utang-piutang dua arah, pelunasan, netting, serta group order Fase 3.
- `receipt-and-data-portability`: OCR struk, lampiran, backup/restore, dan ekspor Fase 4.

### Modified Capabilities

Tidak ada. Belum ada spesifikasi kapabilitas OpenSpec yang sudah ada.

## Impact

- Produk Android yang akan dibangun: UI, navigasi, domain/use case, penyimpanan Room, keamanan lokal, notifikasi, dan pekerjaan latar sesuai fase.
- Data dan perilaku harus mengikuti `arus-erd.md` dan `arus-user-flow.md`; wireframe pada `wireframes/` memberi acuan layar dan komposisi.
- Keputusan teknologi yang belum dikunci perlu diverifikasi sebelum implementasi; rancangan mengasumsikan Android native Kotlin, Jetpack Compose, Room, Flow, Paging 3, WorkManager, dan SQLCipher sebagaimana diarahkan dokumen sumber.
- Tidak ada endpoint/server atau integrasi bank dalam MVP. Backup cloud baru termasuk Fase 4.
