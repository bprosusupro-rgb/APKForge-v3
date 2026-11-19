# APK Forge V3

This is a separate V3 source/build tree based on the working APK Forge V2 project. V2 is left unchanged. V3 uses a distinct Android application ID so it can be installed alongside the existing app.

The website inspector adds optional on-device AI search suggestions when an offline capture or page scan cannot find game files. It uses only the page title as local model input and suggests official Windows, Android/HTML, and complete Ren'Py game-data options. You choose each query before APK Forge opens DuckDuckGo; page content and the current URL are not sent to the model. Search result cards are unverified leads, not endorsements. Web lookup remains opt-in in Settings, and you can step through the remaining suggestions until a useful lead appears.

Search suggestions do not certify a download as safe. HTTPS Windows EXEs can be saved only after confirmation and a structural Windows PE header check, in app-private temporary storage. APK Forge calculates the file's SHA-256 locally and offers a hash-only VirusTotal lookup, manual comparison with a checksum you supply, or export through Android's file picker. It never executes an EXE; the PE check, publisher hostname, reputation lookup, and checksum are not a malware scan or safety guarantee. A Windows launcher by itself cannot be converted into an Android game. For Ren'Py, use a complete game-data/project archive or creator-provided Android build; for HTML, use a complete downloadable site bundle or the online wrapper.

Offline snapshot smoke test on the Android Studio tablet emulator: a captured `example.com` page packaged, installed, and rendered with airplane mode enabled. This caught and fixed a root-path rewrite that had corrupted closing HTML tags. The sample page also re-ran its own `s.js` script and duplicated some localized paragraphs, so this proves basic HTML capture/launch without network—not that arbitrary games or every page script will behave identically offline. Snapshotting is best-effort; server-backed features, DRM, accounts, and content not loaded before capture may still require internet or remain unavailable.

Build V3 with `./gradlew :app:assembleRelease` on Linux or macOS, or `gradlew.bat :app:assembleRelease` on Windows. The release APK is written to `app/build/outputs/apk/release/`. With `release-signing.properties`, Gradle signs with the referenced key. Back up both the properties file and the keystore privately. Android updates require the same application ID and signing certificate.

Build with `-PstandaloneRelease` to use the separate `com.gamebridge.mobile.v5.standalone` application ID. This variant can be installed alongside an existing V3 copy. It has its own library, app data and generated game package IDs. If no signing configuration is supplied, sign the unsigned APK before distribution.

The project bridges the core conversion, offline capture, Ren'Py size/storage, touch/keyboard, archive, source-assessment, and EXE-structure checks into Gradle with `:app:testDebugUnitTest`. The underlying self-running test classes can also be executed directly when diagnosing a specific case.

Build the development/debug APK with `gradlew.bat :app:assembleDebug` only when debugging is needed. Its output is `app/build/outputs/apk/debug/app-debug.apk`.
