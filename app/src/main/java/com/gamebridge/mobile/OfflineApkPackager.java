package com.gamebridge.mobile;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;

import com.android.apksig.ApkSigner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

final class OfflineApkPackager {
    private static final String RENPY_PACKAGE = "com.example.renpylocal";
    private static final String HTML_PACKAGE = "com.example.finalhtml";
    private static final long MAX_GAME_BYTES = RenPyPackageLimits.MAX_EXPANDED_GAME_BYTES;
    private static final int MAX_GAME_FILES = 150000;
    private OfflineApkPackager() { }

    static final class Result {
        final File apk;
        final File gameDataBundle;
        final String packageName;
        final String kind;
        Result(File apk, File gameDataBundle, String packageName, String kind) {
            this.apk = apk; this.gameDataBundle = gameDataBundle; this.packageName = packageName; this.kind = kind;
        }
    }

    static final class Prepared {
        final String packageName;
        final String kind;
        final File gameDataBundle;
        Prepared(String packageName, String kind, File gameDataBundle) {
            this.packageName = packageName; this.kind = kind; this.gameDataBundle = gameDataBundle;
        }
    }

    static Result build(Context context, File inputZip, String appName, String packageSeed) throws Exception {
        return build(context, inputZip, appName, packageSeed, WebGameLinkAnalyzer.Format.UNKNOWN);
    }

    static Result build(Context context, File inputZip, String appName, String packageSeed, WebGameLinkAnalyzer.Format requestedFormat) throws Exception {
        DiagnosticLog.record(context.getFilesDir(), DiagnosticLog.Event.PACKAGER_BUILD_STARTED, 0, 0);
        File template = new File(context.getCacheDir(), "offline-template-" + UUID.randomUUID() + ".apk");
        File unsigned = new File(context.getCacheDir(), "offline-" + UUID.randomUUID() + "-unsigned.apk");
        File signed = new File(context.getCacheDir(), "offline-" + UUID.randomUUID() + ".apk");
        File gameDataBundle = null;
        int stage = 1;
        try {
            String templateName;
            Project project;
            try (ZipFile input = new ZipFile(inputZip)) {
                project = detect(input);
                WebGameLinkAnalyzer.Format detected = project.renpy ? WebGameLinkAnalyzer.Format.RENPY : WebGameLinkAnalyzer.Format.HTML;
                WebGameLinkAnalyzer.validateOverride(detected, requestedFormat);
                templateName = project.renpy ? "renpy-template.apk" : "html-template.apk";
            }
            DiagnosticLog.record(context.getFilesDir(), DiagnosticLog.Event.PACKAGER_PROJECT_DETECTED, project.renpy ? 2 : 1, 0);
            stage = 2;
            try (InputStream in = context.getAssets().open(templateName); OutputStream out = new BufferedOutputStream(new FileOutputStream(template))) {
                copyStream(in, out, new byte[65536], MAX_GAME_BYTES);
            }
            DiagnosticLog.record(context.getFilesDir(), DiagnosticLog.Event.PACKAGER_TEMPLATE_READY, template.length(), 0);
            stage = 3;
            File gameDataDirectory = context.getExternalFilesDir("library-game-data");
            if (gameDataDirectory == null) gameDataDirectory = new File(context.getFilesDir(), "library-game-data");
            Prepared prepared = createUnsigned(template, inputZip, appName, packageSeed, unsigned,
                    requestedFormat, gameDataDirectory);
            gameDataBundle = prepared.gameDataBundle;
            DiagnosticLog.record(context.getFilesDir(), DiagnosticLog.Event.PACKAGER_UNSIGNED_READY, unsigned.length(), 0);
            stage = 4;
            DiagnosticLog.record(context.getFilesDir(), DiagnosticLog.Event.PACKAGER_SIGNING_STARTED, 0, 0);
            sign(context, unsigned, signed);
            if (signed.length() > RenPyPackageLimits.MAX_SIGNED_APK_BYTES) {
                throw new IllegalArgumentException(RenPyPackageLimits.oversizedApkMessage());
            }
            DiagnosticLog.record(context.getFilesDir(), DiagnosticLog.Event.PACKAGER_SIGNING_COMPLETED, signed.length(), 0);
            if (!unsigned.delete()) unsigned.deleteOnExit();
            return new Result(signed, gameDataBundle, prepared.packageName, prepared.kind);
        } catch (Exception e) {
            DiagnosticLog.record(context.getFilesDir(), DiagnosticLog.Event.PACKAGER_FAILED, stage, 0);
            unsigned.delete(); signed.delete(); if (gameDataBundle != null) gameDataBundle.delete(); throw e;
        } finally {
            template.delete();
        }
    }

