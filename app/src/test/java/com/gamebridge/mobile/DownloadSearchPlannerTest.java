package com.gamebridge.mobile;

import java.util.List;

public final class DownloadSearchPlannerTest {
    public static void main(String[] args) {
        String prompt = DownloadSearchPlanner.buildPrompt("My Visual Novel");
        if (!prompt.contains("My Visual Novel") || !prompt.contains("Windows EXE")
                || !prompt.contains("not convertible"))
            throw new AssertionError("search prompt must retain context and conversion safety rules");

        List<String> parsed = DownloadSearchPlanner.parseSuggestions(
                "1. My Visual Novel official download\n2. My Visual Novel Android version\n3. My Visual Novel Ren'Py ZIP", "My Visual Novel");
        if (parsed.size() != 3 || !parsed.get(0).equals("My Visual Novel official download"))
            throw new AssertionError("numbered model suggestions should parse");

        List<String> filtered = DownloadSearchPlanner.parseSuggestions(
                "https://bad.example/payload.exe\nsearch: My Visual Novel crack download\n- My Visual Novel official HTML game", "My Visual Novel");
        if (filtered.size() != 3 || !filtered.get(0).contains("official Windows EXE"))
            throw new AssertionError("unsafe or malformed output should fall back to safe, useful search phrases");
        System.out.println("DownloadSearchPlanner tests passed");
    }
}
