package com.gamebridge.mobile;

public final class DownloadSourceAssessmentTest {
    public static void main(String[] args) {
        DownloadSourceAssessment ownHost = DownloadSourceAssessment.inspect(
                "https://games.example/files/setup.exe?token=x", "https://games.example/play", "Windows download");
        if (ownHost.artifact != DownloadSourceAssessment.Artifact.WINDOWS_EXE
                || !ownHost.https || !ownHost.sameHostAsGamePage || !ownHost.mayOfferReviewDownload())
            throw new AssertionError("HTTPS EXE from the same host should be offered for review, not trusted");
        if (!ownHost.safetyDisclaimer().contains("not a malware scan"))
            throw new AssertionError("assessment must never claim a file is safe");

        DownloadSourceAssessment thirdParty = DownloadSourceAssessment.inspect(
                "https://cdn.example/game.zip", "https://games.example/play", "Complete game ZIP");
        if (thirdParty.sameHostAsGamePage || thirdParty.artifact != DownloadSourceAssessment.Artifact.GAME_ARCHIVE)
            throw new AssertionError("third-party archive provenance should remain distinct");

        DownloadSourceAssessment insecure = DownloadSourceAssessment.inspect(
                "http://games.example/setup.exe", "https://games.example/play", "setup.exe");
        if (insecure.https || insecure.mayOfferReviewDownload())
            throw new AssertionError("insecure EXE sources must not be eligible for review download");
        DownloadSourceAssessment creatorPage = DownloadSourceAssessment.inspect(
                "https://creator.example/download/windows", "https://creator.example/game", "Download for Windows");
        if (creatorPage.artifact != DownloadSourceAssessment.Artifact.WINDOWS_EXE
                || !creatorPage.mayOfferReviewDownload())
            throw new AssertionError("Windows installer links without an .exe suffix should be recognizable");
        System.out.println("DownloadSourceAssessment tests passed");
    }
}
