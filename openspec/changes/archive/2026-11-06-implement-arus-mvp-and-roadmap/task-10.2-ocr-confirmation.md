# Verifikasi Task 10.2

## Perilaku

- OCR mengusulkan total, tanggal, merchant, dan item. Pengguna melihat teks mentah serta penanda `Cek ulang` untuk nilai berkeyakinan rendah.
- Persentase keyakinan adalah heuristik parsing label/format, bukan probabilitas ML Kit.
- Pilihan transaksi biasa membuka form yang dapat diedit; transaksi baru tercatat hanya setelah pengguna menekan Simpan.
- Lampiran pending ditautkan ke transaksi dan pilihan kategori terkonfirmasi memperbarui `merchant_category_hints` secara atomik.
- Pilihan Group Order membuat draft dengan item sumber OCR berstatus `UNASSIGNED`, menautkan lampiran ke draft, lalu membuka langkah item.
- Tabel hint merchant ditambahkan melalui migrasi Room 18→19.

## Verifikasi

- Unit parser lulus untuk total, tanggal, merchant, item, confidence, dan fallback field kosong.
- Tes DAO lulus untuk saran kategori serta kenaikan `useCount`.
- Tes instrumentasi lulus untuk konfirmasi transaksi tanpa penyimpanan otomatis, pengaitan lampiran, opsi Group Order beserta draft/item belum ditetapkan, dan migrasi 18→19.
- Unit dan seluruh 92 tes instrumentasi AVD lulus.
