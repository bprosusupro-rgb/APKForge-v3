package com.gamebridge.mobile;

import java.util.Collections;

public final class WebGameSnapshotTest {
    private static final class RepeatingInputStream extends java.io.InputStream {
        private long remaining;
        RepeatingInputStream(long bytes) { remaining = bytes; }
        @Override public int read() {
            if (remaining == 0) return -1;
            remaining--;
            return 0;
        }
        @Override public int read(byte[] buffer, int offset, int length) {
            if (remaining == 0) return -1;
            int count = (int) Math.min(remaining, length);
            java.util.Arrays.fill(buffer, offset, offset + count, (byte) 0);
            remaining -= count;
            return count;
        }
    }
    private static final class RepeatingPatternInputStream extends java.io.InputStream {
        private final byte[] pattern;
        private long remaining;
        private long position;
        RepeatingPatternInputStream(byte[] pattern, long repetitions) {
            this.pattern = pattern;
            remaining = (long) pattern.length * repetitions;
        }
        @Override public int read() {
            if (remaining == 0) return -1;
            int value = pattern[(int) (position++ % pattern.length)] & 0xff;
            remaining--;
            return value;
        }
        @Override public int read(byte[] buffer, int offset, int length) {
            if (remaining == 0) return -1;
            int count = (int) Math.min(remaining, length);
            for (int i = 0; i < count; i++) buffer[offset + i] = pattern[(int) ((position + i) % pattern.length)];
            position += count;
            remaining -= count;
            return count;
        }
    }
    private static void equal(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
    }

    private static void rejected(String url) {
        boolean failed = false;
        try { WebGameSnapshot.entryPathForUrl(url, false); }
        catch (IllegalArgumentException expected) { failed = true; }
        if (!failed) throw new AssertionError("unsafe/non-web URL must not be captured: " + url);
    }

