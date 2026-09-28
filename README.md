# Minimalist Launcher Android (Zen / Clean Home)

Project ini disiapkan untuk pengembangan **Minimalist Android Launcher** (Home screen replacement) berbasis native **Kotlin + Jetpack Compose**.

## Rekomendasi Tech Stack
- **Language:** Kotlin 2.x
- **UI Framework:** Jetpack Compose (Material 3)
- **Min SDK:** 26 (Android 8.0 Oreo) / Target SDK: 34+ (Android 14/15)
- **Architecture:** Clean Architecture + MVVM / MVI dengan Kotlin Coroutines & StateFlow
- **Dependency Injection:** Hilt / Koin (opsional, untuk launcher ultra-ringan manual DI sudah sangat cukup)

## Essential Launcher Android Manifest
Agar aplikasi dikenali sebagai Home Launcher oleh Android OS,  wajib menyertakan intent-filter berikut pada MainActivity:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:clearTaskOnLaunch="true"
    android:stateNotNeeded="true"
    android:theme="@style/Theme.MinimalistLauncher">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

## Core Launcher API & Concepts
1. **Query Installed Apps:**
   - Gunakan `LauncherApps.getActivityList(null, Process.myUserHandle())` (lebih disukai untuk launcher modern) atau `PackageManager.queryIntentActivities(...)`.
   - Wajib deklarasi permission query di manifest:
     ```xml
     <queries>
         <intent>
             <action android:name="android.intent.action.MAIN" />
             <category android:name="android.intent.category.LAUNCHER" />
         </intent>
     </queries>
     ```
2. **Launch Application:**
   - `context.startActivity(packageManager.getLaunchIntentForPackage(packageName))`
3. **App Info / Uninstall Intent:**
   - Long press item -> buka App Info setting sistem: `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`
4. **Transparent Status/Nav Bar (Edge-to-Edge):**
   - Compose: `enableEdgeToEdge()` + WindowInsets handling agar wallpaper sistem tetap tembus pandang jika diinginkan.

## Fitur Minimalist Launcher yang Direkomendasikan
- [x] Daftar aplikasi teks vertikal A-Z (tanpa icon yang mendistraksi)
- [ ] Opsi toggle icon monochrome
- [x] Alphabet fast scroller (wave) di app drawer
- [x] Tema pitch black (#000) untuk hemat baterai AMOLED
- [x] Fast search bar dengan keyboard auto-popup
- [x] Pin favorit di home screen (maksimal 6 app esensial)
- [x] Gesture swipe up (app drawer) & swipe down (notifikasi sistem)
- [x] Indikator jam (24 jam, center), tanggal, dan battery percentage minimalis
- [x] Info cuaca (Open-Meteo, lokasi perkiraan)
- [x] Alarm berikutnya & acara kalender berikutnya (24 jam ke depan)
- [x] Screen time hari ini (Usage access)
- [x] Shortcut Phone & Camera di pojok bawah home
- [x] Now playing + kontrol Prev/Play-Pause/Next (butuh akses notifikasi)
- [x] Opsi sembunyikan aplikasi (Hide apps) & rename label app

## Cara Pakai
- **Swipe up** di home: buka app drawer (keyboard search langsung muncul, Enter = buka hasil pertama)
- **Swipe down** di home: buka panel notifikasi
- **Tap to show weather / events / screen time / music** di home: aktifkan info tersebut (tiap hint hilang setelah di-tap sekali; bisa diaktifkan lagi lewat App info)
- **Tap jam** = alarm, **tap tanggal** = kalender, **tap cuaca** = prakiraan, **tap baterai** = pemakaian baterai, **tap acara** = buka acaranya
- Search bar ada di bawah drawer, tepat di atas keyboard
- **Long press** app: Add to home / Rename / Hide / App info
- **Drag huruf** di kanan drawer: lompat ke huruf itu (efek wave + bubble)
- App tersembunyi ada di "Hidden apps (n)" paling bawah drawer
- Build & install: `./gradlew installDebug`, lalu tekan Home dan pilih "Minimalist Launcher"

---
*Environment coding-server-0: Java OpenJDK 21 siap pakai.*
