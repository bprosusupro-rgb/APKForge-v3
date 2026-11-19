package com.gamebridge.mobile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class WebGameLinkAnalyzer {
    enum Format { RENPY, HTML, UNKNOWN }

    private WebGameLinkAnalyzer() { }

    static String normalizeInput(String value) {
        String input = value == null ? "" : value.trim();
        if (input.matches("(?i)^https?://[^\\s]+$")) return input;
        if (input.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(:[0-9]+)?(/.*)?$")) return "https://" + input;
        return "";
    }

    static String searchUrl(String query) {
        String value = query == null ? "" : query.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Enter a website or search phrase.");
        return "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static void validateOverride(Format detected, Format requested) {
        if (requested != null && requested != Format.UNKNOWN && requested != detected) {
            throw new IllegalArgumentException("This archive contains " + detected + " game data, not the selected " + requested + " format. Choose the matching runtime or use a complete game ZIP.");
        }
    }

    static boolean isWindowsExecutable(String url) {
        return url != null && url.matches("(?i).*\\.exe(?:$|[?#]).*");
    }

    static Format inferFormat(String url, String label, String pageText, int archiveCount) {
        String link = ((url == null ? "" : url) + " " + (label == null ? "" : label)).toLowerCase(Locale.ROOT);
        boolean renpy = link.matches("(?s).*ren['’]?py.*");
        boolean html = link.matches("(?s).*(?:\\.html?(?:[?#\\s].*)?|html|web game|browser game|play online).*");
        if (renpy && html) return Format.UNKNOWN;
        if (renpy) return Format.RENPY;
        if (html) return Format.HTML;
        String page = pageText == null ? "" : pageText.toLowerCase(Locale.ROOT);
        boolean pageRenpy = page.matches("(?s).*ren['’]?py.*");
        boolean pageHtml = page.matches("(?s).*(?:html game|browser game|web game).*");
        if (pageRenpy && !pageHtml && archiveCount == 1) return Format.RENPY;
        return Format.UNKNOWN;
    }
}
