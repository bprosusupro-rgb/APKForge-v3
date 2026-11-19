package com.gamebridge.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.CRC32;

/** Self-running regression test: javac/java ...RenPyLargeGameAnalyzerTest. */
public final class RenPyLargeGameAnalyzerTest {
    public static void main(String[] args) throws Exception {
        File zip = File.createTempFile("renpy-large-analysis", ".zip");
        try {
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                add(out, "game/script.rpyc", 64);
                add(out, "game/art/game.rpa", 4096);
                add(out, "game/updates.json", 32);
                add(out, "game/updates.ecdsa", 32);
                add(out, "game/public_key.pem", 32);
                add(out, "game/rpu/update-1.rpu", 32);
            }
            RenPyLargeGameAnalyzer.Result result = RenPyLargeGameAnalyzer.analyze(zip, 100L * 1024 * 1024);
            check(result.isRenPy, "detect Ren'Py from game script");
        check(RenPyLargeGameAnalyzer.requiresExternalGameData(2_000L * 1024 * 1024), "large package uses separate game data");
        check(!RenPyLargeGameAnalyzer.requiresExternalGameData(1_500L * 1024 * 1024), "small package stays embedded");
        check(RenPyLargeGameAnalyzer.requiresExternalGameData(1_700L * 1024 * 1024), "headroom keeps the final signed APK below its ceiling");
            check(result.hasUpdateMetadata && result.hasUpdateChunk && result.hasPublicKey,
                    "identify official update kit components");
            check(result.estimatedApkBytes == 100L * 1024 * 1024 + 4096 + 4 * 32 + 64,
                    "include runtime and source ZIP sizes in output estimate");
        } finally { zip.delete(); }
        System.out.println("RenPyLargeGameAnalyzerTest passed");
    }

    private static void add(ZipOutputStream out, String name, long size) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        CRC32 crc = new CRC32();
        byte[] zeros = new byte[65536];
        for (long remaining = size; remaining > 0; remaining -= Math.min(remaining, zeros.length))
            crc.update(zeros, 0, (int)Math.min(remaining, zeros.length));
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(size); entry.setCompressedSize(size); entry.setCrc(crc.getValue());
        out.putNextEntry(entry);
        for (long remaining = size; remaining > 0; remaining -= Math.min(remaining, zeros.length))
            out.write(zeros, 0, (int)Math.min(remaining, zeros.length));
        out.closeEntry();
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
