# Polos

Launcher Android yang hitam polos dan hanya berisi teks. Tanpa ikon, tanpa widget, tanpa warna: jam, beberapa baris info hari ini, favorit, dan daftar aplikasi. Latar #000000 murni supaya piksel AMOLED benar-benar mati, dengan pilihan wallpaper bergaris tipis yang digambar sendiri oleh launcher, tetap gelap.

Semua yang ada di home bisa dimatikan satu per satu dari halaman Settings. Tidak ada analitik, tidak ada akun, tidak ada iklan. Satu-satunya koneksi internet adalah ke Open-Meteo untuk cuaca, dan itu pun hanya kalau diaktifkan.

Package: `id.ibnurizqia.launcher`. Android 8.0 (API 26) ke atas.

## Fitur

### Home
- Jam 24 jam besar, tanggal, cuaca sekarang dan persentase baterai di tengah atas
- Kolom **Today**: suhu tertinggi/terendah, peluang hujan, acara kalender hari ini dan berikutnya, alarm berikutnya
- Kolom **Device**: pemakaian RAM, rentang clock CPU plus status panas, arus baterai (charging/using) dan suhu, diperbarui tiap 5 detik hanya saat home terlihat
- **Favorit** maksimal 6 app, rata kanan dalam jangkauan jempol; tahan lalu geser untuk mengatur urutan
- **Now playing** dengan tombol prev / play-pause / next, untuk pemutar musik apa pun
- Shortcut Phone dan Camera di pojok bawah, screen time hari ini di tengah
- Baris "Show:" untuk fitur yang masih butuh izin; tap untuk memberi izin, tahan untuk menyembunyikan

### App drawer
- Daftar teks A–Z, swipe up dari home untuk membuka
- Index alfabet # A–Z di sisi kanan setinggi list, dengan efek gelombang saat digeser
- Pencarian di bawah, dekat jempol; hasil tersusun dari bawah dengan yang paling cocok tepat di atas kolom pencarian, Enter untuk membuka. Di bawah hasil selalu ada "Search the web" dan "Search Play Store"; Enter tanpa hasil langsung mencari di web
- Baris **Recent** sampai 10 app, bisa digeser ke samping
- Sembunyikan app dan ganti nama label; app kembar (nama sama) diberi subtitle package
- Latar drawer bisa dipilih: home yang diblur, wallpaper saja (tajam atau diblur), atau hitam pekat; level blur 0–10 (Android 12+)
- Tarik ke bawah saat list di posisi teratas untuk menutup drawer

### Gesture
- Swipe up: drawer. Swipe down: panel notifikasi. Double-tap area kosong: kunci layar
- Gesture navigasi untuk HP yang memaksa 3 tombol (misalnya HyperOS dengan launcher pihak ketiga): swipe dari tepi kiri/kanan = Back, swipe up dari bawah tengah = Home, swipe up lalu tahan = Recents. Lewat layanan Accessibility yang hanya memasang strip sentuh tipis di tepi layar; tidak membaca isi layar. Opsional: beberapa app bank (misalnya BCA) menolak jalan selama ada layanan Accessibility yang aktif, jadi dari Settings launcher gesture ini bisa dimatikan dengan satu tap; menyalakan lagi lewat Settings sistem. Kalau Android menandainya "Tidak berfungsi" (setelah update, atau saat HyperOS mematikan prosesnya), launcher menampilkannya sebagai *Not working* di Settings dan hint *fix gestures* di home; perbaikannya matikan lalu nyalakan lagi di Aksesibilitas. Di HyperOS, set Battery saver Polos ke *No restrictions* supaya jarang terjadi. Settings → Gestures → **Service log** mencatat kapan layanan tersambung/diputus dan alasan Android mematikan prosesnya (bisa dibagikan)

