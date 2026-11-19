package com.gamebridge.mobile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class OfflinePackageFormat {
    private static final String RENPY_TEMPLATE_PACKAGE = "com.example.renpylocal";
    private static final String PACKAGE_PREFIX = "com.af.a";
    private static final int PACKAGE_LENGTH = RENPY_TEMPLATE_PACKAGE.length();
    // Generated games are signed with the converter's embedded debug certificate.
    // Each converter release has its own AndroidKeyStore UID/certificate. Keep
    // generated game IDs release-scoped so a newer release can coexist with the
    // previous converter instead of colliding with an incompatible signer.
    private static final String PACKAGE_ID_NAMESPACE = "APKForge-v4";

    private OfflinePackageFormat() { }

    static String renpyAssetPath(String path) {
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("Empty game asset path.");
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) throw new IllegalArgumentException("Absolute game asset paths are not allowed.");
        StringBuilder result = new StringBuilder("assets/x-game/");
        String[] segments = normalized.split("/", -1);
        boolean first = true;
        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            if (segment.equals(".") || segment.equals("..")) throw new IllegalArgumentException("Unsafe game asset path.");
            if (segment.startsWith(".")) return null;
            if (!first) result.append('/');
            result.append("x-").append(segment);
            first = false;
        }
        if (first) throw new IllegalArgumentException("Empty game asset path.");
        return result.toString();
    }

    static String packageFor(String appName, String requestedPackage) {
        return packageFor(appName, requestedPackage, RENPY_TEMPLATE_PACKAGE);
    }

    static String packageFor(String appName, String requestedPackage, String templatePackage) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String identity = PACKAGE_ID_NAMESPACE + "\u0000"
                    + (appName == null ? "" : appName.trim()) + "\u0000"
                    + (requestedPackage == null ? "" : requestedPackage.trim());
            byte[] hash = digest.digest(identity.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) hex.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            int packageLength = templatePackage.length();
            if (packageLength <= PACKAGE_PREFIX.length()) throw new IllegalArgumentException("Invalid template package.");
            String result = PACKAGE_PREFIX + hex.substring(0, packageLength - PACKAGE_PREFIX.length());
            if (result.length() != packageLength) throw new IllegalStateException("Game package does not match the embedded template.");
            return result;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    static byte[] replacePackage(byte[] input, String oldPackage, String newPackage) {
        byte[] oldBytes = oldPackage.getBytes(StandardCharsets.UTF_8);
        byte[] newBytes = newPackage.getBytes(StandardCharsets.UTF_8);
        if (oldBytes.length != newBytes.length) throw new IllegalArgumentException("Package names must have equal UTF-8 length.");
        byte[] output = input.clone();
        int matches = replaceEncoded(output, oldBytes, newBytes);
        matches += replaceEncoded(output, oldPackage.getBytes(java.nio.charset.StandardCharsets.UTF_16LE), newPackage.getBytes(java.nio.charset.StandardCharsets.UTF_16LE));
        if (matches == 0) throw new IllegalArgumentException("The APK manifest does not contain the expected package ID.");
        return output;
    }

    static String fixedAsciiLabel(String value, int length) {
        String clean = value == null ? "" : value.trim().replaceAll("[^\\x20-\\x7E]", "");
        if (clean.isEmpty()) clean = "Game";
        if (clean.length() > length) clean = clean.substring(0, length);
        StringBuilder result = new StringBuilder(clean);
        while (result.length() < length) result.append(' ');
        return result.toString();
    }

    static String cleanGameTitle(String linkText, String fallback) {
        String title = linkText == null ? "" : linkText.trim();
        title = title.replaceFirst("(?i)^download\\s+", "");
        title = title.replaceFirst("(?i)\\s*\\([^)]*\\)\\s*$", "");
        title = title.replaceFirst("(?i)\\s+(?:complete\\s+game\\s+)?zip\\s*$", "");
        title = title.replaceAll("(?i)\\.zip\\s*$", "").replaceAll("['’]", "");
        title = title.replaceAll("[^A-Za-z0-9 _-]+", " ").trim().replaceAll("\\s+", " ");
        if (title.isEmpty() || "download".equalsIgnoreCase(title)) {
            title = fallback == null ? "" : fallback.replaceFirst("(?i)\\.zip$", "").replaceAll("['’]", "").replaceAll("[^A-Za-z0-9 _-]+", " ").trim().replaceAll("\\s+", " ");
        }
        if (title.isEmpty()) title = "Game";
        if (title.length() > 16) {
            int wordEnd = title.lastIndexOf(' ', 16);
            title = title.substring(0, wordEnd > 0 ? wordEnd : 16);
        }
        return title;
    }

    static byte[] replaceFixedText(byte[] input, String oldText, String newText) {
        if (oldText.length() != newText.length()) throw new IllegalArgumentException("APK display labels must have equal character length.");
        byte[] output = input.clone();
        int matches = replaceEncoded(output, oldText.getBytes(StandardCharsets.UTF_8), newText.getBytes(StandardCharsets.UTF_8));
        byte[] oldUtf16 = oldText.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        byte[] newUtf16 = newText.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        matches += replaceEncoded(output, oldUtf16, newUtf16);
        if (matches == 0) throw new IllegalArgumentException("The APK template did not contain its expected display label.");
        return output;
    }

    private static int replaceEncoded(byte[] output, byte[] oldBytes, byte[] newBytes) {
        int matches = 0;
        for (int i = 0; i <= output.length - oldBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < oldBytes.length; j++) if (output[i + j] != oldBytes[j]) { found = false; break; }
            if (found) {
                System.arraycopy(newBytes, 0, output, i, newBytes.length);
                i += oldBytes.length - 1;
                matches++;
            }
        }
        return matches;
    }

    static boolean containsPermission(byte[] xml, String permission) {
        return permissionRange(xml, permission) != null;
    }

    static byte[] removePermission(byte[] xml, String permission) {
        int[] range = permissionRange(xml, permission);
        if (range == null) return xml.clone();
        byte[] result = new byte[xml.length - (range[1] - range[0])];
        System.arraycopy(xml, 0, result, 0, range[0]);
        System.arraycopy(xml, range[1], result, range[0], xml.length - range[1]);
        writeIntLE(result, 4, result.length);
        return result;
    }

    private static int[] permissionRange(byte[] xml, String permission) {
        String[] strings = readXmlStringPool(xml);
        int offset = 8;
        while (offset + 8 <= xml.length) {
            int type = readU16(xml, offset);
            int size = readIntLE(xml, offset + 4);
            if (size < 8 || offset + size > xml.length) throw new IllegalArgumentException("Malformed Android binary XML chunk.");
            if (type == 0x0102) {
                int elementNameIndex = readIntLE(xml, offset + 20);
                String elementName = stringAt(strings, elementNameIndex);
                if ("uses-permission".equals(elementName) || "uses-permission-sdk-23".equals(elementName)) {
                    int attributeStart = readU16(xml, offset + 24);
                    int attributeSize = readU16(xml, offset + 26);
                    int attributeCount = readU16(xml, offset + 28);
                    int firstAttribute = offset + 16 + attributeStart;
                    for (int i = 0; i < attributeCount; i++) {
                        int attribute = firstAttribute + i * attributeSize;
                        String attributeName = stringAt(strings, readIntLE(xml, attribute + 4));
                        if (!"name".equals(attributeName)) continue;
                        int raw = readIntLE(xml, attribute + 8);
                        int valueIndex = raw;
                        if (valueIndex == -1 && xml[attribute + 15] == 0x03) valueIndex = readIntLE(xml, attribute + 16);
                        if (!permission.equals(stringAt(strings, valueIndex))) continue;
                        int end = offset + size;
                        while (end + 8 <= xml.length) {
                            int endSize = readIntLE(xml, end + 4);
                            if (readU16(xml, end) == 0x0103 && readIntLE(xml, end + 20) == elementNameIndex) {
                                return new int[]{offset, end + endSize};
                            }
                            if (endSize < 8 || end + endSize > xml.length) throw new IllegalArgumentException("Malformed Android binary XML element.");
                            end += endSize;
                        }
                        throw new IllegalArgumentException("Unclosed Android permission element.");
                    }
                }
            }
            offset += size;
        }
        return null;
    }

    static boolean booleanAttribute(byte[] xml, String elementName, String attributeName) {
        int attribute = findAndroidAttribute(xml, elementName, attributeName);
        if (attribute < 0 || (xml[attribute + 15] & 0xff) != 0x12) throw new IllegalArgumentException("Android boolean attribute was not found: " + attributeName);
        return readIntLE(xml, attribute + 16) != 0;
    }

    static byte[] setBooleanAttribute(byte[] xml, String elementName, String attributeName, boolean value) {
        byte[] output = xml.clone();
        int attribute = findAndroidAttribute(output, elementName, attributeName);
        if (attribute < 0 || (output[attribute + 15] & 0xff) != 0x12) throw new IllegalArgumentException("Android boolean attribute was not found: " + attributeName);
        writeIntLE(output, attribute + 16, value ? -1 : 0);
        return output;
    }

    private static int findAndroidAttribute(byte[] xml, String elementName, String attributeName) {
        String[] strings = readXmlStringPool(xml);
        int offset = 8;
        while (offset + 8 <= xml.length) {
            int size = readIntLE(xml, offset + 4);
            if (size < 8 || offset + size > xml.length) throw new IllegalArgumentException("Malformed Android binary XML chunk.");
            if (readU16(xml, offset) == 0x0102 && elementName.equals(stringAt(strings, readIntLE(xml, offset + 20)))) {
                int first = offset + 16 + readU16(xml, offset + 24);
                int stride = readU16(xml, offset + 26);
                int count = readU16(xml, offset + 28);
                for (int i = 0; i < count; i++) {
                    int attribute = first + i * stride;
                    String name = stringAt(strings, readIntLE(xml, attribute + 4));
                    String namespace = stringAt(strings, readIntLE(xml, attribute));
                    if (attributeName.equals(name) && "http://schemas.android.com/apk/res/android".equals(namespace)) return attribute;
                }
            }
            offset += size;
        }
        return -1;
    }

    private static String[] readXmlStringPool(byte[] xml) {
        if (xml.length < 8 || readU16(xml, 0) != 0x0003) throw new IllegalArgumentException("Not Android binary XML.");
        int chunk = 8;
        if (readU16(xml, chunk) != 0x0001) throw new IllegalArgumentException("Android XML is missing its string pool.");
        int chunkSize = readIntLE(xml, chunk + 4);
        int count = readIntLE(xml, chunk + 8);
        int flags = readIntLE(xml, chunk + 16);
        int stringsStart = readIntLE(xml, chunk + 20);
        if (count < 0 || chunkSize < 28 || chunk + chunkSize > xml.length || chunk + 28L + count * 4L > chunk + chunkSize) throw new IllegalArgumentException("Malformed Android XML string pool.");
        String[] result = new String[count];
        boolean utf8 = (flags & 0x100) != 0;
        for (int i = 0; i < count; i++) {
            int relative = readIntLE(xml, chunk + 28 + i * 4);
            int pos = chunk + stringsStart + relative;
            if (pos < chunk || pos >= chunk + chunkSize) throw new IllegalArgumentException("Malformed Android XML string offset.");
            if (utf8) {
                int[] utf16Length = readLength8(xml, pos); int[] byteLength = readLength8(xml, utf16Length[1]);
                result[i] = new String(xml, byteLength[1], byteLength[0], StandardCharsets.UTF_8);
            } else {
                int first = readU16(xml, pos); int chars; int data;
                if ((first & 0x8000) == 0) { chars = first; data = pos + 2; }
                else { chars = ((first & 0x7fff) << 16) | readU16(xml, pos + 2); data = pos + 4; }
                result[i] = new String(xml, data, chars * 2, java.nio.charset.StandardCharsets.UTF_16LE);
            }
        }
        return result;
    }

    private static int[] readLength8(byte[] data, int offset) {
        int first = data[offset] & 0xff;
        if ((first & 0x80) == 0) return new int[]{first, offset + 1};
        return new int[]{((first & 0x7f) << 8) | (data[offset + 1] & 0xff), offset + 2};
    }
    private static String stringAt(String[] strings, int index) { return index < 0 || index >= strings.length ? null : strings[index]; }
    private static int readU16(byte[] data, int offset) { return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8); }
    private static int readIntLE(byte[] data, int offset) { return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8) | ((data[offset + 2] & 0xff) << 16) | (data[offset + 3] << 24); }
    private static void writeIntLE(byte[] data, int offset, int value) { for (int i = 0; i < 4; i++) data[offset + i] = (byte) (value >>> (i * 8)); }
}
