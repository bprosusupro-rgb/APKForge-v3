package com.gamebridge.mobile;

import java.util.Locale;

public final class HtmlKeyboardSupport {
    private HtmlKeyboardSupport() {}

    public static boolean shouldShowKeyboard(String tagName, String inputType, boolean contentEditable) {
        if (contentEditable) return true;
        if (tagName == null) return false;
        String tag = tagName.toUpperCase(Locale.ROOT);
        if ("TEXTAREA".equals(tag)) return true;
        if (!"INPUT".equals(tag)) return false;
        String type = inputType == null || inputType.trim().isEmpty()
                ? "text" : inputType.trim().toLowerCase(Locale.ROOT);
        switch (type) {
            case "text": case "search": case "email": case "url": case "tel":
            case "password": case "number":
                return true;
            default:
                return false;
        }
    }
}
