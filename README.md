# Arus

## Build dan tes lokal

Pastikan Android SDK dan JDK 17 tersedia. Jika SDK berada di lokasi standar macOS:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.device.serial=emulator-5554
```

Untuk verifikasi bersih, jalankan:

```bash
./gradlew clean :app:testDebugUnitTest :app:connectedDebugAndroidTest -Pandroid.injected.device.serial=emulator-5554
```

Pastikan AVD `arus_api26` berjalan dan muncul sebagai `emulator-5554` pada `adb devices -l`.
Instal build debug ke perangkat atau emulator yang tersambung melalui ADB:

```bash
./gradlew :app:installDebug -Pandroid.injected.device.serial=emulator-5554
```
