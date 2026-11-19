package com.gamebridge.mobile;

public final class WebGameLinkAnalyzerTest {
    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
    }

    public static void main(String[] args) {
        equal("https://games.example/play", WebGameLinkAnalyzer.normalizeInput("games.example/play"));
        equal("https://html.duckduckgo.com/html/?q=renpy+visual+novel+download", WebGameLinkAnalyzer.searchUrl("renpy visual novel download"));
        equal(WebGameLinkAnalyzer.Format.RENPY, WebGameLinkAnalyzer.inferFormat("https://cdn.example/game.zip", "Download Windows game", "Ren'Py visual novel download", 1));
        equal(WebGameLinkAnalyzer.Format.UNKNOWN, WebGameLinkAnalyzer.inferFormat("https://cdn.example/game.zip", "Download", "This page mentions Ren'Py and HTML games", 4));
        equal(WebGameLinkAnalyzer.Format.HTML, WebGameLinkAnalyzer.inferFormat("https://cdn.example/game.zip", "HTML game ZIP", "", 1));
        equal(WebGameLinkAnalyzer.Format.UNKNOWN, WebGameLinkAnalyzer.inferFormat("https://cdn.example/setup.exe", "Windows installer", "", 1));
        WebGameLinkAnalyzer.validateOverride(WebGameLinkAnalyzer.Format.HTML, WebGameLinkAnalyzer.Format.HTML);
        WebGameLinkAnalyzer.validateOverride(WebGameLinkAnalyzer.Format.RENPY, WebGameLinkAnalyzer.Format.UNKNOWN);
        boolean mismatch = false;
        try { WebGameLinkAnalyzer.validateOverride(WebGameLinkAnalyzer.Format.HTML, WebGameLinkAnalyzer.Format.RENPY); }
        catch (IllegalArgumentException expected) { mismatch = true; }
        if (!mismatch) throw new AssertionError("manual format choice must reject archives that do not match the selected runtime");
        System.out.println("WebGameLinkAnalyzer tests passed");
    }
}
