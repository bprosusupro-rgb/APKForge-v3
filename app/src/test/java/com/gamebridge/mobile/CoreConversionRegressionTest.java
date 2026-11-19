package com.gamebridge.mobile;

import org.junit.Test;

/** Bridges the existing self-running regression checks into Gradle's test task. */
public final class CoreConversionRegressionTest {
    @Test public void offlinePackageTemplatesAndManifestPatching() throws Exception {
        OfflinePackageFormatTest.main(new String[0]);
    }

    @Test public void offlineGameDataSidecarStaysBesideLibraryApk() throws Exception {
        OfflineApkPackagerSidecarTest.main(new String[0]);
    }

    @Test public void largeRenPyPackageIsSplitBelowApkLimit() throws Exception {
        RenPyLargeGameAnalyzerTest.main(new String[0]);
    }

    @Test public void renPyDataUsesFinalLibraryStorage() throws Exception {
        RenPyGameDataStorageTest.main(new String[0]);
    }

    @Test public void offlineSnapshotRewritesRootAndBoundsResources() throws Exception {
        WebGameSnapshotTest.main(new String[0]);
    }

    @Test public void localAiSuggestionsStayWithinOfficialSearchScope() {
        DownloadSearchPlannerTest.main(new String[0]);
        DownloadSearchSequenceTest.main(new String[0]);
    }

    @Test public void htmlSaveBackupAndInPlaceUpdatePolicies() throws Exception {
        LocalGameStateStoreTest.main(new String[0]);
        InstalledBuildUpdatePolicyTest.main(new String[0]);
        HtmlMacroRuntimePathTest.main(new String[0]);
    }

    @Test public void downloadAssessmentNeverClaimsSafety() {
        DownloadSourceAssessmentTest.main(new String[0]);
    }

    @Test public void executableReviewRejectsNonPeFiles() throws Exception {
        WindowsExecutableHeaderTest.main(new String[0]);
    }

    @Test public void onlineArchivePackagingIsBounded() throws Exception {
        OnlineGameArchiveTest.main(new String[0]);
    }

    @Test public void onlineAndOfflineBuildModesStayDistinct() {
        WebBuildModeTest.main(new String[0]);
    }

    @Test public void htmlTouchAndKeyboardAdapterMatchesSource() throws Exception {
        HtmlTouchAdapterSourceTest.main(new String[0]);
        HtmlKeyboardSupportTest.main(new String[0]);
    }

    @Test public void renPyInputAndOutputLimitsAreEnforced() {
        RenPyPackageLimitsTest.main(new String[0]);
    }

    @Test public void gameLinkCandidatesRemainSelectable() {
        WebGameLinkAnalyzerTest.main(new String[0]);
        WebCandidateSelectionTest.main(new String[0]);
    }
}
