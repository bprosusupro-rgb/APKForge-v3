package com.gamebridge.mobile;

import java.lang.reflect.Method;

public final class TinyTouchModelTest {
    public static void main(String[] args) throws Exception {
        Method model;
        try {
            model = Class.forName("com.apkforge.input.TinyTouchModel")
                    .getMethod("shouldSuggestOverlay", int.class, int.class, int.class, int.class);
        } catch (ReflectiveOperationException missing) {
            throw new AssertionError("The on-device touch suggestion model is missing", missing);
        }

        expect(true, model, 1, 0, 1, 0, "canvas with keyboard hints suggests controls");
        expect(false, model, 0, 6, 0, 0, "a page with native touch targets does not suggest a pad");
        expect(false, model, 1, 0, 1, 1, "a page focused on text entry does not suggest game controls");
        expect(false, model, 0, 0, 0, 0, "ordinary pages do not get a false game suggestion");
        expect(false, model, -1, -1, -1, -1, "negative feature counts are clamped safely");
        System.out.println("TinyTouchModel tests passed");
    }

    private static void expect(boolean expected, Method model, int canvases, int touchTargets,
                               int keyboardHints, int textInputs, String description) throws Exception {
        boolean actual = (Boolean) model.invoke(null, canvases, touchTargets, keyboardHints, textInputs);
        if (actual != expected) throw new AssertionError(description + ": expected " + expected + ", got " + actual);
    }
}
