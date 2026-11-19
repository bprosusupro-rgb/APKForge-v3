package com.gamebridge.mobile;

public final class RepairActionGateTest {
    public static void main(String[] args) {
        expect(RepairActionGate.Action.RETRY_CAPTURE,
                RepairActionGate.fromModelOutput("retry_capture"));
        expect(RepairActionGate.Action.REBUILD_WITH_TOUCH_SUPPORT,
                RepairActionGate.fromModelOutput("rebuild_with_touch_support"));
        expect(RepairActionGate.Action.OPEN_OFFICIAL_VERSION_SEARCH,
                RepairActionGate.fromModelOutput("open_official_version_search"));
        expect(RepairActionGate.Action.OPEN_EXE_HASH_CHECK,
                RepairActionGate.fromModelOutput("open_exe_hash_check"));
        expect(RepairActionGate.Action.OPEN_EXE_VERSION_LOOKUP,
                RepairActionGate.fromModelOutput("open_exe_version_lookup"));
        expect(RepairActionGate.Action.REBUILD_WITH_LOCAL_SAVE_SUPPORT,
                RepairActionGate.fromModelOutput("rebuild_with_local_save_support"));
        expect(RepairActionGate.Action.OPEN_WEBSITE_WORKSPACE,
                RepairActionGate.fromModelOutput("open_website_workspace"));
        expect(RepairActionGate.Action.NONE,
                RepairActionGate.fromModelOutput("run_exe:C:/Downloads/game.exe"));
        expect(RepairActionGate.Action.NONE,
                RepairActionGate.fromModelOutput("https://untrusted.example/payload.exe"));
        expect(RepairActionGate.Action.NONE,
                RepairActionGate.fromModelOutput("delete_game_files"));
        expect(RepairActionGate.Action.NONE,
                RepairActionGate.fromModelOutput(null));
        System.out.println("RepairActionGate tests passed");
    }

    private static void expect(RepairActionGate.Action expected, RepairActionGate.Action actual) {
        if (expected != actual) throw new AssertionError("expected " + expected + " but was " + actual);
    }
}
