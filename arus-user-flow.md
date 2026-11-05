# Arus: User Flow & Daftar Layar (v0.6)

Dokumen ini melanjutkan v0.4 (ERD). Perubahan v0.6: utang-piutang dua arah (termasuk piutang lama yang belum tercatat), kategori sepenuhnya customizable tanpa subkategori, serta preset dompet dan kategori bawaan. Fokus: bagaimana pengguna bergerak di aplikasi, layar apa saja yang dibutuhkan, dan aturan perilaku tiap layar.

---

## 1. Prinsip UX

1. **Catat secepat mungkin.** Target: transaksi biasa tercatat dalam ≤ 3 ketukan setelah keypad muncul (nominal → kategori → simpan). Dompet dan tanggal sudah terisi default.
2. **Default pintar, selalu bisa diubah.** Dompet = terakhir dipakai, tanggal = hari ini, kategori = saran dari merchant/riwayat.
3. **Tidak ada penyimpanan diam-diam.** OCR, transaksi rutin mode konfirmasi, dan penyesuaian saldo selalu lewat layar konfirmasi.
4. **Angka harus bisa dijelaskan.** Setiap saldo/total bisa diketuk untuk melihat rinciannya.
5. **Aksi destruktif selalu ada peringatan dan Undo** (snackbar "Urungkan" 5 detik) untuk hapus transaksi biasa.
6. **Offline penuh.** Tidak ada layar yang menunggu internet kecuali backup.

---

## 2. Struktur Navigasi

### Bottom Navigation (5 item)

| Posisi | Menu | Isi |
|---|---|---|
| 1 | **Beranda** | Ringkasan saldo, budget, tugas tertunda |
| 2 | **Transaksi** | Riwayat, pencarian, filter |
| 3 | **(+) Tambah** | FAB tengah, membuka menu aksi cepat |
| 4 | **Anggaran** | Budget per kategori dan laporan |
| 5 | **Lainnya** | Dompet, kategori, piutang, group order, rutin, pengaturan |

### Menu FAB (+)
Bottom sheet berisi 6 aksi:
1. Pengeluaran
2. Pemasukan
3. Transfer
4. Scan Struk (kamera/galeri)
5. Group Order
6. Cocokkan Saldo

> Pengeluaran adalah aksi paling sering. Alternatif: tap singkat FAB = langsung form Pengeluaran, tekan lama = menu lengkap.

### Peta Layar

```
Splash/Kunci
 └─ Onboarding (sekali) ─→ Beranda
Beranda ─ Detail Dompet ─ Rekonsiliasi
        ─ Daftar Tugas Tertunda (konfirmasi rutin, tagih, jatuh tempo)
Transaksi ─ Detail Transaksi ─ Form Edit
          ─ Pencarian & Filter
(+) ─ Form Transaksi / Form Transfer / Scan Struk / Group Order Wizard / Cocokkan Saldo
Anggaran ─ Form Budget ─ Laporan
Lainnya ─ Dompet ─ Kategori ─ Kontak ─ Piutang ─ Group Order ─ Rutin ─ Reminder ─ Backup ─ Pengaturan
```

---

## 3. Daftar Layar

Kode layar dipakai untuk acuan implementasi (`S-xx`). Kolom **Fase** mengikuti rencana pengembangan.

### A. Onboarding & Keamanan

| Kode | Layar | Fase |
|---|---|---|
| S-01 | Splash + Kunci aplikasi (PIN/biometrik) | MVP |
| S-02 | Onboarding: sambutan singkat | MVP |
| S-03 | Onboarding: setup dompet pertama | MVP |
| S-04 | Onboarding: pilih kategori bawaan | MVP |
| S-05 | Atur PIN dan biometrik | MVP |
| S-06 | Pulihkan dari backup (opsi di onboarding) | 4 |

### B. Beranda

| Kode | Layar | Fase |
|---|---|---|
| S-10 | Beranda | MVP |
| S-11 | Rincian Net Worth (aset, utang kredit, piutang info) | MVP |
| S-12 | Tugas Tertunda (inbox) | 2 |

### C. Dompet

| Kode | Layar | Fase |
|---|---|---|
| S-20 | Daftar Dompet (dikelompokkan per tipe, drag urutan) | MVP |
| S-21 | Form Dompet (tambah/edit; field kredit muncul bila tipe Kredit) | MVP |
| S-22 | Detail Dompet (saldo, riwayat, tombol Cocokkan Saldo, Arsipkan) | MVP |
| S-23 | Dompet Terarsip | MVP |

### D. Transaksi

| Kode | Layar | Fase |
|---|---|---|
| S-30 | Riwayat Transaksi (dikelompokkan per hari, paging) | MVP |
| S-31 | Form Transaksi (Pengeluaran/Pemasukan, dengan tab tipe) | MVP |
| S-32 | Form Split (pecah ke beberapa kategori) | 2 |
| S-33 | Detail Transaksi | MVP |
| S-34 | Form Transfer (termasuk biaya admin) | MVP |
| S-35 | Template Transfer (misal Tarik Tunai) | 2 |
| S-36 | Pencarian dan Filter | 2 |
| S-37 | Pilih Kategori (bottom sheet dengan pencarian) | MVP |
| S-38 | Pilih Dompet (bottom sheet) | MVP |

