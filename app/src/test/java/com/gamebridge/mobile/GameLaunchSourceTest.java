package com.gamebridge.mobile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Regression checks for the Play, install-then-play, and system uninstall paths. */
public final class GameLaunchSourceTest {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get("app/src/main/java/com/gamebridge/mobile/MainActivity.java");
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        String manifest = new String(Files.readAllBytes(Paths.get("app/src/main/AndroidManifest.xml")), StandardCharsets.UTF_8);
        require(source.contains("playBuiltButton = button(\"Play built game\", true)"),
                "the converter must expose Play for its latest successful build");
        require(source.contains("play.setOnClickListener(v -> playApk(apk))"),
                "library builds must be playable directly from the library");
        require(source.contains("prepareGame(apk, true)")
                        && source.contains("launch != null && installedBuildMatches(packageName, apk)")
                        && source.contains("writeInstallSession(apk, packageName, launchAfterInstall)"),
                "Play must prepare an install for a missing or changed build");
        require(source.contains("apkforge.launch_after_install") &&
                        source.contains("getLaunchIntentForPackage(packageName)"),
                "a Play-triggered installation must launch the game after Android confirms it");
        require(source.contains("new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse(\"package:\" + packageName))") &&
                        source.contains("Intent.EXTRA_RETURN_RESULT") && manifest.contains("android.permission.REQUEST_DELETE_PACKAGES"),
                "uninstall must open Android's system-confirmed package deletion screen and return its result");
        require(source.contains("if (installedList != null) refreshInstalled()"),
                "the installed-games list must refresh after returning from Android's package UI");
        System.out.println("GameLaunchSource tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
