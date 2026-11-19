package com.gamebridge.mobile;

public final class WebCandidateSelectionTest {
    private static void equal(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void main(String[] args) {
        WebCandidateSelection selection = new WebCandidateSelection();
        WebCandidateSelection.Candidate html = new WebCandidateSelection.Candidate(
                "https://games.example/play", "Game", false, WebGameLinkAnalyzer.Format.HTML);
        WebCandidateSelection.Candidate renpy = new WebCandidateSelection.Candidate(
                "https://cdn.example/game.zip", "Game", true, WebGameLinkAnalyzer.Format.RENPY);

        equal(null, selection.getSelected());
        selection.select(html);
        equal("https://games.example/play", selection.getSelected().url);
        equal(true, selection.isSelected("https://games.example/play"));
        equal(false, selection.isSelected("https://cdn.example/game.zip"));
        selection.select(renpy);
        equal("https://cdn.example/game.zip", selection.getSelected().url);
        equal(false, selection.isSelected("https://games.example/play"));
        equal(true, selection.isSelected("https://cdn.example/game.zip"));
        equal(true, selection.getSelected().archive);
        selection.clear();
        equal(null, selection.getSelected());
        equal(false, selection.isSelected("https://cdn.example/game.zip"));
        boolean rejectedEmpty = false;
        try { selection.select(null); }
        catch (IllegalArgumentException expected) { rejectedEmpty = true; }
        equal(true, rejectedEmpty);
        System.out.println("WebCandidateSelection tests passed");
    }
}
