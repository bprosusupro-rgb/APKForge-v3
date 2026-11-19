package com.gamebridge.mobile;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;

/** Regression coverage for creating a closed, well-formed online-wrapper archive. */
public final class OnlineGameArchiveTest {
    private static void truth(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Class<?> archiveBuilder;
        try {
            archiveBuilder = Class.forName("com.gamebridge.mobile.OnlineGameArchive");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("online wrapper archive creation needs a testable builder", missing);
        }

        Method create = archiveBuilder.getDeclaredMethod("create", File.class, String.class, String.class);
        create.setAccessible(true);
        File directory = java.nio.file.Files.createTempDirectory("apkforge-online-test-").toFile();
        File archive = null;
        try {
            archive = (File) create.invoke(null, directory, "A < Game", "https://example.test/play?a=1&b=2");
            truth(archive != null && archive.isFile(), "online wrapper archive should exist after create returns");
            try (ZipFile zip = new ZipFile(archive)) {
                java.util.zip.ZipEntry page = zip.getEntry("index.html");
                truth(page != null, "online wrapper archive must include index.html");
                String html;
                try (java.io.InputStream input = zip.getInputStream(page)) {
                    html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
                truth(html.contains("https://example.test/play?a=1&amp;b=2"), "wrapper must preserve the target URL in its fallback link");
                truth(html.contains("A &lt; Game"), "wrapper title must be HTML-escaped");
                truth(html.contains("location.replace("), "wrapper must redirect into the game page");
            }

            boolean rejected = false;
            try {
                create.invoke(null, directory, "Bad URL", "javascript:alert(1)");
            } catch (InvocationTargetException expected) {
                rejected = expected.getCause() instanceof java.io.IOException;
            }
            truth(rejected, "only valid HTTP(S) targets may be wrapped");
            System.out.println("OnlineGameArchive tests passed");
        } finally {
            if (archive != null) archive.delete();
            java.io.File[] files = directory.listFiles();
            if (files != null) for (java.io.File file : files) file.delete();
            directory.delete();
        }
    }
}
