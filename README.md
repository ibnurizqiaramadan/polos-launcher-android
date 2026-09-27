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
- [ ] Daftar aplikasi teks vertikal A-Z (tanpa icon yang mendistraksi, opsi toggle icon monochrome)
- [ ] Fast search bar dengan keyboard auto-popup
- [ ] Pin favorit di home screen (maksimal 4-6 app esensial)
- [ ] Gesture swipe up (app drawer) & swipe down (notifikasi sistem)
- [ ] Indikator jam, tanggal, dan battery percentage minimalis
- [ ] Opsi sembunyikan aplikasi (Hide apps) & rename label app

---
*Environment coding-server-0: Java OpenJDK 21 siap pakai.*
