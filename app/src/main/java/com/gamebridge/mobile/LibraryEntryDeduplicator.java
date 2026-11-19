package com.gamebridge.mobile;

import java.util.Set;

final class LibraryEntryDeduplicator {
    private LibraryEntryDeduplicator() { }

    /** Returns true for the first visible build of a package; unreadable legacy APKs stay independent. */
    static boolean shouldKeepPackage(Set<String> seenPackageIds, String packageId) {
        if (packageId == null || packageId.trim().isEmpty()) return true;
        return seenPackageIds.add(packageId);
    }
}