### E. Kategori

| Kode | Layar | Fase |
|---|---|---|
| S-40 | Daftar Kategori (tab Pengeluaran / Pemasukan) | MVP |
| S-41 | Form Kategori (nama, ikon, warna) | MVP |
| S-42 | Dialog Hapus Kategori (pindahkan / arsipkan) | MVP |

### F. Anggaran & Laporan

| Kode | Layar | Fase |
|---|---|---|
| S-50 | Anggaran (progress per kategori + total) | 2 |
| S-51 | Form Budget | 2 |
| S-52 | Laporan: ringkasan periode (pie, tren, masuk vs keluar) | 2 |
| S-53 | Detail Kategori pada periode (daftar transaksi) | 2 |

### G. Transaksi Rutin & Reminder

| Kode | Layar | Fase |
|---|---|---|
| S-60 | Daftar Aturan Rutin | 2 |
| S-61 | Form Aturan Rutin | 2 |
| S-62 | Konfirmasi Transaksi Rutin (dari notifikasi atau inbox) | 2 |
| S-63 | Pengaturan Reminder | 2 |

### H. Kontak & Utang-Piutang

| Kode | Layar | Fase |
|---|---|---|
| S-70 | Daftar Kontak | 3 |
| S-71 | Detail Kontak (total piutang, riwayat) | 3 |
| S-72 | Daftar Utang-Piutang (tab Piutang / Utang, per orang / per order) | 3 |
| S-73 | Form Utang-Piutang manual (dua arah, dengan atau tanpa pergerakan dompet) | 3 |
| S-74 | Catat Pelunasan / Bayar Utang (alokasi ke beberapa catatan) | 3 |
| S-75 | Dialog Selisih Pelunasan (kurang/lebih) | 3 |

### I. Group Order

| Kode | Layar | Fase |
|---|---|---|
| S-80 | Daftar Group Order (Draft / Berjalan / Selesai) | 3 |
| S-81 | Wizard 1: Info order (judul, merchant, tanggal, dompet pembayar, kategori) | 3 |
| S-82 | Wizard 2: Peserta | 3 |
| S-83 | Wizard 3: Item & pemilik | 3 |
| S-84 | Wizard 4: Biaya bersama & diskon | 3 |
| S-85 | Wizard 5: Tinjau hasil hitung per orang | 3 |
| S-86 | Detail Group Order (status tiap peserta, aksi pelunasan) | 3 |
| S-87 | Bagikan Tagihan (teks/gambar untuk WhatsApp) | 3 |

### J. OCR

| Kode | Layar | Fase |
|---|---|---|
| S-90 | Kamera (CameraX) / pilih galeri | 4 |
| S-91 | Crop dan Rotate | 4 |
| S-92 | Hasil OCR → Form konfirmasi (transaksi biasa) | 4 |
| S-93 | Hasil OCR → daftar item "belum ditetapkan" (Group Order) | 4 |

### K. Rekonsiliasi

| Kode | Layar | Fase |
|---|---|---|
| S-100 | Cocokkan Saldo: input saldo asli | MVP |
| S-101 | Cocokkan Saldo: hasil selisih + saran penyebab | MVP |
| S-102 | Mode Cek Mutasi (centang transaksi) | 2 |
| S-103 | Resolusi Selisih (catat transaksi terlupa / pecah / tak terlacak) | MVP |
| S-104 | Riwayat Rekonsiliasi per dompet + grafik kebocoran | 2 |
| S-105 | Hitung Pecahan Uang Tunai | 2 |

### L. Pengaturan & Backup

| Kode | Layar | Fase |
|---|---|---|
| S-110 | Pengaturan (tema, bahasa, awal periode, format) | MVP |
| S-111 | Keamanan (PIN, biometrik, auto-lock) | MVP |
| S-112 | Backup & Restore (login Google, backup sekarang, otomatis) | 4 |
| S-113 | Export (CSV/Excel/PDF, rentang tanggal) | 4 |
| S-114 | Izin & Notifikasi (status izin, bantuan battery optimization) | 2 |

---

## 4. Alur Utama (User Flow)

### 4.1 Pertama Kali Membuka Aplikasi

```
Buka app
 → Onboarding sambutan (2–3 slide, bisa dilewati)
 → Setup dompet pertama:
     pilih tipe (Bank / E-Wallet / Cash / Kredit)
     → pilih dari preset populer (BCA, Mandiri, Seabank, Jago, GoPay, ShopeePay, DANA, OVO, Cash)
       atau buat sendiri
     → isi saldo awal saat ini
     → "Tambah dompet lain?" (loop) / "Lanjut"
 → Pilih kategori bawaan (centang yang dipakai, bisa diedit nanti)
 → Atur PIN + biometrik (boleh lewati, diingatkan lagi nanti)
 → Beranda
```

