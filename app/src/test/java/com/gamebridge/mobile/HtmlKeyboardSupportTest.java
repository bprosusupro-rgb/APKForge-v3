package com.gamebridge.mobile;

public final class HtmlKeyboardSupportTest {
    private static void expect(boolean expected, boolean actual, String message) {
        if (expected != actual) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        expect(true, HtmlKeyboardSupport.shouldShowKeyboard("INPUT", "text", false), "text inputs open the keyboard");
        expect(true, HtmlKeyboardSupport.shouldShowKeyboard("input", "email", false), "email inputs open the keyboard");
        expect(true, HtmlKeyboardSupport.shouldShowKeyboard("TEXTAREA", "", false), "multiline fields open the keyboard");
        expect(true, HtmlKeyboardSupport.shouldShowKeyboard("DIV", "", true), "contenteditable game text fields open the keyboard");
        expect(false, HtmlKeyboardSupport.shouldShowKeyboard("INPUT", "button", false), "buttons do not summon the keyboard");
        expect(false, HtmlKeyboardSupport.shouldShowKeyboard("INPUT", "checkbox", false), "checkboxes do not summon the keyboard");
        expect(false, HtmlKeyboardSupport.shouldShowKeyboard("SELECT", "", false), "dropdowns keep their native picker behavior");
    }
}
