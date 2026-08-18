# CI/CD — GitHub Actions

Workflow: `.github/workflows/android.yml`, dua job: `build` dan
`instrumentation-test` (berjalan setelah `build` sukses).

## Job `build`

1. Checkout repo.
2. Setup JDK 17 + Android SDK (`android-actions/setup-android`).
3. **Cek Gradle wrapper**: jika `gradlew` atau
   `gradle/wrapper/gradle-wrapper.jar` tidak ada di repo, workflow mengunduh
   Gradle 8.7 resmi dari `services.gradle.org` (sumber terpercaya) dan
   menjalankan `gradle wrapper --gradle-version 8.7 --distribution-type all`
   untuk membuatnya sendiri. Repo ini sengaja **tidak** meng-commit
   `gradle-wrapper.jar` — lihat spec section 20 & 21.
4. **Debug keystore deterministik**: dibuat sekali dengan `keytool` memakai
   alias/parameter tetap, lalu di-cache lintas run dengan
   `actions/cache` (key: `siroha-equ-debug-keystore-v1`). Ini yang membuat
   APK dari build CI berikutnya bisa meng-update APK build sebelumnya tanpa
   uninstall — selama key cache tidak dihapus. Ini keystore **debug/testing
   saja**, bukan release keystore, dan tidak pernah di-commit ke repo.
5. Build (`assembleDebug`), unit test (`testDebugUnitTest`), lint
   (`lintDebug`) — dijalankan sebagai step terpisah, **tanpa**
   `continue-on-error` di step build utama, supaya kegagalan salah satu
   benar-benar menggagalkan workflow (tidak disembunyikan dengan `|| true`).
6. Upload artifact: `app-debug` (APK), `unit-test-report`, `lint-report`.
7. Jika ada langkah yang gagal: log dikumpulkan ke `build-logs/` dan
   di-upload sebagai artifact `build-logs`, supaya kegagalan tetap bisa
   didiagnosis dari UI Actions tanpa akses lokal.

## Job `instrumentation-test`

Berjalan di emulator (`reactivecircus/android-emulator-runner`, API 34,
x86_64, profil Pixel 6) dengan akselerasi KVM, menjalankan
`connectedDebugAndroidTest`. Laporan diupload sebagai
`instrumentation-test-report`.

## Yang sengaja TIDAK dibutuhkan workflow ini

Tidak ada Repository Secret yang dibutuhkan sama sekali — tidak
`SIGNING_KEY`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, atau
Firebase/API secret apa pun. Semua build di workflow ini adalah **debug
build** yang ditandatangani dengan debug keystore CI, bukan release build
untuk Play Store. Untuk rilis produksi, tambahkan signing config release
terpisah dengan secret Anda sendiri di luar workflow ini.

## Nama artifact

Konsisten dan jelas agar mudah ditemukan di halaman run Actions:
`app-debug`, `unit-test-report`, `lint-report`, `instrumentation-test-report`,
`build-logs` (hanya muncul saat gagal).
