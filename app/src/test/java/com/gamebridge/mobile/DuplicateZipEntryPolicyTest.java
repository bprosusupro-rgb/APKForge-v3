package com.gamebridge.mobile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class DuplicateZipEntryPolicyTest {
    public static void main(String[] args) throws Exception {
        Class<?> registry;
        try {
            registry = Class.forName("com.gamebridge.mobile.ZipEntryDeduplicator");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("APK merge must tolerate duplicate ZIP paths", missing);
        }
        Constructor<?> constructor = registry.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object entries = constructor.newInstance();
        Method claim = registry.getDeclaredMethod("claim", String.class);
        claim.setAccessible(true);
        require((boolean) claim.invoke(entries, "assets/www/index.html"), "the first entry should be kept");
        require(!(boolean) claim.invoke(entries, "assets/www/index.html"), "a repeated path should be skipped rather than throwing ZipException");
        require(!(boolean) claim.invoke(entries, "assets//www/./index.html"), "equivalent ZIP paths should not create duplicate output paths");
        require(!(boolean) claim.invoke(entries, "assets\\www\\index.html"), "backslash-separated aliases should not create duplicate output paths");
        require((boolean) claim.invoke(entries, "assets/www/next.html"), "different paths should still be kept");
        System.out.println("DuplicateZipEntryPolicy tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
