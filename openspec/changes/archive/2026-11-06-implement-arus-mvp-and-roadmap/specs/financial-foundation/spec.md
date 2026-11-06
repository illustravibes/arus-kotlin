# Spec Delta

## Purpose

Mendefinisikan aturan integritas dan penyimpanan lokal yang menjadi fondasi pencatatan keuangan Arus secara konsisten, offline, terlindungi, dan dapat dimigrasikan.

## ADDED Requirements

### Requirement: Penyimpanan data lokal offline
Aplikasi SHALL menyediakan fungsi inti keuangan tanpa koneksi internet dan mempertahankan data pengguna secara lokal.

#### Scenario: Penggunaan tanpa jaringan
- **WHEN** pengguna mencatat atau melihat data keuangan saat perangkat offline
- **THEN** aplikasi menyelesaikan operasi menggunakan data lokal tanpa menunggu jaringan

### Requirement: Nominal rupiah integer
Sistem SHALL menyimpan seluruh nominal keuangan sebagai bilangan bulat rupiah positif, dengan arah ditentukan oleh jenis transaksi.

#### Scenario: Menyimpan nominal
- **WHEN** transaksi bernilai positif disimpan
- **THEN** nominal tersimpan tanpa pecahan atau pembulatan floating-point dan arah efek mengikuti jenis transaksi

### Requirement: Saldo dihitung dari riwayat
Sistem SHALL menghitung saldo dompet dari saldo awal dan transaksi aktif yang waktu efektifnya tidak melewati waktu saat ini.

#### Scenario: Transaksi terjadwal
- **WHEN** transaksi memiliki waktu efektif di masa depan
- **THEN** transaksi ditandai terjadwal dan tidak mengubah saldo saat ini sampai waktunya tiba

### Requirement: Integritas operasi multi-entitas
Sistem SHALL menyimpan perubahan domain yang terdiri dari beberapa baris data secara atomik.

#### Scenario: Transfer gagal sebagian
- **WHEN** salah satu bagian transfer gagal disimpan
- **THEN** tidak ada bagian transfer atau biaya yang tersimpan

### Requirement: Split transaksi seimbang
Sistem SHALL menyimpan alokasi kategori transaksi dalam split dan menjamin jumlah nominal split sama dengan nominal transaksi untuk pemasukan serta pengeluaran.

#### Scenario: Menyimpan transaksi MVP
- **WHEN** pengguna menyimpan pemasukan atau pengeluaran tanpa pembagian kategori lanjutan
- **THEN** sistem menyimpan satu split berkategori dengan flag laporan dan budget aktif, dalam operasi atomik yang sama dengan transaksi

### Requirement: Migrasi skema aman
Sistem SHALL menggunakan skema database berversi dan memigrasikan data pengguna tanpa kehilangan data yang sudah tersimpan.

#### Scenario: Upgrade database
- **WHEN** aplikasi dibuka dengan database dari versi skema sebelumnya yang didukung
- **THEN** migrasi mempertahankan data dan aplikasi dapat membaca skema baru

### Requirement: Perlindungan database lokal
Aplikasi SHALL mengenkripsi database lokal menggunakan SQLCipher dan melindungi kunci enkripsi melalui mekanisme penyimpanan kunci yang sesuai platform.

#### Scenario: Membuka database terlindungi
- **WHEN** aplikasi memulai sesi dengan kunci yang valid
- **THEN** database dapat dibuka dan data keuangan dibaca tanpa mengekspos kunci ke log atau penyimpanan plaintext

### Requirement: Kunci aplikasi
Aplikasi SHALL menyediakan penguncian aplikasi dengan PIN 6 digit dan biometrik opsional, dengan PIN sebagai fallback bila biometrik tidak tersedia atau gagal. Aplikasi SHALL terkunci segera saat masuk background.

#### Scenario: Biometrik tidak tersedia
- **WHEN** pengguna mengaktifkan kunci aplikasi pada perangkat tanpa biometrik yang dapat digunakan
- **THEN** aplikasi tetap dapat dikunci dan dibuka menggunakan PIN

#### Scenario: Aplikasi kembali dari background
- **WHEN** aplikasi kembali ke foreground setelah dikunci di background
- **THEN** aplikasi meminta autentikasi PIN atau biometrik sebelum menampilkan data keuangan

### Requirement: Perlindungan tampilan aplikasi
Aplikasi SHALL selalu menerapkan `FLAG_SECURE` pada window utama agar konten keuangan dan layar autentikasi tidak masuk screenshot atau preview recent-apps.

#### Scenario: Screenshot atau recent-apps dibuka
- **WHEN** sistem atau aplikasi lain mencoba menangkap layar Arus
- **THEN** sistem tidak menampilkan konten Arus pada hasil tangkapan atau preview

### Requirement: Pengaturan umum aplikasi
Aplikasi SHALL menyediakan pengaturan tema, awal periode, dan format tanggal/angka lokal, serta menampilkan bahasa yang sedang digunakan.

#### Scenario: Mengubah tema dan awal periode
- **WHEN** pengguna memilih tema dan tanggal awal periode baru
- **THEN** aplikasi menyimpan pilihan secara lokal dan memakai pilihan tersebut pada layar terkait setelah aplikasi dibuka ulang

Bahasa Arus tetap Bahasa Indonesia sampai terjemahan tambahan tersedia; label bahasa tidak ditampilkan sebagai kontrol yang tidak berfungsi.
