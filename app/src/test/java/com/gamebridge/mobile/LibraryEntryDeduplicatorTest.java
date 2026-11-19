package com.gamebridge.mobile;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public final class LibraryEntryDeduplicatorTest {
    public static void main(String[] args) throws Exception {
        Class<?> helper;
        try {
            helper = Class.forName("com.gamebridge.mobile.LibraryEntryDeduplicator");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("library entries should be automatically deduplicated by APK package ID", missing);
        }
        Method shouldKeep = helper.getDeclaredMethod("shouldKeepPackage", Set.class, String.class);
        shouldKeep.setAccessible(true);
        Set<String> seen = new HashSet<>();
        require((boolean) shouldKeep.invoke(null, seen, "com.af.a123"), "first APK identity must remain visible");
        require(!(boolean) shouldKeep.invoke(null, seen, "com.af.a123"), "rebuilding the same APK identity must not create another library row");
        require((boolean) shouldKeep.invoke(null, seen, "com.af.a456"), "different games must remain separate entries");
        require((boolean) shouldKeep.invoke(null, seen, null), "unreadable legacy APK identity must not hide unrelated builds");
        require((boolean) shouldKeep.invoke(null, seen, null), "unknown identities must be preserved independently");
        System.out.println("LibraryEntryDeduplicator tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
