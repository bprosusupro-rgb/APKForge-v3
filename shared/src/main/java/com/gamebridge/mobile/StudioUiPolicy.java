package com.gamebridge.mobile;

/** Persisted presentation choice for the Studio and Classic layouts. */
public final class StudioUiPolicy {
    public static final boolean DEFAULT_STUDIO_MODE = true;
    public static final String PREFERENCE_KEY = "studio_ui_enabled";

    private StudioUiPolicy() { }

    public static boolean useStudio(boolean preferenceValue) {
        return preferenceValue;
    }

    public static String label(boolean studioEnabled) {
        return studioEnabled ? "Studio" : "Classic";
    }
}
