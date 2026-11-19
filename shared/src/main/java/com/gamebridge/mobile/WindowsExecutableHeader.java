package com.gamebridge.mobile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Performs a small structural PE check; it is not an antivirus or trust verdict. */
public final class WindowsExecutableHeader {
    public static final class Inspection {
        public final boolean valid;
        public final String architecture;
        public final String reason;

        private Inspection(boolean valid, String architecture, String reason) {
            this.valid = valid;
            this.architecture = architecture;
            this.reason = reason;
        }
    }

    private WindowsExecutableHeader() { }

    public static Inspection inspect(File file) throws IOException {
        if (file == null || !file.isFile() || file.length() < 64)
            return invalid("File is too small to be a Windows executable.");
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            if (readLe16(input, 0) != 0x5a4d) return invalid("Missing Windows MZ signature.");
            long peOffset = readLe32(input, 0x3c);
            if (peOffset < 64 || peOffset > file.length() - 26)
                return invalid("Windows PE header offset is outside the file.");
            if (readByte(input, peOffset) != 'P'
                    || readByte(input, peOffset + 1) != 'E'
                    || readByte(input, peOffset + 2) != 0
                    || readByte(input, peOffset + 3) != 0)
                return invalid("Missing Windows PE signature.");

            int machine = readLe16(input, peOffset + 4);
            int sections = readLe16(input, peOffset + 6);
            int optionalSize = readLe16(input, peOffset + 20);
            if (sections < 1 || sections > 96 || optionalSize < 2)
                return invalid("Windows PE header has invalid section or optional-header data.");
            long optionalHeader = peOffset + 24;
            if (optionalHeader > file.length() - optionalSize)
                return invalid("Windows PE optional header is truncated.");
            int magic = readLe16(input, optionalHeader);
            if (magic != 0x10b && magic != 0x20b)
                return invalid("Unsupported Windows PE optional-header format.");

            String architecture;
            switch (machine) {
                case 0x014c: architecture = "x86"; break;
                case 0x8664: architecture = "x64"; break;
                case 0x01c0:
                case 0x01c4: architecture = "ARM"; break;
                case 0xaa64: architecture = "ARM64"; break;
                default: return invalid("Unrecognized Windows PE processor architecture.");
            }
            if ((magic == 0x20b) != (machine == 0x8664 || machine == 0xaa64))
                return invalid("Windows PE architecture does not match its optional header.");
            return new Inspection(true, architecture, "Valid Windows PE structure; publisher and safety are not verified.");
        }
    }

    private static Inspection invalid(String reason) { return new Inspection(false, "", reason); }

    private static int readByte(RandomAccessFile input, long offset) throws IOException {
        input.seek(offset);
        return input.readUnsignedByte();
    }

    private static int readLe16(RandomAccessFile input, long offset) throws IOException {
        input.seek(offset);
        return input.readUnsignedByte() | (input.readUnsignedByte() << 8);
    }

    private static long readLe32(RandomAccessFile input, long offset) throws IOException {
        input.seek(offset);
        return ((long) input.readUnsignedByte())
                | ((long) input.readUnsignedByte() << 8)
                | ((long) input.readUnsignedByte() << 16)
                | ((long) input.readUnsignedByte() << 24);
    }
}
