package com.gamebridge.mobile;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

public final class DiagnosticLogTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("apkforge-diagnostics-test").toFile();
        try {
            DiagnosticLog.record(root, DiagnosticLog.Event.APP_STARTED, 36, 720);
            DiagnosticLog.record(root, DiagnosticLog.Event.SNAPSHOT_COMPLETED, 12, 4096);
            DiagnosticLog.record(root, DiagnosticLog.Event.PACKAGER_SIGNING_STARTED, 3, 0);
            String log = DiagnosticLog.read(root);
            if (!log.contains("APP_STARTED\t36\t720")) throw new AssertionError("safe numeric app metrics missing");
            if (!log.contains("SNAPSHOT_COMPLETED\t12\t4096")) throw new AssertionError("safe snapshot counters missing");
            if (!log.contains("PACKAGER_SIGNING_STARTED\t3\t0")) throw new AssertionError("packaging stage breadcrumb missing");
            if (log.contains("https://") || log.contains("example.com") || log.contains("index.html")) throw new AssertionError("website data leaked into log");
            DiagnosticLog.recordFailure(root, DiagnosticLog.Event.SNAPSHOT_FAILED, 2,
                    new IllegalStateException("https://private.example/game?token=secret"));
            log = DiagnosticLog.read(root);
            if (!log.contains("FAILURE\tSNAPSHOT_FAILED\t2\tjava.lang.IllegalStateException")) throw new AssertionError("failure type and capture stage missing");
            if (!log.contains("DiagnosticLogTest.main")) throw new AssertionError("useful failure code location missing");
            if (log.contains("private.example") || log.contains("token=secret")) throw new AssertionError("exception message leaked website data");
            File emptyRoot = Files.createTempDirectory("apkforge-diagnostics-export-test").toFile();
            try {
                String firstExport = DiagnosticLog.prepareExport(emptyRoot);
                if (!firstExport.contains("DIAGNOSTIC_EXPORTED\t0\t0")) throw new AssertionError("first export omitted its event or produced an empty file");
            } finally {
                delete(emptyRoot);
            }
            boolean[] delegated = {false};
            Thread.UncaughtExceptionHandler crashHandler = DiagnosticLog.crashHandler(root,
                    (thread, failure) -> delegated[0] = true);
            crashHandler.uncaughtException(Thread.currentThread(),
                    new RuntimeException("https://private.example/crash?session=hidden"));
            log = DiagnosticLog.read(root);
            if (!log.contains("FAILURE\tUNCAUGHT_FAILURE\t0\tjava.lang.RuntimeException")) throw new AssertionError("uncaught crash breadcrumb missing");
            if (!delegated[0]) throw new AssertionError("original Android crash handler was not called");
            if (log.contains("private.example") || log.contains("session=hidden")) throw new AssertionError("uncaught error message leaked website data");
            DiagnosticLog.clear(root);
            if (!DiagnosticLog.read(root).isEmpty()) throw new AssertionError("clear did not empty the log");
            for (int i = 0; i < 10000; i++) DiagnosticLog.record(root, DiagnosticLog.Event.DOWNLOAD_COMPLETED, i, 0);
            if (new File(root, "diagnostics/diagnostics.log").length() > DiagnosticLog.MAX_LOG_BYTES) throw new AssertionError("log exceeded rotation limit");
            System.out.println("DiagnosticLog tests passed");
        } finally {
            delete(root);
        }
    }

    private static void delete(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
