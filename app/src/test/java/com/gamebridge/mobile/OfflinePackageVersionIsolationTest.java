package com.gamebridge.mobile;

/** Prevents v4-built games from colliding with v3 games signed by another app UID. */
public final class OfflinePackageVersionIsolationTest {
    public static void main(String[] args) {
        String packageName = OfflinePackageFormat.packageFor("My Android Game", "com.gamebridge.game");
        if (!"com.af.abe0595a4571842".equals(packageName)) {
            throw new AssertionError("v4 must produce its version-specific game package ID; got " + packageName);
        }
        if ("com.af.a9e0375ce2e49e".equals(packageName)) {
            throw new AssertionError("v4 must not reuse the package ID of the v3-generated HTML test game");
        }
        System.out.println("Offline package version isolation tests passed");
    }
}
