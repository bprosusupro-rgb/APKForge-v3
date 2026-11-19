package com.gamebridge.mobile;

public final class RepairModelCatalogTest {
    public static void main(String[] args) {
        RepairModelCatalog.Model compact = RepairModelCatalog.byId(RepairModelCatalog.COMPACT_ID);
        RepairModelCatalog.Model gemma = RepairModelCatalog.byId(RepairModelCatalog.GEMMA_ID);
        require(compact.bytes == 142_819_328L, "compact artifact size is pinned");
        require(compact.sha256.length() == 64, "compact artifact digest is pinned");
        require(gemma.bytes == 2_588_147_712L, "Gemma artifact size is pinned");
        require("181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c".equals(gemma.sha256), "Gemma official LFS SHA-256 is pinned");
        require(gemma.downloadUrl.contains("/resolve/6e5c4f1/"), "Gemma URL points to immutable verified revision");
        require("https".equals(gemma.downloadUrl.substring(0, 5)), "model download uses HTTPS");
        require(!RepairModelCatalog.hasEnoughMemory(RepairModelCatalog.GEMMA_ID, gemma.minimumAvailableMemoryBytes - 1), "Gemma fails closed on low free memory");
        require(RepairModelCatalog.hasEnoughMemory(RepairModelCatalog.GEMMA_ID, gemma.minimumAvailableMemoryBytes), "Gemma can run above its memory guard");
        require(RepairModelCatalog.hasEnoughMemory(RepairModelCatalog.COMPACT_ID, 512L * 1024L * 1024L), "compact model remains available on lower-memory devices");
        require(RepairModelCatalog.byId("unknown").id.equals(RepairModelCatalog.COMPACT_ID), "unknown preference falls back to compact model");
        System.out.println("RepairModelCatalog tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
