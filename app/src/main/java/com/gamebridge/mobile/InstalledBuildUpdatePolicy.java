package com.gamebridge.mobile;

/** Decides whether the selected library APK is different from the installed build. */
final class InstalledBuildUpdatePolicy {
    private InstalledBuildUpdatePolicy() { }

    static boolean shouldReplace(String installedSha256, String candidateSha256) {
        return installedSha256 == null || candidateSha256 == null
                || !installedSha256.equalsIgnoreCase(candidateSha256);
    }
}