**Aturan:**
- Saldo awal = saldo asli hari ini, sehingga pengguna tidak perlu memasukkan riwayat lama.
- Preset dompet hanya mengisi nama, ikon, dan warna. Tidak ada koneksi ke bank.
- Minimal 1 dompet agar bisa lanjut.

### 4.2 Tambah Pengeluaran (Quick Add)

```
Tap (+) → Pengeluaran
 → Keypad nominal muncul otomatis (fokus langsung ke nominal)
 → Chip kategori: 5 terakhir/terfavorit + "Lainnya"
 → Dompet (default: terakhir dipakai) · Tanggal (default: hari ini)
 → [opsional] catatan, merchant, lampirkan foto, pecah kategori
 → Simpan
 → Snackbar "Tersimpan · Urungkan"
```

**Aturan:**
- Tombol "Simpan & tambah lagi" untuk input beruntun.
- Dompet kredit: setelah simpan, jika melewati limit tampilkan peringatan (tidak memblokir).
- Dompet bersaldo kurang (bukan kredit): peringatan "Saldo tidak cukup, saldo akan minus", boleh lanjut (karena mungkin saldo app belum akurat; ini pemicu saran rekonsiliasi).
- Setelah simpan, bila budget kategori melewati 80% atau 100%, tampilkan banner/notif.

### 4.3 Tambah Pemasukan
Sama dengan 4.2, tetapi kategori pemasukan (Gaji, Bonus, dll) dan label "Masuk ke dompet".

### 4.4 Transfer Antar Dompet

```
Tap (+) → Transfer
 → Dari [dompet] → Ke [dompet] (tombol tukar ⇄)
 → Nominal
 → Biaya admin (opsional, input nominal; default 0)
 → Tanggal, catatan
 → Simpan
```

**Hasil di database:** `TRANSFER_OUT` + `TRANSFER_IN` (+ `EXPENSE` kategori Admin bila ada biaya), semua dalam satu `transfer_group`.

**Aturan:**
- Dompet asal dan tujuan tidak boleh sama.
- Bila tujuan = dompet Kredit, label berubah menjadi "Bayar tagihan" dan ada tombol cepat "Bayar penuh (terpakai saat ini)".
- Biaya admin dibebankan ke dompet **asal** secara default (opsi ubah).

**Skenario: Tarik tunai (Seabank → Mandiri → Cash)**
```
Template "Tarik Tunai" (S-35):
 Langkah 1: Seabank → Mandiri (nominal X, admin 0 atau sesuai)
 Langkah 2: Mandiri → Cash (nominal X, admin 6.500)
Satu layar, dua transfer, simpan sekaligus.
```
Template menyimpan pasangan dompet favorit dan nominal terakhir.

### 4.5 Kelola Dompet

```
Lainnya → Dompet → [+]
 → Pilih tipe → isi form → Simpan
```

**Detail Dompet (S-22):**
- Header: saldo (atau "Terpakai/Sisa limit" untuk kredit).
- Tombol: **Cocokkan Saldo**, Edit, Arsipkan.
- Daftar transaksi dompet ini.
- Hapus tidak tersedia bila ada transaksi (hanya Arsipkan).

### 4.6 Kelola Kategori

```
Lainnya → Kategori → tab Pengeluaran/Pemasukan
 → [+] atau ketuk kategori → form (nama, ikon, warna) → Simpan
 → Hapus → Dialog:
     ○ Pindahkan transaksi ke kategori lain [pilih]
     ○ Arsipkan (disembunyikan dari pilihan baru, riwayat tetap)
```

**Aturan:**
- **Semua kategori bawaan bisa diedit dan dihapus**, sama seperti kategori buatan sendiri. Tidak ada subkategori (struktur datar); jika nanti dibutuhkan, ditambahkan lewat migrasi.
- Kategori sistem (Admin, Selisih Tak Terlacak, Ikhlas, Bunga/Denda, Penyesuaian) tidak bisa dihapus; nama, ikon, warna boleh diubah.
- Picker ikon: grid dengan pencarian. Picker warna: palet 16 warna preset.

### 4.7 Budget

```
Tab Anggaran → [+ Budget]
 → Pilih kategori (atau "Total bulanan")
 → Nominal
 → Rollover? (toggle) · Ingatkan di (80%)
 → Simpan
```

**Tampilan Anggaran (S-50):**
- Selektor periode (awal periode mengikuti pengaturan: tgl 1 atau tgl gajian).
- Kartu total bulanan di atas, lalu daftar kategori dengan progress bar: hijau (<80%), kuning (80–100%), merah (>100%).
- Teks: "Terpakai Rp… dari Rp… · sisa Rp… · rata-rata aman/hari Rp…".
- Ketuk kategori membuka daftar transaksi yang terhitung (S-53).

### 4.8 Transaksi Rutin

```
Lainnya → Rutin → [+]
 → Nama, tipe (pengeluaran / pemasukan / transfer)
 → Nominal, dompet, kategori
 → Frekuensi (harian/mingguan/bulanan/tahunan), tanggal mulai, tanggal akhir (opsional)
 → Mode: ○ Otomatis dicatat  ○ Minta konfirmasi
 → Reminder (H-3, H-1, H) bila mode konfirmasi
 → Simpan
```

