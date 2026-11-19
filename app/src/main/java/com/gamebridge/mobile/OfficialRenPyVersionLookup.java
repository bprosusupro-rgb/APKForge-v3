package com.gamebridge.mobile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the current Ren'Py release page only; never downloads or launches an EXE. */
final class OfficialRenPyVersionLookup {
    static final String LATEST_URL = "https://www.renpy.org/latest.html";
    private static final int MAX_HTML_BYTES = 1024 * 1024;
    private static final Pattern VERSION = Pattern.compile("(?i)Ren(?:'|&#39;|&#x27;)?Py\\s+([0-9][0-9A-Za-z.+-]{1,30})");

    static final class Result {
        final String version;
        final String pageUrl;
        Result(String version, String pageUrl) { this.version = version; this.pageUrl = pageUrl; }
    }

    private OfficialRenPyVersionLookup() { }

    static Result lookup() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_URL).openConnection();
        connection.setConnectTimeout(12_000);
        connection.setReadTimeout(12_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "APKForge/3.9.8 (RenPy release lookup)");
        try {
            connection.connect();
            URL finalUrl = connection.getURL();
            if (!"https".equalsIgnoreCase(finalUrl.getProtocol())
                    || !"www.renpy.org".equalsIgnoreCase(finalUrl.getHost())) {
                throw new SecurityException("The Ren'Py release lookup redirected away from its official site.");
            }
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("Official Ren'Py release page returned HTTP " + connection.getResponseCode() + ".");
            }
            byte[] html;
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > MAX_HTML_BYTES) throw new IllegalStateException("Official release page exceeded the one MiB response limit.");
                    output.write(buffer, 0, count);
                }
                html = output.toByteArray();
            }
            String version = parseVersion(new String(html, StandardCharsets.UTF_8));
            if (version == null) throw new IllegalStateException("Could not read a version from the official Ren'Py page.");
            return new Result(version, finalUrl.toString());
        } finally {
            connection.disconnect();
        }
    }

    static String parseVersion(String html) {
        if (html == null) return null;
        Matcher matcher = VERSION.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }
}
