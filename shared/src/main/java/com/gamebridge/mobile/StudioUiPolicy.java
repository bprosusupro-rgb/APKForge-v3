package com.gamebridge.mobile;

/** Persisted presentation choice for the opt-in Studio redesign. */
public final class StudioUiPolicy {
    public static final String PREFERENCE_KEY = "studio_ui_enabled";

    private StudioUiPolicy() { }

    public static boolean useStudio(boolean preferenceValue) {
        return preferenceValue;
    }

    public static String label(boolean studioEnabled) {
        return studioEnabled ? "Studio" : "Classic";
    }
}
