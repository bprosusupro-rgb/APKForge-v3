package com.gamebridge.mobile;

public final class WebCapturePayloadTest {
    public static void main(String[] args) {
        String source = "<!doctype html>" + "x".repeat(150_000);
        WebCapturePayload payload = new WebCapturePayload(source.length());
        for (int offset = 0; offset < source.length(); offset += WebCapturePayload.MAX_CHUNK_CHARS) {
            payload.append(source.substring(offset, Math.min(source.length(), offset + WebCapturePayload.MAX_CHUNK_CHARS)));
        }
        if (!source.equals(payload.finish())) throw new AssertionError("chunked page markup did not reconstruct exactly");

        try { new WebCapturePayload(4 * 1024 * 1024 + 1); }
        catch (IllegalArgumentException oldLimit) {
            throw new AssertionError("an 8 MiB capture allowance should accept a page larger than the old 4 MiB limit", oldLimit);
        }

        boolean tooLarge = false;
        try { new WebCapturePayload(WebCapturePayload.MAX_HTML_CHARS + 1); }
        catch (IllegalArgumentException expected) { tooLarge = true; }
        if (!tooLarge) throw new AssertionError("oversize page markup must be rejected before allocation");

        boolean incomplete = false;
        try { new WebCapturePayload(4).append("abc").finish(); }
        catch (IllegalStateException expected) { incomplete = true; }
        if (!incomplete) throw new AssertionError("truncated page must not be packaged silently");

        boolean overflow = false;
        try { new WebCapturePayload(3).append("abcd"); }
        catch (IllegalArgumentException expected) { overflow = true; }
        if (!overflow) throw new AssertionError("a chunk must not exceed the declared HTML length");

        boolean giantChunk = false;
        try { new WebCapturePayload(WebCapturePayload.MAX_CHUNK_CHARS + 1).append("x".repeat(WebCapturePayload.MAX_CHUNK_CHARS + 1)); }
        catch (IllegalArgumentException expected) { giantChunk = true; }
        if (!giantChunk) throw new AssertionError("one oversized callback must not bypass chunking");
        System.out.println("WebCapturePayload tests passed");
    }
}
