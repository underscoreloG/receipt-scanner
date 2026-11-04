# Receipt Scanner

Minimal Android app: one button opens Google's ML Kit Document Scanner (auto edge detection, crop, flatten), saves each page as a JPEG in `Pictures/Receipts`, then reopens the scanner so you can scan the next receipt. Press X in the scanner to stop.

- Java, no AndroidX UI, minSdk 29, targetSdk 36
- Needs Google Play services on the phone

## Build

1. Create `local.properties` with `sdk.dir=<path to Android SDK>`.
2. Run `gradle assembleDebug` (Gradle 8.14+, JDK 17+). No Gradle wrapper is checked in.
3. Install `app/build/outputs/apk/debug/app-debug.apk` with `adb install -r`.

`gradle.properties` sets `android.overridePathCheck=true` because the Windows profile path contains an accented character. Remove it if your path is plain ASCII.
