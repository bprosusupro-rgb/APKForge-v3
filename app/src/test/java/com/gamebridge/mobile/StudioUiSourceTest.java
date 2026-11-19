package com.gamebridge.mobile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class StudioUiSourceTest {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get("app/src/main/java/com/gamebridge/mobile/MainActivity.java");
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        require(source.contains("StudioUiPolicy.PREFERENCE_KEY"), "UI choice must be persisted");
        require(source.contains("setStudioUiEnabled(true)") && source.contains("setStudioUiEnabled(false)"), "Settings must offer both UI choices");
        require(source.contains("buildStudioBottomNavigation(root, 0)"), "Studio phone layout must have its own navigation");
        require(source.contains("showPhonePage(browserPage, 1)") && source.contains("showPhonePage(settingsPage, 3)"), "Studio navigation must persist on browser and settings pages");
        require(source.contains("i == selectedDestination"), "Studio navigation must show the active destination");
        require(source.contains("buildStudioTabletWorkspace(page, hero, offline, source, settings"), "Studio tablet convert page must use its dedicated workspace layout");
        require(source.contains("workspace.setOrientation(LinearLayout.HORIZONTAL)"), "Studio tablet build flow must use two columns");
        require(source.contains("insets.getSystemWindowInsetTop()"), "Tablet navigation must clear the Android status bar");
        require(source.contains("\"Build\", \"Web\", \"Library\", \"Settings\""), "Studio navigation covers the core destinations");
        require(source.contains("setTranslationY(dp(8))") && source.contains("ValueAnimator.areAnimatorsEnabled()"), "Studio navigation animation is subtle and respects reduced-motion system settings");
        require(source.contains("boolean keepBrowser = showingBrowser") && source.contains("selectedText.setText(savedSelection)"), "switching UI modes preserves page and selected game state");
        require(source.contains("Install / update") && source.contains("installedBuildMatches"), "playing a rebuilt game must install changed APK files in place instead of silently opening the old build");
        System.out.println("StudioUiSource tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
