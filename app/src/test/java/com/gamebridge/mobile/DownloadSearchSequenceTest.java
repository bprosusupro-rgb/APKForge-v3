package com.gamebridge.mobile;

import java.util.Arrays;
import java.util.Collections;

public final class DownloadSearchSequenceTest {
    public static void main(String[] args) {
        DownloadSearchSequence sequence = new DownloadSearchSequence();
        sequence.start(Arrays.asList("official Windows", "official Android", "Ren'Py project ZIP"));
        require("official Windows".equals(sequence.currentQuery()), "start with the official Windows query");
        require("official Android".equals(sequence.advanceAfterEmpty()), "try an alternate official Android search after no results");
        require("Ren'Py project ZIP".equals(sequence.advanceAfterEmpty()), "try complete Ren'Py game data after no results");
        require(sequence.advanceAfterEmpty() == null && !sequence.isActive(), "stop after the bounded query list is exhausted");

        sequence.start(Arrays.asList("first", "second"));
        sequence.stopAfterLeads();
        require(!sequence.isActive() && sequence.advanceAfterEmpty() == null, "stop searching as soon as reviewable leads appear");
        sequence.start(Collections.emptyList());
        require(!sequence.isActive() && sequence.currentQuery() == null, "empty model output must not trigger a search");
        System.out.println("DownloadSearchSequence tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