    static Prepared createUnsigned(File template, File inputZip, String appName, String packageSeed, File output) throws Exception {
        return createUnsigned(template, inputZip, appName, packageSeed, output, WebGameLinkAnalyzer.Format.UNKNOWN);
    }

    static Prepared createUnsigned(File template, File inputZip, String appName, String packageSeed, File output, WebGameLinkAnalyzer.Format requestedFormat) throws Exception {
        return createUnsigned(template, inputZip, appName, packageSeed, output, requestedFormat,
                output.getAbsoluteFile().getParentFile());
    }

    static Prepared createUnsigned(File template, File inputZip, String appName, String packageSeed,
                                   File output, WebGameLinkAnalyzer.Format requestedFormat,
                                   File gameDataDirectory) throws Exception {
        Project project;
        try (ZipFile input = new ZipFile(inputZip)) { project = detect(input); }
        WebGameLinkAnalyzer.validateOverride(project.renpy ? WebGameLinkAnalyzer.Format.RENPY : WebGameLinkAnalyzer.Format.HTML, requestedFormat);
        String templatePackage = project.renpy ? RENPY_PACKAGE : HTML_PACKAGE;
        String packageName = OfflinePackageFormat.packageFor(appName, packageSeed, templatePackage);
        String templateLabel = project.renpy ? "RenPy Local SDK Test" : "Final HTML Smoke";
        String gameLabel = OfflinePackageFormat.fixedAsciiLabel(appName, templateLabel.length());
        File gameDataBundle = null;
        try {
            long estimated = project.renpy
                    ? RenPyLargeGameAnalyzer.analyze(inputZip, template.length()).estimatedApkBytes : 0;
            boolean externalGameData = project.renpy && RenPyLargeGameAnalyzer.requiresExternalGameData(estimated);
            if (externalGameData) {
                gameDataBundle = gameDataBundleTarget(output, gameDataDirectory);
                RenPyGameDataBundle.write(inputZip, project.gameDir, gameDataBundle);
            }
            copyTemplateAndGame(template, project, inputZip, templatePackage, packageName, templateLabel, gameLabel,
                    externalGameData, output);
            if (output.length() > RenPyPackageLimits.MAX_UNSIGNED_APK_BYTES) {
                throw new IllegalArgumentException(RenPyPackageLimits.oversizedApkMessage());
            }
        } catch (Exception failure) {
            output.delete();
            if (gameDataBundle != null) gameDataBundle.delete();
            throw failure;
        }
        return new Prepared(packageName, project.renpy ? "RenPy" : "HTML", gameDataBundle);
    }

    static File gameDataBundleTarget(File output, File gameDataDirectory) {
        File directory = gameDataDirectory == null
                ? output.getAbsoluteFile().getParentFile() : gameDataDirectory;
        return new File(directory, output.getName() + ".game-data.zip");
    }

    private static final class Project {
        final boolean renpy;
        final String root;
        final String gameDir;
        final String htmlRoot;
        final String htmlIndex;
        Project(boolean renpy, String root, String gameDir, String htmlRoot, String htmlIndex) {
            this.renpy = renpy; this.root = root; this.gameDir = gameDir; this.htmlRoot = htmlRoot; this.htmlIndex = htmlIndex;
        }
    }

