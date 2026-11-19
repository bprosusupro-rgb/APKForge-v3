package com.gamebridge.mobile;

import java.util.Locale;

/** Evidence-based first aid for common APK Forge failures; unknown cases go to the local model. */
public final class RepairAdvice {
    public enum Kind { UNKNOWN, OFFLINE_HTML, TOUCH_INPUT, LOCAL_SAVE, RENPY_STATE, RENPY_PACKAGE, RENPY_VERSION, EXE_SAFETY, EXE_VERSION_LOOKUP }

    public static final class Advice {
        public final Kind kind;
        public final String message;
        public final RepairActionGate.Action action;

        private Advice(Kind kind, String message, RepairActionGate.Action action) {
            this.kind = kind;
            this.message = message;
            this.action = action;
        }
    }

    private RepairAdvice() { }

    public static Advice analyze(String report) {
        String text = report == null ? "" : report.toLowerCase(Locale.ROOT);
        if (text.contains("offline") && containsAny(text, "blank", "white screen", "missing asset", "missing file", "resource")) {
            return new Advice(Kind.OFFLINE_HTML,
                    "A blank offline page often means the game needs assets or server data that were not captured; this does not prove the APK is damaged. "
                            + "If the game page is still open in APK Forge, use Retry offline capture to rebuild its snapshot; otherwise open it online, play through the needed screens, then capture it again. "
                            + "Accounts, multiplayer, and server-only features still need the original service.",
                    RepairActionGate.Action.RETRY_CAPTURE);
        }
        if ((text.contains(".exe") || text.contains(" exe"))
                && containsAny(text, "version", "build", "identify")) {
            return new Advice(Kind.EXE_VERSION_LOOKUP,
                    "I can calculate the selected EXE's SHA-256 and look up an existing VirusTotal report. It may show version metadata if that exact file is already known there; APK Forge cannot guarantee the version from an arbitrary EXE. "
                            + "The EXE is not uploaded or run. Enable user-started web lookups in Settings first.",
                    RepairActionGate.Action.OPEN_EXE_VERSION_LOOKUP);
        }
        if ((text.contains(".exe") || text.contains(" exe"))
                && containsAny(text, "safe", "virus", "malware", "scan", "reputation")) {
            return new Advice(Kind.EXE_SAFETY,
                    "I can calculate the selected EXE's SHA-256 on this device and open its VirusTotal hash report. "
                            + "The EXE is not uploaded or run; reputation results can be missing or wrong and are not a guarantee of safety. "
                            + "Enable user-started web lookups in Settings first.",
                    RepairActionGate.Action.OPEN_EXE_HASH_CHECK);
        }
        boolean saveIssue = containsAny(text, "save", "progress", "state lost", "state disappeared");
        boolean renpy = text.contains("renpy") || text.contains("ren’py");
        if (saveIssue && renpy) {
            return new Advice(Kind.RENPY_STATE,
                    "Ren’Py progress is managed by the game's own save system in its private app storage. The HTML localStorage repair does not apply to Ren’Py, and rebuilding cannot recover save data that was already lost. Check that you are opening the same installed game and use its in-game Load menu.",
                    RepairActionGate.Action.NONE);
        }
        if (saveIssue && containsAny(text, "html", "website", "browser", "webview", "web game")) {
            return new Advice(Kind.LOCAL_SAVE,
                    "I can rebuild this HTML wrapper with APK Forge's localStorage checkpoint. It can preserve browser-game progress that the game stores in localStorage; it cannot restore IndexedDB, cookies, server/account saves, or progress already lost.",
                    RepairActionGate.Action.REBUILD_WITH_LOCAL_SAVE_SUPPORT);
        }
        if ((text.contains("renpy") || text.contains("ren’py"))
                && containsAny(text, "version", "update", "latest", "newest")) {
            return new Advice(Kind.RENPY_VERSION,
                    "I can check the latest Ren’Py SDK version published by renpy.org. That does not identify an arbitrary EXE's embedded version or prove it safe. "
                            + "For a selected installer, use the separate hash-reputation check; APK Forge never runs or uploads the EXE.",
                    RepairActionGate.Action.OPEN_OFFICIAL_VERSION_SEARCH);
        }
        if (containsAny(text, "touch", "tap", "keyboard", "mouse pointer")) {
            return new Advice(Kind.TOUCH_INPUT,
                    renpy
                            ? "Ren’Py games use the bundled native Android runtime, not the HTML touch adapter. APK Forge cannot safely rewrite a Ren’Py game's input code without its project and a reproducible issue."
                            : "I can rebuild this HTML game with APK Forge's built-in touch-to-mouse adapter and keyboard control. "
                                    + "This fixes common missed taps only; custom canvas controls, timing and the game's own JavaScript bugs may need a game-specific patch.",
                    renpy ? RepairActionGate.Action.NONE : RepairActionGate.Action.REBUILD_WITH_TOUCH_SUPPORT);
        }
        if ((text.contains("renpy") || text.contains("ren’py") || text.contains(".exe"))
                && containsAny(text, "crash", "launch", "missing", "won't open", "cannot open")) {
            return new Advice(Kind.RENPY_PACKAGE,
                    "A Ren’Py Windows .exe alone is not an Android game package. Select the complete distribution, including compiled game/ data and its matching lib/ and runtime files. "
                            + "APK Forge uses its bundled Android Ren’Py runtime; it cannot repair missing or incompatible game data automatically.",
                    RepairActionGate.Action.NONE);
        }
        return new Advice(Kind.UNKNOWN, "", RepairActionGate.Action.NONE);
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }
}
