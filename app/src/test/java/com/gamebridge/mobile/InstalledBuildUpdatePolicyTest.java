package com.gamebridge.mobile;

public final class InstalledBuildUpdatePolicyTest {
    public static void main(String[] args) {
        require(!InstalledBuildUpdatePolicy.shouldReplace("abc123", "ABC123"),
                "an identical already-installed APK should launch without reinstalling");
        require(InstalledBuildUpdatePolicy.shouldReplace("old-build", "new-build"),
                "a rebuilt APK with the same package identity must be offered as an in-place update");
        require(InstalledBuildUpdatePolicy.shouldReplace(null, "new-build"),
                "an unreadable or missing installed build must not prevent installing the selected APK");
        System.out.println("Installed build update policy tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
