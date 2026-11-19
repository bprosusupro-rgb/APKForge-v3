package com.gamebridge.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Exercises APK ZIP assembly without Android signing or a device. */
public final class OfflineApkPackagerSidecarTest {
    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("apkforge-sidecar-apk-test").toFile();
        File template = new File(dir, "template.apk");
        File source = new File(dir, "source.zip");
        File embedded = new File(dir, "embedded.apk");
        File separate = new File(dir, "separate.apk");
        try {
            createTemplate(template);
            createGame(source);
            Object project = project();
            copyTemplate(template, source, embedded, project, false);
            copyTemplate(template, source, separate, project, true);
            try (ZipFile apk = new ZipFile(embedded)) {
                require(apk.getEntry("assets/x-game/x-script.rpyc") != null, "small games must remain embedded");
                require(apk.getEntry("assets/apkforge-external-game-data.flag") == null, "embedded builds must not ask for sidecar data");
            }
            try (ZipFile apk = new ZipFile(separate)) {
                require(apk.getEntry("assets/x-game/x-script.rpyc") == null, "large game assets must stay out of the installable APK");
                require(apk.getEntry("assets/apkforge-external-game-data.flag") != null, "separate-data APKs must activate the import launcher");
            }
            System.out.println("OfflineApkPackager sidecar tests passed");
        } finally {
            template.delete(); source.delete(); embedded.delete(); separate.delete(); dir.delete();
        }
    }

    private static void copyTemplate(File template, File source, File output, Object project, boolean external) throws Exception {
        Method method = OfflineApkPackager.class.getDeclaredMethod("copyTemplateAndGame", File.class,
                project.getClass(), File.class, String.class, String.class, String.class, String.class,
                boolean.class, File.class);
        method.setAccessible(true);
        method.invoke(null, template, project, source, "com.example.renpylocal",
                OfflinePackageFormat.packageFor("Test", "com.gamebridge.game", "com.example.renpylocal"),
                "RenPy Local SDK Test", "Test", external, output);
    }

    private static Object project() throws Exception {
        Class<?> type = Class.forName("com.gamebridge.mobile.OfflineApkPackager$Project");
        Constructor<?> constructor = type.getDeclaredConstructor(boolean.class, String.class, String.class, String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(true, "Example", "Example/game/", null, null);
    }

    private static void createTemplate(File file) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            entry(zip, "AndroidManifest.xml", "<manifest package=\"com.example.renpylocal\"><application/></manifest>");
            entry(zip, "assets/x-game/x-template-placeholder", "placeholder");
        }
    }

    private static void createGame(File file) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            entry(zip, "Example/game/script.rpyc", "compiled script");
        }
    }

    private static void entry(ZipOutputStream zip, String name, String value) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
