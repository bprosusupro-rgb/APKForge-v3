package com.gamebridge.mobile;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Per-game private checkpoint store for HTML WebView localStorage snapshots. */
public final class LocalGameStateStore {
    private static final int MAX_STATE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_BACKUP_BYTES = MAX_STATE_BYTES + 128 * 1024;
    private static final String FORMAT_VERSION = "apkforge-local-save-v1";

    private LocalGameStateStore() { }

    public static File stateFile(File appFiles, String origin) throws Exception {
        String key = originKey(origin);
        File directory = new File(appFiles, "web-saves");
        return new File(directory, key + ".json");
    }

    public static void save(File appFiles, String origin, String json) throws Exception {
        if (json == null) throw new IllegalArgumentException("Save state is empty.");
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        if (data.length > MAX_STATE_BYTES) throw new IllegalArgumentException("This game's browser save is larger than the 8 MiB local-save limit.");
        File destination = stateFile(appFiles, origin);
        File directory = destination.getParentFile();
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Could not create private local-save storage.");
        File temporary = new File(directory, destination.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(data);
            output.getFD().sync();
        }
        if (!temporary.renameTo(destination)) {
            if (destination.exists() && !destination.delete()) {
                temporary.delete();
                throw new IllegalStateException("Could not replace the previous local save.");
            }
            if (!temporary.renameTo(destination)) {
                temporary.delete();
                throw new IllegalStateException("Could not commit the local save.");
            }
        }
    }

    public static String load(File appFiles, String origin) throws Exception {
        File file = stateFile(appFiles, origin);
        if (!file.isFile()) return null;
        long length = file.length();
        if (length > MAX_STATE_BYTES) throw new IllegalStateException("Saved browser state exceeds the 8 MiB local-save limit.");
        byte[] data = new byte[(int) length];
        try (java.io.FileInputStream input = new java.io.FileInputStream(file)) {
            int offset = 0;
            while (offset < data.length) {
                int count = input.read(data, offset, data.length - offset);
                if (count < 0) break;
                offset += count;
            }
            if (offset != data.length) throw new java.io.IOException("Local save changed while it was being read.");
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    /** Creates a portable, origin-bound backup without including a URL or game files. */
    public static byte[] exportBackup(File appFiles, String origin) throws Exception {
        String state = load(appFiles, origin);
        if (state == null) throw new IllegalStateException("This game has no local save to export yet.");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            put(zip, "format.txt", FORMAT_VERSION.getBytes(StandardCharsets.UTF_8));
            put(zip, "origin.sha256", originKey(origin).getBytes(StandardCharsets.UTF_8));
            put(zip, "state.json", state.getBytes(StandardCharsets.UTF_8));
        }
        byte[] backup = bytes.toByteArray();
        if (backup.length > MAX_BACKUP_BYTES) throw new IllegalStateException("Save backup exceeds the supported size.");
        return backup;
    }

    /** Restores only a well-formed backup made for the exact same game origin. */
    public static void restoreBackup(File appFiles, String origin, byte[] backup) throws Exception {
        if (backup == null || backup.length == 0 || backup.length > MAX_BACKUP_BYTES) {
            throw new IllegalArgumentException("The save backup is empty or too large.");
        }
        Map<String, byte[]> entries = new HashMap<>();
        int total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(backup))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !(name.equals("format.txt") || name.equals("origin.sha256") || name.equals("state.json"))
                        || entries.containsKey(name)) throw new IllegalArgumentException("The save backup has an unsupported or duplicate entry.");
                ByteArrayOutputStream content = new ByteArrayOutputStream();
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_STATE_BYTES + 256) throw new IllegalArgumentException("The save backup expands beyond the supported size.");
                    content.write(buffer, 0, count);
                }
                entries.put(name, content.toByteArray());
                zip.closeEntry();
            }
        } catch (IOException malformed) {
            throw new IllegalArgumentException("The save backup is damaged or is not an APK Forge save file.", malformed);
        }
        byte[] format = entries.get("format.txt");
        byte[] savedOrigin = entries.get("origin.sha256");
        byte[] state = entries.get("state.json");
        if (format == null || savedOrigin == null || state == null
                || !FORMAT_VERSION.equals(new String(format, StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("The save backup is incomplete or has an unsupported version.");
        }
        if (!originKey(origin).equals(new String(savedOrigin, StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("This save backup belongs to a different game page.");
        }
        String json = new String(state, StandardCharsets.UTF_8);
        String trimmed = json.trim();
        if (trimmed.isEmpty() || trimmed.charAt(0) != '{' || trimmed.charAt(trimmed.length() - 1) != '}') {
            throw new IllegalArgumentException("The save backup contains invalid browser save data.");
        }
        save(appFiles, origin, json);
    }

    private static void put(ZipOutputStream zip, String name, byte[] value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value);
        zip.closeEntry();
    }

    private static String originKey(String origin) throws Exception {
        if (origin == null || origin.trim().isEmpty() || "null".equals(origin)) return "offline-html";
        String candidate = origin.trim();
        if (candidate.toLowerCase(Locale.ROOT).startsWith("file:")) return "offline-html";
        URI uri = URI.create(candidate);
        String scheme = uri.getScheme();
        if (scheme == null) throw new IllegalArgumentException("The game save origin is invalid.");
        scheme = scheme.toLowerCase(Locale.ROOT);
        if ("file".equals(scheme)) return "offline-html";
        if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null) {
            throw new IllegalArgumentException("Only game-page origins can own a local save.");
        }
        String normalized = scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(64);
        for (byte value : digest) hex.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return hex.toString();
    }
}
