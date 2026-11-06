# Verifikasi Task 10.4

## Implementasi

- Rentang tanggal memakai hari awal inklusif dan hari akhir inklusif di UI, lalu dikonversi ke batas akhir eksklusif pada query.
- Export memuat transaksi aktif, kategori/split, dompet, merchant, catatan, status verifikasi, dan lampiran.
- CSV memakai escaping RFC 4180, Excel memakai berkas XLSX OOXML, dan PDF mendukung beberapa halaman.
- Storage Access Framework menyimpan file ke lokasi pilihan; file terakhir dibagikan memakai `Intent.createChooser`.

## Verifikasi

- Tes DAO membuktikan batas rentang tanggal dan rincian transaksi.
- Tes file memvalidasi isi CSV, XML XLSX, signature PDF, serta Android share chooser.
- UI test memastikan route ekspor menampilkan tanggal dan ketiga format.
- Unit dan seluruh 97 tes instrumentasi AVD lulus.