**Saat jatuh tempo:**
- **Otomatis:** transaksi dibuat oleh WorkManager, notifikasi ringan "Admin bank Rp… tercatat".
- **Konfirmasi:** notifikasi dengan aksi **Catat / Ubah nominal / Lewati**. Bila tidak ditanggapi, masuk ke Tugas Tertunda (S-12) hingga ditangani.
- **Catch-up:** saat app dibuka setelah lama mati, semua `recurring_occurrence` yang terlewat diproses (otomatis) atau dimasukkan ke antrean (konfirmasi). Duplikat dicegah oleh indeks unik rule + tanggal.

### 4.9 Group Order (Wizard 5 langkah)

```
Tap (+) → Group Order  (atau dari OCR: Scan Struk → "Jadikan Group Order")
```

**Langkah 1: Info order (S-81)**
- Judul, merchant, tanggal, dompet pembayar, kategori (default Makan).

**Langkah 2: Peserta (S-82)**
- Daftar peserta: kamu sudah otomatis ada ("Aku").
- Tambah dari kontak internal atau ketik nama baru (otomatis disimpan ke kontak).

**Langkah 3: Item & pemilik (S-83)**
- Tambah item manual (nama, harga, qty), atau item datang dari OCR dengan status **Belum ditetapkan**.
- Tiap item: ketuk → pilih pemilik (satu atau banyak; patungan dibagi rata atau custom).
- Mode cepat: pilih peserta dulu, lalu ketuk item-item miliknya.
- Penanda merah pada item tanpa pemilik. Tombol Lanjut aktif hanya bila semua item bertuan.

**Langkah 4: Biaya bersama (S-84)**
- Tambah komponen: Ongkir, Biaya layanan, Pajak, Voucher/Diskon (nominal negatif).
- Tiap komponen punya mode sendiri: **Rata / Proporsional / Input manual**.
- Mode **Input manual** (kasus Shopee yang sudah menghitung per orang): tampilkan kolom nominal akhir per peserta. Bar validasi: "Total diinput Rp… dari Rp… · selisih Rp…" (merah bila tidak cocok).

**Langkah 5: Tinjau (S-85)**
- Tabel per orang: item, biaya, **total tagihan**.
- Pengaturan pembulatan: sisa receh ditanggung siapa (default: Aku).
- Cek akhir: Σ tagihan = total yang dibayar. Bila tidak cocok, tombol Finalisasi nonaktif dan selisih ditampilkan.
- **Finalisasi** → sistem membuat:
  1. Satu transaksi pengeluaran sebesar total dari dompet pembayar.
  2. Split: bagian pribadi (masuk laporan dan budget) + split piutang per peserta.
  3. Satu piutang per peserta.

**Setelah finalisasi (S-86 Detail):**
- Daftar peserta dengan status: Belum / Sebagian / Lunas, dan sisa nominal.
- Tombol **Catat pelunasan** per orang, **Bagikan tagihan** (S-87), **Atur reminder tagih**.
- Status order berubah menjadi *Selesai* saat semua piutang lunas atau di-ikhlaskan.

**Draft:** wizard otomatis menyimpan draft; bisa dilanjutkan dari S-80.

### 4.10 Bagikan Tagihan (S-87)

- Pilih peserta (satu atau semua).
- Pratinjau teks/gambar: judul order, rincian item miliknya, biaya bersama, total, tujuan bayar (diisi dari catatan dompet tujuan favorit, bisa pilih beberapa: GoPay/ShopeePay/BCA).
- Tombol: Salin teks, Bagikan ke WhatsApp/aplikasi lain.
- Aplikasi tidak mengirim apa pun sendiri; semua lewat share sheet Android.

### 4.11 Catat Pelunasan (S-74)

```
Dari Detail Group Order / Daftar Piutang / Detail Kontak
 → Pilih peserta (atau banyak peserta bila satu transfer untuk beberapa orang)
 → Nominal diterima
 → Masuk ke dompet [GoPay / ShopeePay / BCA / Cash]
 → Alokasi ke piutang (otomatis ke yang tertua dulu, bisa diubah manual)
 → Tanggal, catatan
 → Simpan
```

**Dialog selisih (S-75), muncul bila nominal ≠ sisa piutang:**

| Kondisi | Pilihan |
|---|---|
| **Kurang** | Biarkan sebagai sisa piutang · **Ikhlaskan** (dicatat pengeluaran kategori Ikhlas) |
| **Lebih** | **Kembalikan** (membuat transfer keluar/pengembalian dari dompet tujuan) · **Ikhlaskan** jadi pemasukan lain |

**Aturan:**
- Pelunasan bukan pemasukan; tidak masuk laporan pemasukan.
- Pembatalan pelunasan dilakukan dari riwayat piutang (menghapus transaksi dan alokasinya).

### 4.12 Scan Struk (OCR)

