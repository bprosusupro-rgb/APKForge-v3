package com.gamebridge.mobile;

public final class OfflinePackageFormatTest {
    private static void equal(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
    }
    private static void truth(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static byte[] apkEntry(String apkPath, String entryName) throws Exception {
        try (java.util.zip.ZipFile apk = new java.util.zip.ZipFile(apkPath)) {
            java.util.zip.ZipEntry entry = apk.getEntry(entryName);
            if (entry == null) throw new AssertionError("missing template APK entry: " + entryName);
            try (java.io.InputStream in = apk.getInputStream(entry)) { return in.readAllBytes(); }
        }
    }
    private static String assetsRoot() {
        java.io.File directory = new java.io.File("").getAbsoluteFile();
        while (directory != null) {
            java.io.File[] candidates = {
                    new java.io.File(directory, "app/src/main/assets"),
                    new java.io.File(directory, "src/main/assets")
            };
            for (java.io.File candidate : candidates) {
                if (new java.io.File(candidate, "html-template.apk").isFile()
                        && new java.io.File(candidate, "renpy-template.apk").isFile()) {
                    return candidate.getAbsolutePath() + java.io.File.separator;
                }
            }
            directory = directory.getParentFile();
        }
        throw new AssertionError("could not locate both APKForge template APK assets from the current working directory");
    }
    private static void makeZip(java.io.File file, String[][] entries) throws Exception {
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(file))) {
            for (String[] item : entries) {
                zip.putNextEntry(new java.util.zip.ZipEntry(item[0]));
                zip.write(item[1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
    private static String apkText(java.util.zip.ZipFile apk, String entry, String encoding) throws Exception {
        java.util.zip.ZipEntry item = apk.getEntry(entry);
        if (item == null) throw new AssertionError("missing generated APK entry: " + entry);
        try (java.io.InputStream in = apk.getInputStream(item)) {
            return new String(in.readAllBytes(), java.nio.charset.Charset.forName(encoding));
        }
    }
    private static void checkInstallableResourceTable(java.io.File apkFile) throws Exception {
        try (java.util.zip.ZipFile apk = new java.util.zip.ZipFile(apkFile)) {
            java.util.zip.ZipEntry resourceTable = apk.getEntry("resources.arsc");
            truth(resourceTable != null && resourceTable.getMethod() == java.util.zip.ZipEntry.STORED, "resources.arsc must be stored, not compressed, for Android 11+ installs");
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(apkFile.toPath());
        byte[] entryName = "resources.arsc".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (int offset = 0; offset + 30 + entryName.length <= bytes.length; offset++) {
            if (bytes[offset] != 'P' || bytes[offset + 1] != 'K' || bytes[offset + 2] != 3 || bytes[offset + 3] != 4) continue;
            int nameLength = (bytes[offset + 26] & 0xff) | ((bytes[offset + 27] & 0xff) << 8);
            int extraLength = (bytes[offset + 28] & 0xff) | ((bytes[offset + 29] & 0xff) << 8);
            if (nameLength != entryName.length || offset + 30 + nameLength + extraLength > bytes.length) continue;
            boolean matches = true;
            for (int i = 0; i < nameLength; i++) if (bytes[offset + 30 + i] != entryName[i]) { matches = false; break; }
            if (matches) {
                int dataOffset = offset + 30 + nameLength + extraLength;
                truth((dataOffset & 3) == 0, "resources.arsc data must be aligned to a 4-byte boundary");
                return;
            }
        }
        throw new AssertionError("resources.arsc local ZIP entry was not found");
    }
    private static void checkRepackaging() throws Exception {
        String assets = assetsRoot();
        java.io.File folder = java.nio.file.Files.createTempDirectory("apkforge-repack-test-").toFile();
        java.io.File htmlZip = new java.io.File(folder, "web.zip");
        java.io.File htmlOut = new java.io.File(folder, "web.apk");
        makeZip(htmlZip, new String[][]{{"web/game.html", "<h1>Offline</h1>"}, {"web/./game.html", "<h1>Duplicate alias</h1>"}, {"web/assets/play.js", "window.ready=true"}});
        OfflineApkPackager.Prepared html = OfflineApkPackager.createUnsigned(new java.io.File(assets + "html-template.apk"), htmlZip, "Web Game", "com.example.web", htmlOut);
        checkInstallableResourceTable(htmlOut);
        truth(!OfflinePackageFormat.isDebuggable(apkEntry(htmlOut.getPath(), "AndroidManifest.xml")),
                "Generated HTML games must be non-debuggable.");
        try (java.util.zip.ZipFile apk = new java.util.zip.ZipFile(htmlOut)) {
            truth(apkText(apk, "assets/www/index.html", "UTF-8").contains("game.html"), "WebView entrypoint must route to a non-index HTML page");
            equal("<h1>Offline</h1>", apkText(apk, "assets/www/game.html", "UTF-8"));
            truth(apk.getEntry("assets/www/./game.html") == null, "equivalent dot-segment paths must not create duplicate game assets");
            equal("window.ready=true", apkText(apk, "assets/www/assets/play.js", "UTF-8"));
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = apk.entries();
            while (entries.hasMoreElements()) {
                truth(!entries.nextElement().getName().contains("\\"), "generated HTML APK asset paths must use Android-compatible forward slashes");
            }
            String manifest = apkText(apk, "AndroidManifest.xml", "UTF-16LE");
            truth(manifest.contains(html.packageName), "HTML APK must contain its unique package ID");
            truth(manifest.contains("com.example.finalhtml.MainActivity"), "HTML APK must keep the activity class name baked into its DEX");
            truth(manifest.contains("android.hardware.touchscreen"), "HTML APK must declare touch as optional for headset/controller devices");
            String htmlDex = apkText(apk, "classes.dex", "ISO-8859-1");
            truth(htmlDex.contains("Lcom/apkforge/input/WebGameInputMapper;"), "HTML APK must package the tested controller-to-keyboard bridge");
        }

        java.io.File renpyZip = new java.io.File(folder, "renpy.zip");
        java.io.File renpyOut = new java.io.File(folder, "renpy.apk");
        makeZip(renpyZip, new String[][]{{"dist/Test.exe", "windows-exe"}, {"dist/lib/runtime.dll", "windows-runtime"}, {"dist/game/main.rpyc", "compiled-game"}});
        OfflineApkPackager.Prepared renpy = OfflineApkPackager.createUnsigned(new java.io.File(assets + "renpy-template.apk"), renpyZip, "RenPy Game", "com.example.renpy", renpyOut);
        checkInstallableResourceTable(renpyOut);
        boolean rejectedWrongRuntime = false;
        try {
            OfflineApkPackager.createUnsigned(new java.io.File(assets + "renpy-template.apk"), renpyZip, "RenPy Game", "com.example.renpy", new java.io.File(folder, "wrong-runtime.apk"), WebGameLinkAnalyzer.Format.HTML);
        } catch (IllegalArgumentException expected) { rejectedWrongRuntime = expected.getMessage().contains("not the selected HTML format"); }
        truth(rejectedWrongRuntime, "a manual HTML override must not silently package a Ren’Py archive");
        try (java.util.zip.ZipFile apk = new java.util.zip.ZipFile(renpyOut)) {
            equal("compiled-game", apkText(apk, "assets/x-game/x-main.rpyc", "UTF-8"));
            truth(apk.getEntry("assets/x-Test.exe") == null, "Windows executable must not be packaged into the Android APK");
            truth(apkText(apk, "AndroidManifest.xml", "UTF-16LE").contains(renpy.packageName), "RenPy APK must contain its unique package ID");
            String resourceTable = apkText(apk, "resources.arsc", "UTF-16LE");
            truth(resourceTable.contains(renpy.packageName), "Ren'Py resource table namespace must match its generated package ID");
            truth(!resourceTable.contains("com.example.renpylocal"), "Ren'Py resource table must not retain its template namespace");
            truth(apk.getEntry("META-INF/CERT.RSA") == null, "the old template signature must be removed before signing");
        }
        if ("keep".equals(System.getProperty("apkforge.keepTests"))) {
            System.out.println("SMOKE_OUTPUT=" + folder.getAbsolutePath());
        } else {
            for (java.io.File child : java.util.Objects.requireNonNull(folder.listFiles())) child.delete();
            folder.delete();
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 3 && "--strip-internet".equals(args[0])) {
            java.nio.file.Path source = java.nio.file.Paths.get(args[1]);
            java.nio.file.Path target = java.nio.file.Paths.get(args[2]);
            byte[] manifest = java.nio.file.Files.readAllBytes(source);
            java.nio.file.Files.write(target, OfflinePackageFormat.removePermission(manifest, "android.permission.INTERNET"));
            return;
        }
        if (args.length == 6 && "--repack".equals(args[0])) {
            OfflineApkPackager.Prepared result = OfflineApkPackager.createUnsigned(new java.io.File(args[1]), new java.io.File(args[2]), args[3], args[4], new java.io.File(args[5]));
            System.out.println("APK_KIND=" + result.kind + " PACKAGE=" + result.packageName);
            return;
        }
        if (args.length == 6 && "--sign".equals(args[0])) {
            java.security.KeyStore keys = java.security.KeyStore.getInstance("PKCS12");
            try (java.io.InputStream in = new java.io.FileInputStream(args[3])) { keys.load(in, args[5].toCharArray()); }
            java.security.PrivateKey key = (java.security.PrivateKey) keys.getKey(args[4], args[5].toCharArray());
            java.security.cert.X509Certificate certificate = (java.security.cert.X509Certificate) keys.getCertificate(args[4]);
            com.android.apksig.ApkSigner.SignerConfig signer = new com.android.apksig.ApkSigner.SignerConfig.Builder("APK Forge test", key, java.util.Collections.singletonList(certificate)).build();
            new com.android.apksig.ApkSigner.Builder(java.util.Collections.singletonList(signer)).setInputApk(new java.io.File(args[1])).setOutputApk(new java.io.File(args[2]))
                    .setMinSdkVersion(21).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
            return;
        }
        equal("assets/x-game/x-images/x-title.png", OfflinePackageFormat.renpyAssetPath("images/title.png"));
        equal("assets/x-game/x-script.rpyc", OfflinePackageFormat.renpyAssetPath("script.rpyc"));
        boolean rejected = false;
        try { OfflinePackageFormat.renpyAssetPath("../outside.txt"); }
        catch (IllegalArgumentException expected) { rejected = true; }
        truth(rejected, "Ren'Py asset paths must not escape the game directory");
        String first = OfflinePackageFormat.packageFor("My Game", "com.example.mygame");
        equal(first, OfflinePackageFormat.packageFor("My Game", "com.example.mygame"));
        String originalSeed = OfflinePackageFormat.converterPackageSeed("com.example.mygame", "com.gamebridge.mobile.v5");
        equal(first, OfflinePackageFormat.packageFor("My Game", originalSeed));
        String standaloneSeed = OfflinePackageFormat.converterPackageSeed("com.example.mygame", "com.gamebridge.mobile.v5.standalone");
        truth(!first.equals(OfflinePackageFormat.packageFor("My Game", standaloneSeed)),
                "a standalone converter with a separate signing key must use different generated game IDs");
        String legacyPackage = legacyPackageFor("My Game", "com.example.mygame", "com.example.renpylocal");
        truth(!first.equals(legacyPackage), "V3-generated game IDs must not collide with V2 APKs signed by the previous certificate");
        truth(!first.equals(OfflinePackageFormat.packageFor("Another Game", "com.example.other")), "different games need different package IDs");
        String htmlPackage = OfflinePackageFormat.packageFor("Web Game", "com.example.web", "com.example.finalhtml");
        equal("21", Integer.toString(htmlPackage.length()));
        String installableHtmlPackage = OfflinePackageFormat.packageFor("My Android Game", "com.gamebridge.game", "com.example.finalhtml");
        truth(installableHtmlPackage.matches("com\\.af\\.a[a-f0-9]{13}"), "generated Android package segments must start with a letter");
        String installableRenpyPackage = OfflinePackageFormat.packageFor("My Android Game", "com.gamebridge.game");
        truth(installableRenpyPackage.matches("com\\.af\\.a[a-f0-9]{14}"), "generated Ren'Py package segments must start with a letter");
        truth(first.matches("com\\.af\\.a[a-f0-9]{14}"), "package ID must be valid and template-length");
        String oldId = "com.example.renpylocal";
        byte[] data = ("prefix:" + oldId + ";authority:" + oldId + ".fileprovider").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] changed = OfflinePackageFormat.replacePackage(data, oldId, first);
        truth(new String(changed, java.nio.charset.StandardCharsets.UTF_8).contains(first + ";authority:" + first + ".fileprovider"), "manifest authorities must track the generated package ID");
        String label = OfflinePackageFormat.fixedAsciiLabel("A Very Long Name For My Game", 20);
        equal("A Very Long Name For", label);
        equal("RenPy Tutorial", OfflinePackageFormat.cleanGameTitle("Download Ren'Py Tutorial (complete game ZIP)", "renpy-tutorial.zip"));
        equal("HTML browser", OfflinePackageFormat.cleanGameTitle("Download HTML browser game ZIP", "html-game.zip"));
        equal("Cool Browser", OfflinePackageFormat.cleanGameTitle("download.zip", "Cool Browser Game.zip"));
        byte[] text = OfflinePackageFormat.replaceFixedText("RenPy Local SDK Test".getBytes(java.nio.charset.StandardCharsets.UTF_8), "RenPy Local SDK Test", label);
        equal(label, new String(text, java.nio.charset.StandardCharsets.UTF_8));

        String base = assetsRoot();
        byte[] renpyManifest = apkEntry(base + "renpy-template.apk", "AndroidManifest.xml");
        OfflinePackageFormat.replacePackage(renpyManifest, "com.example.renpylocal", first);
        byte[] renpyResources = apkEntry(base + "renpy-template.apk", "resources.arsc");
        byte[] patchedRenpyResources = OfflinePackageFormat.replacePackage(renpyResources, "com.example.renpylocal", first);
        truth(!new String(patchedRenpyResources, java.nio.charset.StandardCharsets.UTF_16LE).contains("com.example.renpylocal"), "Ren'Py resource table package namespace must track the generated package ID");
        OfflinePackageFormat.replaceFixedText(patchedRenpyResources, "RenPy Local SDK Test", label);
        OfflinePackageFormat.replaceFixedText(apkEntry(base + "renpy-template.apk", "assets/x-android.json"), "RenPy Local SDK Test", label);
        String htmlId = OfflinePackageFormat.packageFor("Web Game", "com.example.web", "com.example.finalhtml");
        OfflinePackageFormat.replacePackage(apkEntry(base + "html-template.apk", "AndroidManifest.xml"), "com.example.finalhtml", htmlId);
        byte[] htmlManifest = apkEntry(base + "html-template.apk", "AndroidManifest.xml");
        OfflinePackageFormat.replaceFixedText(htmlManifest, "Final HTML Smoke", OfflinePackageFormat.fixedAsciiLabel("Web Game", 16));
        byte[] safeHtmlManifest = OfflinePackageFormat.disableDebugging(htmlManifest);
        truth(!OfflinePackageFormat.isDebuggable(safeHtmlManifest), "HTML templates with or without a debug flag must produce a non-debuggable manifest");
        String[] converterCandidates = {
                "android-app/app/build/outputs/apk/debug/app-debug.apk",
                "app/build/outputs/apk/debug/app-debug.apk"
        };
        for (String converterApk : converterCandidates) {
            if (!new java.io.File(converterApk).isFile()) continue;
            byte[] converterManifest = apkEntry(converterApk, "AndroidManifest.xml");
            truth(OfflinePackageFormat.containsPermission(converterManifest, "android.permission.INTERNET"), "the old converter shell has a network permission to remove");
            byte[] offlineManifest = OfflinePackageFormat.removePermission(converterManifest, "android.permission.INTERNET");
            truth(!OfflinePackageFormat.containsPermission(offlineManifest, "android.permission.INTERNET"), "standalone converter manifest must not request network access");
            break;
        }
        checkRepackaging();
        System.out.println("OfflinePackageFormat tests passed");
    }

    private static String legacyPackageFor(String appName, String requestedPackage, String templatePackage) throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(
                (appName.trim() + "\u0000" + requestedPackage.trim()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : hash) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
        return "com.af.a" + hex.substring(0, templatePackage.length() - "com.af.a".length());
    }
}
