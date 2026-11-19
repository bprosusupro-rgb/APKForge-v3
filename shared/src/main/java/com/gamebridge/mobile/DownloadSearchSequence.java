package com.gamebridge.mobile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps user-started AI searches moving through distinct queries until leads appear or options run out. */
public final class DownloadSearchSequence {
    private List<String> queries = Collections.emptyList();
    private int index = -1;
    private boolean active;

    public void start(List<String> suggestions) {
        queries = suggestions == null ? Collections.emptyList() : new ArrayList<>(suggestions);
        index = queries.isEmpty() ? -1 : 0;
        active = index >= 0;
    }

    public String currentQuery() {
        return active && index >= 0 && index < queries.size() ? queries.get(index) : null;
    }

    /** Advances after an empty/failed search; returns null once the bounded list is exhausted. */
    public String advanceAfterEmpty() {
        if (!active) return null;
        index++;
        if (index >= queries.size()) {
            active = false;
            return null;
        }
        return queries.get(index);
    }

    public void stopAfterLeads() { active = false; }
    public boolean isActive() { return active; }
    public int index() { return index; }
    public int size() { return queries.size(); }
}
