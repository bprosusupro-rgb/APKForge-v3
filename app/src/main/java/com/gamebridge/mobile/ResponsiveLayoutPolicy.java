package com.gamebridge.mobile;

final class ResponsiveLayoutPolicy {
    private ResponsiveLayoutPolicy() { }

    static boolean isTabletMode(int screenWidthDp, int smallestScreenWidthDp) {
        return screenWidthDp >= 840 || smallestScreenWidthDp >= 600;
    }
}
