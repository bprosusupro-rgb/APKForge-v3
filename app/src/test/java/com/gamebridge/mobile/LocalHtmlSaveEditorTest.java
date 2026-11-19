package com.gamebridge.mobile;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Regression coverage for APK Forge's offline HTML save editor. */
public final class LocalHtmlSaveEditorTest {
    @Test public void offlineSaveEditorFeatureExists() throws Exception {
        feature();
    }

    @Test public void onlinePagesStayReadOnlyAndCurrencyHintIsOnlyANameMatch() throws Exception {
        Class<?> editor = feature();
        assertFalse((Boolean) editor.getMethod("mayEditOnlinePage", String.class).invoke(null, "https://game.example/play"));
        assertFalse((Boolean) editor.getMethod("mayEditOnlinePage", String.class).invoke(null, "file:///game/index.html"));
        assertTrue((Boolean) editor.getMethod("looksLikeCurrency", String.class).invoke(null, "/player/softCurrency"));
        assertFalse((Boolean) editor.getMethod("looksLikeCurrency", String.class).invoke(null, "/player/score"));
    }

    @Test public void offlineBackupCanBeInspectedEditedAndExported() throws Exception {
        File root = Files.createTempDirectory("apkforge-save-lab-").toFile();
        File target = Files.createTempDirectory("apkforge-save-target-").toFile();
        try {
            String origin = "file:///android_asset/www/index.html";
            LocalGameStateStore.save(root, origin,
                    "{\"profile\":\"{\\\"coins\\\":12,\\\"inventory\\\":{\\\"gems\\\":4}}\",\"level\":\"3\"}");
            byte[] original = LocalGameStateStore.exportBackup(root, origin);
            invoke("importBackup", new Class[]{File.class, byte[].class}, root, original);

            @SuppressWarnings("unchecked")
            Map<String, String> fields = (Map<String, String>) invoke("numericFields", new Class[]{File.class}, root);
            assertEquals("12", fields.get("/profile/coins"));
            assertEquals("4", fields.get("/profile/inventory/gems"));
            assertEquals("3", fields.get("/level"));

            Map<String, String> changes = new LinkedHashMap<>();
            changes.put("/profile/coins", "999");
            invoke("applyValues", new Class[]{File.class, Map.class}, root, changes);
            byte[] edited = (byte[]) invoke("exportBackup", new Class[]{File.class}, root);
            LocalGameStateStore.restoreBackup(target, "file:///android_asset/www/index.html", edited);
            String restored = LocalGameStateStore.load(target, "file:///android_asset/www/index.html");
            assertTrue("edited currency is kept as the game's localStorage string",
                    restored.contains("\\\"coins\\\":999"));
            assertTrue("other save values are preserved", restored.contains("\\\"gems\\\":4"));
        } finally {
            deleteTree(root);
            deleteTree(target);
        }
    }

    @Test public void onlineBackupsAreRejectedByTheOfflineEditor() throws Exception {
        File root = Files.createTempDirectory("apkforge-save-lab-online-").toFile();
        try {
            LocalGameStateStore.save(root, "https://game.example/play", "{\"coins\":\"10\"}");
            byte[] onlineBackup = LocalGameStateStore.exportBackup(root, "https://game.example/play");
            expectFailure("importBackup", new Class[]{File.class, byte[].class}, root, onlineBackup);
        } finally {
            deleteTree(root);
        }
    }

    @Test public void invalidOrUnknownNumericEditsDoNotPartiallyChangeSave() throws Exception {
        File root = Files.createTempDirectory("apkforge-save-lab-invalid-").toFile();
        try {
            String origin = "file:///game/index.html";
            LocalGameStateStore.save(root, origin, "{\"coins\":\"10\",\"name\":\"hero\"}");
            byte[] original = LocalGameStateStore.exportBackup(root, origin);
            invoke("importBackup", new Class[]{File.class, byte[].class}, root, original);
            Map<String, String> invalid = new LinkedHashMap<>();
            invalid.put("/coins", "500");
            invalid.put("/missing", "1");
            expectFailure("applyValues", new Class[]{File.class, Map.class}, root, invalid);
            assertEquals("10", ((Map<?, ?>) invoke("numericFields", new Class[]{File.class}, root)).get("/coins"));
            invalid.clear();
            invalid.put("/coins", "NaN");
            expectFailure("applyValues", new Class[]{File.class, Map.class}, root, invalid);
        } finally {
            deleteTree(root);
        }
    }

    @Test public void aiPromptContainsOnlyLocalSaveFieldsAndNeverTheOrigin() throws Exception {
        File root = Files.createTempDirectory("apkforge-save-lab-prompt-").toFile();
        try {
            String origin = "file:///private/path/not-for-the-model.html";
            LocalGameStateStore.save(root, origin, "{\"coins\":\"10\"}");
            invoke("importBackup", new Class[]{File.class, byte[].class}, root,
                    LocalGameStateStore.exportBackup(root, origin));
            String prompt = (String) invoke("analysisPrompt", new Class[]{File.class}, root);
            assertTrue(prompt.contains("/coins"));
            assertTrue(prompt.contains("10"));
            assertFalse(prompt.contains("private/path"));
            assertTrue(prompt.toLowerCase().contains("offline"));
            assertTrue(prompt.length() <= 1_000);
        } finally {
            deleteTree(root);
        }
    }

    private static Class<?> feature() {
        try { return Class.forName("com.gamebridge.mobile.OfflineHtmlSaveEditor"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("Offline HTML save editor is not implemented yet", missing); }
    }

    private static Object invoke(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = feature().getMethod(name, parameterTypes);
        try { return method.invoke(null, args); }
        catch (InvocationTargetException wrapped) {
            Throwable cause = wrapped.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw wrapped;
        }
    }

    private static void expectFailure(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        try { invoke(name, parameterTypes, args); }
        catch (IllegalArgumentException | SecurityException expected) { return; }
        fail(name + " should reject unsafe or invalid input");
    }

    private static void deleteTree(File root) {
        File[] children = root.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        root.delete();
    }
}
