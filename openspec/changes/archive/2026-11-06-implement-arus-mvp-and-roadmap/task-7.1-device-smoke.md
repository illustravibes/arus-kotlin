# Task 7.1 Device Smoke Test

## Device

- Model: Samsung SM-A546E
- Android: 17 (API 37)
- Build: `A546EXXUNGZJ5`
- Test date: 2026-11-05

## Results

- Fingerprint sensor detected; two fingerprints enrolled.
- App PIN setup completed; app-lock preference contains a PIN hash.
- Biometric prompt displayed from Arus.
- Biometric authentication succeeded at least once; Arus returned to its home screen.
- Biometric preference persisted as enabled.
- Returning from background triggered app lock and displayed biometric prompt again.
- One attempt showed `Biometrik gagal. PIN tetap aktif.` and required PIN fallback.
- PIN fallback screen appeared; successful PIN submission and unlock were not independently verified.

## Status

Task 7.1 verification is complete. PIN success/failure and relocking passed on the Samsung device; biometric success and background relock were observed during the device check. Biometric-unavailable fallback passed on API 26 AVD. The prompt-failure test skips on devices with enrolled biometrics.

Task 7.4 verification is complete. Physical One UI tests cover installation, onboarding, transaction, transfer, home, reconciliation resolution, and app lock.

## Automated Verification

- Earlier instrumentation suite passed on `arus_api26(AVD)` (Android 8.0.0/API 26), 61/61 tests, including biometric-unavailable fallback.
- The Samsung SM-A546E PIN setup, incorrect/correct PIN entry, biometric prompt, successful biometric authentication, and relock after background return were verified.
- Successful PIN entry unlocked the app during `appLockSettingsCanSetAndDisablePin` on the connected Samsung device.

## Verifikasi Android 17

- AVD `emulator-5554` was available; connected instrumentation target resolved to Samsung SM-A546E, Android 17/API 37.
- `./gradlew :app:testDebugUnitTest` lulus.
- Initial run failed on 10 UI tests because Espresso 3.5.0 called missing `InputManager.getInstance()` on API 37.
- Pinned `androidx.test.espresso:espresso-core:3.7.0`; rerun passed on SM-A546E with 60 passed and 1 skipped. The skipped test simulates a device without enrolled biometrics.
- `androidx.test.espresso:espresso-core:3.7.0` resolves the API 37 Espresso failure.

## Cakupan Task 7.4 pada Perangkat Fisik

- `./gradlew :app:connectedDebugAndroidTest` ran on Samsung SM-A546E, Android 17/API 37; 61 passed, 1 skipped because the device has enrolled biometrics.
- Connected suite installed the debug build and passed transaction save/Undo, home navigation, and app-lock PIN tests.
- Focused onboarding test completed setup and verified the new wallet on the physical device.
- Focused transfer test saved both linked transfer rows with matching amount and group ID.
- Focused reconciliation test resolved the untracked difference and confirmed the resulting balance.
- Duplicate USB/Wi-Fi ADB targets caused one concurrent run to fail with test-state collisions; disconnecting Wi-Fi ADB left one USB target, and the full suite passed.
