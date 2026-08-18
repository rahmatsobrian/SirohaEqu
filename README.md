# Siroha Equ

Advanced system-wide equalizer & audio device tuning untuk Android, dibangun
dengan Kotlin + Jetpack Compose + Material 3.

> **Status:** kerangka proyek fungsional lengkap secara source code (audio
> engine, EQ engine, device profile system, preset system, logging,
> diagnostics, tests, CI). Build/APK belum diverifikasi di lingkungan
> penulisan ini karena tidak ada Android SDK/network di sandbox tempat kode
> ini ditulis — **verifikasi build sesungguhnya terjadi di GitHub Actions**
> begitu repo ini di-push (lihat `docs/CI.md`).

## Fitur

- Equalizer 5/10/15/20/31-band + parametric EQ (peak/low-shelf/high-shelf/
  low-pass/high-pass/notch), dengan grafik EQ realtime yang bisa di-drag.
- Preamp, bass/treble/sub-bass/air boost, loudness, balance L/R, mono,
  stereo widening, crossfeed, compressor, limiter, output gain, volume
  normalization — model data tersedia penuh (`ProcessingChain`); sebagian
  diterapkan langsung ke AudioEffect platform, sisanya adalah state yang
  siap dihubungkan ke engine DSP kustom jika diperlukan (lihat
  `docs/AUDIO_ENGINE.md` untuk batas API platform yang jujur dijelaskan).
- 19 preset built-in (Flat, Bass Boost, Deep Bass, Vocal, Clear Vocal,
  Podcast, Rock, Pop, Classical, Jazz, EDM, Gaming, Movie, Acoustic, Treble
  Boost, Warm, Bright, Neutral, V-shaped) + create/edit/duplicate/rename/
  delete/export/import (JSON, dengan `schemaVersion` untuk migrasi).
- Device profile system: deteksi perangkat output aktif via
  `AudioDeviceCallback`, auto-apply profile saat device berganti (best-effort
  matching + fallback manual).
- Halaman Device Tuning bergaya aplikasi tuning IEM premium: info perangkat,
  status kemampuan (capability-gated, tidak pernah crash saat fitur tidak
  didukung), editor parametric EQ.
- Logging terstruktur lokal (Room) + halaman Diagnostics (view/clear/export/
  share), tidak pernah mengirim data otomatis.
- Material You / Dynamic Color, dark/light/system theme, edge-to-edge.
- Foreground service yang re-attach otomatis saat Bluetooth/wired headset
  reconnect atau audio route berubah.

## Android yang didukung

Android 10 (API 29) s.d. Android 16, `minSdk 29`, `targetSdk/compileSdk 35`.

## Struktur proyek

```
app/src/main/java/com/rahmatsobrian/sirohaequ/
  SirohaEquApp.kt              Application + uncaught exception handler
  MainActivity.kt              NavHost, entry point
  audio/                       Audio engine (lihat docs/AUDIO_ENGINE.md)
  data/                        DataStore settings, Room (preset & device profile)
  data/model/                  EqBand, Preset, DeviceProfile, dll (semua @Serializable)
  logging/                     AppLogger + Room diagnostic log store
  service/                     AudioProcessingService (foreground service)
  ui/                          Compose screens + ViewModel + theme
app/src/test/                  Unit test (JVM murni, tanpa Android framework)
app/src/androidTest/           Instrumentation test (Compose UI)
.github/workflows/android.yml  CI: build, unit test, lint, instrumentation test
docs/AUDIO_ENGINE.md           Arsitektur & keterbatasan platform audio
docs/CI.md                     Detail pipeline CI
```

## Cara build

```bash
git clone <repo-url>
cd SirohaEqu
./gradlew :app:assembleDebug
```

Jika `./gradlew` belum ada (repo ini sengaja tidak meng-commit
`gradle-wrapper.jar`, lihat `docs/CI.md`), buat dulu dengan Gradle sistem:

```bash
gradle wrapper --gradle-version 8.7 --distribution-type all
```

APK debug akan ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Cara menjalankan test

```bash
./gradlew :app:testDebugUnitTest        # unit test (JVM)
./gradlew :app:connectedDebugAndroidTest # instrumentation test (emulator/device)
./gradlew :app:lintDebug                 # lint
```

## Cara membuat preset

Buka **Presets** → *"+ Simpan pengaturan saat ini sebagai preset baru"*,
atau edit langsung band di **Device Tuning** lalu simpan sebagai preset.
Export/import memakai format JSON dengan field `schemaVersion` sehingga
tetap kompatibel meski struktur berubah di update mendatang.

## Cara membuat device profile

Di **Device Tuning**, setelah menyambungkan perangkat audio, simpan preset
+ pengaturan saat ini sebagai profile untuk perangkat itu. Auto-apply
dicoba lebih dulu berdasarkan nama produk perangkat (jika tersedia dari
Android); jika tidak yakin, profil bisa dipilih manual.

## Troubleshooting

- **EQ tidak berefek**: cek status di halaman utama (`Idle`/`Terbatas`/
  `Gagal`). Equalizer hanya aktif setelah menerima audio session dari
  aplikasi pemutar musik yang mengirim broadcast standar Android — lihat
  `docs/AUDIO_ENGINE.md`.
- **Device tidak terdeteksi otomatis**: gunakan pemilihan profil manual di
  Device Tuning; deteksi nama perangkat bergantung pada data yang diekspos
  OEM/firmware ke Android.
- **Fitur tertentu abu-abu/nonaktif**: berarti backend audio pada
  perangkat/koneksi saat ini tidak mengekspos AudioEffect terkait — alasan
  spesifik ditampilkan di kartu "Informasi Perangkat".

## Cara mengambil diagnostic log

Settings → Diagnostics → **Export Diagnostic Report**. File JSON dibuat di
cache aplikasi lalu dibagikan lewat share sheet Android (FileProvider) —
tidak pernah dikirim otomatis ke mana pun.

## Prioritas desain

Stabilitas > keamanan > kompatibilitas > audio processing > performa > UI,
sesuai spesifikasi awal proyek. Tidak ada fitur yang mengklaim kontrol
firmware/hardware yang sebenarnya tidak diberikan Android API.
