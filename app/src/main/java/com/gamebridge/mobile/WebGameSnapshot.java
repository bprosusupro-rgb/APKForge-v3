package com.gamebridge.mobile;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FilterInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class WebGameSnapshot {
    private static final int MAX_RESOURCES = 900;
    private static final long MAX_RESOURCE_BYTES = 1792L * 1024L * 1024L;
    // Snapshot input is streamed; the APK packager independently enforces its 1.9 GiB final-file cap.
    private static final long MAX_TOTAL_BYTES = 1792L * 1024L * 1024L;

    static final class CaptureResult {
        final int capturedResources;
        final int skippedResources;
        final long bytes;
        CaptureResult(int capturedResources, int skippedResources, long bytes) {
            this.capturedResources = capturedResources; this.skippedResources = skippedResources; this.bytes = bytes;
        }
    }

    private WebGameSnapshot() { }

    static CaptureResult capture(File output, String pageUrl, String title, String html, List<String> resourceUrls,
                                 String userAgent, Map<String, String> cookies) throws Exception {
        return capture(output, pageUrl, title, html, resourceUrls, userAgent, cookies, WebGameSnapshot::download);
    }

    interface ResourceFetcher {
        Downloaded fetch(String address, String referer, String userAgent, String cookie) throws Exception;
    }

    static CaptureResult capture(File output, String pageUrl, String title, String html, List<String> resourceUrls,
                                 String userAgent, Map<String, String> cookies, ResourceFetcher fetcher) throws Exception {
        if (html == null || html.isEmpty() || html.length() > MAX_RESOURCE_BYTES) throw new IllegalArgumentException("The current game page is empty or too large to capture.");
        String page = withoutFragment(pageUrl);
        String pageEntry = entryPathForUrl(page, true);
        Set<String> resources = new LinkedHashSet<>();
        int omitted = 0;
        if (resourceUrls != null) for (String value : resourceUrls) {
            if (value == null || value.isEmpty()) continue;
            String url = withoutFragment(value);
            try { entryPathForUrl(url, false); } catch (IllegalArgumentException invalid) { continue; }
            if (url.equals(page) || resources.contains(url)) continue;
            if (resources.size() >= MAX_RESOURCES) { omitted++; continue; }
            resources.add(url);
        }
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put(page, pageEntry);
        for (String resource : resources) paths.put(resource, entryPathForUrl(resource, false));

        int captured = 0, skipped = omitted;
        long total = html.getBytes(StandardCharsets.UTF_8).length;
        if (total > MAX_TOTAL_BYTES) throw new IllegalArgumentException("The page markup alone exceeds the offline capture size limit.");
        try (ZipOutputStream zip = new ZipOutputStream(new java.io.BufferedOutputStream(new FileOutputStream(output)))) {
            String savedHtml = rewriteReferences(html, pageEntry, page, paths);
            writeZip(zip, pageEntry, savedHtml.getBytes(StandardCharsets.UTF_8));
            for (String resource : resources) {
                if (total >= MAX_TOTAL_BYTES) { skipped++; continue; }
                Downloaded downloaded;
                try {
                    downloaded = fetcher.fetch(resource, page, userAgent, cookies == null ? null : cookies.get(resource));
                } catch (Exception failedResource) {
                    skipped++;
                    continue;
                }
                try (Downloaded source = downloaded) {
                    if (source.length > MAX_RESOURCE_BYTES || source.length >= 0 && total + source.length > MAX_TOTAL_BYTES) {
                        skipped++;
                        continue;
                    }
                    String contentType = source.contentType.toLowerCase(java.util.Locale.ROOT);
                    boolean rewriteText = contentType.contains("text/css") || contentType.contains("javascript")
                            || resource.matches("(?i).*\\.(?:css|m?js)(?:[?#].*)?$");
                    if (rewriteText) {
                        total += writeRewrittenZip(zip, paths.get(resource), source.stream, paths.get(resource), resource, paths,
                                MAX_TOTAL_BYTES - total);
                    } else {
                        long remaining = MAX_TOTAL_BYTES - total;
                        if (source.length > remaining) { skipped++; continue; }
                        long written = writeZip(zip, paths.get(resource), source.stream, remaining);
                        total += written;
                    }
                    captured++;
                }
            }
            String relativePage = relativePath("index.html", pageEntry);
            String safeTitle = title == null ? "Saved game" : title.replaceAll("[<>\"&]", " ").trim();
            String launcher = "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + safeTitle + "</title></head><body><p>Opening captured game…</p><a href=\"" + relativePage + "\">Open game</a><script>location.replace(\"" + relativePage + "\")</script></body></html>";
            writeZip(zip, "index.html", launcher.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            if (!output.delete()) output.deleteOnExit();
            throw failure;
        }
        return new CaptureResult(captured, skipped, total);
    }

    static final class Downloaded implements AutoCloseable {
        final InputStream stream;
        final String contentType;
        final long length;
        Downloaded(byte[] bytes, String contentType) { this(new ByteArrayInputStream(bytes), contentType, bytes.length); }
        Downloaded(InputStream stream, String contentType, long length) {
            this.stream = stream;
            this.contentType = contentType == null ? "" : contentType;
            this.length = length;
        }
        @Override public void close() throws IOException { stream.close(); }
    }

    private static Downloaded download(String address, String referer, String userAgent, String cookie) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(10000); connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", userAgent == null || userAgent.isEmpty() ? "APKForge/1.0" : userAgent);
        connection.setRequestProperty("Referer", referer);
        if (cookie != null && !cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) { connection.disconnect(); throw new IllegalStateException("HTTP " + status); }
            long length = connection.getContentLengthLong();
            if (length > MAX_RESOURCE_BYTES) { connection.disconnect(); throw new IllegalArgumentException("resource exceeds per-file limit"); }
            InputStream response = new BufferedInputStream(connection.getInputStream());
            InputStream managed = new FilterInputStream(response) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { connection.disconnect(); }
                }
            };
            return new Downloaded(managed, connection.getContentType(), length);
        } catch (Exception failure) {
            connection.disconnect();
            throw failure;
        }
    }

    private static String withoutFragment(String value) {
        if (value == null) throw new IllegalArgumentException("Invalid web resource URL.");
        int fragment = value.indexOf('#');
        return fragment < 0 ? value : value.substring(0, fragment);
    }

    private static void writeZip(ZipOutputStream zip, String path, byte[] bytes) throws Exception {
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry); zip.write(bytes); zip.closeEntry();
    }

    private static long writeZip(ZipOutputStream zip, String path, InputStream input, long maximum) throws Exception {
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        byte[] buffer = new byte[65536];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (read > maximum - total) throw new IOException("Downloaded resources exceeded the offline snapshot size limit.");
            zip.write(buffer, 0, read);
            total += read;
        }
        zip.closeEntry();
        return total;
    }

    private static final class TextNode {
        final Map<Character, TextNode> next = new LinkedHashMap<>();
        String replacement;
    }

    private static final class BoundedOutputStream extends OutputStream {
        private final OutputStream output;
        private final long maximum;
        private long count;
        BoundedOutputStream(OutputStream output, long maximum) { this.output = output; this.maximum = maximum; }
        private void check(int size) throws IOException {
            if (size > maximum - count) throw new IOException("Rewritten resources exceeded the offline snapshot size limit.");
        }
        @Override public void write(int value) throws IOException { check(1); output.write(value); count++; }
        @Override public void write(byte[] bytes, int offset, int length) throws IOException {
            check(length); output.write(bytes, offset, length); count += length;
        }
    }

    private static long writeRewrittenZip(ZipOutputStream zip, String path, InputStream input,
                                          String sourceFile, String sourceUrl, Map<String, String> localPaths,
                                          long maximum) throws Exception {
        Map<String, String> replacements = replacementPaths(sourceFile, sourceUrl, localPaths);
        TextNode root = new TextNode();
        int longest = 1;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            String key = replacement.getKey();
            if (key.isEmpty()) continue;
            longest = Math.max(longest, key.length());
            TextNode node = root;
            for (int i = 0; i < key.length(); i++) {
                char character = key.charAt(i);
                TextNode next = node.next.get(character);
                if (next == null) { next = new TextNode(); node.next.put(character, next); }
                node = next;
            }
            node.replacement = replacement.getValue();
        }

        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        BoundedOutputStream bounded = new BoundedOutputStream(zip, maximum);
        OutputStreamWriter writer = new OutputStreamWriter(bounded, StandardCharsets.UTF_8);
        try (PushbackReader reader = new PushbackReader(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)), longest + 1)) {
            StringBuilder pending = new StringBuilder(longest + 1);
            int first;
            while ((first = reader.read()) != -1) {
                TextNode node = root.next.get((char) first);
                if (node == null) { writer.write(first); continue; }
                pending.setLength(0);
                pending.append((char) first);
                String bestReplacement = node.replacement;
                int bestLength = bestReplacement == null ? 0 : 1;
                while (!node.next.isEmpty()) {
                    int nextCharacter = reader.read();
                    if (nextCharacter == -1) break;
                    pending.append((char) nextCharacter);
                    TextNode next = node.next.get((char) nextCharacter);
                    if (next == null) break;
                    node = next;
                    if (node.replacement != null) {
                        bestReplacement = node.replacement;
                        bestLength = pending.length();
                    }
                }
                if (bestLength > 0) {
                    writer.write(bestReplacement);
                    if (pending.length() > bestLength) {
                        char[] rest = pending.substring(bestLength).toCharArray();
                        reader.unread(rest);
                    }
                } else {
                    writer.write(pending.charAt(0));
                    if (pending.length() > 1) {
                        char[] rest = pending.substring(1).toCharArray();
                        reader.unread(rest);
                    }
                }
            }
            writer.flush();
        }
        zip.closeEntry();
        return bounded.count;
    }

    private static Map<String, String> replacementPaths(String sourceFile, String sourceUrl, Map<String, String> localPaths) {
        Map<String, String> replacements = new LinkedHashMap<>();
        List<String> urls = new ArrayList<>(localPaths.keySet());
        urls.sort(Comparator.comparingInt(String::length).reversed());
        for (String url : urls) {
            String relative = relativePath(sourceFile, localPaths.get(url));
            replacements.put(url, relative);
            replacements.put(url.replace("&", "&amp;"), relative);
            if (sourceUrl != null) {
                try {
                    URI source = new URI(sourceUrl), target = new URI(url);
                    if (sameOrigin(source, target)) {
                        String rooted = target.getRawPath() + (target.getRawQuery() == null ? "" : "?" + target.getRawQuery());
                        // A root path is a single slash. Replacing it globally corrupts every
                        // HTML closing tag ("</tag>") in the captured document.
                        if (rooted.startsWith("/") && !rooted.equals("/")) replacements.put(rooted, relative);
                    }
                } catch (Exception ignored) { }
            }
        }
        return replacements;
    }

    static String entryPathForUrl(String value, boolean page) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null || uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException("Only public http(s) web resources can be saved in an offline game.");
            }
            String rawPath = uri.getRawPath();
            if (rawPath == null || rawPath.isEmpty()) rawPath = "/";
            rawPath = rawPath.replaceAll("(?i)%2e", ".").replaceAll("(?i)%2f", "/");
            List<String> segments = new ArrayList<>();
            for (String segment : rawPath.split("/")) {
                if (segment.isEmpty() || segment.equals(".")) continue;
                String decoded = segment;
                for (int i = 0; i < 3; i++) {
                    String next = decodePathSegment(decoded);
                    if (next.equals(decoded)) break;
                    decoded = next;
                }
                if (decoded.equals("..") || decoded.equals(".")) throw new IllegalArgumentException("A web resource URL attempted to leave its site path.");
                if (decoded.indexOf('/') >= 0 || decoded.indexOf('\\') >= 0 || decoded.indexOf('\u0000') >= 0) throw new IllegalArgumentException("Unsafe path in web resource URL.");
                segments.add(segment);
            }
            if (segments.isEmpty() || rawPath.endsWith("/")) segments.add(page ? "index.html" : "resource.bin");
            String last = segments.get(segments.size() - 1);
            if (page && !last.matches("(?i).*\\.html?$")) segments.set(segments.size() - 1, last + ".html");
            else if (!page && !last.contains(".")) segments.set(segments.size() - 1, last + ".bin");
            if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
                int index = segments.size() - 1;
                String filename = segments.get(index);
                int dot = filename.lastIndexOf('.');
                segments.set(index, (dot > 0 ? filename.substring(0, dot) : filename) + "--" + shortHash(uri.getRawQuery()) + (dot > 0 ? filename.substring(dot) : ""));
            }
            StringBuilder path = new StringBuilder("_snapshot/").append(scheme).append('/').append(uri.getHost().toLowerCase(java.util.Locale.ROOT));
            if (uri.getPort() >= 0) path.append('-').append(uri.getPort());
            for (String segment : segments) path.append('/').append(segment);
            return path.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid web resource URL.", e);
        }
    }

    static String relativePath(String fromFile, String toFile) {
        String[] from = fromFile.split("/"), to = toFile.split("/");
        int fromDirectories = from.length - 1, common = 0;
        while (common < fromDirectories && common < to.length && from[common].equals(to[common])) common++;
        StringBuilder relative = new StringBuilder();
        for (int i = common; i < fromDirectories; i++) relative.append("../");
        for (int i = common; i < to.length; i++) {
            if (relative.length() > 0 && relative.charAt(relative.length() - 1) != '/') relative.append('/');
            relative.append(to[i]);
        }
        return relative.length() == 0 ? "./" + to[to.length - 1] : relative.toString();
    }

    static String rewriteReferences(String content, String sourceFile, Map<String, String> localPaths) {
        return rewriteReferences(content, sourceFile, null, localPaths);
    }

    static String rewriteReferences(String content, String sourceFile, String sourceUrl, Map<String, String> localPaths) {
        String result = content;
        for (Map.Entry<String, String> replacement : replacementPaths(sourceFile, sourceUrl, localPaths).entrySet()) {
            result = result.replace(replacement.getKey(), replacement.getValue());
        }
        return result;
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme() != null && left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost() != null && left.getHost().equalsIgnoreCase(right.getHost()) && left.getPort() == right.getPort();
    }

    private static String shortHash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 4; i++) text.append(String.format(java.util.Locale.ROOT, "%02x", bytes[i] & 0xff));
            return text.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private static String decodePathSegment(String value) {
        try { return URLDecoder.decode(value.replace("+", "%2B"), "UTF-8"); }
        catch (java.io.UnsupportedEncodingException e) { throw new IllegalStateException("UTF-8 is unavailable.", e); }
    }
}
