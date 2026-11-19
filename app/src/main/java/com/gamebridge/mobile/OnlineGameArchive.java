package com.gamebridge.mobile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builds a closed, minimal HTML archive for an online WebView game wrapper. */
final class OnlineGameArchive {
    private OnlineGameArchive() { }

    static File create(File directory, String title, String targetUrl) throws IOException {
        if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) {
            throw new IOException("Could not create the online game cache folder.");
        }
        String safeUrl = validateUrl(targetUrl);
        String safeTitle = escapeHtml(title == null || title.trim().isEmpty() ? "Web Game" : title.trim());
        String html = "<!doctype html><html><head><meta charset=utf-8>"
                + "<meta name=viewport content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"
                + "<meta name=referrer content=no-referrer><title>" + safeTitle + "</title></head>"
                + "<body style='margin:0;background:#15161c;color:#f7f4ff;font:16px sans-serif'>"
                + "<p>Opening online game…</p><a href=\"" + escapeHtml(safeUrl) + "\">"
                + "Tap here if the game does not open.</a><script>location.replace("
                + javascriptString(safeUrl) + ")</script></body></html>";

        File archive = File.createTempFile("online-html-", ".zip", directory);
        boolean complete = false;
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archive)))) {
            zip.putNextEntry(new ZipEntry("index.html"));
            zip.write(html.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            complete = true;
        } finally {
            if (!complete) archive.delete();
        }
        return archive;
    }

    private static String validateUrl(String value) throws IOException {
        if (value == null || value.length() > 8192) throw new IOException("The game URL is empty or too long.");
        final URI uri;
        try { uri = new URI(value); }
        catch (Exception invalid) { throw new IOException("The game URL is invalid.", invalid); }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.toLowerCase(Locale.ROOT).equals("http")
                || scheme.toLowerCase(Locale.ROOT).equals("https"))
                || uri.getHost() == null || uri.getRawUserInfo() != null) {
            throw new IOException("Only public HTTP(S) game pages can be wrapped.");
        }
        return uri.toASCIIString();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String javascriptString(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': result.append("\\\\"); break;
                case '"': result.append("\\\""); break;
                case '<': result.append("\\u003c"); break;
                case '>': result.append("\\u003e"); break;
                case '&': result.append("\\u0026"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\u2028': result.append("\\u2028"); break;
                case '\u2029': result.append("\\u2029"); break;
                default: result.append(c);
            }
        }
        return result.append('"').toString();
    }
}
