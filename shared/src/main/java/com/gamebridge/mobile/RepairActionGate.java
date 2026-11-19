package com.gamebridge.mobile;

import java.util.Locale;

/** Maps model output to the tiny set of APK Forge actions that are safe to offer. */
public final class RepairActionGate {
    public enum Action {
        NONE,
        RETRY_CAPTURE,
        REBUILD_WITH_TOUCH_SUPPORT,
        REBUILD_WITH_LOCAL_SAVE_SUPPORT,
        OPEN_WEBSITE_WORKSPACE,
        OPEN_OFFICIAL_VERSION_SEARCH,
        OPEN_EXE_HASH_CHECK,
        OPEN_EXE_VERSION_LOOKUP,
        OPEN_COMPATIBILITY_GUIDE
    }

    private RepairActionGate() { }

    public static Action fromModelOutput(String output) {
        if (output == null) return Action.NONE;
        String normalized = output.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "retry_capture": return Action.RETRY_CAPTURE;
            case "rebuild_with_touch_support": return Action.REBUILD_WITH_TOUCH_SUPPORT;
            case "rebuild_with_local_save_support": return Action.REBUILD_WITH_LOCAL_SAVE_SUPPORT;
            case "open_website_workspace": return Action.OPEN_WEBSITE_WORKSPACE;
            case "open_official_version_search": return Action.OPEN_OFFICIAL_VERSION_SEARCH;
            case "open_exe_hash_check": return Action.OPEN_EXE_HASH_CHECK;
            case "open_exe_version_lookup": return Action.OPEN_EXE_VERSION_LOOKUP;
            case "open_compatibility_guide": return Action.OPEN_COMPATIBILITY_GUIDE;
            default: return Action.NONE;
        }
    }
}
