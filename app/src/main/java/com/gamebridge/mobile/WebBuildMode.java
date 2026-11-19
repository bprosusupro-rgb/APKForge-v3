package com.gamebridge.mobile;

/** The two honest ways to turn a browser game page into an Android app. */
final class WebBuildMode {
    enum Mode { OFFLINE, ONLINE }

    private WebBuildMode() { }

    static String actionLabel(Mode mode) {
        return mode == Mode.ONLINE ? "Build online wrapper" : "Build offline snapshot";
    }

    static String description(Mode mode) {
        return mode == Mode.ONLINE
                ? "Opens the live game page. An internet connection is required to play. HTML localStorage progress is checkpointed privately on this device; server-only saves still need the website account."
                : "Saves the page and loaded files for offline play. HTML localStorage progress is saved on this device; server-only game features may still need internet.";
    }
}
