# Audio Engine — Cara Kerja

## Arsitektur

```
audio/
  AudioEngine.kt            Koordinator utama. Satu-satunya kelas yang
                             disentuh ViewModel/UI. Bertanggung jawab atas
                             lifecycle sesi audio dan crash-safety.
  AudioCapabilities.kt      Probe: mendeteksi efek apa yang benar-benar
                             tersedia pada audio session saat ini.
  EqualizerEngine.kt        Wrapper android.media.audiofx.Equalizer.
                             Memproyeksikan band app-level (5/10/15/20/31/
                             parametric) ke band native hardware.
  BassTrebleEngine.kt       Wrapper BassBoost + LoudnessEnhancer.
  AudioDeviceManager.kt     Deteksi perangkat output aktif + pencocokan ke
                             DeviceProfile tersimpan.
  GainMath.kt                Matematika interpolasi gain murni (testable di
                             JVM biasa, dipakai bareng oleh EqualizerEngine
                             dan EqGraph agar preview = hasil nyata).
```

UI (Compose) tidak pernah memanggil `android.media.audiofx.*` secara
langsung — selalu lewat `AudioEngine`. Ini yang memungkinkan backend diganti
di masa depan (mis. AAudio/Oboe native engine) tanpa menyentuh UI.

## Bagaimana equalizer "menempel" ke audio yang sedang diputar

Android **tidak** menyediakan API publik bagi aplikasi biasa (non-root,
non-privileged) untuk menerapkan audio effect ke *seluruh* audio sistem
tanpa syarat. Ini bukan keterbatasan aplikasi ini, melainkan batas platform
Android itu sendiri sejak effect framework diperkenalkan.

Mekanisme yang tersedia dan dipakai aplikasi ini:

1. Aplikasi pemutar musik yang kooperatif (banyak music player mainstream
   melakukan ini) mengirim broadcast
   `AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION` berisi
   `audioSessionId` miliknya ketika mulai memutar audio.
2. `AudioProcessingService` mendengarkan broadcast tersebut dan memanggil
   `AudioEngine.attachToSession(sessionId)`.
3. Effect (`Equalizer`, `BassBoost`, dst) dilekatkan ke session id itu, jadi
   pemrosesan berlaku untuk audio dari aplikasi tersebut selama sesi itu
   aktif.
4. Saat `ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION` diterima (mis. lagu
   berhenti, headset dicabut, koneksi Bluetooth putus), engine dilepas dan
   menunggu broadcast open berikutnya — termasuk otomatis re-attach saat
   Bluetooth/wired headset reconnect atau aplikasi dibuka kembali.

**Yang TIDAK dilakukan aplikasi ini, dan tidak diklaim bisa dilakukan:**
- Mengubah firmware perangkat audio (IEM/DAC/dst). "Device Tuning" adalah
  *software profile/tuning layer* di sisi Android, bukan flashing firmware.
- Menerapkan EQ ke semua aplikasi tanpa syarat tanpa root — ini butuh akses
  yang tidak diberikan Android ke aplikasi biasa.
- Menjamin nama perangkat 100% akurat — `AudioDeviceInfo.getProductName()`
  sering generik tergantung OEM/firmware, sehingga auto-profile matching
  bersifat *best-effort* dan profil manual selalu tersedia sebagai fallback.

Jika suatu fitur (mis. BassBoost strength) tidak didukung hardware/backend
yang sedang aktif, `AudioCapabilities` mendeteksinya lebih dulu, UI
menampilkan status "tidak tersedia" beserta alasannya, dan aplikasi tidak
crash — sesuai kontrak "detect → disable → explain → never crash".

## Proyeksi band ke hardware EQ native

Equalizer effect platform Android umumnya hanya mengekspos sedikit band
native (sering 5). Untuk mendukung mode 10/15/20/31-band dan parametric EQ
di atas hardware sekasar itu, setiap band native diberi gain hasil
interpolasi tertimbang segitiga (triangular weighting) dari seluruh band
app-level terhadap jarak frekuensi logaritmik (`GainMath.interpolatedGainDb`).
Kurva yang ditampilkan di `EqGraph` memakai fungsi matematika yang sama
persis, sehingga apa yang digambar di layar selalu konsisten dengan apa
yang benar-benar diterapkan ke audio — bukan sekadar ilustrasi.

Konsekuensi jujur: pada hardware dengan native band sangat sedikit, bentuk
parametric (peak/shelf/notch) yang tajam tidak bisa direproduksi 100%
presisi di DSP — hasil akhirnya adalah aproksimasi terbaik yang tersedia,
bukan implementasi DSP parametric filter kustom (yang akan membutuhkan
audio effect chain native/AAudio, di luar cakupan versi ini).
