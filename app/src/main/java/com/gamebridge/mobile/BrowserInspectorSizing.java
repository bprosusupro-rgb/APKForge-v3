package com.gamebridge.mobile;

/** Keeps the game inspector usable without taking the page off screen. */
final class BrowserInspectorSizing {
    private static final int MIN_PHONE_HEIGHT_DP = 104;
    private static final int MAX_PHONE_HEIGHT_DP = 560;
    private static final int MIN_TABLET_WIDTH_DP = 256;
    private static final int MAX_TABLET_WIDTH_DP = 520;

    private BrowserInspectorSizing() {}

    static int phoneHeight(int requestedHeightDp, int screenHeightDp) {
        int maximum = Math.max(MIN_PHONE_HEIGHT_DP,
                Math.min(MAX_PHONE_HEIGHT_DP, screenHeightDp * 74 / 100));
        return Math.max(MIN_PHONE_HEIGHT_DP, Math.min(maximum, requestedHeightDp));
    }

    static int tabletWidth(int requestedWidthDp, int screenWidthDp) {
        int maximum = Math.max(MIN_TABLET_WIDTH_DP,
                Math.min(MAX_TABLET_WIDTH_DP, screenWidthDp * 46 / 100));
        return Math.max(MIN_TABLET_WIDTH_DP, Math.min(maximum, requestedWidthDp));
    }
}
