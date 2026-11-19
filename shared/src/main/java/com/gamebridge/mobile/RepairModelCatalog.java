package com.gamebridge.mobile;

/** Immutable, checksum-pinned on-device language-model choices. */
public final class RepairModelCatalog {
    public static final String COMPACT_ID = "smollm2-135m";
    public static final String GEMMA_ID = "gemma-4-e2b";

    public static final class Model {
        public final String id;
        public final String fileName;
        public final String downloadUrl;
        public final String sha256;
        public final long bytes;
        public final int maxContextTokens;
        public final String displayName;
        public final long minimumAvailableMemoryBytes;

        private Model(String id, String fileName, String downloadUrl, String sha256,
                      long bytes, int maxContextTokens, String displayName, long minimumAvailableMemoryBytes) {
            this.id = id;
            this.fileName = fileName;
            this.downloadUrl = downloadUrl;
            this.sha256 = sha256;
            this.bytes = bytes;
            this.maxContextTokens = maxContextTokens;
            this.displayName = displayName;
            this.minimumAvailableMemoryBytes = minimumAvailableMemoryBytes;
        }
    }

    private static final Model COMPACT = new Model(COMPACT_ID,
            "SmolLM2_135M_Instruct.litertlm",
            "https://huggingface.co/litert-community/SmolLM2-135M-Instruct/resolve/8111e0a65fda719f0a6855e8e1a8ec8c3f9ccb22/SmolLM2_135M_Instruct.litertlm",
            "ccdc5c85735743f081b7d44ca309cab569f76c0f2f0e8e163449a63721969c37",
            142_819_328L, 512, "SmolLM2 135M · Compact", 256L * 1024L * 1024L);
    private static final Model GEMMA = new Model(GEMMA_ID,
            "Gemma-4-E2B-it.litertlm",
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1/gemma-4-E2B-it.litertlm",
            "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            2_588_147_712L, 512, "Gemma 4 E2B · Advanced", 2L * 1024L * 1024L * 1024L);

    private RepairModelCatalog() { }

    public static Model compact() { return COMPACT; }
    public static Model gemma() { return GEMMA; }
    public static Model byId(String id) { return GEMMA_ID.equals(id) ? GEMMA : COMPACT; }
    public static boolean isGemma(String id) { return GEMMA_ID.equals(id); }
    public static boolean hasEnoughMemory(String id, long availableBytes) {
        return availableBytes >= byId(id).minimumAvailableMemoryBytes;
    }
}