### Wallpaper
20 wallpaper yang digambar sendiri oleh launcher, bukan file gambar, jadi tajam di resolusi apa pun dan APK tetap kecil. Tiap pola digambar sekali (di thread latar) ke bitmap seukuran layar lalu disimpan, jadi animasi dan scroll di atasnya tetap ringan. Semua berlatar hitam dengan garis redup (#262626) supaya teks tetap kontras:
- Garis: Lines, Diamonds, Grid, Chevron, Hexagons, Spiral, Rays, Waves, Rings
- Abstrak: Contours (garis kontur), Ridges (ala sampul *Unknown Pleasures*), Flow (garis arus), Arcs (ubin Truchet), Mesh (segitiga), Dots, Stars
- Glow: Glow, Ember, Aurora, pendar warna gelap tanpa garis
- Black: hitam polos

### Panduan
- Saat pertama kali dibuka, tur singkat menyorot bagian-bagian home (jam, favorit, Today, pintasan, gesture); bisa dilewati kapan saja
- Halaman **How to use** di Settings merangkum semua gesture dan tap, dan bisa memutar ulang tur-nya

### Settings
Tahan area kosong di home, atau "Launcher settings" di paling bawah drawer. Semua perubahan langsung berlaku:
- Wallpaper
- Home: cuaca, persentase baterai, now playing, acara kalender, alarm, Device, screen time, Phone dan Camera, baris "Show:"
- App drawer: latar (home diblur / wallpaper / hitam) dan level blur, Recent, index alfabet, keyboard langsung terbuka
- Gesture: gesture navigasi, double-tap kunci layar, swipe down notifikasi
- Halaman **Permissions**: tiap akses opsional dengan status Granted / Not granted; tap yang belum untuk langsung ke tempat memberinya. Baris di Settings menampilkan ringkasan "n of 5 granted"
- Pintasan ke pemilih default home app

Elemen yang dimatikan juga berhenti mengambil data: Device mati berarti tidak ada polling, cuaca mati berarti tidak ada permintaan jaringan.

## Cara pakai
- **Swipe up** di home: buka app drawer. **Swipe down**: panel notifikasi
- **Tap jam** = alarm, **tap tanggal** = kalender, **tap cuaca** = prakiraan, **tap baterai** = detail baterai, **tap acara** = buka acaranya
- **Tahan app**: Add to home / Rename / Hide / App info / Uninstall (Uninstall hanya untuk app yang bukan bawaan sistem)
- **Tahan favorit lalu geser** ke atas atau bawah: ubah urutan. Tahan tanpa geser tetap membuka menu
- **Tahan area kosong** di home: Settings
- **Geser di huruf** sisi kanan drawer: lompat ke huruf itu; huruf redup berarti tidak ada app dengan awalan itu
- App tersembunyi ada di "Hidden apps (n)" di paling bawah drawer

## Izin
Semua opsional. Launcher tetap berfungsi penuh tanpa satu pun izin ini; yang hilang hanya info terkait.

| Izin | Untuk | Dipakai oleh |
|---|---|---|
| Lokasi perkiraan | Cuaca (Open-Meteo, dengan nama daerah dari Geocoder) | `Weather.kt` |
| Kalender | Acara hari ini dan berikutnya | `MainActivity.kt` |
| Usage access | Screen time dan daftar Recent | `ScreenTime.kt` |
| Akses notifikasi | Now playing, lewat `MediaSessionManager` (notifikasi tidak dibaca) | `NowPlaying.kt` |
| Accessibility | Gesture navigasi dan kunci layar, lewat `performGlobalAction` (isi layar tidak dibaca) | `Gestures.kt` |
| Internet | Hanya ke `api.open-meteo.com` | `Weather.kt` |

## Tech stack

| | |
|---|---|
| Bahasa | Kotlin 2.1.20 |
| UI | Jetpack Compose, Material 3 (BOM 2025.06.01) |
| Build | Android Gradle Plugin 8.11.0, Gradle 8.14.3, JDK 17 toolchain (diunduh otomatis lewat foojay) |
| SDK | min 26, target dan compile 36 |
| Dependensi | `material3`, `activity-compose`, `core-ktx`. Tidak ada library pihak ketiga lain |
| Release | R8 minify dan shrinkResources; APK sekitar 1,2 MB |

Arsitektur sengaja dibuat sederhana: satu Activity dengan state Compose, SharedPreferences untuk semua pengaturan, coroutine untuk kerja I/O. Tanpa ViewModel, DI, atau database, karena tidak ada yang membutuhkannya.

## Struktur folder

```
app/src/main/
├── AndroidManifest.xml          intent-filter HOME, izin, dua service (gesture, media)
├── java/id/ibnurizqia/launcher/
│   ├── MainActivity.kt          Activity, state, home screen, app drawer, menu, dialog, halaman Settings
│   ├── Tour.kt                  tur sorotan saat pertama kali dibuka dan halaman How to use
│   ├── Diagnostics.kt           log layanan gesture dan alasan proses dimatikan (halaman Service log)
│   ├── LauncherSettings.kt      semua pengaturan: pilihan wallpaper dan toggle, tersimpan ke SharedPreferences
│   ├── Wallpaper.kt             20 wallpaper (path, titik, gradien), digambar sekali ke bitmap yang di-cache
│   ├── Gestures.kt              GestureService (Accessibility): strip tepi untuk Back/Home/Recents, kunci layar
│   ├── NowPlaying.kt            MediaListener dan MediaWatcher untuk now playing, plus ikon transport
│   ├── Weather.kt               lokasi, Geocoder, dan permintaan ke Open-Meteo
│   ├── ScreenTime.kt            screen time hari ini dan package yang baru dipakai (UsageStatsManager)
│   └── SystemStats.kt           RAM, clock CPU (sysfs), status panas, dan detail baterai
├── ic_launcher-playstore.png    ikon 512×512 untuk halaman Play Store (tidak ikut ke APK)
└── res/
    ├── drawable/ic_launcher_foreground.xml  logo "Po" sebagai vector; juga jadi layer monokrom (themed icon)
    ├── mipmap-anydpi-v26/ic_launcher.xml    adaptive icon: latar hitam, logo, monokrom
    ├── values/colors.xml        hitam untuk latar ikon
    ├── values/strings.xml       deskripsi layanan Accessibility
    ├── values/themes.xml        tema window hitam, tanpa action bar
    └── xml/gesture_service.xml  konfigurasi layanan Accessibility
```

File di root: `build.gradle.kts` dan `app/build.gradle.kts` (plugin, dependensi, signing release), `settings.gradle.kts` (foojay resolver), `LICENSE`.

## Build

```sh
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew installDebug         # lalu tekan Home dan pilih "Polos"
```

Build release ditandatangani kalau ada `keystore.properties` di root (file ini dan `*.jks` ada di `.gitignore`):

```properties
storeFile=../polos.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Tanpa file itu, `assembleRelease` tetap jalan tapi menghasilkan APK tanpa tanda tangan.

### Catatan HyperOS / MIUI
- Play Protect bisa memblokir pemasangan APK sideload yang meminta akses Accessibility atau notifikasi. Pasang lewat `adb install`, atau matikan sementara pemindaian Play Protect
- Sebelum mengaktifkan layanan Accessibility atau akses notifikasi untuk APK sideload: App info → ⋮ → *Allow restricted settings*
- HyperOS memaksa navigasi 3 tombol untuk launcher pihak ketiga. Gesture navigasi Polos dibuat untuk itu. Kalau mau menyembunyikan tombolnya: `adb shell settings put global force_fsg_nav_bar 1` (kembalikan dengan `0`)

## Privasi

Tidak ada data yang dikumpulkan. Satu-satunya permintaan jaringan adalah ke Open-Meteo untuk cuaca, dan hanya kalau diaktifkan. Selengkapnya di [PRIVACY.md](PRIVACY.md).

## Lisensi

Polos dirilis di bawah [GNU GPLv3](LICENSE). Bebas dipakai dan diubah; versi turunannya wajib tetap open source dengan lisensi yang sama.

Copyright (C) 2026 Ibnu Rizqia Ramadan