```
Tap (+) → Scan Struk
 → Pilih: Kamera / Galeri
 → Crop & Rotate (S-91)
 → Proses OCR on-device (indikator progres)
 → Pilih tujuan hasil:
     ○ Transaksi biasa → Form Konfirmasi (S-92)
     ○ Group Order     → Daftar item belum ditetapkan (S-93)
```

**Form Konfirmasi (S-92):**
- Terisi otomatis: total, tanggal, merchant. Kategori disarankan dari `merchant_category_hint`.
- Field hasil OCR berkonfidensi rendah diberi penanda kuning agar dicek.
- Foto struk terlampir otomatis (opsi hapus).
- Tombol "Pecah per item/kategori" membuka form split dengan item hasil OCR.
- Simpan menulis transaksi, lalu memperbarui hint kategori untuk merchant itu.

**Kegagalan:** bila OCR tidak menemukan total, tampilkan teks mentah agar pengguna menandai/menyalin bagian yang benar, atau lanjut isi manual dengan foto tetap terlampir.

**Izin kamera** diminta saat pertama menekan Kamera. Bila ditolak, tawarkan galeri.

### 4.13 Cocokkan Saldo (Rekonsiliasi)

```
Dompet → Cocokkan Saldo  (atau FAB → Cocokkan Saldo → pilih dompet)

S-100  Input saldo asli
        (Cash: hitung manual atau hitung per pecahan)
        ↓
S-101  Hasil: Saldo app Rp A · Saldo asli Rp B · Selisih Rp D
        Bantuan penyebab:
          • Selisih minus → rutin belum tercatat, biaya admin terlupa
          • Selisih plus → piutang yang mungkin sudah dibayar, bunga, cashback
          • "Kandidat pas": transaksi/piutang/rutin yang nominalnya sama dengan selisih
        Pilihan:
          [Cek mutasi]   → S-102
          [Selesaikan]   → S-103
        ↓
S-102  Mode Cek Mutasi
        Daftar transaksi dompet sejak checkpoint terakhir.
        Pengguna membuka mutasi bank/e-wallet, mencentang yang cocok.
        Yang tidak tercentang: edit/hapus/tandai bermasalah.
        Tombol "Tambah transaksi yang terlupa" (form cepat berulang).
        Selisih terus dihitung ulang secara langsung.
        ↓
S-103  Resolusi sisa selisih
        ○ Sudah ketemu, sisanya 0 → Selesai
        ○ Pecah ke beberapa kategori (misal admin 6.500 + jajan 20.000)
        ○ Catat sisa sebagai "Selisih Tak Terlacak"
        ○ Saldo awal salah → koreksi saldo awal dompet (bukan lewat penyesuaian)
        → Simpan checkpoint
```

**Setelah simpan:**
- Transaksi sampai tanggal checkpoint ditandai terverifikasi (ikon centang).
- Ringkasan: "Terlacak Rp X · Tak terlacak Rp Y".
- Edit/hapus transaksi terverifikasi menampilkan peringatan dan menandai checkpoint "perlu rekonsiliasi ulang".

**Kasus kredit/paylater:** label menjadi "Tagihan di aplikasi" vs "Terpakai di Arus", dan ada tombol cepat **Catat sebagai bunga/denda**.

### 4.14 Pencarian dan Filter (S-36)

- Kata kunci (catatan, merchant, nama kontak).
- Filter: tipe, kategori (multi), dompet (multi), rentang tanggal, rentang nominal, status verifikasi, ada lampiran.
- Hasil menampilkan total hasil filter di atas daftar.
- Simpan filter sebagai pintasan (fase lanjut).

### 4.15 Backup & Restore (S-112)

**Backup:**
```
Lainnya → Backup & Restore → Masuk dengan Google
 → Pilih: Backup sekarang / Otomatis (harian/mingguan, hanya Wi-Fi)
 → Opsi kata sandi enkripsi backup (disarankan; dilupakan = tidak bisa dipulihkan, diberi peringatan jelas)
```

**Restore saat ganti HP:**
```
Install Arus → Onboarding → "Pulihkan dari backup"
 → Login Google → pilih backup (tampil tanggal dan ukuran) → masukkan kata sandi
 → Proses → Beranda
```

**Aturan:**
- Backup berisi database dan lampiran; disimpan di Drive appDataFolder (tidak tampil di Drive pengguna).
- Restore menimpa seluruh data lokal setelah konfirmasi eksplisit.
- Jika versi skema backup lebih baru daripada app, tolak dengan pesan "Perbarui aplikasi".

### 4.16 Kunci Aplikasi

- Kunci saat app dibuka dan setelah di background lebih dari X menit (pilihan: langsung, 1, 5, 15 menit).
- Biometrik dengan fallback PIN.
- Layar recent apps disembunyikan (FLAG_SECURE, opsi).
- Lupa PIN → satu-satunya jalan: reset app lalu restore dari backup (diperingatkan sebelum mengaktifkan PIN).

### 4.17 Utang-Piutang Manual (Dua Arah)

Dipakai untuk pinjam-meminjam di luar group order. Satu fitur, dua arah:

