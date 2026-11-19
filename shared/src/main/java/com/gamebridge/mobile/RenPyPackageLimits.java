package com.gamebridge.mobile;

/** Size policy for large Ren'Py inputs and installable universal APK outputs. */
final class RenPyPackageLimits {
    static final long MAX_INPUT_ARCHIVE_BYTES = 6L * 1024L * 1024L * 1024L;
    static final long MAX_EXPANDED_GAME_BYTES = 6L * 1024L * 1024L * 1024L;
    // ZIP32 reserves 0xffffffff as the Zip64 sentinel; APK signing/runtime
    // support for individual Zip64 asset entries is not portable.
    static final long MAX_GAME_FILE_BYTES = 0xffff_fffeL;
    // Ren'Py documents a 2 GiB universal APK ceiling. Keep headroom for signing
    // blocks and ZIP metadata instead of emitting an APK at the hard boundary.
    static final long MAX_SIGNED_APK_BYTES = 1900L * 1024L * 1024L;
    static final long MAX_UNSIGNED_APK_BYTES = MAX_SIGNED_APK_BYTES - 16L * 1024L * 1024L;

    private RenPyPackageLimits() { }

    static String oversizedApkMessage() {
        return "The finished APK would exceed APK Forge's 1.9 GiB installable size limit. "
                + "Ren'Py game data above the safe embedded size is packaged in a separate offline ZIP. "
                + "The final APK still must remain below 1.9 GiB; this game could not be reduced enough to fit.";
    }
}
