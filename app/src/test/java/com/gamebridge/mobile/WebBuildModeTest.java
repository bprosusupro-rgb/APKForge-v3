package com.gamebridge.mobile;

public final class WebBuildModeTest {
    public static void main(String[] args) {
        if (!"Build offline snapshot".equals(WebBuildMode.actionLabel(WebBuildMode.Mode.OFFLINE)))
            throw new AssertionError("offline mode must offer an offline build");
        if (!"Build online wrapper".equals(WebBuildMode.actionLabel(WebBuildMode.Mode.ONLINE)))
            throw new AssertionError("online mode must offer an online wrapper");
        if (!WebBuildMode.description(WebBuildMode.Mode.OFFLINE).contains("loaded files"))
            throw new AssertionError("offline mode must explain its captured-file requirement");
        if (!WebBuildMode.description(WebBuildMode.Mode.ONLINE).contains("internet"))
            throw new AssertionError("online mode must disclose the internet requirement");
        if (!WebBuildMode.description(WebBuildMode.Mode.ONLINE).contains("localStorage"))
            throw new AssertionError("online wrapper mode must disclose on-device HTML game-state checkpoints");
        System.out.println("WebBuildMode tests passed");
    }
}
