package com.gamebridge.mobile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

/** Keeps resize handling from reloading either the converter preview or exported HTML game. */
public final class ActivityResizePolicyTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        String appManifest = read(projectFile("app/src/main/AndroidManifest.xml"));
        String htmlManifest = read(projectFile("html-template-source/app/src/main/AndroidManifest.xml"));
        String appActivity = read(projectFile("app/src/main/java/com/gamebridge/mobile/MainActivity.java"));
        String htmlActivity = read(projectFile("html-template-source/app/src/main/java/com/example/finalhtml/MainActivity.java"));

        for (String dimension : new String[]{"screenSize", "smallestScreenSize", "screenLayout"}) {
            require(appManifest.contains(dimension), "APK Forge activity must handle " + dimension + " without recreation");
            require(htmlManifest.contains(dimension), "exported HTML activity must handle " + dimension + " without recreation");
        }
        require(appActivity.contains("browserWebView.requestLayout()") && appActivity.contains("browserWebView.invalidate()"),
                "converter WebView must relayout in-place after resize");
        require(htmlActivity.contains("gameView.requestLayout()") && htmlActivity.contains("gameView.invalidate()"),
                "exported game WebView must relayout in-place after resize");
        int resizeStart = appActivity.indexOf("onConfigurationChanged(Configuration configuration)");
        int resizeEnd = appActivity.indexOf("@Override public void onBackPressed", resizeStart);
        String resizeHandler = appActivity.substring(resizeStart, resizeEnd);
        require(!resizeHandler.contains("loadUrl(") && !resizeHandler.contains("destroy()"),
                "resize handling must not reload or destroy the game WebView");
        require(resizeHandler.contains("updateResponsiveLayout(configuration)"),
                "orientation and window resizing must switch between phone and tablet layouts automatically");
        require(appActivity.contains("ResponsiveLayoutPolicy.isTabletMode"),
                "tablet selection must use a tested width policy");
        int pageSelectionSetup = appActivity.indexOf("FrameLayout.LayoutParams selectionParams");
        int tabletPreviewStart = appActivity.indexOf("if (tabletMode) {", pageSelectionSetup);
        int phonePreviewStart = appActivity.indexOf("} else {", tabletPreviewStart);
        String tabletPreviewSetup = appActivity.substring(tabletPreviewStart, phonePreviewStart);
        require(tabletPreviewSetup.contains("previewColumn.addView(pageSelection, fullWithTop")
                        && !tabletPreviewSetup.contains("previewFrame.addView(pageSelection"),
                "tablet page-selection banner must sit outside the game viewport so it cannot cover game content");
        System.out.println("ActivityResizePolicy tests passed");
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
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
