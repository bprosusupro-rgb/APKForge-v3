package com.gamebridge.mobile;

import java.io.File;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Fast, non-extracting preflight for Ren'Py games that need separate local game data. */
public final class RenPyLargeGameAnalyzer {
    public static final long APK_FORGE_LIMIT_BYTES = 1900L * 1024 * 1024;
    public static final long MAX_EMBEDDED_ESTIMATE_BYTES = 1600L * 1024 * 1024;
    private RenPyLargeGameAnalyzer() { }

    public static final class Result {
        public final boolean isRenPy, hasUpdateMetadata, hasUpdateChunk, hasPublicKey;
        public final long gameCompressedBytes, estimatedApkBytes;
        public final int archiveCount;
        Result(boolean renpy, boolean metadata, boolean chunk, boolean key, long bytes, long estimate, int count) {
            isRenPy=renpy; hasUpdateMetadata=metadata; hasUpdateChunk=chunk; hasPublicKey=key;
            gameCompressedBytes=bytes; estimatedApkBytes=estimate; archiveCount=count;
        }
        public boolean hasOfficialUpdateKit() { return hasUpdateMetadata && hasUpdateChunk && hasPublicKey; }
    }

    public static boolean requiresExternalGameData(long estimatedApkBytes) {
        return estimatedApkBytes > MAX_EMBEDDED_ESTIMATE_BYTES;
    }

    public static Result analyze(File archive, long runtimeBytes) throws Exception {
        boolean renpy=false, metadata=false, chunk=false, key=false;
        long compressed=0; int archives=0;
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries=zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry=entries.nextElement();
                if (entry.isDirectory()) continue;
                String path=entry.getName().replace('\\','/').toLowerCase(Locale.ROOT);
                int game=path.indexOf("game/");
                if (game < 0) continue;
                String relative=path.substring(game+5);
                if (relative.endsWith(".rpyc") || relative.endsWith(".rpy")) renpy=true;
                if (relative.endsWith(".rpa")) archives++;
                if (relative.equals("updates.json")) metadata=true;
                if (relative.equals("updates.ecdsa") || relative.matches(".*\\.rpu(?:/.*)?$")) chunk=true;
                if (relative.equals("public_key.pem")) key=true;
                long size=entry.getCompressedSize() >= 0 ? entry.getCompressedSize() : entry.getSize();
                if (size > 0 && Long.MAX_VALUE-compressed >= size) compressed+=size;
            }
        }
        long estimate = compressed > Long.MAX_VALUE-runtimeBytes ? Long.MAX_VALUE : compressed+runtimeBytes;
        return new Result(renpy,metadata,chunk,key,compressed,estimate,archives);
    }
}
