# Arus

## Build dan tes lokal

Pastikan Android SDK dan JDK 17 tersedia. Jika SDK berada di lokasi standar macOS:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Instal ke perangkat Android yang tersambung melalui ADB:

```bash
./gradlew :app:installDebug
```
