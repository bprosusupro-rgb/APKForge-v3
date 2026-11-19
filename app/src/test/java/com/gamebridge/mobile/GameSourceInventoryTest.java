package com.gamebridge.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class GameSourceInventoryTest {
    public static void main(String[] args) throws Exception {
        File archive = File.createTempFile("apkforge-inventory", ".zip");
        try {
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(archive))) {
                add(out, "Novel/game/script.rpyc", 80);
                add(out, "Novel/game/script.rpy", 80);
                add(out, "Novel/game/data.rpa", 400);
                add(out, "Novel/game/images/scene.webp", 240);
                add(out, "Novel/game/audio/theme.ogg", 300);
                add(out, "Novel/game/.cache/ignore.bin", 16);
                add(out, "Novel/runtime.exe", 120);
            }
            GameSourceInventory.Result result = GameSourceInventory.scan(archive, 50_000);
            check(result.isRenPy, "detect compiled Ren'Py game from game directory");
            check(result.totalFiles == 7 && result.includedFiles == 5 && result.excludedFiles == 2,
                    "account for every file and intentional exclusions");
            check(result.scriptFiles == 2 && result.archiveFiles == 1 && result.imageFiles == 1
                    && result.audioVideoFiles == 1, "classify Ren'Py game-data types");
            check(result.includedBytes == 1100, "sum only files the APK packager includes");
            check(result.estimatedApkBytes() == 51_100, "include the embedded Ren'Py runtime in output estimate");
            check(ConversionEta.estimateSeconds(0) >= 1, "empty work still has a nonzero startup estimate");
            check(ConversionEta.estimateSeconds(4L * 1024 * 1024 * 1024) > ConversionEta.estimateSeconds(10L * 1024 * 1024),
                    "larger projects receive a longer ETA");
            check(ConversionEta.estimateSeconds(100L * 1024 * 1024, 20L * 1024 * 1024)
                    < ConversionEta.estimateSeconds(100L * 1024 * 1024), "learned device throughput improves later estimates");
            check(ConversionEta.estimateSeconds(100L * 1024 * 1024, 1) <= 60,
                    "tiny-game timing samples cannot make a medium game's ETA explode");
            check(ConversionEta.updatedRate(10L * 1024 * 1024, 30L * 1024 * 1024) == 18L * 1024 * 1024,
                    "blend measured throughput to avoid unstable ETA jumps");
            check(ConversionEta.updatedRate(0, 1) == 2L * 1024 * 1024,
                    "clamp implausibly low first-run rates to a useful floor");
            check(ConversionEta.updatedRate(0, 1024L * 1024 * 1024) == 64L * 1024 * 1024,
                    "clamp implausibly high first-run rates to a safe ceiling");
            check(ConversionEta.format(3661).contains("1h") && ConversionEta.format(3661).contains("1m"),
                    "format long estimates as hours and minutes");
        } finally { archive.delete(); }
        File html = File.createTempFile("apkforge-html-inventory", ".zip");
        try {
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(html))) {
                add(out, "web/index.html", 128);
                add(out, "web/assets/game.js", 256);
                add(out, "web/assets/scene.png", 512);
            }
            GameSourceInventory.Result result = GameSourceInventory.scan(html, 50_000);
            check(!result.isRenPy && result.totalFiles == 3 && result.includedFiles == 3,
                    "inventory every file from an HTML game root");
            check(result.webFiles == 1 && result.scriptFiles == 1 && result.imageFiles == 1,
                    "classify HTML, JavaScript, and image data");
            check(result.workBytes(html.length()) >= result.includedBytes,
                    "ETA workload includes the archive and packaged content");
        } finally { html.delete(); }
        System.out.println("GameSourceInventoryTest passed");
    }
    private static void add(ZipOutputStream out, String path, int size) throws Exception {
        byte[] bytes = new byte[size];
        java.util.Arrays.fill(bytes, (byte)'x');
        out.putNextEntry(new ZipEntry(path)); out.write(bytes); out.closeEntry();
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
