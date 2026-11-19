package com.gamebridge.mobile;

/** Keeps exactly one discovered web-game candidate active for the primary conversion action. */
final class WebCandidateSelection {
    static final class Candidate {
        final String url;
        final String title;
        final boolean archive;
        final WebGameLinkAnalyzer.Format format;

        Candidate(String url, String title, boolean archive, WebGameLinkAnalyzer.Format format) {
            if (url == null || url.trim().isEmpty()) throw new IllegalArgumentException("Candidate URL is required.");
            this.url = url;
            this.title = title == null || title.trim().isEmpty() ? "Game link" : title.trim();
            this.archive = archive;
            this.format = format == null ? WebGameLinkAnalyzer.Format.UNKNOWN : format;
        }
    }

    private Candidate selected;

    Candidate getSelected() {
        return selected;
    }

    void select(Candidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("Choose a game link first.");
        selected = candidate;
    }

    void clear() {
        selected = null;
    }

    boolean isSelected(String url) {
        return selected != null && selected.url.equals(url);
    }
}