- **Piutang** (orang berutang ke kamu)
- **Utang** (kamu berutang ke orang)

```
Lainnya → Utang-Piutang → [+]
 → Arah: ○ Aku meminjamkan (piutang)  ○ Aku meminjam (utang)
 → Kontak (pilih/ketik nama), nominal, tanggal, jatuh tempo (opsional), catatan
 → Pergerakan uang:
     ○ Catat dengan dompet   → pilih dompet (uang keluar / masuk dari dompet itu)
     ○ Catat saja            → tanpa mengubah saldo dompet (untuk piutang/utang LAMA yang belum tercatat)
 → Simpan
```

**Dua mode pergerakan uang**

| Mode | Kapan dipakai | Efek ke saldo dompet | Efek ke laporan |
|---|---|---|---|
| **Dengan dompet** | Pinjaman baru yang terjadi sekarang | Piutang: dompet **berkurang**. Utang: dompet **bertambah** | Tidak ada (bukan pengeluaran/pemasukan) |
| **Catat saja** | Piutang/utang lama yang lupa dicatat, atau terjadi sebelum memakai Arus | Tidak ada | Tidak ada |

**Saat dilunasi (S-74)**
- **Piutang dibayar orang lain ke kamu:** dompet tujuan bertambah, tercatat sebagai pelunasan (bukan pemasukan). Pada piutang lama ("catat saja"), saldo dompet memang naik tanpa pengeluaran pasangan; ini benar karena uang keluarnya terjadi sebelum Arus atau memang tak tercatat, dan rekonsiliasi tidak akan menganggapnya selisih.
- **Utang yang kamu bayar:** dompet berkurang, tercatat sebagai pembayaran utang (bukan pengeluaran).
- Mendukung **cicilan**, satu transfer untuk beberapa catatan, dan dialog selisih kurang/lebih (S-75) seperti piutang group order.
- **Bunga/denda utang** yang benar-benar kamu bayar dicatat terpisah sebagai pengeluaran kategori Bunga & Denda.

**Selisih khusus utang**
- Piutang diikhlaskan: pengeluaran kategori Ikhlas (sama seperti group order).
- Utang dihapuskan oleh pemberi pinjaman: pemasukan kategori sistem **Utang Dihapuskan**.

**Tampilan S-72**
- Dua tab: **Piutang** dan **Utang**, masing-masing dengan total di atas.
- Daftar dikelompokkan per orang; ketuk orang untuk melihat rincian per catatan (asal: group order atau manual).
- Penanda jatuh tempo dan hari keterlambatan.
- Kontak yang punya piutang sekaligus utang menampilkan **saldo bersih** ("Budi berutang bersih Rp 40.000 ke kamu"), dengan opsi **saling hapus (netting)** yang membuat catatan penyeimbang tanpa menyentuh dompet.

**Reminder:** jatuh tempo piutang ("tagih"), jatuh tempo utang ("bayar", H-3/H-1/H), dan pengingat berkala untuk yang belum ada tanggal jatuh tempo.

**Dampak ke ERD (diusulkan untuk v0.5 skema)**
- Tabel `receivable` diganti nama menjadi **`debt`**, ditambah kolom `direction` (`LENT` = piutang, `BORROWED` = utang) dan `origin_transaction_id` (null = "catat saja").
- Tipe transaksi baru: `LOAN_OUT` (pinjamkan uang, dompet berkurang) dan `LOAN_IN` (terima pinjaman, dompet bertambah), serta `DEBT_PAY_OUT` (bayar utang, dompet berkurang). Ketiganya **tidak masuk laporan** pengeluaran/pemasukan. Pelunasan piutang tetap `REPAYMENT_IN`.
- `repayment_allocation` dipakai untuk kedua arah tanpa perubahan.
- Kategori sistem baru: `DEBT_FORGIVEN`.
- Net worth = aset − utang kredit − Σ sisa `debt` berarah `BORROWED`.

---

## 5. Notifikasi dan Deep Link

| Notifikasi | Pemicu | Aksi di notifikasi | Tujuan saat diketuk |
|---|---|---|---|
| Reminder catat harian | Jam yang diatur | Tambah pengeluaran | S-31 |
| Tagihan rutin akan jatuh tempo | H-3, H-1, H | Catat, Lewati | S-62 |
| Transaksi rutin menunggu konfirmasi | Saat jatuh tempo | Catat, Ubah, Lewati | S-62 |
| Transaksi rutin otomatis tercatat | Setelah dieksekusi | – | S-33 |
| Budget 80% / 100% | Setelah transaksi tersimpan | – | S-53 |
| Jatuh tempo kredit | H-3, H-1, H | Bayar tagihan | S-34 (tujuan terisi) |
| Tagih peserta | N hari setelah finalisasi | Bagikan tagihan | S-87 |
| Piutang jatuh tempo | Pada/di sekitar jatuh tempo | Catat pelunasan | S-74 |
| Utang jatuh tempo | H-3, H-1, H | Bayar utang | S-74 |
| Rekonsiliasi berkala | Jadwal per dompet | Cocokkan sekarang | S-100 |
| Ringkasan mingguan | Hari/jam yang diatur | – | S-52 |

