package com.gamebridge.mobile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class HtmlTouchAdapterSourceTest {
    public static void main(String[] args) throws Exception {
        Path asset = projectFile("html-template-source/app/src/main/assets/apkforge-touch-adapter.js");
        String source = new String(Files.readAllBytes(asset), StandardCharsets.UTF_8);
        require(source.contains("__apkForgeTouchAdapterV1"), "adapter must install once per page");
        require(source.contains("getBoundingClientRect()"), "adapter must measure actual game controls");
        require(source.contains("Math.min(22, (44 - rect.width) / 2)"), "adapter must enlarge small touch targets");
        require(source.contains("target.click()"), "invisible hit areas must activate the original game element");
        require(source.contains("overlapsAnotherControl") || source.contains("first.blocked"),
                "overlapping controls must not receive ambiguous hit areas");
        require(source.contains("opacity:0") && source.contains("pointer-events:auto"),
                "touch hit areas must be invisible but tappable");
        require(source.contains("overlapsAnotherControl") || source.contains("first.blocked"),
                "neighboring controls must not get overlapping hit regions");
        require(!source.contains("APKForgeControls") && !source.contains("gamepad"),
                "adapter must not add a controller UI");
        require(source.contains("apkforge-tools-toggle") && source.contains("aria-expanded"),
                "adapter must provide an accessible expandable in-game tools button");
        require(source.contains("apkforge-mouse-toggle") && source.contains("aria-pressed"),
                "expanded tools must provide an accessible mouse mode toggle");
        require(source.contains("apkforge-keyboard") && source.contains("APKForgeTextInput"),
                "expanded tools must provide a keyboard button wired to the Android keyboard bridge");
        require(source.contains("env(safe-area-inset-right)") && source.contains("env(safe-area-inset-bottom)"),
                "floating tools must respect device safe areas");
        require(source.contains("touchstart") && source.contains("dispatchEvent(mouseEvent"),
                "mouse mode must translate touch into coordinate-aware mouse events");
        require(source.contains("dispatchMouse(\"mousedown\", touch.clientX, touch.clientY, target, false)") &&
                        source.contains("document.elementFromPoint(touch.clientX, touch.clientY), false") &&
                        source.contains("dispatchMouse(\"mouseup\", x, y, target, false)") &&
                        source.contains("dispatchMouse(\"click\", x, y, target, false)") &&
                        source.contains("if (pointer && window.PointerEvent)"),
                "touch-to-mouse fallback must not duplicate the native pointer events already sent by Android WebView");
        require(source.contains("data-apkforge-macro-add-tap") && source.contains("Add tap"),
                "offline game tools must let players manually add tap actions");
        require(source.contains("data-apkforge-macro-add-scroll") && source.contains("Add scroll"),
                "offline game tools must let players manually add scroll actions");
        require(source.contains("data-apkforge-macro-add-wait") && source.contains("Add wait"),
                "offline game tools must let players manually add wait actions");
        require(source.contains("draggable") && source.contains("dragstart") && source.contains("dragover"),
                "macro actions must be reorderable by dragging");
        require(source.contains("macroTouchDragIndex") && source.contains("pointerup") && source.contains("elementFromPoint(event.clientX, event.clientY)"),
                "macro actions must also reorder with touch dragging on phones and tablets");
        require(source.contains("data-apkforge-step-delay") && source.contains("data-apkforge-step-repeat"),
                "each macro action must expose editable timing and repeat controls");
        require(source.contains("data-apkforge-step-x") && source.contains("data-apkforge-step-y") &&
                        source.contains("data-apkforge-step-dx") && source.contains("data-apkforge-step-dy"),
                "tap coordinates and scroll distances must be manually editable");
        require(source.contains("data-apkforge-macro-stage") && source.contains("data-apkforge-stage-dots") &&
                        source.contains("data-apkforge-stage-done"),
                "macro editor must provide a full-screen game placement overlay");
        require(source.contains("pointer-events:none") && source.contains("pointer-events:auto") &&
                        source.contains("data-apkforge-stage-dot"),
                "placement overlay must leave the game visible while making only its numbered markers interactive");
        require(source.contains("macroStage.getBoundingClientRect()") && source.contains("macroStagePointerId"),
                "macro tap markers must be draggable across the actual game viewport");
        require(source.contains("data-apkforge-stage-place") && source.contains("Position on game") &&
                        source.contains("Tap the game to position the selected marker"),
                "users must be able to select and directly place a tap marker over the real game");
        require(source.contains("Pick scroll area") && source.contains("macroPickingMode") &&
                        source.contains("scrollHeight > candidate.clientHeight"),
                "manual scroll steps must let users choose a nested game scroll area");
        require(!source.contains("Record macro") && !source.contains("Stop recording") &&
                        !source.contains("data-apkforge-macro-record"),
                "macro creation must be manual and must not ask users to record their clicks");
        require(source.contains("data-apkforge-macro-play") && source.contains("Play macro"),
                "offline game tools must replay a saved macro");
        require(source.contains("scrollBy") && source.contains("type === \"scroll\"") && source.contains("type === \"wait\"") &&
                        source.contains("repeatCount") && source.contains("repeatInterval"),
                "macros must replay configurable tap, scroll, wait, and repeated actions");
        require(source.contains("innerWidth") && source.contains("innerHeight"),
                "tap locations must be normalized so playback adapts to phone and tablet viewport sizes");
        require(source.contains("__apkforge_macro_v1") && source.contains("localStorage"),
                "offline macros must persist locally across game launches without network storage");
        require(source.contains("MAX_MACRO_STEPS") && source.contains("MAX_MACRO_DURATION_MS"),
                "manual macros must be bounded to avoid unbounded local storage or playback");
        require(source.contains("data-apkforge-tool-ui") && source.contains("closest('[data-apkforge-tool-ui]'"),
                "tool UI must not be treated as a game touch target");
        require(source.contains("width:min(384px,calc(100vw - 24px))") &&
                        source.contains("max-height:min(76vh,calc(100vh - 112px))"),
                "floating macro panel must stay within the viewport width and height");
        require(source.contains("data-apkforge-add-row style=\"display:grid;grid-template-columns:repeat(3,minmax(0,1fr))") &&
                        source.contains("data-apkforge-control-row style=\"display:grid;grid-template-columns:repeat(2,minmax(0,1fr))"),
                "macro and input controls must reflow into compact touch-friendly rows");
        require(source.contains("data-apkforge-macro-footer style=\"position:sticky") &&
                        source.contains("data-apkforge-macro-play"),
                "macro playback actions must remain in a visible sticky footer while editing steps");
        require(source.contains("__apkForgeTouchTarget") && source.contains("elementFromPoint(x, y)"),
                "macro replay must resolve invisible expanded touch targets back to the actual game control");
        Path template = projectFile("app/src/main/assets/html-template.apk");
        try (ZipFile apk = new ZipFile(template.toFile())) {
            ZipEntry packagedAdapter = apk.getEntry("assets/apkforge-touch-adapter.js");
            require(packagedAdapter != null, "offline HTML template must include the touch and macro adapter");
            String packaged = new String(apk.getInputStream(packagedAdapter).readAllBytes(), StandardCharsets.UTF_8);
            require(packaged.equals(source), "offline HTML template must package the current tested adapter source");
        }
        String runtime = new String(Files.readAllBytes(projectFile(
                "html-template-source/app/src/main/java/com/example/finalhtml/MainActivity.java")), StandardCharsets.UTF_8);
        require(source.contains("APKForgeLocalSaves.onToolsExpanded(open)") &&
                        runtime.contains("onToolsExpanded(boolean expanded)") &&
                        runtime.contains("saveButton.setVisibility(expanded ? View.GONE : View.VISIBLE)"),
                "expanding offline game tools must temporarily hide the native save button so it cannot block macro controls");
        System.out.println("HtmlTouchAdapterSource tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Path projectFile(String relativePath) {
        Path directory = Paths.get("").toAbsolutePath().normalize();
        while (directory != null) {
            Path candidate = directory.resolve(relativePath);
            if (Files.isRegularFile(candidate)) return candidate;
            directory = directory.getParent();
        }
        throw new AssertionError("could not locate project file from the test working directory: " + relativePath);
    }
}
