package com.gamebridge.mobile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Builds a separate, streamable Ren'Py game-data archive for APKs too large to embed. */
final class RenPyGameDataBundle {
    private static final long MAX_EXPANDED_GAME_BYTES = 6L * 1024L * 1024L * 1024L;
    private static final int MAX_GAME_FILES = 150_000;
    private static final int BUFFER_SIZE = 64 * 1024;

    private RenPyGameDataBundle() { }

    /** Writes game files beneath {@code game/}; leaves the destination untouched on failure. */
    static void write(File inputArchive, String gameDirectory, File outputArchive) throws Exception {
        if (inputArchive == null || !inputArchive.isFile()) {
            throw new IllegalArgumentException("The selected Ren'Py game archive is unavailable.");
        }
        if (outputArchive == null || outputArchive.exists()) {
            throw new IllegalArgumentException("Choose a new destination for the game-data bundle.");
        }
        String root = normalizeRoot(gameDirectory);
        File parent = outputArchive.getAbsoluteFile().getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IllegalStateException("Could not create the game-data output folder.");
        }

        File temporary = new File(parent, outputArchive.getName() + ".partial-" + UUID.randomUUID());
        boolean published = false;
        try {
            long total = 0;
            int count = 0;
            boolean playableData = false;
            Set<String> paths = new HashSet<>();
            byte[] buffer = new byte[BUFFER_SIZE];
            try (ZipFile source = new ZipFile(inputArchive);
                 FileOutputStream fileOut = new FileOutputStream(temporary);
                 BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
                 ZipOutputStream bundle = new ZipOutputStream(bufferedOut)) {
                Enumeration<? extends ZipEntry> entries = source.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String sourcePath = normalizePath(entry.getName());
                    if (entry.isDirectory() || !sourcePath.startsWith(root)) continue;

                    String relative = sourcePath.substring(root.length());
                    if (relative.isEmpty()) continue;
                    String lower = relative.toLowerCase(java.util.Locale.ROOT);
                    if (lower.endsWith(".rpa") || lower.endsWith(".rpyc") || lower.endsWith(".rpy")) playableData = true;
                    String target = "game/" + relative;
                    if (!paths.add(target)) {
                        throw new IllegalArgumentException("The game archive contains duplicate game-data paths.");
                    }
                    if (++count > MAX_GAME_FILES) {
                        throw new IllegalArgumentException("The Ren'Py game contains more than 150,000 files.");
                    }
                    long size = entry.getSize();
                    if (size < 0 || size > MAX_EXPANDED_GAME_BYTES - total) {
                        throw new IllegalArgumentException("The Ren'Py game data exceeds the 6 GiB import-processing limit.");
                    }
                    total += size;

                    // The source ZIP already records size and CRC. STORE avoids re-compressing
                    // multi-gigabyte RPA/media payloads while still validating them on read.
                    ZipEntry output = new ZipEntry(target);
                    output.setMethod(ZipEntry.STORED);
                    output.setSize(size);
                    output.setCompressedSize(size);
                    output.setCrc(entry.getCrc());
                    if (entry.getTime() > 0) output.setTime(entry.getTime());
                    bundle.putNextEntry(output);
                    long copied = 0;
                    try (InputStream in = new BufferedInputStream(source.getInputStream(entry))) {
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            copied += read;
                            if (copied > size) {
                                throw new IllegalArgumentException("A game-data entry expanded beyond its declared ZIP size.");
                            }
                            bundle.write(buffer, 0, read);
                        }
                    }
                    if (copied != size) {
                        throw new IllegalArgumentException("A game-data entry did not match its declared ZIP size.");
                    }
                    bundle.closeEntry();
                }
            }
            if (count == 0 || !playableData) {
                throw new IllegalArgumentException("No Ren'Py scripts or RPA archives were found under the game's game/ folder.");
            }
            Files.move(temporary.toPath(), outputArchive.toPath(), StandardCopyOption.ATOMIC_MOVE);
            published = true;
        } finally {
            if (!published) Files.deleteIfExists(temporary.toPath());
        }
    }

    private static String normalizeRoot(String value) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("The Ren'Py game folder was not identified.");
        String root = normalizePath(value);
        if (!root.endsWith("/")) root += "/";
        return root;
    }

    private static String normalizePath(String value) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("The game archive contains an empty path.");
        String path = value.replace('\\', '/');
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") || path.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("The game archive contains an absolute or invalid path.");
        }
        for (String segment : path.split("/", -1)) {
            if (segment.equals("..") || segment.equals(".")) {
                throw new IllegalArgumentException("The game archive contains an unsafe path.");
            }
        }
        return path;
    }
}