**Catatan teknis perilaku:**
- Android 13+: izin `POST_NOTIFICATIONS` diminta kontekstual, yaitu saat pengguna pertama kali mengaktifkan reminder, bukan saat install.
- Reminder presisi (tagihan) memakai exact alarm; bila izin tidak diberikan, jatuh ke WorkManager dengan peringatan "bisa meleset beberapa menit".
- Layar S-114 menampilkan status izin, dan panduan mematikan optimasi baterai untuk merek yang agresif (Xiaomi, Oppo, Vivo, dll).

---

## 6. Beranda (S-10): Susunan Konten

```
┌──────────────────────────────┐
│ Net Worth     Rp 12.450.000  │  ← ketuk: S-11
│ Aset 13.2jt · Utang kredit 750rb│
├──────────────────────────────┤
│ Dompet (scroll horizontal)   │
│ [BCA] [Seabank] [GoPay] [Cash]│  ← ketuk: S-22
├──────────────────────────────┤
│ Perlu tindakan (3)           │  ← ketuk: S-12
│  • Bayar kos – konfirmasi    │
│  • Budi belum bayar (3 hari) │
│  • Cash belum dicocokkan 14h │
├──────────────────────────────┤
│ Anggaran bulan ini           │
│  total ▓▓▓▓▓▓░░░ 68%         │
│  Makan ▓▓▓▓▓▓▓▓░ 85% ⚠       │
├──────────────────────────────┤
│ Piutang Rp 160.000 · Utang Rp 0│  ← ketuk: S-72
├──────────────────────────────┤
│ Transaksi terbaru (5)        │
└──────────────────────────────┘
```

**Aturan:**
- Dompet dengan `include_in_total = false` tampil, tetapi diberi label "di luar total".
- Piutang ditampilkan sebagai info terpisah dan tidak menambah net worth (belum tentu kembali). Utang pribadi ke orang **mengurangi** net worth (konservatif), sama seperti utang kredit.
- Bagian "Perlu tindakan" disembunyikan bila kosong.

---

## 7. Layar Detail Transaksi (S-33): Variasi per Tipe

| Tipe | Konten khusus |
|---|---|
| Pengeluaran/Pemasukan | Split (bila ada), lampiran, status verifikasi |
| Transfer | Kedua dompet, admin, tombol "Edit transfer" mengedit seluruh grup |
| Pembayaran group order | Tautan ke group order, ringkasan bagian pribadi vs piutang |
| Pelunasan | Alokasi ke piutang mana saja, tautan ke kontak |
| Penyesuaian rekonsiliasi | Tautan ke checkpoint, label "Selisih Tak Terlacak" |
| Dari rutin | Tautan ke aturan rutin |

Menu ⋮: Edit, Duplikat, Hapus (dengan peringatan bila terverifikasi atau terkait group order/pelunasan).

---

## 8. State Kosong, Error, dan Edge Case UI

| Situasi | Perilaku |
|---|---|
| Belum ada transaksi | Ilustrasi + tombol "Catat transaksi pertama" |
| Belum ada budget | Ajakan "Atur budget makan" dengan saran nominal dari rata-rata 30 hari |
| Hapus dompet bertransaksi | Ditolak, tawarkan Arsipkan |
| Hapus group order yang sudah ada pelunasan | Ditolak, jelaskan langkah pembatalan |
| Transfer ke dompet sama | Tombol Simpan nonaktif + pesan |
| Nominal 0 | Simpan nonaktif |
| Tanggal di masa depan | Boleh (untuk jadwal), tetapi tidak masuk saldo sampai tanggalnya tiba (diberi label "Terjadwal") |
| Ganti zona waktu | Tampilan mengikuti zona waktu perangkat; tanggal transaksi tetap pada instan aslinya |
| Edit group order setelah ada pelunasan | Hitung ulang bagian; piutang menyesuaikan; riwayat pelunasan tak diubah; tampilkan selisih sebelum disimpan |
| Rekonsiliasi dengan transaksi terjadwal di masa depan | Diabaikan dari perhitungan saldo |
| Kamera/izin ditolak | Fallback ke galeri atau input manual |
| Backup gagal | Notifikasi dan percobaan ulang; status terakhir terlihat di S-112 |

---

## 9. Pemetaan Layar ke Fase

| Fase | Layar |
|---|---|
| **MVP** | S-01–S-05, S-10, S-11, S-20–S-23, S-30, S-31, S-33, S-34, S-37, S-38, S-40–S-42, S-100, S-101, S-103, S-110, S-111 |
| **Fase 2** | S-12, S-32, S-35, S-36, S-50–S-53, S-60–S-63, S-102, S-104, S-105, S-114 |
| **Fase 3** | S-70–S-75, S-80–S-87 |
| **Fase 4** | S-06, S-90–S-93, S-112, S-113 |
| **Fase 5** | Goals, widget, insight, forecast (belum didetailkan) |

---

## 10. Keputusan yang Dikunci (v0.6)

