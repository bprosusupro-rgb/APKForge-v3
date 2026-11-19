package com.gamebridge.mobile;

/** Conservative, device-independent conversion estimate; exact time depends on storage and media compression. */
final class ConversionEta {
    private static final long STARTUP_SECONDS = 5;
    private static final long MIN_BYTES_PER_SECOND = 2L * 1024 * 1024;
    private static final long MAX_BYTES_PER_SECOND = 64L * 1024 * 1024;
    private static final long ESTIMATED_BYTES_PER_SECOND = 6L * 1024 * 1024;
    private ConversionEta() { }

    static long estimateSeconds(long bytesToProcess) {
        return estimateSeconds(bytesToProcess, ESTIMATED_BYTES_PER_SECOND);
    }

    static long estimateSeconds(long bytesToProcess, long measuredBytesPerSecond) {
        if (bytesToProcess <= 0) return STARTUP_SECONDS;
        // Bound learned rates: tiny projects are dominated by fixed setup time, not throughput.
        long rate = measuredBytesPerSecond > 0 ? clampRate(measuredBytesPerSecond) : ESTIMATED_BYTES_PER_SECOND;
        long work = bytesToProcess / rate;
        if (bytesToProcess % rate != 0) work++;
        return work > Long.MAX_VALUE - STARTUP_SECONDS ? Long.MAX_VALUE : STARTUP_SECONDS + work;
    }

    static long updatedRate(long previousRate, long measuredRate) {
        if (measuredRate <= 0) return previousRate <= 0 ? 0 : clampRate(previousRate);
        long latest = clampRate(measuredRate);
        if (previousRate <= 0) return latest;
        // 60% history / 40% latest build smooths out one unusually fast or slow run.
        long previous = clampRate(previousRate);
        return (previous * 3 + latest * 2) / 5;
    }

    private static long clampRate(long rate) {
        return Math.max(MIN_BYTES_PER_SECOND, Math.min(MAX_BYTES_PER_SECOND, rate));
    }

    static String format(long seconds) {
        long safe=Math.max(1,seconds), hours=safe/3600, minutes=(safe%3600)/60, remainder=safe%60;
        if (hours>0) return hours+"h "+minutes+"m";
        if (minutes>0) return minutes+"m "+remainder+"s";
        return remainder+"s";
    }
}
