package com.gamebridge.mobile;

public final class RenPyPackageLimitsTest {
    public static void main(String[] args) {
        long gib = 1024L * 1024L * 1024L;
        if (RenPyPackageLimits.MAX_INPUT_ARCHIVE_BYTES < 5L * gib) {
            throw new AssertionError("5 GiB source archives must be accepted for processing");
        }
        if (RenPyPackageLimits.MAX_EXPANDED_GAME_BYTES < 5L * gib) {
            throw new AssertionError("5 GiB expanded projects must be accepted for processing");
        }
        if (RenPyPackageLimits.MAX_GAME_FILE_BYTES >= 4L * gib) {
            throw new AssertionError("individual APK entries must stay below the ZIP64 sentinel");
        }
        if (RenPyPackageLimits.MAX_SIGNED_APK_BYTES >= 2L * gib) {
            throw new AssertionError("APK output must retain headroom below Ren'Py's 2 GiB ceiling");
        }
        if (!RenPyPackageLimits.oversizedApkMessage().contains("separate offline ZIP")) {
            throw new AssertionError("oversized games must explain the local sidecar-data route");
        }
        System.out.println("RenPyPackageLimits tests passed");
    }
}
