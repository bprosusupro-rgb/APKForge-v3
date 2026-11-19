# APK Forge HTML game template

This source project produces `android-app/app/src/main/assets/html-template.apk`.
The packager replaces `assets/www/` contents for each generated HTML game.

The WebView is focusable. Taps and gestures are delivered directly to the game
page as normal Android/WebView touch input; APK Forge does not draw a gamepad
or synthesize replacement gameplay controls. A bundled touch adapter adds
invisible hit areas around small, existing HTML buttons, links, labels, and
accessible controls, forwarding taps to the original element without covering
neighboring controls or changing their appearance. It uses the same adapter
for local/offline pages and top-level online pages. HTML text
inputs, text areas, and editable game content request Android's on-screen
keyboard when focused.
Buttons, checkboxes, and dropdowns do not trigger the text keyboard. Physical
keyboard and connected hardware game controllers are supported where the game
itself recognizes their input. Ren'Py games use Ren'Py's Android runtime, which
maps touchscreen input to mouse input. Arbitrary game-specific keyboard
controls cannot be safely rewritten into touch controls without understanding
and changing that game's source. Headset pointer/keyboard behavior needs
verification on the actual VR device.

Build with Android SDK API 36, Android Gradle Plugin 8.13, Gradle 8.14.5, and JDK 17 or newer:

```powershell
gradle --offline assembleDebug
```

Copy `app/build/outputs/apk/debug/app-debug.apk` over the matching HTML
template asset only after verifying the build. The APK Forge packager strips
the template's signing metadata and signs each resulting game APK itself.
