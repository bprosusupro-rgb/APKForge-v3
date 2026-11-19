package com.gamebridge.mobile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Standalone regression tests; run with java after compiling shared sources. */
public final class RenPyGameDataBundleTest {
    public static void main(String[] args) throws Exception {
        copiesOnlyRenPyGameTreeAndAddsRuntimeRoot();
        rejectsZipSlipBeforePublishingBundle();
        rejectsBundleWithoutRenPyContent();
        System.out.println("RenPyGameDataBundle tests passed");
    }

    private static void copiesOnlyRenPyGameTreeAndAddsRuntimeRoot() throws Exception {
        File dir = Files.createTempDirectory("apkforge-renpy-bundle-test").toFile();
        File input = new File(dir, "windows-game.zip");
        File output = new File(dir, "game-data.zip");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(input))) {
                entry(zip, "Example.exe", "not runtime data");
                entry(zip, "Example/lib/python3.11/site-packages/readme.txt", "not runtime data");
                entry(zip, "Example/game/script.rpyc", "compiled script");
                entry(zip, "Example/game/archive.rpa", "packed assets");
                entry(zip, "Example/game/images/title.png", "image bytes");
            }

            RenPyGameDataBundle.write(input, "Example/game/", output);

            try (ZipFile bundle = new ZipFile(output)) {
                require(bundle.getEntry("game/script.rpyc") != null, "compiled scripts must be importable");
                require(bundle.getEntry("game/archive.rpa") != null, "RPA assets must be importable");
                require(bundle.getEntry("game/images/title.png") != null, "loose assets must be importable");
                require(bundle.getEntry("Example.exe") == null, "Windows executables must not go in the sidecar");
                require(bundle.getEntry("lib/python3.11/site-packages/readme.txt") == null, "Windows runtime files must not go in the sidecar");
            }
        } finally {
            input.delete();
            output.delete();
            dir.delete();
        }
    }

    private static void rejectsZipSlipBeforePublishingBundle() throws Exception {
        File dir = Files.createTempDirectory("apkforge-renpy-zip-slip-test").toFile();
        File input = new File(dir, "unsafe.zip");
        File output = new File(dir, "game-data.zip");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(input))) {
                entry(zip, "Example/game/../../outside.txt", "must never escape");
            }
            boolean rejected = false;
            try {
                RenPyGameDataBundle.write(input, "Example/game/", output);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            require(rejected, "unsafe game-data paths must be rejected");
            require(!output.exists() || output.length() == 0, "a rejected archive must not leave a usable bundle");
        } finally {
            input.delete();
            output.delete();
            dir.delete();
        }
    }

    private static void rejectsBundleWithoutRenPyContent() throws Exception {
        File dir = Files.createTempDirectory("apkforge-renpy-empty-bundle-test").toFile();
        File input = new File(dir, "not-a-game.zip");
        File output = new File(dir, "game-data.zip");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(input))) {
                entry(zip, "Example/game/readme.txt", "not enough to launch Ren'Py");
            }
            boolean rejected = false;
            try {
                RenPyGameDataBundle.write(input, "Example/game/", output);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            require(rejected, "a sidecar without scripts or RPA archives must fail");
            require(!output.exists() || output.length() == 0, "invalid sidecars must not leave a usable bundle");
        } finally {
            input.delete();
            output.delete();
            dir.delete();
        }
    }

    private static void entry(ZipOutputStream zip, String name, String text) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
