package com.gamebridge.mobile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Reports observable source properties without pretending that they prove a download is safe. */
public final class DownloadSourceAssessment {
    public enum Artifact { WINDOWS_EXE, GAME_ARCHIVE, WEB_PAGE, OTHER }

    public final String host;
    public final Artifact artifact;
    public final boolean https;
    public final boolean sameHostAsGamePage;
    public final String evidenceLabel;

    private DownloadSourceAssessment(String host, Artifact artifact, boolean https,
                                     boolean sameHostAsGamePage, String evidenceLabel) {
        this.host = host;
        this.artifact = artifact;
        this.https = https;
        this.sameHostAsGamePage = sameHostAsGamePage;
        this.evidenceLabel = evidenceLabel;
    }

    public static DownloadSourceAssessment inspect(String candidateUrl, String gamePageUrl, String linkLabel) {
        URI candidate = parse(candidateUrl);
        if (candidate == null || candidate.getHost() == null
                || !("https".equalsIgnoreCase(candidate.getScheme()) || "http".equalsIgnoreCase(candidate.getScheme()))) {
            return new DownloadSourceAssessment("Unknown host", Artifact.OTHER, false, false,
                    "Could not verify the link address");
        }
        String host = candidate.getHost().toLowerCase(Locale.ROOT);
        URI origin = parse(gamePageUrl);
        boolean sameHost = origin != null && origin.getHost() != null
                && host.equalsIgnoreCase(origin.getHost());
        String path = candidate.getPath() == null ? "" : candidate.getPath().toLowerCase(Locale.ROOT);
        String label = linkLabel == null ? "" : linkLabel.toLowerCase(Locale.ROOT);
        boolean windowsInstaller = label.matches("(?s).*(?:windows|win64|win32).*(?:download|installer|setup|\\.exe).*" )
                || label.matches("(?s).*(?:download|installer|setup|\\.exe).*(?:windows|win64|win32).*");
        Artifact artifact = path.endsWith(".exe") || label.matches("(?s).*\\.exe(?:\\s|$).*") || windowsInstaller
                ? Artifact.WINDOWS_EXE
                : path.matches("(?s).*\\.(zip|7z|rar|apk|html?)$") || label.matches("(?s).*\\.(zip|7z|rar|apk|html?)(?:\\s|$).*")
                ? Artifact.GAME_ARCHIVE
                : Artifact.WEB_PAGE;
        boolean https = "https".equalsIgnoreCase(candidate.getScheme());
        String evidence = !https ? "HTTP only · connection is not encrypted"
                : sameHost ? "HTTPS · same hostname as the game page (not proof of publisher identity)"
                : "HTTPS · third-party hostname (publisher not verified)";
        return new DownloadSourceAssessment(host, artifact, https, sameHost, evidence);
    }

    public boolean mayOfferReviewDownload() {
        return https && artifact == Artifact.WINDOWS_EXE;
    }

    public String safetyDisclaimer() {
        return "This is source/transport evidence only—not a malware scan or safety guarantee. Never run this Windows EXE on Android. It cannot be converted directly into a playable APK.";
    }

    private static URI parse(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return new URI(value.trim()); }
        catch (URISyntaxException invalid) { return null; }
    }
}
