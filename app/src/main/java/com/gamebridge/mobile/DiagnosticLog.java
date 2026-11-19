package com.gamebridge.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;

/** Local, bounded diagnostics containing only fixed event codes and numeric counters. */
final class DiagnosticLog {
    static final int MAX_LOG_BYTES = 256 * 1024;
    enum Event {
        APP_STARTED,
        ANALYSIS_COMPLETED, ANALYSIS_FAILED,
        DOWNLOAD_STARTED, DOWNLOAD_COMPLETED, DOWNLOAD_FAILED,
        SNAPSHOT_STARTED, SNAPSHOT_COMPLETED, SNAPSHOT_FAILED,
        CONVERSION_STARTED, HTML_COMPLETED, RENPY_COMPLETED, CONVERSION_FAILED,
        PACKAGER_BUILD_STARTED, PACKAGER_PROJECT_DETECTED, PACKAGER_TEMPLATE_READY,
        PACKAGER_UNSIGNED_READY, PACKAGER_SIGNING_STARTED, PACKAGER_SIGNING_COMPLETED, PACKAGER_FAILED,
        APK_INSTALL_FAILED, WEBVIEW_RENDERER_GONE, UNCAUGHT_FAILURE, DIAGNOSTIC_EXPORTED
    }

    private DiagnosticLog() { }

    static Thread.UncaughtExceptionHandler crashHandler(File filesDir, Thread.UncaughtExceptionHandler previous) {
        return new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread thread, Throwable failure) {
                try { recordFailure(filesDir, Event.UNCAUGHT_FAILURE, 0, failure); }
                finally { if (previous != null) previous.uncaughtException(thread, failure); }
            }
        };
    }

    static synchronized void installCrashHandler(File filesDir) {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        if (previous instanceof CapturingExceptionHandler) return;
        Thread.setDefaultUncaughtExceptionHandler(new CapturingExceptionHandler(filesDir, previous));
    }

    private static final class CapturingExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final File filesDir;
        private final Thread.UncaughtExceptionHandler previous;
        CapturingExceptionHandler(File filesDir, Thread.UncaughtExceptionHandler previous) {
            this.filesDir = filesDir;
            this.previous = previous;
        }
        @Override public void uncaughtException(Thread thread, Throwable failure) {
            crashHandler(filesDir, previous).uncaughtException(thread, failure);
        }
    }

    static synchronized void record(File filesDir, Event event, long metric1, long metric2) {
        if (filesDir == null || event == null) return;
        append(filesDir, Instant.now().toString() + "\t" + event.name() + "\t" + metric1 + "\t" + metric2 + "\n");
    }

    static synchronized void recordFailure(File filesDir, Event event, int stage, Throwable failure) {
        if (filesDir == null || event == null || failure == null) return;
        StringBuilder row = new StringBuilder(256).append(Instant.now()).append("\tFAILURE\t")
                .append(event.name()).append('\t').append(stage).append('\t')
                .append(safeToken(failure.getClass().getName()));
        StackTraceElement[] frames = failure.getStackTrace();
        if (frames != null) {
            for (int i = 0, written = 0; i < frames.length && written < 4; i++) {
                StackTraceElement frame = frames[i];
                row.append('\t').append(safeToken(frame.getClassName())).append('.')
                        .append(safeToken(frame.getMethodName())).append(':').append(Math.max(0, frame.getLineNumber()));
                written++;
            }
        }
        row.append('\n');
        append(filesDir, row.toString());
    }

    private static String safeToken(String value) {
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_.$-]", "_");
    }

    private static void append(File filesDir, String row) {
        try {
            File directory = new File(filesDir, "diagnostics");
            if (!directory.isDirectory() && !directory.mkdirs()) return;
            File file = new File(directory, "diagnostics.log");
            byte[] added = row.getBytes(StandardCharsets.UTF_8);
            if (!file.isFile() || file.length() + added.length <= MAX_LOG_BYTES) {
                try (FileOutputStream out = new FileOutputStream(file, true)) { out.write(added); }
                return;
            }
            byte[] previous = file.isFile() ? Files.readAllBytes(file.toPath()) : new byte[0];
            int keep = Math.max(0, Math.min(previous.length, MAX_LOG_BYTES - added.length));
            int start = previous.length - keep;
            while (start < previous.length && start > 0 && previous[start] != '\n') start++;
            byte[] output = new byte[previous.length - start + added.length];
            System.arraycopy(previous, start, output, 0, previous.length - start);
            System.arraycopy(added, 0, output, previous.length - start, added.length);
            Files.write(file.toPath(), output);
        } catch (Exception ignored) {
            // Diagnostics must never interfere with conversion or gameplay.
        }
    }

    static synchronized String read(File filesDir) {
        File file = new File(new File(filesDir, "diagnostics"), "diagnostics.log");
        try { return file.isFile() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : ""; }
        catch (Exception ignored) { return ""; }
    }

    static synchronized String prepareExport(File filesDir) {
        record(filesDir, Event.DIAGNOSTIC_EXPORTED, 0, 0);
        return read(filesDir);
    }

    static synchronized void clear(File filesDir) {
        File file = new File(new File(filesDir, "diagnostics"), "diagnostics.log");
        try { if (file.isFile()) Files.delete(file.toPath()); }
        catch (Exception ignored) { }
    }
}
