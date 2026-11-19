package com.gamebridge.mobile;

public final class RepairAdviceTest {
    public static void main(String[] args) {
        RepairAdvice.Advice offline = RepairAdvice.analyze("HTML blank screen offline");
        expect(RepairAdvice.Kind.OFFLINE_HTML, offline.kind);
        expect(RepairActionGate.Action.RETRY_CAPTURE, offline.action);
        expect(true, offline.message.contains("server"));

        RepairAdvice.Advice touch = RepairAdvice.analyze("The game's touch controls do not work");
        expect(RepairAdvice.Kind.TOUCH_INPUT, touch.kind);
        expect(RepairActionGate.Action.REBUILD_WITH_TOUCH_SUPPORT, touch.action);

        RepairAdvice.Advice renpyTouch = RepairAdvice.analyze("RenPy game taps do not work");
        expect(RepairAdvice.Kind.TOUCH_INPUT, renpyTouch.kind);
        expect(RepairActionGate.Action.NONE, renpyTouch.action);

        RepairAdvice.Advice renpy = RepairAdvice.analyze("RenPy exe crashes when I launch it");
        expect(RepairAdvice.Kind.RENPY_PACKAGE, renpy.kind);
        expect(true, renpy.message.contains("game/"));

        RepairAdvice.Advice version = RepairAdvice.analyze("Check the latest RenPy version");
        expect(RepairAdvice.Kind.RENPY_VERSION, version.kind);
        expect(RepairActionGate.Action.OPEN_OFFICIAL_VERSION_SEARCH, version.action);

        RepairAdvice.Advice safety = RepairAdvice.analyze("Is this RenPy setup.exe safe from viruses?");
        expect(RepairAdvice.Kind.EXE_SAFETY, safety.kind);
        expect(RepairActionGate.Action.OPEN_EXE_HASH_CHECK, safety.action);
        expect(true, safety.message.contains("not a guarantee"));

        RepairAdvice.Advice exeVersion = RepairAdvice.analyze("Find the RenPy setup.exe version online");
        expect(RepairAdvice.Kind.EXE_VERSION_LOOKUP, exeVersion.kind);
        expect(RepairActionGate.Action.OPEN_EXE_VERSION_LOOKUP, exeVersion.action);

        RepairAdvice.Advice htmlSave = RepairAdvice.analyze("My HTML game's save progress disappears after closing it");
        expect(RepairAdvice.Kind.LOCAL_SAVE, htmlSave.kind);
        expect(RepairActionGate.Action.REBUILD_WITH_LOCAL_SAVE_SUPPORT, htmlSave.action);

        RepairAdvice.Advice renpySave = RepairAdvice.analyze("My RenPy game lost its save progress");
        expect(RepairAdvice.Kind.RENPY_STATE, renpySave.kind);
        expect(RepairActionGate.Action.NONE, renpySave.action);

        expect(RepairAdvice.Kind.UNKNOWN, RepairAdvice.analyze("hello there").kind);
        expect(RepairAdvice.Kind.UNKNOWN, RepairAdvice.analyze("   ").kind);
        System.out.println("RepairAdvice tests passed");
    }

    private static void expect(boolean expected, boolean actual) {
        if (expected != actual) throw new AssertionError("expected " + expected + " but was " + actual);
    }

    private static void expect(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + " but was " + actual);
    }
}
