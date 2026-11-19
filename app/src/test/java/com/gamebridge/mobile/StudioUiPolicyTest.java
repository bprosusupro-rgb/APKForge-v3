package com.gamebridge.mobile;

public final class StudioUiPolicyTest {
    public static void main(String[] args) {
        if (StudioUiPolicy.useStudio(false)) throw new AssertionError("classic should remain the default");
        if (!StudioUiPolicy.useStudio(true)) throw new AssertionError("studio preference should be respected");
        if (!"Classic".equals(StudioUiPolicy.label(false))) throw new AssertionError("classic label");
        if (!"Studio".equals(StudioUiPolicy.label(true))) throw new AssertionError("studio label");
        if (!"studio_ui_enabled".equals(StudioUiPolicy.PREFERENCE_KEY)) throw new AssertionError("stable preference key");
        System.out.println("StudioUiPolicy tests passed");
    }
}
