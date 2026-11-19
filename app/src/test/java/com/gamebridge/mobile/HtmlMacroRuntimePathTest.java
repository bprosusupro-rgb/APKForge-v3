package com.gamebridge.mobile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Ensures offline captures and online wrappers share the touch/macro-enabled HTML runtime. */
public final class HtmlMacroRuntimePathTest {
    public static void main(String[] args) throws Exception {
        Path projectRoot = findProjectRoot(Paths.get("").toAbsolutePath());
        Path runtimePath = projectRoot.resolve("html-template-source/app/src/main/java/com/example/finalhtml/MainActivity.java");
        String runtime = read(runtimePath);
        require(runtime.contains("readAsset(\"apkforge-touch-adapter.js\")"),
                "HTML runtime must load the packaged touch and macro adapter");
        require(runtime.contains("view.evaluateJavascript(touchAdapter, null)"),
                "HTML runtime must install the adapter into every game page");
        require(runtime.contains("@Override public void onPageFinished"),
                "online navigation and offline page loads must both trigger adapter setup");
        require(runtime.contains("Export save backup…") && runtime.contains("Import save backup…"),
                "HTML games must expose portable save export and import actions");
        require(runtime.contains("LocalGameStateStore.exportBackup") && runtime.contains("LocalGameStateStore.restoreBackup"),
                "save backup actions must persist and restore validated per-origin save data");

        String packager = read(projectRoot.resolve("app/src/main/java/com/gamebridge/mobile/OfflineApkPackager.java"));
        require(packager.contains("templateName = project.renpy ? \"renpy-template.apk\" : \"html-template.apk\""),
                "offline HTML archives must use the touch-enabled HTML runtime template");
        String online = read(projectRoot.resolve("app/src/main/java/com/gamebridge/mobile/OnlineGameArchive.java"));
        require(online.contains("location.replace("),
                "online APK wrappers must navigate the same runtime WebView to the game URL");
        System.out.println("HTML macro runtime path tests passed");
    }

    private static Path findProjectRoot(Path start) {
        for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
            if (Files.isDirectory(candidate.resolve("html-template-source"))
                    && Files.isDirectory(candidate.resolve("app/src/main/java"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not locate APK Forge project root from " + start);
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
