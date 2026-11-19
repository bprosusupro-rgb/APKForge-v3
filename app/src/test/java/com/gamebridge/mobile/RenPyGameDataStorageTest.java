package com.gamebridge.mobile;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/** Prevents large sidecar builds from needing a second full-size copy on another storage mount. */
public final class RenPyGameDataStorageTest {
    public static void main(String[] args) throws Exception {
        File cache = Files.createTempDirectory("apkforge-cache").toFile();
        File library = Files.createTempDirectory("apkforge-library-data").toFile();
        File apk = new File(cache, "large-game.apk");
        try {
            Class<?> packager = Class.forName("com.gamebridge.mobile.OfflineApkPackager");
            Method targetMethod = packager.getDeclaredMethod(
                    "gameDataBundleTarget", File.class, File.class);
            targetMethod.setAccessible(true);
            File target = (File) targetMethod.invoke(null, apk, library);
            require(target.getParentFile().getCanonicalFile().equals(library.getCanonicalFile()),
                    "large game data must be produced on the final library storage mount");
            require(target.getName().equals("large-game.apk.game-data.zip"),
                    "game-data bundle name must remain unique to its APK");
            System.out.println("Ren'Py game-data storage tests passed");
        } finally {
            targetCleanup(cache);
            targetCleanup(library);
        }
    }

    private static void targetCleanup(File directory) throws Exception {
        File[] children = directory.listFiles();
        if (children != null) for (File child : children) Files.deleteIfExists(child.toPath());
        Files.deleteIfExists(directory.toPath());
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
