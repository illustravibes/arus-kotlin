# Verifikasi Task 10.1

## Implementasi

- CameraX menyediakan preview dan pengambilan foto; galeri memakai Storage Access Framework tanpa izin penyimpanan.
- Izin kamera diminta saat pengguna memilih Kamera. Penolakan tetap menyediakan Galeri dan Input manual.
- Editor mendukung crop melalui sudut serta tombol aksesibel untuk tiap sisi, dan rotasi 90 derajat.
- Foto hasil crop disimpan di direktori privat aplikasi. OCR memakai model Latin ML Kit bundled, sehingga pengenalan berjalan di perangkat tanpa unduhan model.
- Lampiran pending menyimpan jalur foto internal dan teks OCR; pemautan ke transaksi menunggu konfirmasi Task 10.2.
- Room migrasi 17→18 menambahkan tabel lampiran dan indeks pemilik.

## Verifikasi

- Unit test lulus.
- Suite instrumentasi penuh lulus 88/88 pada AVD API 26.
- Tes khusus mencakup crop/rotate, salin foto galeri ke penyimpanan privat, OCR bundled membaca teks sintetis, fallback kamera-ditolak, migrasi 17→18, DAO lampiran, dan alur CameraX ambil foto → crop/rotate → OCR → lampiran pending.
- Skenario Scan Struk → Input manual juga lulus.
- Dialog Transfer kini mengosongkan fokus keyboard saat dibuka dan menerapkan IME insets; tes transfer penuh kembali lulus.

## Batas

- OCR hanya menghasilkan teks mentah. Parsing nominal/tanggal/merchant, konfirmasi transaksi, dan pengaitan lampiran termasuk Task 10.2.
