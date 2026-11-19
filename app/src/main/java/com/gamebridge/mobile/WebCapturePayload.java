package com.gamebridge.mobile;

/** Assembles bounded WebView HTML responses without one oversized IPC callback. */
final class WebCapturePayload {
    static final int MAX_CHUNK_CHARS = 48 * 1024;
    static final int MAX_HTML_CHARS = 8 * 1024 * 1024;
    private final int expectedChars;
    private final StringBuilder content;

    WebCapturePayload(int expectedChars) {
        if (expectedChars <= 0 || expectedChars > MAX_HTML_CHARS) {
            throw new IllegalArgumentException("The current game page is empty or too large to capture safely.");
        }
        this.expectedChars = expectedChars;
        this.content = new StringBuilder(expectedChars);
    }

    WebCapturePayload append(String chunk) {
        if (chunk == null || chunk.length() > MAX_CHUNK_CHARS || content.length() + chunk.length() > expectedChars) {
            throw new IllegalArgumentException("Invalid or oversized page capture chunk.");
        }
        content.append(chunk);
        return this;
    }

    String finish() {
        if (content.length() != expectedChars) throw new IllegalStateException("The page capture was incomplete; no APK was built.");
        return content.toString();
    }
}