    private static Project detect(ZipFile zip) throws Exception {
        List<String> names = new ArrayList<>();
        java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = safeZipName(entry.getName());
            if (name != null && !entry.isDirectory()) names.add(name);
            if (names.size() > MAX_GAME_FILES) throw new IllegalArgumentException("The project contains too many files.");
        }
        for (String name : names) {
            String[] parts = name.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if (!parts[i].equalsIgnoreCase("game")) continue;
                String root = join(parts, 0, i);
                String game = root.isEmpty() ? "game/" : root + "/game/";
                boolean compiledGame = false, windowsExe = false, windowsLib = false;
                for (String candidate : names) {
                    if (candidate.startsWith(game) && (candidate.toLowerCase(Locale.ROOT).endsWith(".rpyc") || candidate.toLowerCase(Locale.ROOT).endsWith(".rpa"))) compiledGame = true;
                    if (candidate.startsWith(root.isEmpty() ? "" : root + "/") && candidate.toLowerCase(Locale.ROOT).endsWith(".exe") && candidate.indexOf('/', root.isEmpty() ? 0 : root.length() + 1) < 0) windowsExe = true;
                    if (candidate.startsWith(root.isEmpty() ? "lib/" : root + "/lib/")) windowsLib = true;
                }
                if (compiledGame && (windowsExe && windowsLib || !windowsExe)) return new Project(true, root, game, null, null);
            }
        }
        String best = null;
        for (String name : names) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (!lower.endsWith("/index.html") && !lower.endsWith("/index.htm") && !lower.equals("index.html") && !lower.equals("index.htm")) continue;
            String directory = name.substring(0, name.lastIndexOf('/') + 1);
            if (best == null || lower.endsWith("/index.html") && !best.toLowerCase(Locale.ROOT).endsWith("/index.html") || directory.length() < best.substring(0, best.lastIndexOf('/') + 1).length()) best = name;
        }
        if (best != null) return new Project(false, null, null, commonHtmlRoot(names), best);
        for (String name : names) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return new Project(false, null, null, commonHtmlRoot(names), name);
        }
        throw new IllegalArgumentException("No HTML page or compiled RenPy game data was found. Select a complete RenPy Windows game folder or an HTML project.");
    }

    private static String commonHtmlRoot(List<String> names) {
        String[] common = null;
        int length = 0;
        for (String name : names) {
            String[] parts = name.split("/");
            int directories = parts.length - 1;
            if (common == null) { common = parts; length = directories; continue; }
            int shared = 0;
            while (shared < length && shared < directories && common[shared].equals(parts[shared])) shared++;
            length = shared;
        }
        return common == null || length == 0 ? "" : join(common, 0, length) + "/";
    }

    private static void copyTemplateAndGame(File templateFile, Project project, File inputZip, String oldPackage, String newPackage, String oldLabel, String newLabel, boolean externalGameData, File output) throws Exception {
        try (InputStream templateStream = new FileInputStream(templateFile); ZipInputStreamCompat template = new ZipInputStreamCompat(templateStream); ZipFile input = new ZipFile(inputZip); CountingOutputStream counted = new CountingOutputStream(new BufferedOutputStream(new FileOutputStream(output)), RenPyPackageLimits.MAX_UNSIGNED_APK_BYTES); ZipOutputStream out = new ZipOutputStream(counted)) {
            ZipEntry entry;
            byte[] buffer = new byte[65536];
            ZipEntryDeduplicator outputEntries = new ZipEntryDeduplicator();
            while ((entry = template.getNextEntry()) != null) {
                String name = entry.getName();
                if (isSigningMetadata(name)) continue;
                if (name.equals("assets/apkforge-external-game-data.flag")) continue;
                if (project.renpy && (name.startsWith("assets/x-game/") || isWindowsExeAsset(name))) continue;
                if (!project.renpy && name.replace('\\', '/').startsWith("assets/www/")) continue;
                if (!outputEntries.claim(name)) {
                    template.closeEntry();
                    continue;
                }
                if (name.equals("resources.arsc")) {
                    byte[] resources = readAll(template, buffer, MAX_GAME_BYTES);
                    if (project.renpy) {
                        resources = OfflinePackageFormat.replacePackage(resources, oldPackage, newPackage);
                        resources = OfflinePackageFormat.replaceFixedText(resources, oldLabel, newLabel);
                    }
                    ZipEntry resourceEntry = new ZipEntry(name);
                    if (entry.getTime() > 0) resourceEntry.setTime(entry.getTime());
                    resourceEntry.setMethod(ZipEntry.STORED);
                    resourceEntry.setSize(resources.length);
                    resourceEntry.setCompressedSize(resources.length);
                    java.util.zip.CRC32 crc = new java.util.zip.CRC32(); crc.update(resources);
                    resourceEntry.setCrc(crc.getValue());
                    resourceEntry.setExtra(alignmentExtra(name, counted.getCount(), resourceEntry.getExtra()));
                    out.putNextEntry(resourceEntry);
                    out.write(resources);
                    out.closeEntry();
                    template.closeEntry();
                    continue;
                }
                ZipEntry copy = new ZipEntry(name);
                if (entry.getTime() > 0) copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                if (name.equals("AndroidManifest.xml")) {
                    byte[] manifest = readAll(template, buffer, MAX_GAME_BYTES);
                    byte[] patched = OfflinePackageFormat.replacePackage(manifest, oldPackage, newPackage);
                    if (!project.renpy) {
                        patched = OfflinePackageFormat.replaceFixedText(patched, newPackage + ".MainActivity", oldPackage + ".MainActivity");
                        patched = OfflinePackageFormat.replaceFixedText(patched, oldLabel, newLabel);
                        patched = OfflinePackageFormat.setBooleanAttribute(patched, "application", "debuggable", false);
                    }
                    out.write(patched);
                } else if (project.renpy && name.equals("assets/x-android.json")) {
                    byte[] metadata = readAll(template, buffer, MAX_GAME_BYTES);
                    byte[] patched = OfflinePackageFormat.replacePackage(metadata, oldPackage, newPackage);
                    out.write(OfflinePackageFormat.replaceFixedText(patched, oldLabel, newLabel));
                } else {
                    copyStream(template, out, buffer, MAX_GAME_BYTES);
                }
                out.closeEntry();
                template.closeEntry();
            }
            if (externalGameData) {
                String marker = "assets/apkforge-external-game-data.flag";
                if (outputEntries.claim(marker)) {
                    out.putNextEntry(new ZipEntry(marker));
                    out.write("apkforge-renpy-game-data-v1\n".getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();
                }
            }
            long total = 0; int count = 0;
            java.util.Enumeration<? extends ZipEntry> sources = input.entries();
            while (sources.hasMoreElements()) {
                ZipEntry source = sources.nextElement();
                if (source.isDirectory()) continue;
                String path = safeZipName(source.getName());
                if (path == null) continue;
                String target;
                if (project.renpy) {
                    if (externalGameData) continue;
                    if (!path.startsWith(project.gameDir)) continue;
                    String relative = path.substring(project.gameDir.length());
                    target = OfflinePackageFormat.renpyAssetPath(relative);
                    if (target == null) continue;
                } else {
                    if (!path.startsWith(project.htmlRoot)) continue;
                    String relative = path.substring(project.htmlRoot.length());
                    if (relative.isEmpty()) continue;
                    target = "assets/www/" + relative;
                    if (target.equals("assets/www/")) continue;
                }
                if (!outputEntries.claim(target)) continue;
                if (source.getSize() >= 0 && source.getSize() > RenPyPackageLimits.MAX_GAME_FILE_BYTES) {
                    throw new IllegalArgumentException("One game file is larger than APK Forge can safely store as an Android APK ZIP entry. Split that file into smaller game archives/files, or use Ren'Py's official large-game downloader with hosted data.");
                }
                if (++count > MAX_GAME_FILES) throw new IllegalArgumentException("The project contains too many files.");
                ZipEntry targetEntry = new ZipEntry(target);
                out.putNextEntry(targetEntry);
                try (InputStream sourceStream = input.getInputStream(source)) {
                    long copied = copyStream(sourceStream, out, buffer, MAX_GAME_BYTES - total);
                    total += copied;
                    if (total > MAX_GAME_BYTES) throw new IllegalArgumentException("The game data exceeds APK Forge's 6 GiB expanded processing limit. Even below that limit, the finished APK must remain under 1.9 GiB to stay installable.");
                }
                out.closeEntry();
            }
            if (!project.renpy && !containsHtmlIndex(input, project.htmlIndex)) throw new IllegalArgumentException("Could not read the selected HTML entry point.");
            if (!project.renpy) {
                String relativeIndex = project.htmlIndex.substring(project.htmlRoot.length());
                if (!relativeIndex.equalsIgnoreCase("index.html")) {
                    String url = relativeIndex.replace("%", "%25").replace(" ", "%20").replace("#", "%23").replace("?", "%3F");
                    byte[] redirect = ("<!doctype html><meta http-equiv=\"refresh\" content=\"0;url=" + url + "\"><script>location.replace(" + quoteJs(url) + ")</script>").getBytes(StandardCharsets.UTF_8);
                    if (outputEntries.claim("assets/www/index.html")) {
                        out.putNextEntry(new ZipEntry("assets/www/index.html")); out.write(redirect); out.closeEntry();
                    }
                }
            }
        }
    }

    private static String quoteJs(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

    private static byte[] alignmentExtra(String name, long outputOffset, byte[] existing) {
        int existingLength = existing == null ? 0 : existing.length;
        int needed = (int) ((4 - ((outputOffset + 30 + name.getBytes(StandardCharsets.UTF_8).length + existingLength) & 3)) & 3);
        if (needed == 0) return existing;
        int totalLength = needed < 4 ? needed + 4 : needed;
        byte[] extra = java.util.Arrays.copyOf(existing == null ? new byte[0] : existing, existingLength + totalLength);
        int payloadLength = totalLength - 4;
        extra[existingLength] = 0x35; extra[existingLength + 1] = (byte) 0xd9;
        extra[existingLength + 2] = (byte) payloadLength; extra[existingLength + 3] = (byte) (payloadLength >>> 8);
        return extra;
    }

    private static boolean containsHtmlIndex(ZipFile zip, String selected) { return zip.getEntry(selected) != null; }
    private static boolean isWindowsExeAsset(String name) { return name.startsWith("assets/x-") && name.toLowerCase(Locale.ROOT).endsWith(".exe"); }
    private static boolean isSigningMetadata(String name) {
        if (!name.toUpperCase(Locale.ROOT).startsWith("META-INF/")) return false;
        String leaf = name.substring(name.lastIndexOf('/') + 1).toUpperCase(Locale.ROOT);
        return leaf.equals("MANIFEST.MF") || leaf.endsWith(".SF") || leaf.endsWith(".RSA") || leaf.endsWith(".DSA") || leaf.endsWith(".EC");
    }
    private static String safeZipName(String name) {
        if (name == null) return null;
        String path = name.replace('\\', '/');
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*")) throw new IllegalArgumentException("Project archive contains an absolute path.");
        for (String part : path.split("/")) if (part.equals("..")) throw new IllegalArgumentException("Project archive contains a path traversal entry.");
        return path;
    }
    private static String join(String[] parts, int start, int end) {
        StringBuilder value = new StringBuilder();
        for (int i = start; i < end; i++) { if (value.length() > 0) value.append('/'); value.append(parts[i]); }
        return value.toString();
    }
    private static long copyStream(InputStream in, OutputStream out, byte[] buffer, long limit) throws Exception {
        long total = 0; int read;
        while ((read = in.read(buffer)) != -1) { total += read; if (total > limit) throw new IllegalArgumentException("Expanded project exceeds the allowed size."); out.write(buffer, 0, read); }
        return total;
    }
    private static byte[] readAll(InputStream in, byte[] buffer, long limit) throws Exception {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        copyStream(in, bytes, buffer, limit); return bytes.toByteArray();
    }

    private static void sign(Context context, File input, File output) throws Exception {
        final String alias = "apkforge-local-game-signer-v1";
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (!store.containsAlias(alias)) {
            Date start = new Date(); Date end = new Date(start.getTime() + 25L * 365L * 24L * 60L * 60L * 1000L);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            generator.initialize(new KeyPairGeneratorSpec.Builder(context).setAlias(alias).setSubject(new javax.security.auth.x500.X500Principal("CN=APK Forge Local Games"))
                    .setSerialNumber(BigInteger.ONE).setStartDate(start).setEndDate(end).build());
            generator.generateKeyPair(); store.load(null);
        }
        PrivateKey key = (PrivateKey) store.getKey(alias, null);
        X509Certificate cert = (X509Certificate) store.getCertificate(alias);
        if (key == null || cert == null) throw new IllegalStateException("Could not initialize the on-device game signing key.");
        ApkSigner.SignerConfig signer = new ApkSigner.SignerConfig.Builder("APK Forge", key, Collections.singletonList(cert)).build();
        new ApkSigner.Builder(Collections.singletonList(signer)).setInputApk(input).setOutputApk(output)
                .setMinSdkVersion(21).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
    }

    private static final class ZipInputStreamCompat extends java.util.zip.ZipInputStream {
        ZipInputStreamCompat(InputStream in) { super(new BufferedInputStream(in)); }
    }

    private static final class CountingOutputStream extends OutputStream {
        private final OutputStream output;
        private final long maximum;
        private long count;
        CountingOutputStream(OutputStream output, long maximum) { this.output = output; this.maximum = maximum; }
        long getCount() { return count; }
        private void reserve(long bytes) throws java.io.IOException {
            if (bytes > maximum - count) throw new java.io.IOException(RenPyPackageLimits.oversizedApkMessage());
        }
        @Override public void write(int value) throws java.io.IOException { reserve(1); output.write(value); count++; }
        @Override public void write(byte[] bytes, int offset, int length) throws java.io.IOException { reserve(length); output.write(bytes, offset, length); count += length; }
        @Override public void flush() throws java.io.IOException { output.flush(); }
        @Override public void close() throws java.io.IOException { output.close(); }
    }
}
