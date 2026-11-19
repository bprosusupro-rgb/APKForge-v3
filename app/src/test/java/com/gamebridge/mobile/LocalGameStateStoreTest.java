package com.gamebridge.mobile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class LocalGameStateStoreTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("apkforge-local-state-").toFile();
        try {
            String origin = "https://games.example/play";
            String state = "{\"slot\":\"chapter-4\",\"score\":27}";
            LocalGameStateStore.save(root, origin, state);
            require(state.equals(LocalGameStateStore.load(root, origin)), "saved game progress must be readable after an app restart");
            require(LocalGameStateStore.load(root, "https://other.example/play") == null, "one website must not receive another origin's saved progress");
            LocalGameStateStore.save(root, "file://", "{\"offline\":\"slot-2\"}");
            require("{\"offline\":\"slot-2\"}".equals(LocalGameStateStore.load(root, "file:///android_asset/www/index.html")), "offline HTML save origins must survive Android WebView file URLs");
            byte[] offlineBackup = LocalGameStateStore.exportBackup(root, "file:///android_asset/www/index.html");
            LocalGameStateStore.save(root, "file://", "{\"offline\":\"later\"}");
            LocalGameStateStore.restoreBackup(root, "file://offline", offlineBackup);
            require("{\"offline\":\"slot-2\"}".equals(LocalGameStateStore.load(root, "file://")),
                    "offline HTML progress must be exportable and restorable under its local-file origin");
            require(!new String(Files.readAllBytes(LocalGameStateStore.stateFile(root, origin).toPath()), StandardCharsets.UTF_8).contains(origin), "saved state files must not disclose the original website address");
            byte[] backup = LocalGameStateStore.exportBackup(root, origin);
            LocalGameStateStore.save(root, origin, "{\"slot\":\"later\"}");
            LocalGameStateStore.restoreBackup(root, origin, backup);
            require(state.equals(LocalGameStateStore.load(root, origin)), "an exported save backup must restore the exact earlier progress");
            LocalGameStateStore.save(root, "https://other.example/play", "{\"slot\":\"other-site\"}");
            expectFailure(() -> LocalGameStateStore.restoreBackup(root, "https://other.example/play", backup),
                    "a save backup must not be restorable into a different website origin");
            require("{\"slot\":\"other-site\"}".equals(LocalGameStateStore.load(root, "https://other.example/play")),
                    "a rejected cross-origin backup must leave the target site's save untouched");
            System.out.println("LocalGameStateStore tests passed");
        } finally {
            File stateFile = new File(root, "web-saves");
            if (stateFile.isDirectory()) for (File child : stateFile.listFiles()) child.delete();
            stateFile.delete();
            root.delete();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private interface CheckedAction { void run() throws Exception; }

    private static void expectFailure(CheckedAction action, String message) throws Exception {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(message);
    }
}
