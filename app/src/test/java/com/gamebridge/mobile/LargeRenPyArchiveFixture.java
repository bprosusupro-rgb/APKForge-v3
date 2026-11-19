package com.gamebridge.mobile;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Creates a highly-compressible 5 GiB logical Ren'Py ZIP for device smoke tests. */
public final class LargeRenPyArchiveFixture {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Pass the ZIP output path.");
        long size = 1280L * 1024L * 1024L;
        byte[] zeroes = new byte[1024 * 1024];
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])))) {
            zip.putNextEntry(new ZipEntry("TestGame/game/script.rpyc"));
            zip.write("fixture only".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (int part = 1; part <= 4; part++) {
                zip.putNextEntry(new ZipEntry("TestGame/game/large-compressible-payload-" + part + ".bin"));
                for (long written = 0; written < size; written += zeroes.length) zip.write(zeroes);
                zip.closeEntry();
            }
        }
    }
}