    private static void captureLocalSite() throws Exception {
        java.io.File archive = java.io.File.createTempFile("snapshot-test-", ".zip");
        try {
            String base = "http://example.test";
            String html = "<html><script src=\"" + base + "/game.js\"></script></html>";
            WebGameSnapshot.CaptureResult result = WebGameSnapshot.capture(archive, base + "/index.html", "Fixture", html, java.util.Collections.singletonList(base + "/game.js"), "SnapshotTest/1", Collections.emptyMap(), (url, referer, userAgent, cookie) -> new WebGameSnapshot.Downloaded("window.gameReady=true;".getBytes(java.nio.charset.StandardCharsets.UTF_8), "application/javascript"));
            if (result.capturedResources != 1) throw new AssertionError("expected the real local JS asset to be captured");
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive)) {
                if (zip.getEntry("index.html") == null) throw new AssertionError("offline launcher entry is missing");
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
                boolean scriptFound = false;
                while (entries.hasMoreElements()) { java.util.zip.ZipEntry entry = entries.nextElement(); if (entry.getName().endsWith("/game.js")) { scriptFound = true; try (java.io.InputStream in = zip.getInputStream(entry)) { equal("window.gameReady=true;", new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)); } } }
                if (!scriptFound) throw new AssertionError("captured game script is missing from the archive");
                try (java.io.InputStream in = zip.getInputStream(zip.getEntry("_snapshot/http/example.test/index.html"))) {
                    String savedHtml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (!savedHtml.contains("game.js")) throw new AssertionError("captured page did not link to its local asset");
                    if (savedHtml.contains(base + "/game.js")) throw new AssertionError("captured page still links to the remote game script");
                }
            }
        } finally { archive.delete(); }
    }

    private static void captureRootPagePreservesMarkup() throws Exception {
        java.io.File archive = java.io.File.createTempFile("snapshot-root-", ".zip");
        try {
            String base = "https://example.test/";
            String html = "<!doctype html><html><head><title>Root page</title></head>"
                    + "<body><p>Playable page</p><a href=\"/\">Home</a></body></html>";
            WebGameSnapshot.capture(archive, base, "Root page", html, Collections.emptyList(),
                    "SnapshotTest/1", Collections.emptyMap(), (url, referer, userAgent, cookie) -> {
                        throw new AssertionError("a root page with no resources should not fetch anything");
                    });
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive)) {
                java.util.zip.ZipEntry page = zip.getEntry("_snapshot/https/example.test/index.html");
                if (page == null) throw new AssertionError("captured root page is missing");
                try (java.io.InputStream in = zip.getInputStream(page)) {
                    String saved = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (!saved.contains("</title></head>")) throw new AssertionError("root URL rewriting corrupted closing tags: " + saved);
                    if (!saved.contains("</body></html>")) throw new AssertionError("root URL rewriting corrupted document endings: " + saved);
                    if (!saved.contains("href=\"/\"")) throw new AssertionError("a slash must not be substituted outside a URL token");
                }
            }
        } finally { archive.delete(); }
    }

    private static void captureMoreThanTheOldResourceLimit() throws Exception {
        java.io.File archive = java.io.File.createTempFile("snapshot-many-", ".zip");
        try {
            java.util.List<String> resources = new java.util.ArrayList<>();
            for (int i = 0; i < 305; i++) resources.add("https://cdn.example/assets/" + i + ".bin");
            WebGameSnapshot.CaptureResult result = WebGameSnapshot.capture(
                    archive, "https://game.example/index.html", "Many assets", "<html>ok</html>", resources,
                    "SnapshotTest/1", Collections.emptyMap(), (url, referer, userAgent, cookie) ->
                            new WebGameSnapshot.Downloaded(new byte[] {1, 2, 3}, "application/octet-stream"));
            if (result.capturedResources != 305) {
                throw new AssertionError("all 305 requested assets should be captured, got " + result.capturedResources);
            }
        } finally { archive.delete(); }
    }

    private static void reportResourcesBeyondCaptureLimit() throws Exception {
        java.io.File archive = java.io.File.createTempFile("snapshot-over-limit-", ".zip");
        try {
            java.util.List<String> resources = new java.util.ArrayList<>();
            for (int i = 0; i < 905; i++) resources.add("https://cdn.example/assets/" + i + ".bin");
            WebGameSnapshot.CaptureResult result = WebGameSnapshot.capture(
                    archive, "https://game.example/index.html", "Over limit", "<html>ok</html>", resources,
                    "SnapshotTest/1", Collections.emptyMap(), (url, referer, userAgent, cookie) ->
                            new WebGameSnapshot.Downloaded(new byte[] {1}, "application/octet-stream"));
            if (result.capturedResources != 900 || result.skippedResources != 5) {
                throw new AssertionError("all resources beyond the 900-file cap must be reported; captured="
                        + result.capturedResources + ", skipped=" + result.skippedResources);
            }
        } finally { archive.delete(); }
    }

    private static void offlineCaptureAllowsLargeGames() throws Exception {
        java.lang.reflect.Field limit = WebGameSnapshot.class.getDeclaredField("MAX_TOTAL_BYTES");
        limit.setAccessible(true);
        long bytes = limit.getLong(null);
        if (bytes <= 1024L * 1024L * 1024L) {
            throw new AssertionError("offline capture must exceed its old 1 GiB total-resource limit");
        }
        if (bytes != 1792L * 1024L * 1024L) {
            throw new AssertionError("offline capture must use the increased 1.75 GiB streaming budget; got " + bytes);
        }
    }

    private static void captureStreamsPastOldLimitWithoutBuffering() throws Exception {
        java.io.File archive = java.io.File.createTempFile("snapshot-large-stream-", ".zip");
        long assetBytes = 1600L * 1024L * 1024L;
        try {
            WebGameSnapshot.CaptureResult result = WebGameSnapshot.capture(
                    archive, "https://game.example/index.html", "Large stream", "<html>ok</html>",
                    Collections.singletonList("https://cdn.example/large.bin"), "SnapshotTest/1", Collections.emptyMap(),
                    (url, referer, userAgent, cookie) -> new WebGameSnapshot.Downloaded(
                            new RepeatingInputStream(assetBytes), "application/octet-stream", assetBytes));
            if (result.capturedResources != 1 || result.skippedResources != 0) {
                throw new AssertionError("large streamed resource must be captured, not skipped");
            }
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive)) {
                java.util.zip.ZipEntry asset = zip.getEntry("_snapshot/https/cdn.example/large.bin");
                if (asset == null || asset.getSize() != assetBytes) {
                    throw new AssertionError("captured ZIP entry must retain the complete 1.6 GiB resource");
                }
            }
        } finally { archive.delete(); }
    }

    private static void captureRewritesLargeScriptsAsAStream() throws Exception {
        java.io.File archive = java.io.File.createTempFile("snapshot-large-script-", ".zip");
        byte[] pattern = "const asset='https://cdn.example/assets/icon.png';\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long scriptBytes = (long) pattern.length * 400_000;
        try {
            WebGameSnapshot.CaptureResult result = WebGameSnapshot.capture(
                    archive, "https://game.example/index.html", "Large script",
                    "<script src=\"https://cdn.example/game.js\"></script>",
                    java.util.Arrays.asList("https://cdn.example/game.js", "https://cdn.example/assets/icon.png"),
                    "SnapshotTest/1", Collections.emptyMap(), (url, referer, userAgent, cookie) -> {
                        if (url.endsWith("icon.png")) return new WebGameSnapshot.Downloaded(new byte[]{1}, "image/png");
                        return new WebGameSnapshot.Downloaded(new RepeatingPatternInputStream(pattern, 400_000),
                                "application/javascript", scriptBytes);
                    });
            if (result.capturedResources != 2 || result.skippedResources != 0) {
                throw new AssertionError("large JavaScript and its image must both be captured");
            }
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive)) {
                java.util.zip.ZipEntry script = zip.getEntry("_snapshot/https/cdn.example/game.js");
                if (script == null || script.getSize() >= scriptBytes) {
                    throw new AssertionError("large script should be complete and have its remote image URL rewritten");
                }
                try (java.io.InputStream in = zip.getInputStream(script)) {
                    byte[] prefix = in.readNBytes(120);
                    String text = new String(prefix, java.nio.charset.StandardCharsets.UTF_8);
                    if (!text.contains("assets/icon.png") || text.contains("https://cdn.example/assets/icon.png")) {
                        throw new AssertionError("large streamed script must point at the captured local image");
                    }
                }
            }
        } finally { archive.delete(); }
    }

    public static void main(String[] args) {
        equal("_snapshot/https/example.com/games/play.html", WebGameSnapshot.entryPathForUrl("https://example.com/games/play", true));
        equal("../../cdn.example/assets/game.js", WebGameSnapshot.relativePath("_snapshot/https/example.com/games/index.html", "_snapshot/https/cdn.example/assets/game.js"));
        equal("<script src=\"../../cdn.example/game.js\"></script>", WebGameSnapshot.rewriteReferences("<script src=\"https://cdn.example/game.js\"></script>", "_snapshot/https/example.com/game/index.html", Collections.singletonMap("https://cdn.example/game.js", "_snapshot/https/cdn.example/game.js")));
        rejected("javascript:alert(1)");
        rejected("https://example.com/../../outside.js");
        rejected("https://example.com/a/%252e%252e/secret.js");
        try {
            captureLocalSite();
            captureRootPagePreservesMarkup();
            captureMoreThanTheOldResourceLimit();
            reportResourcesBeyondCaptureLimit();
            offlineCaptureAllowsLargeGames();
            captureStreamsPastOldLimitWithoutBuffering();
            captureRewritesLargeScriptsAsAStream();
        } catch (Exception e) { throw new AssertionError("offline capture integration failed", e); }
        System.out.println("WebGameSnapshot tests passed");
    }
}
