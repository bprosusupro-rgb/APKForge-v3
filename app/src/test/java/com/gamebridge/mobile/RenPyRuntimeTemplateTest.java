package com.gamebridge.mobile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Ensures the runtime APK packaged by APK Forge contains its large-game import launcher. */
public final class RenPyRuntimeTemplateTest {
    public static void main(String[] args) throws Exception {
        File template = new File("app/src/main/assets/renpy-template.apk");
        require(template.isFile(), "Ren'Py runtime template APK must exist");

        boolean manifestRegistersImporter = false;
        boolean dexContainsImporter = false;
        try (ZipFile apk = new ZipFile(template)) {
            ZipEntry manifest = apk.getEntry("AndroidManifest.xml");
            require(manifest != null, "runtime template manifest must exist");
            manifestRegistersImporter = contains(apk.getInputStream(manifest),
                    "com.apkforge.validation.LargeGameImportActivity");

            Enumeration<? extends ZipEntry> entries = apk.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().matches("classes(?:\\d+)?\\.dex")) {
                    dexContainsImporter |= contains(apk.getInputStream(entry),
                            "Lcom/apkforge/validation/LargeGameImportActivity;");
                }
            }
        }

        require(manifestRegistersImporter,
                "large Ren'Py runtime manifest must register its game-data import launcher");
        require(dexContainsImporter,
                "large Ren'Py runtime must include the game-data import activity in DEX");
        System.out.println("Ren'Py runtime template launcher tests passed");
    }

    private static boolean contains(InputStream input, String needle) throws Exception {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            byte[] data = out.toByteArray();
            return contains(data, needle.getBytes(StandardCharsets.UTF_8))
                    || contains(data, needle.getBytes(StandardCharsets.UTF_16LE));
        }
    }

    private static boolean contains(byte[] data, byte[] needle) {
        outer: for (int start = 0; start <= data.length - needle.length; start++) {
            for (int i = 0; i < needle.length; i++) {
                if (data[start + i] != needle[i]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
