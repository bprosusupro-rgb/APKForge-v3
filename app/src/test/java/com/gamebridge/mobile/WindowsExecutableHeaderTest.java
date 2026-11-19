package com.gamebridge.mobile;

import java.io.File;
import java.io.RandomAccessFile;

public final class WindowsExecutableHeaderTest {
    public static void main(String[] args) throws Exception {
        File file = File.createTempFile("apkforge-pe-check", ".bin");
        try {
            try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
                out.setLength(88 + 240);
                out.seek(0);
                out.write('M'); out.write('Z');
                writeLe32(out, 0x3c, 64);
                out.seek(64);
                out.write('P'); out.write('E'); out.write(0); out.write(0);
                writeLe16(out, 68, 0x8664);
                writeLe16(out, 70, 3);
                writeLe16(out, 84, 240);
                writeLe16(out, 88, 0x20b);
            }
            WindowsExecutableHeader.Inspection valid = WindowsExecutableHeader.inspect(file);
            require(valid.valid && "x64".equals(valid.architecture), "well-formed x64 Windows PE must pass structural inspection");
            require(valid.reason.contains("not verified"), "inspection must not claim the publisher or safety is verified");

            try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
                out.seek(0); out.write("HTML".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            }
            require(!WindowsExecutableHeader.inspect(file).valid, "HTML renamed as .exe must be rejected");

            try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
                out.setLength(96); out.seek(0); out.write('M'); out.write('Z'); writeLe32(out, 0x3c, 0xfffffff0L);
            }
            require(!WindowsExecutableHeader.inspect(file).valid, "out-of-range PE offsets must be rejected");
        } finally {
            if (file.exists() && !file.delete()) file.deleteOnExit();
        }
        System.out.println("Windows executable header tests passed");
    }

    private static void writeLe16(RandomAccessFile file, long offset, int value) throws Exception {
        file.seek(offset); file.write(value & 0xff); file.write((value >>> 8) & 0xff);
    }

    private static void writeLe32(RandomAccessFile file, long offset, long value) throws Exception {
        file.seek(offset);
        for (int index = 0; index < 4; index++) file.write((int) (value >>> (index * 8)) & 0xff);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
