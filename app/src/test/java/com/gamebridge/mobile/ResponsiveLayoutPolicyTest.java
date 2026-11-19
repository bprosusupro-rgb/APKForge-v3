package com.gamebridge.mobile;

import java.lang.reflect.Method;

public final class ResponsiveLayoutPolicyTest {
    public static void main(String[] args) throws Exception {
        Class<?> policy;
        try {
            policy = Class.forName("com.gamebridge.mobile.ResponsiveLayoutPolicy");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("responsive layout policy should be implemented", missing);
        }
        Method isTablet = policy.getDeclaredMethod("isTabletMode", int.class, int.class);
        isTablet.setAccessible(true);
        require((boolean) isTablet.invoke(null, 1000, 500), "wide landscape windows should use tablet navigation");
        require((boolean) isTablet.invoke(null, 500, 600), "tablet-sized devices in portrait should retain tablet navigation");
        require(!(boolean) isTablet.invoke(null, 800, 500), "phone-sized windows should use the compact layout");
        System.out.println("ResponsiveLayoutPolicy tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
