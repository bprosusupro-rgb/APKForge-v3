package com.gamebridge.mobile;

public final class BrowserInspectorSizingTest {
    public static void main(String[] args) {
        if (BrowserInspectorSizing.phoneHeight(250, 800) != 250)
            throw new AssertionError("phone inspector resize should preserve a valid requested height");
        if (BrowserInspectorSizing.phoneHeight(10, 800) != 104)
            throw new AssertionError("phone inspector must not shrink below its usable collapsed height");
        if (BrowserInspectorSizing.phoneHeight(900, 400) != 296)
            throw new AssertionError("phone inspector must leave room for the page on a short landscape screen");
        if (BrowserInspectorSizing.tabletWidth(320, 1200) != 320)
            throw new AssertionError("tablet inspector should honor its requested width inside the limits");
        if (BrowserInspectorSizing.tabletWidth(900, 800) != 368)
            throw new AssertionError("tablet inspector should never consume most of a compact tablet");
        if (BrowserInspectorSizing.tabletWidth(40, 1200) != 256)
            throw new AssertionError("tablet inspector must retain a usable minimum width");
        System.out.println("BrowserInspectorSizing tests passed");
    }
}
