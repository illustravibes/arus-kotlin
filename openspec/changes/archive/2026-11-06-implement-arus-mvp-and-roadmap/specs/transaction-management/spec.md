# Spec Delta

## Purpose

Menetapkan pencatatan transaksi dan transfer yang cepat, dapat dijelaskan, serta menjaga saldo dompet dan laporan tetap konsisten.

## ADDED Requirements

### Requirement: Pencatatan pemasukan dan pengeluaran
Aplikasi SHALL memungkinkan pengguna mencatat pemasukan atau pengeluaran dengan nominal, dompet, kategori, dan waktu efektif.

#### Scenario: Pengeluaran cepat
- **WHEN** pengguna memasukkan nominal, memilih kategori dan menyimpan dengan dompet default yang tersedia
- **THEN** transaksi tersimpan, saldo dompet diperbarui dari riwayat, dan aplikasi menyediakan aksi Urungkan selama lima detik

### Requirement: Validasi transaksi
Aplikasi SHALL menolak nominal nol dan memperingatkan pengguna sebelum transaksi membuat saldo dompet non-kredit menjadi negatif.

#### Scenario: Saldo tidak cukup
- **WHEN** pengguna menyimpan pengeluaran yang melebihi saldo dompet non-kredit
- **THEN** aplikasi memperingatkan bahwa saldo akan negatif dan tetap mengizinkan pengguna melanjutkan

### Requirement: Transfer antar dompet
Aplikasi SHALL membuat transfer keluar dan masuk serta biaya admin opsional sebagai satu operasi yang terhubung.

#### Scenario: Transfer dengan biaya
- **WHEN** pengguna menyimpan transfer antar dua dompet berbeda dengan biaya admin
- **THEN** saldo asal berkurang sebesar transfer dan biaya, saldo tujuan bertambah sebesar transfer, dan biaya masuk laporan pengeluaran

### Requirement: Riwayat dan detail transaksi
Aplikasi SHALL menampilkan riwayat transaksi berkelompok menurut tanggal dan detail yang menjelaskan efek dompet serta kategori atau split terkait.

#### Scenario: Membuka detail transaksi
- **WHEN** pengguna memilih transaksi dari riwayat
- **THEN** aplikasi menampilkan nominal, waktu, dompet, kategori, catatan, dan status terjadwal atau verifikasi yang relevan

### Requirement: Perubahan dan penghapusan transaksi
Aplikasi SHALL memungkinkan transaksi biasa diedit atau dihapus dengan konfirmasi yang sesuai, menyediakan Undo untuk penghapusan biasa, dan memberi peringatan untuk transaksi yang terverifikasi atau terkait operasi khusus.

#### Scenario: Menghapus transaksi biasa
- **WHEN** pengguna menghapus transaksi yang tidak terkunci checkpoint dan bukan bagian operasi khusus
- **THEN** transaksi dihapus secara aman, saldo dan laporan dihitung ulang, dan aksi Urungkan tersedia selama lima detik

### Requirement: Pengelolaan kategori
Aplikasi SHALL memungkinkan kategori pemasukan dan pengeluaran dibuat, diedit, diarsipkan, atau dihapus dengan pemindahan transaksi bila diperlukan; kategori sistem tidak dapat dihapus.

#### Scenario: Menghapus kategori yang digunakan
- **WHEN** pengguna memilih hapus kategori biasa yang masih direferensikan transaksi
- **THEN** aplikasi meminta pengguna memindahkan transaksi ke kategori lain atau mengarsipkan kategori
