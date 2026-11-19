package com.apkforge.input;

/** Small offline linear model that recommends (but never forces) a keyboard-style touch pad. */
public final class TinyTouchModel {
    private TinyTouchModel() { }

    public static boolean shouldSuggestOverlay(int canvasCount, int touchTargetCount,
                                               int keyboardHintCount, int textInputCount) {
        double score = -1.10
                + 1.35 * clamp(canvasCount, 1)
                + 1.25 * clamp(keyboardHintCount, 1)
                - 0.45 * clamp(touchTargetCount, 6)
                - 1.40 * clamp(textInputCount, 2);
        return score >= 0.75;
    }

    private static int clamp(int value, int maximum) {
        return Math.max(0, Math.min(value, maximum));
    }
}
