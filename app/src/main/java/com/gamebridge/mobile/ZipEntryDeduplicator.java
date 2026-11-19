package com.gamebridge.mobile;

import java.util.HashSet;
import java.util.Set;

/** Tracks output paths so duplicate ZIP entries cannot abort APK assembly. */
final class ZipEntryDeduplicator {
    private final Set<String> seen = new HashSet<>();

    boolean claim(String path) {
        if (path == null || path.isEmpty()) return false;
        String normalized = path.replace('\\', '/');
        StringBuilder key = new StringBuilder(normalized.length());
        for (String part : normalized.split("/", -1)) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (key.length() > 0) key.append('/');
            key.append(part);
        }
        // ZIP names use forward slashes; collapse harmless separator and dot-segment
        // aliases before claiming so equivalent paths cannot collide downstream.
        return key.length() > 0 && seen.add(key.toString());
    }
}
