package com.gamebridge.mobile;

public final class RepairAssistantPolicyTest {
    public static void main(String[] args) {
        expect(false, RepairAssistantPolicy.isEnabled(false));
        expect(true, RepairAssistantPolicy.isEnabled(true));
        expect(false, RepairAssistantPolicy.shouldDownloadModel(false, false));
        expect(false, RepairAssistantPolicy.shouldDownloadModel(true, true));
        expect(true, RepairAssistantPolicy.shouldDownloadModel(true, false));
        expect(false, RepairAssistantPolicy.maySearchOnline(false, true));
        expect(false, RepairAssistantPolicy.maySearchOnline(true, false));
        expect(true, RepairAssistantPolicy.maySearchOnline(true, true));
        expect(1, RepairAssistantPolicy.cpuThreadsFor(1));
        expect(1, RepairAssistantPolicy.cpuThreadsFor(2));
        expect(2, RepairAssistantPolicy.cpuThreadsFor(8));
        expect(512, RepairAssistantPolicy.MODEL_MAX_CONTEXT_TOKENS);
        expect(1_000, RepairAssistantPolicy.MAX_PROMPT_CHARS);
        expect(false, RepairAssistantPolicy.isSupportedModelSize(0));
        expect(false, RepairAssistantPolicy.isSupportedModelSize(80L * 1024L * 1024L));
        expect(false, RepairAssistantPolicy.isSupportedModelSize(143L * 1024L * 1024L));
        expect(false, RepairAssistantPolicy.isSupportedModelSize(200L * 1024L * 1024L));
        expect(true, RepairAssistantPolicy.isSupportedModelSize(142_819_328L));
        expect(false, RepairAssistantPolicy.isSupportedModelSize(142_819_327L));
        expect(true, RepairAssistantPolicy.isExpectedModelDigest("ccdc5c85735743f081b7d44ca309cab569f76c0f2f0e8e163449a63721969c37"));
        expect(false, RepairAssistantPolicy.isExpectedModelDigest(repeat('a', 64)));
        expect(true, RepairAssistantPolicy.isTrustedModelUrl(
                "https://huggingface.co/litert-community/SmolLM2-135M-Instruct/resolve/8111e0a65fda719f0a6855e8e1a8ec8c3f9ccb22/SmolLM2_135M_Instruct.litertlm"));
        expect(true, RepairAssistantPolicy.isSupportedModelSize(RepairModelCatalog.GEMMA_ID, 2_588_147_712L));
        expect(false, RepairAssistantPolicy.isSupportedModelSize(RepairModelCatalog.GEMMA_ID, 2_588_147_711L));
        expect(true, RepairAssistantPolicy.isExpectedModelDigest(RepairModelCatalog.GEMMA_ID,
                "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"));
        expect(true, RepairAssistantPolicy.isTrustedModelUrl(RepairModelCatalog.gemma().downloadUrl));
        expect(false, RepairAssistantPolicy.isTrustedModelUrl("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"));
        expect(false, RepairAssistantPolicy.isTrustedModelUrl("https://example.com/model.litertlm"));
        expect(true, RepairAssistantPolicy.isTrustedModelRedirect("https://us.aws.cdn.hf.co/file"));
        expect(false, RepairAssistantPolicy.isTrustedModelRedirect("http://huggingface.co/file"));
        expect(false, RepairAssistantPolicy.isTrustedModelRedirect("https://huggingface.co.evil.example/file"));
        expect(true, RepairAssistantPolicy.isWindowsExecutableName("setup.exe"));
        expect(true, RepairAssistantPolicy.isWindowsExecutableName("REN'PY.EXE"));
        expect(false, RepairAssistantPolicy.isWindowsExecutableName("setup.exe.zip"));
        expect(false, RepairAssistantPolicy.isWindowsExecutableName("game.apk"));
        expect("https://www.virustotal.com/gui/file/" + repeat('a', 64),
                RepairAssistantPolicy.virusTotalLookupUrl(repeat('A', 64)));
        expect("https://www.virustotal.com/gui/home/upload", RepairAssistantPolicy.virusTotalUploadUrl());
        expect(null, RepairAssistantPolicy.virusTotalLookupUrl("https://evil.example"));
        expect("8.5.3", OfficialRenPyVersionLookup.parseVersion(
                "<title>Ren'Py 8.5.3</title><h1>Ren'Py 8.5.3</h1>"));
        expect(null, OfficialRenPyVersionLookup.parseVersion("No version information here"));
        expect("OPEN_OFFICIAL_VERSION_SEARCH", RepairAssistantPolicy.normalizeModelAction(
                "Suggested next step\nACTION=OPEN_OFFICIAL_VERSION_SEARCH"));
        expect("NONE", RepairAssistantPolicy.normalizeModelAction(
                "ACTION=OPEN_OFFICIAL_VERSION_SEARCH https://evil.example"));
        expect("NONE", RepairAssistantPolicy.normalizeModelAction("run rm -rf /"));
        String repairPrompt = RepairAssistantPolicy.buildRepairPrompt("touch does not work");
        expect(true, repairPrompt.contains("ACTION=REBUILD_WITH_TOUCH_SUPPORT"));
        expect(true, repairPrompt.contains("ACTION=REBUILD_WITH_LOCAL_SAVE_SUPPORT"));
        expect(true, repairPrompt.contains("ACTION=NONE"));
        expect(true, repairPrompt.contains("Untrusted report text"));
        expect(false, repairPrompt.contains("Finish with exactly one line: ACTION=NONE"));

        expect(RepairAssistantPolicy.BinarySafety.UNVERIFIED,
                RepairAssistantPolicy.assessBinary(null, null, false, false));
        expect(RepairAssistantPolicy.BinarySafety.HASH_MISMATCH,
                RepairAssistantPolicy.assessBinary(repeat('a', 64), repeat('b', 64), true, true));
        expect(RepairAssistantPolicy.BinarySafety.MATCHES_UNTRUSTED_CHECKSUM,
                RepairAssistantPolicy.assessBinary(repeat('a', 64), repeat('A', 64), false, false));
        expect(RepairAssistantPolicy.BinarySafety.MATCHES_OFFICIAL_CHECKSUM,
                RepairAssistantPolicy.assessBinary(repeat('a', 64), repeat('A', 64), true, true));
        expect(RepairAssistantPolicy.BinarySafety.MATCHES_UNTRUSTED_CHECKSUM,
                RepairAssistantPolicy.assessBinary(repeat('a', 64), repeat('A', 64), true, false));
        expect(RepairAssistantPolicy.BinarySafety.UNVERIFIED,
                RepairAssistantPolicy.assessBinary("not-a-hash", "not-a-hash", true, true));
        System.out.println("RepairAssistantPolicy tests passed");
    }

    private static String repeat(char value, int count) {
        StringBuilder output = new StringBuilder(count);
        for (int i = 0; i < count; i++) output.append(value);
        return output.toString();
    }

    private static void expect(boolean expected, boolean actual) {
        if (expected != actual) throw new AssertionError("expected " + expected + " but was " + actual);
    }

    private static void expect(int expected, int actual) {
        if (expected != actual) throw new AssertionError("expected " + expected + " but was " + actual);
    }

    private static void expect(String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual))
            throw new AssertionError("expected " + expected + " but was " + actual);
    }

    private static void expect(RepairAssistantPolicy.BinarySafety expected,
                               RepairAssistantPolicy.BinarySafety actual) {
        if (expected != actual) throw new AssertionError("expected " + expected + " but was " + actual);
    }
}