1. **Utang-piutang dua arah**: piutang dan utang, termasuk catatan lama tanpa pergerakan dompet (lihat 4.17).
2. **Kategori**: hanya kategori datar (tanpa subkategori), seluruhnya bisa ditambah, diedit, dan dihapus (kecuali kategori sistem yang perannya tetap).
3. **Preset bawaan**: kategori default (Lampiran A) dan preset dompet bank, e-wallet, kredit/paylater, serta tunai (Lampiran C).

---

## Lampiran A: Usulan Kategori Bawaan

> Semua kategori pengeluaran dan pemasukan di bawah hanyalah titik awal: bisa diubah nama, ikon, warna, atau dihapus. Saat onboarding pengguna memilih mana yang dipakai.

### Pengeluaran
| Kategori | Ikon | Warna |
|---|---|---|
| Makan & Minum | restaurant | Oranye |
| Kos / Sewa | home | Biru |
| Wifi & Internet | wifi | Biru muda |
| Listrik & Air | bolt | Kuning |
| Transport | directions_bus | Hijau |
| Bensin | local_gas_station | Hijau tua |
| Belanja | shopping_bag | Pink |
| Pulsa & Paket Data | smartphone | Ungu |
| Kesehatan | favorite | Merah |
| Hiburan | movie | Ungu tua |
| Pendidikan | school | Indigo |
| Langganan | autorenew | Teal |
| Sosial & Hadiah | card_giftcard | Magenta |
| Lainnya | more_horiz | Abu-abu |

### Pemasukan
| Kategori | Ikon | Warna |
|---|---|---|
| Gaji | payments | Hijau |
| Bonus | emoji_events | Emas |
| Freelance | work | Teal |
| Hadiah | redeem | Pink |
| Cashback & Bunga | savings | Biru |
| Lainnya | more_horiz | Abu-abu |

### Sistem (tidak bisa dihapus)
| Kode | Nama default | Dipakai oleh |
|---|---|---|
| `ADMIN_FEE` | Admin Bank | Biaya transfer/tarik tunai, transaksi rutin admin |
| `UNTRACKED_DIFF` | Selisih Tak Terlacak | Rekonsiliasi |
| `WRITE_OFF` | Ikhlas | Piutang yang diikhlaskan |
| `INTEREST_PENALTY` | Bunga & Denda | Rekonsiliasi kredit |
| `BALANCE_ADJUST` | Penyesuaian Saldo | Koreksi teknis |
| `DEBT_FORGIVEN` | Utang Dihapuskan | Utang yang dibebaskan pemberi pinjaman (pemasukan) |

---

## Lampiran C: Preset Dompet Bawaan

Preset hanya mengisi **nama, tipe, ikon, dan warna awal**. Tidak ada koneksi ke bank. Ikon memakai avatar inisial berwarna (bukan logo resmi) untuk menghindari masalah merek dagang; pengguna bisa mengganti ikon dan warna. Pengguna juga selalu bisa membuat dompet kustom di luar daftar ini.

### Bank (tipe `BANK`)
| Kelompok | Preset |
|---|---|
| Bank besar | BCA, Mandiri, BRI, BNI, BTN, BSI (Bank Syariah Indonesia) |
| Bank swasta | CIMB Niaga, Permata, Danamon, OCBC, Maybank, Panin, Mega, Muamalat |
| Bank digital | Seabank, Jago, blu by BCA Digital, Jenius, Allo Bank, Neo Commerce, Superbank, Line Bank, Hibank |

### E-Wallet (tipe `EWALLET`)
GoPay, OVO, DANA, ShopeePay, LinkAja, iSaku, Sakuku, AstraPay

### Kredit / Paylater (tipe `CREDIT`)
Kartu Kredit (umum), ShopeePay Later / SPayLater, GoPay Later, Kredivo, Akulaku, Traveloka PayLater, Indodana

### Tunai (tipe `CASH`)
- **Tunai / Dompet** (dibuat otomatis sebagai pilihan awal)
- Celengan (opsional, default `include_in_total` mati)

### Aturan preset
- Daftar preset berbentuk data (file JSON di aplikasi), jadi mudah ditambah tanpa mengubah kode.
- Preset yang sudah dipilih tidak muncul lagi di daftar saat menambah dompet berikutnya, tetapi nama yang sama tetap boleh dibuat manual (misal dua rekening BCA: "BCA Utama" dan "BCA Tabungan").
- Warna tiap preset berupa warna perkiraan yang bisa diubah.

---

## Lampiran D: Langkah Berikutnya

1. Konfirmasi perubahan skema di bagian 4.17 (rename `receivable` → `debt`, tipe transaksi baru).
2. Pilih salah satu:
   - **Kode Room**: entity, DAO, relasi, migrasi, dan query saldo/laporan untuk MVP.
   - **Wireframe / desain visual** layar kunci (Beranda, Form Transaksi, Group Order Wizard, Cocokkan Saldo).
   - **Struktur proyek Android**: modul, package, dan konfigurasi Hilt/Compose/Navigation.
