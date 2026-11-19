package com.gamebridge.mobile;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Dialog;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 10;
    private static final int PICK_FOLDER = 11;
    private static final int SAVE_APK = 12;
    private static final int REQUEST_INSTALL_PERMISSION = 13;
    private static final int REQUEST_UNINSTALL = 21;
    private static final int SAVE_DIAGNOSTICS = 14;
    private static final int SAVE_GAME_DATA = 15;
    private static final int CHECK_EXE = 16;
    private static final int IMPORT_REPAIR_MODEL = 17;
    private static final int SAVE_REVIEW_EXE = 18;
    private static final int OPEN_OFFLINE_SAVE_BACKUP = 19;
    private static final int EXPORT_OFFLINE_SAVE_BACKUP = 20;
    private static final int BG = Color.rgb(12, 16, 23);
    private static final int CARD = Color.rgb(24, 31, 43);
    private static final int INPUT = Color.rgb(17, 23, 34);
    private static final int WHITE = Color.rgb(246, 249, 255);
    private static final int MUTED = Color.rgb(171, 186, 207);
    private static final int MINT = Color.rgb(167, 249, 214);
    private static final int AQUA = Color.rgb(144, 229, 212);
    private static final int GREEN = Color.rgb(169, 232, 145);
    private static final int BLUE = Color.rgb(120, 200, 255);
    private static final int AMBER = Color.rgb(255, 190, 82);
    // Allow 5 GiB game archives with headroom; the final single APK is still
    // constrained to Ren'Py/Android's installable size limit.
    private static final long MAX_DOWNLOAD_BYTES = RenPyPackageLimits.MAX_INPUT_ARCHIVE_BYTES;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService assistantWorker = Executors.newSingleThreadExecutor();
    private EditText nameField, packageField;
    private TextView selectedText, statusText;
    private Button buildApkButton, playBuiltButton, saveApkButton;
    private LinearLayout libraryList, installedList;
    private ProgressBar progress;
    private Uri selectedFile, selectedFolder;
    private File lastApk;
    private File lastGameDataBundle;
    private File pendingGameDataSave;
    private File selectedLocalFile;
    private ScrollView convertScroll;
    private LinearLayout convertRoot;
    private int currentConvertSection;
    private FrameLayout tabletContent;
    private LinearLayout tabletSidebar;
    private boolean tabletMode;
    private boolean studioMode;
    private Bitmap browserBrandBitmap;
    private LinearLayout librarySection, installedSection;
    private EditText browserAddress;
    private TextView browserStatus, browserResults;
    private LinearLayout browserResultsList;
    private Button selectedCandidateAction;
    private File pendingReviewExe;
    private List<String> aiDownloadSearchQueries = Collections.emptyList();
    private int aiDownloadSearchIndex = -1;
    private final DownloadSearchSequence aiDownloadSearchSequence = new DownloadSearchSequence();
    private boolean aiAutomaticSearchActive;
    private String aiAutomaticSearchAnalyzedUrl = "";
    private final WebCandidateSelection candidateSelection = new WebCandidateSelection();
    private ScrollView browserResultsScroll;
    private ProgressBar browserProgress;
    private WebView browserWebView;
    private View browserPreviewEmpty;
    private TextView browserPreviewTitle, browserPreviewHint;
    private View browserStatusDot;
    private boolean browserLoadFailed;
    private View browserPage;
    private View diagnosticsPage;
    private View settingsPage;
    private View offlineSaveEditorPage;
    private TextView diagnosticsText;
    private TextView offlineSaveStatus, offlineSaveAiResult;
    private LinearLayout offlineSaveFields;
    private final LinkedHashMap<String, EditText> offlineSaveInputs = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> offlineSaveDraft = new LinkedHashMap<>();
    private Dialog navigationDrawer;
    private boolean showingBrowser;
    private boolean showingDiagnostics;
    private boolean showingSettings;
    private boolean showingOfflineSaveEditor;
    private WebGameLinkAnalyzer.Format selectedFormatOverride = WebGameLinkAnalyzer.Format.UNKNOWN;
    private Button tabletConvertButton, tabletBrowserButton, tabletLibraryButton, tabletInstalledButton, tabletDiagnosticsButton, tabletSettingsButton, tabletSaveEditorButton;
    private Switch localAiSwitch, onlineLookupSwitch;
    private Button compactModelButton, gemmaModelButton, classicUiButton, studioUiButton;
    private EditText repairQuestion;
    private TextView assistantResult;
    private Button assistantActionButton;
    private RepairActionGate.Action visibleAssistantAction = RepairActionGate.Action.NONE;
    private Button offlineModeButton, onlineModeButton, websiteBuildButton, saveGameDataButton;
    private TextView websiteModeDescription;
    private WebBuildMode.Mode selectedWebsiteMode = WebBuildMode.Mode.OFFLINE;
    private boolean busy;
    private boolean preparingGame;
    private LinearLayout phoneInspector;
    private LinearLayout phoneInspectorBody;
    private ScrollView phoneInspectorScroll;
    private TextView browserSelectedSite;
    private TextView pageSelectionTitle;
    private View pageSelectionOverlay;
    private Button phoneInspectorToggle;
    private boolean phoneInspectorExpanded;
    private int phoneInspectorHeightDp = 272;
    private int tabletInspectorWidthDp = 320;
    private LinearLayout tabletInspector;
    private TextView tabletInspectorWidthLabel;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        DiagnosticLog.installCrashHandler(getFilesDir());
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        buildUi();
        int width = getResources().getConfiguration().screenWidthDp;
        worker.execute(() -> DiagnosticLog.record(getFilesDir(), DiagnosticLog.Event.APP_STARTED,
                Build.VERSION.SDK_INT, width));
        refreshLibrary();
    }

    @Override protected void onResume() {
        super.onResume();
        // PackageInstaller returns here after its confirmation UI; reflect the
        // completed install or uninstall without requiring a manual refresh.
        if (installedList != null) refreshInstalled();
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateResponsiveLayout(configuration);
        // Keep the existing WebView instance alive while Android resizes the activity/window.
        // Recreating it reloads games and discards JavaScript-only session state.
        if (browserWebView != null) {
            browserWebView.requestLayout();
            browserWebView.invalidate();
        }
        View root = getWindow().getDecorView();
        root.requestLayout();
        root.invalidate();
    }

    @Override public void onBackPressed() {
        if (showingBrowser && browserWebView != null && browserWebView.canGoBack()) {
            browserWebView.goBack();
        } else if (showingBrowser || showingDiagnostics || showingSettings || showingOfflineSaveEditor || (studioMode && currentConvertSection != 0)) {
            showConvertPage(0);
        } else {
            super.onBackPressed();
        }
    }

    private void buildUi() {
        studioMode = getSharedPreferences("appearance", MODE_PRIVATE)
                .getBoolean(StudioUiPolicy.PREFERENCE_KEY, StudioUiPolicy.DEFAULT_STUDIO_MODE);
        android.content.res.Configuration config = getResources().getConfiguration();
        tabletMode = ResponsiveLayoutPolicy.isTabletMode(config.screenWidthDp, config.smallestScreenWidthDp);
        ScrollView scroll = new ScrollView(this);
        convertScroll = scroll;
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setFocusableInTouchMode(true);
        page.requestFocus();
        int horizontalInset = studioMode ? (tabletMode ? 28 : 14) : 18;
        page.setPadding(dp(horizontalInset), dp(studioMode ? 20 : 16), dp(horizontalInset), dp(20));
        page.setBackgroundColor(BG);
        scroll.addView(page);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(22), dp(22), dp(22), dp(23));
        hero.setBackground(new StudioHeaderDrawable());
        TextView brand = label("APK Forge V3", 13, AQUA, true);
        hero.addView(brand);
        TextView title = label(tabletMode ? "Build an Android game" : "Build an\nAndroid game",
                tabletMode ? 36 : 32, WHITE, true);
        if (Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        title.setLetterSpacing(-0.025f);
        title.setPadding(0, dp(12), 0, dp(8));
        hero.addView(title);
        hero.addView(label("Package HTML and Ren’Py games on this device.", 14, MUTED, false));
        page.addView(hero, full());
        final View[] destinations = new View[3];
        destinations[0] = hero;
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        Button convertTab = button("Convert", true);
        Button libraryTab = button("Library", false);
        Button installedTab = button("Installed", false);
        convertTab.setOnClickListener(v -> scroll.smoothScrollTo(0, destinations[0].getTop()));
        libraryTab.setOnClickListener(v -> scroll.smoothScrollTo(0, destinations[1] == null ? 0 : destinations[1].getTop()));
        installedTab.setOnClickListener(v -> scroll.smoothScrollTo(0, destinations[2] == null ? 0 : destinations[2].getTop()));
        nav.addView(convertTab, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(libraryTab, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(installedTab, new LinearLayout.LayoutParams(0, dp(48), 1));
        if (studioMode) nav.setVisibility(View.GONE);
        addGap(page, 10);
        page.addView(nav, full());

        addGap(page, 16);
        LinearLayout source = card(page, "Game source", "Choose an HTML file, a ZIP, or a Ren’Py folder with compiled game data.");
        source.setOnDragListener((view, event) -> {
            if (event.getAction() == android.view.DragEvent.ACTION_DROP && event.getClipData() != null && event.getClipData().getItemCount() > 0) {
                Uri dropped = event.getClipData().getItemAt(0).getUri();
                if (dropped == null) return false;
                selectedFile = dropped;
                selectedFolder = null;
                selectedLocalFile = null;
                selectedText.setText("Dropped file: " + displayName(dropped));
                selectedText.setTextColor(AQUA);
                return true;
            }
            return event.getAction() == android.view.DragEvent.ACTION_DRAG_STARTED || event.getAction() == android.view.DragEvent.ACTION_DRAG_ENDED;
        });
        Button htmlButton = button("Choose HTML file or ZIP", false);
        htmlButton.setOnClickListener(v -> pickFile());
        source.addView(htmlButton, fullWithTop(12));
        Button renpyButton = button("Choose Ren’Py game folder", false);
        renpyButton.setOnClickListener(v -> pickFolder());
        source.addView(renpyButton, fullWithTop(8));
        selectedText = label("No game selected", 12, MUTED, false);
        selectedText.setPadding(0, dp(12), 0, 0);
        source.addView(selectedText);

        addGap(page, 14);
        LinearLayout settings = card(page, "App details", "Keep the same title and package seed when updating a game.");
        TextView nameLabel = label("Launcher title · 16 characters max", 12, MUTED, true);
        nameField = input("Game name");
        nameField.setId(View.generateViewId());
        nameField.setText("My Android Game");
        nameLabel.setLabelFor(nameField.getId());
        settings.addView(nameLabel, fullWithTop(14));
        settings.addView(nameField, fullWithTop(5));
        TextView packageLabel = label("Package identity seed", 12, MUTED, true);
        packageField = input("com.studio.mygame");
        packageField.setId(View.generateViewId());
        packageField.setText("com.gamebridge.game");
        packageLabel.setLabelFor(packageField.getId());
        settings.addView(packageLabel, fullWithTop(12));
        settings.addView(packageField, fullWithTop(5));

        addGap(page, 14);
        Button convert = button("Build playable APK", true);
        buildApkButton = convert;
        convert.setEnabled(!busy);
        convert.setOnClickListener(v -> convert());
        page.addView(convert, full());
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        page.addView(progress, fullWithTop(8));
        statusText = label("Ready to build on this device.", 12, MUTED, false);
        statusText.setGravity(Gravity.CENTER);
        statusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        statusText.setPadding(dp(4), dp(12), dp(4), dp(8));
        page.addView(statusText, full());
        playBuiltButton = button("Play built game", true);
        playBuiltButton.setContentDescription("Launch the latest built game, installing it first if needed");
        playBuiltButton.setEnabled(lastApk != null && lastApk.isFile());
        playBuiltButton.setOnClickListener(v -> playApk(lastApk));
        page.addView(playBuiltButton, full());
        saveApkButton = button("Save finished APK", false);
        saveApkButton.setEnabled(lastApk != null && lastApk.isFile());
        saveApkButton.setOnClickListener(v -> saveApk());
        page.addView(saveApkButton, fullWithTop(8));
        saveGameDataButton = button("Save separate Ren’Py game data ZIP", false);
        saveGameDataButton.setVisibility(lastGameDataBundle != null && lastGameDataBundle.isFile() ? View.VISIBLE : View.GONE);
        saveGameDataButton.setOnClickListener(v -> saveGameData(lastGameDataBundle));
        page.addView(saveGameDataButton, full());
        addGap(page, 18);
        LinearLayout offline = card(page, "Offline packaging",
                "APK Forge signs builds on this device. Export both files when a large Ren’Py build includes a separate data ZIP.");
        addGap(page, 16);
        LinearLayout library = card(page, "Game library", "Play, update or remove your finished builds.");
        librarySection = library;
        destinations[1] = library;
        Button refreshLibraryButton = button("Refresh library", false);
        refreshLibraryButton.setOnClickListener(v -> refreshLibrary());
        library.addView(refreshLibraryButton, fullWithTop(12));
        libraryList = new LinearLayout(this);
        libraryList.setOrientation(LinearLayout.VERTICAL);
        library.addView(libraryList, fullWithTop(8));
        addGap(page, 14);
        LinearLayout installed = card(page, "Installed games", "Open or uninstall apps installed on this device.");
        installedSection = installed;
        destinations[2] = installed;
        Button refreshInstalledButton = button("Refresh installed apps", false);
        refreshInstalledButton.setOnClickListener(v -> refreshInstalled());
        installed.addView(refreshInstalledButton, fullWithTop(12));
        installedList = new LinearLayout(this);
        installedList.setOrientation(LinearLayout.VERTICAL);
        installed.addView(installedList, fullWithTop(8));
        if (studioMode && tabletMode) {
            buildStudioTabletWorkspace(page, hero, offline, source, settings, convert, progress,
                    statusText, playBuiltButton, saveApkButton, saveGameDataButton);
        }
        LinearLayout root = new LinearLayout(this);
        convertRoot = root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(studioMode ? (tabletMode ? 12 : 12) : 18), dp(8), dp(studioMode ? (tabletMode ? 12 : 12) : 18), 0);
        Button menuButton = button(studioMode ? "APK FORGE  /  STUDIO" : "☰     APK FORGE MENU", false);
        menuButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        menuButton.setOnClickListener(v -> showNavigationDrawer());
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(-1, dp(50));
        menuParams.bottomMargin = dp(4);
        root.addView(menuButton, menuParams);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (studioMode && !tabletMode) {
            menuButton.setVisibility(View.GONE);
            buildStudioBottomNavigation(root, 0);
            page.removeView(librarySection);
            page.removeView(installedSection);
        }
        if (tabletMode) {
            menuButton.setVisibility(View.GONE);
            page.removeView(librarySection);
            page.removeView(installedSection);
            buildTabletShell(root);
        } else {
            showAppContent(root);
        }
        if (studioMode) presentStudioView(root);
    }

    private void updateResponsiveLayout(Configuration configuration) {
        boolean tabletNow = ResponsiveLayoutPolicy.isTabletMode(
                configuration.screenWidthDp, configuration.smallestScreenWidthDp);
        if (tabletNow == tabletMode || convertRoot == null) return;

        String savedName = nameField == null ? "My Android Game" : nameField.getText().toString();
        String savedPackage = packageField == null ? "com.gamebridge.game" : packageField.getText().toString();
        String savedSelection = selectedText == null ? "No game selected" : selectedText.getText().toString();
        String savedStatus = statusText == null ? "Ready to build on this device." : statusText.getText().toString();
        int savedScrollY = convertScroll == null ? 0 : convertScroll.getScrollY();
        boolean keepBrowser = showingBrowser;
        boolean keepDiagnostics = showingDiagnostics;
        boolean keepSettings = showingSettings;
        boolean keepSaveEditor = showingOfflineSaveEditor;
        int selectedSection = visibleConvertSection();

        if (navigationDrawer != null && navigationDrawer.isShowing()) navigationDrawer.dismiss();
        // Recreate only the converter chrome; browserPage/browserWebView are retained and reparented below.
        captureOfflineSaveDraft();
        diagnosticsPage = null;
        diagnosticsText = null;
        settingsPage = null;
        offlineSaveEditorPage = null;
        tabletMode = tabletNow;
        buildUi();
        nameField.setText(savedName);
        packageField.setText(savedPackage);
        selectedText.setText(savedSelection);
        statusText.setText(savedStatus);
        setBusy(busy, null);
        refreshLibrary();
        refreshInstalled();

        if (keepBrowser) openBrowser();
        else if (keepDiagnostics) openDiagnostics();
        else if (keepSettings) openSettings();
        else if (keepSaveEditor) openOfflineSaveEditor();
        else {
            showConvertPage(selectedSection);
            if (!tabletNow && selectedSection == 0 && savedScrollY > 0) {
                convertScroll.post(() -> convertScroll.scrollTo(0, savedScrollY));
            }
        }
    }

    private void buildTabletShell(View converterPage) {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setBackgroundColor(BG);
        tabletSidebar = new LinearLayout(this);
        tabletSidebar.setOrientation(LinearLayout.VERTICAL);
        tabletSidebar.setPadding(dp(16), dp(18), dp(16), dp(16));
        tabletSidebar.setBackground(gradient(new int[]{Color.rgb(23, 31, 44), Color.rgb(12, 16, 23)}, 0));
        LinearLayout.LayoutParams sideParams = new LinearLayout.LayoutParams(dp(236), -1);
        shell.addView(tabletSidebar, sideParams);
        tabletSidebar.addView(label("APK Forge V3", 20, WHITE, true), fullWithTop(8));
        addGap(tabletSidebar, 20);
        tabletConvertButton = tabletNavItem(studioMode ? "Convert" : "⌂     Convert", 0);
        tabletBrowserButton = tabletNavItem(studioMode ? "Web workspace" : "◎     Website browser", 1);
        tabletLibraryButton = tabletNavItem(studioMode ? "Library" : "▣     Game library", 2);
        tabletInstalledButton = tabletNavItem(studioMode ? "Installed" : "▤     Installed games", 3);
        tabletDiagnosticsButton = tabletNavItem(studioMode ? "Diagnostics" : "☷     Diagnostics", 4);
        tabletSettingsButton = tabletNavItem(studioMode ? "Settings" : "⚙     Settings", 5);
        tabletSaveEditorButton = tabletNavItem(studioMode ? "Offline saves" : "▧     Offline HTML saves", 6);
        addGap(tabletSidebar, 16);
        tabletContent = new FrameLayout(this);
        tabletContent.setBackgroundColor(BG);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0, -1, 1);
        contentParams.leftMargin = dp(12);
        shell.addView(tabletContent, contentParams);
        tabletContent.addView(converterPage, new FrameLayout.LayoutParams(-1, -1));
        showAppContent(shell);
        selectTabletNav(tabletConvertButton);
    }

    private void buildStudioTabletWorkspace(LinearLayout page, View hero, LinearLayout offline,
            LinearLayout source, LinearLayout appDetails, Button buildButton, ProgressBar buildProgress,
            TextView buildStatus, Button playButton, Button saveButton, Button saveGameData) {
        page.removeAllViews();
        page.setPadding(dp(28), dp(24), dp(28), dp(28));
        page.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(hero, full());

        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.HORIZONTAL);
        workspace.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams workspaceParams = fullWithTop(20);
        page.addView(workspace, workspaceParams);

        LinearLayout projectColumn = new LinearLayout(this);
        projectColumn.setOrientation(LinearLayout.VERTICAL);
        workspace.addView(projectColumn, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams detailsColumnParams = new LinearLayout.LayoutParams(0, -2, 1);
        detailsColumnParams.leftMargin = dp(16);
        LinearLayout detailsColumn = new LinearLayout(this);
        detailsColumn.setOrientation(LinearLayout.VERTICAL);
        workspace.addView(detailsColumn, detailsColumnParams);

        projectColumn.addView(source, full());
        projectColumn.addView(offline, fullWithTop(14));

        detailsColumn.addView(appDetails, full());
        buildStatus.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        buildStatus.setPadding(dp(4), dp(10), dp(4), dp(8));
        detailsColumn.addView(buildButton, fullWithTop(14));
        detailsColumn.addView(buildProgress, fullWithTop(8));
        detailsColumn.addView(buildStatus, full());
        detailsColumn.addView(playButton, fullWithTop(4));
        detailsColumn.addView(saveButton, fullWithTop(4));
        detailsColumn.addView(saveGameData, fullWithTop(8));

    }

    private void buildStudioBottomNavigation(LinearLayout root, int selectedDestination) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(5), dp(5), dp(5), dp(5));
        GradientDrawable navSurface = gradient(new int[]{Color.rgb(41, 53, 71), Color.rgb(25, 34, 48)}, 26);
        navSurface.setStroke(dp(1), Color.rgb(66, 82, 102));
        nav.setBackground(navSurface);
        String[] labels = {"Build", "Web", "Library", "Settings"};
        int[] icons = {R.drawable.nav_build, R.drawable.nav_web, R.drawable.nav_library, R.drawable.nav_settings};
        for (int i = 0; i < labels.length; i++) {
            final int destination = i;
            Button item = button(labels[i], i == selectedDestination);
            item.setTextSize(11);
            item.setMinHeight(dp(48));
            item.setPadding(dp(2), dp(4), dp(2), dp(4));
            item.setCompoundDrawablePadding(dp(1));
            item.setCompoundDrawablesWithIntrinsicBounds(0, icons[i], 0, 0);
            item.setSelected(i == selectedDestination);
            item.setCompoundDrawableTintList(ColorStateList.valueOf(i == selectedDestination ? BG : AQUA));
            item.setOnClickListener(v -> {
                if (destination == 0) showConvertPage(0);
                else if (destination == 1) openBrowser();
                else if (destination == 2) showConvertPage(1);
                else openSettings();
            });
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, dp(52), 1);
            itemParams.leftMargin = dp(i == 0 ? 0 : 4);
            itemParams.rightMargin = dp(i == labels.length - 1 ? 0 : 4);
            nav.addView(item, itemParams);
        }
        LinearLayout.LayoutParams navParams = fullWithTop(6);
        navParams.setMargins(dp(6), dp(6), dp(6), dp(8));
        root.addView(nav, navParams);
    }

    private void showAppContent(View page) {
        if (page.getParent() instanceof ViewGroup) ((ViewGroup) page.getParent()).removeView(page);
        FrameLayout content = new FrameLayout(this);
        content.setBackgroundColor(BG);
        content.addView(page, new FrameLayout.LayoutParams(-1, -1));
        if (Build.VERSION.SDK_INT >= 30) {
            content.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets safe = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                int keyboard = insets.getInsets(WindowInsets.Type.ime()).bottom;
                view.setPadding(safe.left, safe.top, safe.right, Math.max(safe.bottom, keyboard));
                return WindowInsets.CONSUMED;
            });
        }
        setContentView(content);
        content.requestApplyInsets();
    }

    private void showPhonePage(View page, int selectedDestination) {
        if (page.getParent() instanceof ViewGroup) ((ViewGroup) page.getParent()).removeView(page);
        if (studioMode) {
            LinearLayout shell = new LinearLayout(this);
            shell.setOrientation(LinearLayout.VERTICAL);
            shell.setBackgroundColor(BG);
            shell.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
            buildStudioBottomNavigation(shell, selectedDestination);
            showAppContent(shell);
            presentStudioView(shell);
        } else {
            showAppContent(page);
        }
    }

    private void presentStudioView(View view) {
        if (!studioMode || view == null || !ValueAnimator.areAnimatorsEnabled()) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(8));
        view.animate().alpha(1f).translationY(0f).setDuration(180)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private Button tabletNavItem(String text, int destination) {
        Button item = button(text, false);
        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        item.setTextSize(13);
        item.setOnClickListener(v -> {
            if (destination == 0) showConvertPage(0);
            else if (destination == 1) openBrowser();
            else if (destination == 4) openDiagnostics();
            else if (destination == 5) openSettings();
            else if (destination == 6) openOfflineSaveEditor();
            else showConvertPage(destination - 1);
        });
        tabletSidebar.addView(item, fullWithTop(8));
        return item;
    }

    private void selectTabletNav(Button selected) {
        if (!tabletMode) return;
        Button[] items = {tabletConvertButton, tabletBrowserButton, tabletLibraryButton, tabletInstalledButton, tabletDiagnosticsButton, tabletSettingsButton, tabletSaveEditorButton};
        for (Button item : items) if (item != null) {
            styleButton(item, item == selected);
            item.setSelected(item == selected);
        }
    }

    private void showTabletSection(LinearLayout section) {
        if (section.getParent() instanceof ViewGroup) ((ViewGroup) section.getParent()).removeView(section);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));
        content.addView(section, full());
        scroll.addView(content);
        tabletContent.removeAllViews();
        tabletContent.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        presentStudioView(scroll);
    }

    private void openBrowser() {
        showingBrowser = true;
        showingDiagnostics = false;
        showingSettings = false;
        showingOfflineSaveEditor = false;
        if (browserPage == null) browserPage = buildBrowserPage();
        else if (browserWebView == null) {
            if (browserPage.getParent() instanceof ViewGroup) ((ViewGroup) browserPage.getParent()).removeView(browserPage);
            browserPage = buildBrowserPage();
        }
        if (tabletMode) {
            selectTabletNav(tabletBrowserButton);
            if (browserPage.getParent() instanceof ViewGroup) ((ViewGroup) browserPage.getParent()).removeView(browserPage);
            tabletContent.removeAllViews();
            tabletContent.addView(browserPage, new FrameLayout.LayoutParams(-1, -1));
        } else {
            showPhonePage(browserPage, 1);
        }
        presentStudioView(browserPage);
    }

    private void openDiagnostics() {
        showingBrowser = false;
        showingDiagnostics = true;
        showingSettings = false;
        showingOfflineSaveEditor = false;
        if (diagnosticsPage == null) diagnosticsPage = buildDiagnosticsPage();
        refreshDiagnostics();
        if (tabletMode) {
            selectTabletNav(tabletDiagnosticsButton);
            if (diagnosticsPage.getParent() instanceof ViewGroup) ((ViewGroup) diagnosticsPage.getParent()).removeView(diagnosticsPage);
            tabletContent.removeAllViews();
            tabletContent.addView(diagnosticsPage, new FrameLayout.LayoutParams(-1, -1));
        } else showPhonePage(diagnosticsPage, 3);
        presentStudioView(diagnosticsPage);
    }

    private View buildDiagnosticsPage() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(12));
        root.setBackgroundColor(BG);
        Button back = button("‹     APK FORGE MENU", false);
        back.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        if (tabletMode) back.setVisibility(View.GONE);
        back.setOnClickListener(v -> showConvertPage(0));
        root.addView(back, full());
        root.addView(label("Diagnostics", 27, WHITE, true), fullWithTop(16));
        root.addView(label("Private on-device log for troubleshooting. It records event codes, numeric counters, and a sanitized failure type/code location. Website URLs, domains, titles, page contents, filenames, cookies, and error messages are never written. Nothing is uploaded.", 13, MUTED, false), fullWithTop(8));
        LinearLayout actions = new LinearLayout(this);
        Button refresh = button("Refresh", false);
        refresh.setOnClickListener(v -> refreshDiagnostics());
        actions.addView(refresh, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button export = button("Export log…", true);
        export.setOnClickListener(v -> exportDiagnostics());
        LinearLayout.LayoutParams exportParams = new LinearLayout.LayoutParams(0, dp(46), 1.3f);
        exportParams.leftMargin = dp(8);
        actions.addView(export, exportParams);
        Button clear = button("Clear", false);
        clear.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Clear diagnostics?").setMessage("This deletes the local diagnostics log from APK Forge. It cannot be undone.").setNegativeButton("Cancel", null).setPositiveButton("Clear log", (dialog, which) -> { DiagnosticLog.clear(getFilesDir()); refreshDiagnostics(); }).show());
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, dp(46), 0.8f);
        clearParams.leftMargin = dp(8);
        actions.addView(clear, clearParams);
        root.addView(actions, fullWithTop(14));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        diagnosticsText = label("No diagnostic events yet.", 11, GREEN, false);
        diagnosticsText.setTypeface(Typeface.MONOSPACE);
        diagnosticsText.setPadding(dp(12), dp(12), dp(12), dp(12));
        diagnosticsText.setBackground(rounded(INPUT, 13));
        scroll.addView(diagnosticsText);
        LinearLayout.LayoutParams logParams = new LinearLayout.LayoutParams(-1, 0, 1);
        logParams.topMargin = dp(12);
        root.addView(scroll, logParams);
        return root;
    }

    private void openSettings() {
        showingBrowser = false;
        showingDiagnostics = false;
        showingSettings = true;
        showingOfflineSaveEditor = false;
        if (settingsPage == null) settingsPage = buildSettingsPage();
        refreshAssistantSettings();
        if (tabletMode) {
            selectTabletNav(tabletSettingsButton);
            if (settingsPage.getParent() instanceof ViewGroup) ((ViewGroup) settingsPage.getParent()).removeView(settingsPage);
            tabletContent.removeAllViews();
            tabletContent.addView(settingsPage, new FrameLayout.LayoutParams(-1, -1));
        } else showPhonePage(settingsPage, 3);
        presentStudioView(settingsPage);
    }

    private void openOfflineSaveEditor() {
        showingBrowser = false;
        showingDiagnostics = false;
        showingSettings = false;
        showingOfflineSaveEditor = true;
        if (offlineSaveEditorPage == null) offlineSaveEditorPage = buildOfflineSaveEditorPage();
        if (tabletMode) {
            selectTabletNav(tabletSettingsButton);
            if (offlineSaveEditorPage.getParent() instanceof ViewGroup) ((ViewGroup) offlineSaveEditorPage.getParent()).removeView(offlineSaveEditorPage);
            tabletContent.removeAllViews();
            tabletContent.addView(offlineSaveEditorPage, new FrameLayout.LayoutParams(-1, -1));
        } else showPhonePage(offlineSaveEditorPage, 3);
        presentStudioView(offlineSaveEditorPage);
    }

    private View buildOfflineSaveEditorPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(tabletMode ? 28 : 16), dp(14), dp(tabletMode ? 28 : 16), dp(28));
        root.setBackgroundColor(BG);
        scroll.addView(root);

        Button back = button("‹     APK FORGE MENU", false);
        back.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        if (tabletMode) back.setVisibility(View.GONE);
        back.setOnClickListener(v -> showConvertPage(0));
        root.addView(back, full());
        root.addView(label("Offline HTML save editor", 26, WHITE, true), fullWithTop(12));
        root.addView(label("Inspect a save exported by an HTML game packaged as a local file. Edit existing numeric fields, then export and import the save back into that same game.", 14, MUTED, false), fullWithTop(6));

        LinearLayout safety = card(root, "LOCAL SAVE ONLY", "This tool edits an imported APK Forge save backup, not a running game. Online-page backups are rejected; online pages and server-held progress remain read-only.");
        safety.setBackground(rounded(Color.rgb(34, 48, 44), 18));

        LinearLayout workflow = card(root, "HOW TO USE IT", "In your packaged HTML game, open its save menu → Export save backup. Import that .apkforge-save here, review the detected fields, and edit only values you recognize. Export the edited backup, then use the game’s Import save backup option.");
        workflow.addView(label("Only browser localStorage is included. Website accounts, server state, cookies, IndexedDB and the game's code are not changed.", 12, MUTED, false), fullWithTop(8));

        Button importButton = button("Import .apkforge-save backup", true);
        importButton.setOnClickListener(v -> chooseOfflineSaveBackup());
        root.addView(importButton, fullWithTop(12));

        LinearLayout fieldsCard = card(root, "NUMERIC SAVE FIELDS", "Only existing numbers or numeric strings are listed. Paths are shown as stored; “possible currency” is a name-based hint, not a guarantee.");
        offlineSaveFields = new LinearLayout(this);
        offlineSaveFields.setOrientation(LinearLayout.VERTICAL);
        fieldsCard.addView(offlineSaveFields, fullWithTop(8));
        offlineSaveStatus = label("Import an offline HTML save backup to begin.", 13, MUTED, false);
        offlineSaveStatus.setPadding(dp(11), dp(10), dp(11), dp(10));
        offlineSaveStatus.setBackground(rounded(INPUT, 12));
        fieldsCard.addView(offlineSaveStatus, fullWithTop(8));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button apply = button("Apply to backup copy", true);
        apply.setOnClickListener(v -> applyOfflineSaveEdits());
        actions.addView(apply, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button export = button("Export…", false);
        export.setOnClickListener(v -> chooseOfflineSaveExport());
        LinearLayout.LayoutParams exportParams = new LinearLayout.LayoutParams(0, dp(50), 0.72f);
        exportParams.leftMargin = dp(8);
        actions.addView(export, exportParams);
        root.addView(actions, fullWithTop(10));

        LinearLayout aiCard = card(root, "OPTIONAL ON-DEVICE AI", "AI can suggest which field names might represent virtual currency. It receives only the local field paths and numbers shown above—not the site, URL, save file, or game content. It never applies edits.");
        Button analyze = button("Analyze fields with local AI", false);
        analyze.setOnClickListener(v -> analyzeOfflineSaveFields());
        aiCard.addView(analyze, fullWithTop(10));
        offlineSaveAiResult = label("AI is optional. Enable it and install a model in Settings to use this analysis.", 13, MUTED, false);
        offlineSaveAiResult.setPadding(dp(11), dp(10), dp(11), dp(10));
        offlineSaveAiResult.setBackground(rounded(INPUT, 12));
        aiCard.addView(offlineSaveAiResult, fullWithTop(8));
        Button settings = button("Open local AI settings", false);
        settings.setOnClickListener(v -> openSettings());
        aiCard.addView(settings, fullWithTop(8));
        try {
            Map<String, String> saved = OfflineHtmlSaveEditor.numericFields(getFilesDir());
            renderOfflineSaveFields(saved);
        } catch (Exception ignored) { }
        return scroll;
    }

    private void chooseOfflineSaveBackup() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.setType("application/octet-stream");
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(picker, OPEN_OFFLINE_SAVE_BACKUP);
    }

    private void chooseOfflineSaveExport() {
        if (offlineSaveInputs.isEmpty()) {
            Toast.makeText(this, "Import an offline save backup first.", Toast.LENGTH_SHORT).show();
            return;
        }
        LinkedHashMap<String, String> pending = new LinkedHashMap<>();
        for (Map.Entry<String, EditText> field : offlineSaveInputs.entrySet()) pending.put(field.getKey(), field.getValue().getText().toString());
        try {
            OfflineHtmlSaveEditor.applyValues(getFilesDir(), pending);
            offlineSaveDraft.clear();
            offlineSaveDraft.putAll(pending);
        } catch (Exception invalid) {
            offlineSaveStatus.setText("Export cancelled: check every numeric field; no invalid changes were applied.");
            offlineSaveStatus.setTextColor(Color.rgb(255, 190, 120));
            return;
        }
        Intent picker = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        picker.setType("application/octet-stream");
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.putExtra(Intent.EXTRA_TITLE, "edited-offline-game-save.apkforge-save");
        startActivityForResult(picker, EXPORT_OFFLINE_SAVE_BACKUP);
    }

    private void loadOfflineSaveBackup(Uri source) {
        offlineSaveDraft.clear();
        offlineSaveStatus.setText("Reading and validating the local backup…");
        worker.execute(() -> {
            try (InputStream input = getContentResolver().openInputStream(source)) {
                if (input == null) throw new IllegalStateException("Could not read the selected save backup.");
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int count;
                int maximum = 8 * 1024 * 1024 + 128 * 1024;
                while ((count = input.read(buffer)) != -1) {
                    if (count > maximum - bytes.size()) throw new IllegalArgumentException("Save backup exceeds the supported size.");
                    bytes.write(buffer, 0, count);
                }
                OfflineHtmlSaveEditor.importBackup(getFilesDir(), bytes.toByteArray());
                Map<String, String> fields = OfflineHtmlSaveEditor.numericFields(getFilesDir());
                runOnUiThread(() -> renderOfflineSaveFields(fields));
            } catch (Exception failure) {
                runOnUiThread(() -> {
                    offlineSaveInputs.clear();
                    offlineSaveFields.removeAllViews();
                    offlineSaveStatus.setText("Backup rejected or unreadable. Choose an APK Forge save exported from a local-file HTML game. Online-page backups cannot be edited here.");
                    offlineSaveStatus.setTextColor(Color.rgb(255, 190, 120));
                    Toast.makeText(this, "Could not import that offline save backup.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void renderOfflineSaveFields(Map<String, String> values) {
        offlineSaveInputs.clear();
        offlineSaveFields.removeAllViews();
        if (values.isEmpty()) {
            offlineSaveStatus.setText("Save imported, but no editable numeric fields were found.");
            offlineSaveStatus.setTextColor(MUTED);
            return;
        }
        for (Map.Entry<String, String> field : values.entrySet()) {
            String path = field.getKey();
            TextView name = label(path + (OfflineHtmlSaveEditor.looksLikeCurrency(path) ? "  ·  possible currency" : ""), 12, OfflineHtmlSaveEditor.looksLikeCurrency(path) ? AMBER : WHITE, true);
            offlineSaveFields.addView(name, fullWithTop(7));
            EditText value = input("Numeric value");
            value.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            value.setText(offlineSaveDraft.containsKey(path) ? offlineSaveDraft.get(path) : field.getValue());
            offlineSaveFields.addView(value, fullWithTop(4));
            offlineSaveInputs.put(path, value);
        }
        offlineSaveStatus.setText(values.size() + " numeric fields loaded into a private offline copy. Applying changes does not alter the source game file.");
        offlineSaveStatus.setTextColor(GREEN);
    }

    private void applyOfflineSaveEdits() {
        if (offlineSaveInputs.isEmpty()) {
            Toast.makeText(this, "Import an offline save backup first.", Toast.LENGTH_SHORT).show();
            return;
        }
        LinkedHashMap<String, String> updates = new LinkedHashMap<>();
        for (Map.Entry<String, EditText> field : offlineSaveInputs.entrySet()) updates.put(field.getKey(), field.getValue().getText().toString());
        try {
            OfflineHtmlSaveEditor.applyValues(getFilesDir(), updates);
            offlineSaveDraft.clear();
            offlineSaveDraft.putAll(updates);
            renderOfflineSaveFields(OfflineHtmlSaveEditor.numericFields(getFilesDir()));
            offlineSaveStatus.setText("Edits applied to the private backup copy. Export it, then import it through the same local game’s save menu.");
        } catch (Exception failure) {
            offlineSaveStatus.setText("No edits were applied. Check every numeric value and try again.");
            offlineSaveStatus.setTextColor(Color.rgb(255, 190, 120));
        }
    }

    private void analyzeOfflineSaveFields() {
        if (offlineSaveInputs.isEmpty()) {
            offlineSaveAiResult.setText("Import a local HTML save backup first.");
            return;
        }
        if (!getSharedPreferences("repair-assistant", MODE_PRIVATE).getBoolean("enabled", false)) {
            offlineSaveAiResult.setText("Local AI is off. Enable it in Settings and install a model to analyze these field names.");
            return;
        }
        if (!RepairAssistantModel.isInstalled(this)) {
            offlineSaveAiResult.setText("The selected local model is not installed yet. Download it in Settings, then run this analysis again.");
            return;
        }
        offlineSaveAiResult.setText("Analyzing only the displayed local field paths and values on this device…");
        assistantWorker.execute(() -> {
            try {
                String prompt = OfflineHtmlSaveEditor.analysisPrompt(getFilesDir());
                String answer = RepairAssistantModel.analyzeOfflineSave(this, prompt);
                runOnUiThread(() -> offlineSaveAiResult.setText(answer));
            } catch (Exception failure) {
                runOnUiThread(() -> offlineSaveAiResult.setText("Local analysis failed. Check model settings and available memory, then retry."));
            } catch (OutOfMemoryError exhausted) {
                runOnUiThread(() -> offlineSaveAiResult.setText("This device did not have enough free memory for local analysis. Try the Compact model or close other apps."));
            }
        });
    }

    private void exportOfflineSaveBackup(Uri destination) {
        worker.execute(() -> {
            try {
                byte[] backup = OfflineHtmlSaveEditor.exportBackup(getFilesDir());
                try (OutputStream output = getContentResolver().openOutputStream(destination)) {
                    if (output == null) throw new IllegalStateException("Could not open the chosen export location.");
                    output.write(backup);
                }
                runOnUiThread(() -> Toast.makeText(this, "Edited offline save backup exported. Import it from the local game’s save menu.", Toast.LENGTH_LONG).show());
            } catch (Exception failure) {
                runOnUiThread(() -> Toast.makeText(this, "Could not export the edited save backup.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void captureOfflineSaveDraft() {
        if (!showingOfflineSaveEditor || offlineSaveInputs.isEmpty()) return;
        offlineSaveDraft.clear();
        for (Map.Entry<String, EditText> field : offlineSaveInputs.entrySet()) {
            offlineSaveDraft.put(field.getKey(), field.getValue().getText().toString());
        }
    }

    private View buildSettingsPage() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(24));
        root.setBackgroundColor(BG);
        Button back = button("‹     APK FORGE MENU", false);
        back.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        if (tabletMode) back.setVisibility(View.GONE);
        back.setOnClickListener(v -> showConvertPage(0));
        root.addView(back, full());
        root.addView(label("Settings", 27, WHITE, true), fullWithTop(14));
        root.addView(label("Appearance, diagnostics and on-device repair tools.", 13, MUTED, false), fullWithTop(4));
        addGap(root, 14);

        LinearLayout appearance = card(root, "Appearance", "Choose between Studio navigation and the Classic layout.");
        LinearLayout appearanceChoices = new LinearLayout(this);
        appearanceChoices.setOrientation(LinearLayout.HORIZONTAL);
        classicUiButton = button("Classic", false);
        studioUiButton = button("Studio", false);
        appearanceChoices.addView(classicUiButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams studioParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        studioParams.leftMargin = dp(8);
        appearanceChoices.addView(studioUiButton, studioParams);
        appearance.addView(appearanceChoices, fullWithTop(10));
        classicUiButton.setOnClickListener(v -> setStudioUiEnabled(false));
        studioUiButton.setOnClickListener(v -> setStudioUiEnabled(true));

        addGap(root, 14);
        LinearLayout aiCard = card(root, "Local repair assistant", "Optional downloadable CPU models. Built-in repair checks work without AI; model answers are suggestions, not automatic game edits.");
        localAiSwitch = new Switch(this);
        localAiSwitch.setText("Enable local AI repair help");
        localAiSwitch.setTextColor(WHITE);
        localAiSwitch.setTextSize(14);
        localAiSwitch.setPadding(0, dp(8), 0, dp(8));
        aiCard.addView(localAiSwitch, fullWithTop(10));
        aiCard.addView(label("Choose a model for this device", 13, WHITE, true), fullWithTop(12));
        LinearLayout modelChoices = new LinearLayout(this);
        modelChoices.setOrientation(LinearLayout.HORIZONTAL);
        compactModelButton = button("Compact\n135M · 143 MB", false);
        gemmaModelButton = button("Gemma 4 E2B\n2.59 GB · Advanced", false);
        modelChoices.addView(compactModelButton, new LinearLayout.LayoutParams(0, dp(58), 1));
        LinearLayout.LayoutParams gemmaChoiceParams = new LinearLayout.LayoutParams(0, dp(58), 1);
        gemmaChoiceParams.leftMargin = dp(8);
        modelChoices.addView(gemmaModelButton, gemmaChoiceParams);
        aiCard.addView(modelChoices, fullWithTop(7));
        compactModelButton.setOnClickListener(v -> selectAssistantModel(RepairModelCatalog.COMPACT_ID));
        gemmaModelButton.setOnClickListener(v -> selectAssistantModel(RepairModelCatalog.GEMMA_ID));
        TextView modelState = label("Selected model status appears here.", 12, MUTED, false);
        modelState.setTag("assistant-model-state");
        aiCard.addView(modelState, fullWithTop(5));
        aiCard.addView(label("Compact is intended for lower-memory devices. Gemma downloads 2.59 GB and needs at least 2 GB currently free RAM before inference; it may be slow or unavailable on weaker phones. Neither model is included in the APK.", 12, MUTED, false), fullWithTop(4));
        localAiSwitch.setOnCheckedChangeListener((button, checked) -> onAssistantToggleChanged(checked));
        Button importModel = button("Import verified model file…", false);
        importModel.setOnClickListener(v -> pickRepairModel());
        aiCard.addView(importModel, fullWithTop(8));
        aiCard.addView(label("Useful when this device is offline: APK Forge accepts only the exact pinned model checksum.", 11, MUTED, false), fullWithTop(4));

        addGap(root, 14);
        LinearLayout onlineCard = card(root, "WEB VERSION & FILE CHECKS", "Online checks are separate and off by default. APK Forge never runs Windows EXE files or uploads them.");
        onlineLookupSwitch = new Switch(this);
        onlineLookupSwitch.setText("Allow user-started web lookups");
        onlineLookupSwitch.setTextColor(WHITE);
        onlineLookupSwitch.setTextSize(14);
        onlineCard.addView(onlineLookupSwitch, fullWithTop(8));
        onlineLookupSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !localAiSwitch.isChecked()) {
                onlineLookupSwitch.setChecked(false);
                Toast.makeText(this, "Enable the local assistant first.", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences("repair-assistant", MODE_PRIVATE).edit().putBoolean("online_lookup", checked).apply();
        });
        Button checkExe = button("Choose EXE · check its hash reputation", false);
        checkExe.setOnClickListener(v -> pickExeForHashCheck());
        onlineCard.addView(checkExe, fullWithTop(8));
        onlineCard.addView(label("Hash lookup sends only the SHA-256. A separate, confirmed VirusTotal upload shares the selected sample with its security partners. Neither result guarantees safety.", 12, MUTED, false), fullWithTop(7));

        addGap(root, 14);
        LinearLayout helperCard = card(root, "ASK ABOUT A CONVERSION ERROR", "Keep the report focused: the assistant receives only the text you enter. Website URLs and page contents are not added automatically.");
        repairQuestion = new EditText(this);
        repairQuestion.setHint("Describe the error or paste a short message…");
        repairQuestion.setHintTextColor(MUTED);
        repairQuestion.setTextColor(WHITE);
        repairQuestion.setTextSize(14);
        repairQuestion.setGravity(Gravity.TOP | Gravity.START);
        repairQuestion.setMinLines(3);
        repairQuestion.setMaxLines(6);
        repairQuestion.setPadding(dp(12), dp(10), dp(12), dp(10));
        repairQuestion.setBackground(rounded(INPUT, 13));
        helperCard.addView(repairQuestion, fullWithTop(10));
        Button diagnosticsButton = button("Load local diagnostics (review before analysis)", false);
        diagnosticsButton.setOnClickListener(v -> {
            String log = DiagnosticLog.read(getFilesDir());
            if (log.isEmpty()) {
                Toast.makeText(this, "There are no local diagnostics yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (log.length() > 2800) log = log.substring(log.length() - 2800);
            repairQuestion.setText(log);
        });
        helperCard.addView(diagnosticsButton, fullWithTop(8));
        Button ask = button("Analyze on this device", true);
        ask.setOnClickListener(v -> runRepairAssistant(repairQuestion.getText().toString()));
        helperCard.addView(ask, fullWithTop(8));
        assistantResult = label("Assistant is off.", 13, MUTED, false);
        assistantResult.setPadding(dp(11), dp(10), dp(11), dp(10));
        assistantResult.setBackground(rounded(INPUT, 12));
        helperCard.addView(assistantResult, fullWithTop(8));
        helperCard.addView(label("CPU-only inference is not benchmarked on this device. Compact can give weak or wrong answers; Gemma may be stronger but requires much more memory. No game files are changed, no EXE is run, and AI is not a malware scan.", 11, MUTED, false), fullWithTop(7));
        assistantActionButton = button("Open official Ren’Py version page", false);
        assistantActionButton.setVisibility(View.GONE);
        assistantActionButton.setOnClickListener(v -> runVisibleAssistantAction());
        helperCard.addView(assistantActionButton, fullWithTop(8));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        return scroll;
    }

    private void refreshAssistantSettings() {
        if (localAiSwitch == null || onlineLookupSwitch == null) return;
        android.content.SharedPreferences preferences = getSharedPreferences("repair-assistant", MODE_PRIVATE);
        localAiSwitch.setOnCheckedChangeListener(null);
        localAiSwitch.setChecked(preferences.getBoolean("enabled", false));
        localAiSwitch.setOnCheckedChangeListener((button, checked) -> onAssistantToggleChanged(checked));
        onlineLookupSwitch.setOnCheckedChangeListener(null);
        onlineLookupSwitch.setChecked(preferences.getBoolean("online_lookup", false));
        onlineLookupSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !localAiSwitch.isChecked()) {
                onlineLookupSwitch.setChecked(false);
                Toast.makeText(this, "Enable the local assistant first.", Toast.LENGTH_SHORT).show();
                return;
            }
            preferences.edit().putBoolean("online_lookup", checked).apply();
        });
        RepairModelCatalog.Model model = RepairAssistantModel.selectedModel(this);
        boolean installed = RepairAssistantModel.isInstalled(this, model);
        TextView status = settingsPage.findViewWithTag("assistant-model-state");
        if (status != null) status.setText(installed
                ? model.displayName + " ready · CPU inference · loaded only while answering"
                : model.displayName + " · " + (model.bytes / (1024L * 1024L)) + " MiB download · Apache-2.0 · not installed");
        renderSettingsChoices();
        assistantActionButton.setVisibility(View.GONE);
        visibleAssistantAction = RepairActionGate.Action.NONE;
        assistantResult.setText(preferences.getBoolean("enabled", false)
                ? "Ready. Enter a conversion error and ask for suggested repair steps."
                : "Assistant is off.");
    }

    private void setStudioUiEnabled(boolean enabled) {
        boolean current = getSharedPreferences("appearance", MODE_PRIVATE)
                .getBoolean(StudioUiPolicy.PREFERENCE_KEY, StudioUiPolicy.DEFAULT_STUDIO_MODE);
        if (current == enabled) return;
        String savedName = nameField == null ? "My Android Game" : nameField.getText().toString();
        String savedPackage = packageField == null ? "com.gamebridge.game" : packageField.getText().toString();
        String savedSelection = selectedText == null ? "No game selected" : selectedText.getText().toString();
        String savedStatus = statusText == null ? "Ready to build on this device." : statusText.getText().toString();
        boolean keepBrowser = showingBrowser;
        boolean keepDiagnostics = showingDiagnostics;
        boolean keepSettings = showingSettings;
        boolean keepSaveEditor = showingOfflineSaveEditor;
        int selectedSection = visibleConvertSection();
        int savedScroll = convertScroll == null ? 0 : convertScroll.getScrollY();
        getSharedPreferences("appearance", MODE_PRIVATE).edit()
                .putBoolean(StudioUiPolicy.PREFERENCE_KEY, enabled).apply();
        captureOfflineSaveDraft();
        diagnosticsPage = null;
        diagnosticsText = null;
        settingsPage = null;
        offlineSaveEditorPage = null;
        buildUi();
        nameField.setText(savedName);
        packageField.setText(savedPackage);
        selectedText.setText(savedSelection);
        statusText.setText(savedStatus);
        setBusy(busy, null);
        refreshLibrary();
        refreshInstalled();
        if (keepBrowser) openBrowser();
        else if (keepDiagnostics) openDiagnostics();
        else if (keepSettings) openSettings();
        else if (keepSaveEditor) openOfflineSaveEditor();
        else {
            showConvertPage(selectedSection);
            if (!tabletMode && savedScroll > 0) convertScroll.post(() -> convertScroll.scrollTo(0, savedScroll));
        }
    }

    private void renderSettingsChoices() {
        boolean gemma = RepairModelCatalog.isGemma(RepairAssistantModel.selectedModel(this).id);
        if (compactModelButton != null) {
            styleButton(compactModelButton, !gemma);
            compactModelButton.setSelected(!gemma);
        }
        if (gemmaModelButton != null) {
            styleButton(gemmaModelButton, gemma);
            gemmaModelButton.setSelected(gemma);
        }
        boolean studio = getSharedPreferences("appearance", MODE_PRIVATE)
                .getBoolean(StudioUiPolicy.PREFERENCE_KEY, StudioUiPolicy.DEFAULT_STUDIO_MODE);
        if (classicUiButton != null) {
            styleButton(classicUiButton, !studio);
            classicUiButton.setSelected(!studio);
        }
        if (studioUiButton != null) {
            styleButton(studioUiButton, studio);
            studioUiButton.setSelected(studio);
        }
    }

    private void selectAssistantModel(String modelId) {
        android.content.SharedPreferences preferences = getSharedPreferences("repair-assistant", MODE_PRIVATE);
        if (modelId.equals(preferences.getString("model_id", RepairModelCatalog.COMPACT_ID))) return;
        preferences.edit().putString("model_id", modelId).putBoolean("enabled", false).putBoolean("online_lookup", false).apply();
        if (localAiSwitch != null) localAiSwitch.setChecked(false);
        if (onlineLookupSwitch != null) onlineLookupSwitch.setChecked(false);
        refreshAssistantSettings();
        Toast.makeText(this, "Model selected. Enable local AI to download it.", Toast.LENGTH_LONG).show();
    }

    private void onAssistantToggleChanged(boolean checked) {
        android.content.SharedPreferences preferences = getSharedPreferences("repair-assistant", MODE_PRIVATE);
        if (!checked) {
            preferences.edit().putBoolean("enabled", false).apply();
            if (onlineLookupSwitch != null) onlineLookupSwitch.setChecked(false);
            return;
        }
        if (RepairAssistantModel.isInstalled(this)) {
            preferences.edit().putBoolean("enabled", true).apply();
            Toast.makeText(this, "Local repair assistant enabled.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Download the local AI model?")
                .setMessage("APK Forge will download " + RepairAssistantModel.selectedModel(this).displayName + " (" + (RepairAssistantModel.selectedModel(this).bytes / (1024L * 1024L)) + " MiB) from its checksum-pinned LiteRT Community release over HTTPS. It stays in this app's private storage and is not included in game APKs.")
                .setNegativeButton("Not now", (dialog, which) -> localAiSwitch.setChecked(false))
                .setPositiveButton("Download & enable", (dialog, which) -> downloadRepairModel())
                .setOnCancelListener(dialog -> localAiSwitch.setChecked(false))
                .show();
    }

    private void downloadRepairModel() {
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        TextView status = label("Connecting to the model host…", 13, MUTED, false);
        LinearLayout content = new LinearLayout(this);
        content.setPadding(dp(20), dp(8), dp(20), dp(8));
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(status, fullWithTop(8));
        content.addView(bar, fullWithTop(10));
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Downloading on-device model")
                .setView(content)
                .setNegativeButton("Cancel", (dialog, which) -> cancelled.set(true))
                .create();
        progressDialog.setOnCancelListener(dialog -> cancelled.set(true));
        progressDialog.show();
        assistantWorker.execute(() -> {
            try {
                RepairAssistantModel.download(this, RepairAssistantModel.selectedModel(this), new RepairAssistantModel.ProgressListener() {
                    @Override public void onProgress(long received, long expected) {
                        runOnUiThread(() -> {
                            if (progressDialog.isShowing() && expected > 0) {
                                int percent = (int) Math.min(100, received * 100 / expected);
                                bar.setProgress(percent);
                                status.setText(String.format(Locale.ROOT, "Downloaded %.1f / %.1f MiB", received / 1048576f, expected / 1048576f));
                            }
                        });
                    }
                    @Override public boolean isCancelled() { return cancelled.get(); }
                });
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    getSharedPreferences("repair-assistant", MODE_PRIVATE).edit().putBoolean("enabled", true).apply();
                    if (localAiSwitch != null) localAiSwitch.setChecked(true);
                    refreshAssistantSettings();
                    Toast.makeText(this, "Model downloaded. Local assistant is ready.", Toast.LENGTH_LONG).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (localAiSwitch != null) localAiSwitch.setChecked(false);
                    getSharedPreferences("repair-assistant", MODE_PRIVATE).edit().putBoolean("enabled", false).apply();
                    if (assistantResult != null) assistantResult.setText(cancelled.get()
                            ? "Model download cancelled. The incomplete download was removed."
                            : "Model download failed. Check your connection and try again.");
                    Toast.makeText(this, cancelled.get() ? "Model download cancelled." : "Could not download the model. No partial model was kept.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void pickRepairModel() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("application/octet-stream");
        picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream", "application/x-binary", "*/*"});
        startActivityForResult(picker, IMPORT_REPAIR_MODEL);
    }

    private void importRepairModel(Uri uri) {
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        TextView status = label("Checking the selected model…", 13, MUTED, false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(8));
        content.addView(status, fullWithTop(8));
        content.addView(bar, fullWithTop(10));
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Import verified AI model")
                .setView(content)
                .setNegativeButton("Cancel", (d, which) -> cancelled.set(true))
                .create();
        dialog.setOnCancelListener(d -> cancelled.set(true));
        dialog.show();
        assistantWorker.execute(() -> {
            try {
                InputStream input = getContentResolver().openInputStream(uri);
                RepairAssistantModel.importModel(this, input, new RepairAssistantModel.ProgressListener() {
                    @Override public void onProgress(long received, long expected) {
                        runOnUiThread(() -> {
                            if (dialog.isShowing()) {
                                bar.setProgress((int) Math.min(100, received * 100 / expected));
                                status.setText(String.format(Locale.ROOT, "Verified model copy: %.1f / %.1f MiB", received / 1048576f, expected / 1048576f));
                            }
                        });
                    }
                    @Override public boolean isCancelled() { return cancelled.get(); }
                });
                runOnUiThread(() -> {
                    dialog.dismiss();
                    refreshAssistantSettings();
                    Toast.makeText(this, "The pinned local model is ready. Turn it on when you want to use AI.", Toast.LENGTH_LONG).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    assistantResult.setText(cancelled.get()
                            ? "Model import cancelled. The incomplete copy was removed."
                            : "Model import failed: only the exact pinned LiteRT model is accepted.");
                    Toast.makeText(this, cancelled.get() ? "Model import cancelled." : "The selected file did not match the trusted model.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void runRepairAssistant(String question) {
        android.content.SharedPreferences preferences = getSharedPreferences("repair-assistant", MODE_PRIVATE);
        if (!RepairAssistantPolicy.isEnabled(preferences.getBoolean("enabled", false))) {
            assistantResult.setText("Turn on the local assistant in Settings first.");
            return;
        }
        if (!RepairAssistantModel.isInstalled(this)) {
            assistantResult.setText("The local model is missing. Turn the assistant off and on to download it.");
            return;
        }
        RepairAdvice.Advice knownAdvice = RepairAdvice.analyze(question);
        if (knownAdvice.kind != RepairAdvice.Kind.UNKNOWN) {
            assistantResult.setText("APK Forge repair guide (known limits):\n\n" + knownAdvice.message);
            showAssistantAction(knownAdvice.action);
            return;
        }
        assistantResult.setText("Loading the small model on CPU…");
        assistantActionButton.setVisibility(View.GONE);
        visibleAssistantAction = RepairActionGate.Action.NONE;
        assistantWorker.execute(() -> {
            try {
                String answer = RepairAssistantModel.generate(this, question);
                RepairActionGate.Action action = RepairActionGate.Action.valueOf(
                        RepairAssistantPolicy.normalizeModelAction(answer));
                runOnUiThread(() -> {
                    assistantResult.setText(answer);
                    showAssistantAction(action);
                });
            } catch (Exception error) {
                String detail = error.getMessage();
                if (detail == null || detail.isBlank()) detail = "No additional details were provided.";
                if (detail.length() > 180) detail = detail.substring(0, 180);
                String safeDetail = detail;
                runOnUiThread(() -> assistantResult.setText("The on-device model could not answer ("
                        + error.getClass().getSimpleName() + "): " + safeDetail));
            } catch (OutOfMemoryError lowMemory) {
                runOnUiThread(() -> assistantResult.setText("This device did not have enough free memory for the local model. Close other apps or use APK Forge’s built-in repair checks instead."));
            }
        });
    }

    private void showAssistantAction(RepairActionGate.Action action) {
        visibleAssistantAction = action == null ? RepairActionGate.Action.NONE : action;
        if (assistantActionButton == null) return;
        boolean onlineAllowed = RepairAssistantPolicy.maySearchOnline(
                getSharedPreferences("repair-assistant", MODE_PRIVATE).getBoolean("enabled", false),
                getSharedPreferences("repair-assistant", MODE_PRIVATE).getBoolean("online_lookup", false));
        if (visibleAssistantAction == RepairActionGate.Action.OPEN_OFFICIAL_VERSION_SEARCH && onlineAllowed) {
            assistantActionButton.setText("Check official Ren’Py version page");
            assistantActionButton.setVisibility(View.VISIBLE);
        } else if (visibleAssistantAction == RepairActionGate.Action.OPEN_EXE_HASH_CHECK && onlineAllowed) {
            assistantActionButton.setText("Choose EXE & check hash reputation");
            assistantActionButton.setVisibility(View.VISIBLE);
        } else if (visibleAssistantAction == RepairActionGate.Action.OPEN_EXE_VERSION_LOOKUP && onlineAllowed) {
            assistantActionButton.setText("Choose EXE · look up version & reputation");
            assistantActionButton.setVisibility(View.VISIBLE);
        } else if (visibleAssistantAction == RepairActionGate.Action.RETRY_CAPTURE) {
            assistantActionButton.setText("Retry offline capture…");
            assistantActionButton.setVisibility(View.VISIBLE);
        } else if (visibleAssistantAction == RepairActionGate.Action.REBUILD_WITH_TOUCH_SUPPORT) {
            assistantActionButton.setText("Rebuild with touch support…");
            assistantActionButton.setVisibility(View.VISIBLE);
        } else if (visibleAssistantAction == RepairActionGate.Action.REBUILD_WITH_LOCAL_SAVE_SUPPORT) {
            assistantActionButton.setText("Rebuild with local save support…");
            assistantActionButton.setVisibility(View.VISIBLE);
        } else if (visibleAssistantAction == RepairActionGate.Action.OPEN_WEBSITE_WORKSPACE) {
            assistantActionButton.setText("Open website workspace to retry capture");
            assistantActionButton.setVisibility(View.VISIBLE);
        } else {
            assistantActionButton.setVisibility(View.GONE);
        }
    }

    private void runVisibleAssistantAction() {
        if (visibleAssistantAction == RepairActionGate.Action.OPEN_OFFICIAL_VERSION_SEARCH) {
            openOfficialRenPyVersions();
        } else if (visibleAssistantAction == RepairActionGate.Action.OPEN_EXE_HASH_CHECK) {
            pickExeForHashCheck();
        } else if (visibleAssistantAction == RepairActionGate.Action.OPEN_EXE_VERSION_LOOKUP) {
            pickExeForHashCheck();
        } else if (visibleAssistantAction == RepairActionGate.Action.RETRY_CAPTURE) {
            startGuidedHtmlRebuild("Open the live game page, play through the screens you need, then capture it again.");
        } else if (visibleAssistantAction == RepairActionGate.Action.REBUILD_WITH_TOUCH_SUPPORT) {
            startGuidedHtmlRebuild("Open the live game page before rebuilding with touch support.");
        } else if (visibleAssistantAction == RepairActionGate.Action.REBUILD_WITH_LOCAL_SAVE_SUPPORT) {
            startGuidedLocalSaveRebuild();
        } else if (visibleAssistantAction == RepairActionGate.Action.OPEN_WEBSITE_WORKSPACE) {
            openBrowser();
        }
    }

    private void startGuidedHtmlRebuild(String missingPageMessage) {
        selectedWebsiteMode = WebBuildMode.Mode.OFFLINE;
        updateWebsiteMode();
        openBrowser();
        if (browserWebView != null && isHttpUrl(browserWebView.getUrl())) {
            captureCurrentWebsite();
        } else {
            setBrowserStatus(missingPageMessage, Color.rgb(255, 190, 120));
        }
    }

    private void startGuidedLocalSaveRebuild() {
        openBrowser();
        if (browserWebView == null || !isHttpUrl(browserWebView.getUrl())) {
            setBrowserStatus("Open the HTML game page you want to rebuild. APK Forge can checkpoint localStorage saves, not server or IndexedDB saves.", Color.rgb(255, 190, 120));
        } else if (selectedWebsiteMode == WebBuildMode.Mode.ONLINE) {
            wrapCurrentWebsite();
        } else {
            captureCurrentWebsite();
        }
    }

    private void openOfficialRenPyVersions() {
        if (!RepairAssistantPolicy.maySearchOnline(
                getSharedPreferences("repair-assistant", MODE_PRIVATE).getBoolean("enabled", false),
                getSharedPreferences("repair-assistant", MODE_PRIVATE).getBoolean("online_lookup", false))) {
            Toast.makeText(this, "Turn on both assistant and web lookup in Settings first.", Toast.LENGTH_LONG).show();
            return;
        }
        assistantResult.setText("Checking the official Ren’Py release page…");
        assistantWorker.execute(() -> {
            try {
                OfficialRenPyVersionLookup.Result result = OfficialRenPyVersionLookup.lookup();
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Official Ren’Py release")
                        .setMessage("Latest SDK listed on renpy.org: Ren’Py " + result.version
                                + "\n\nOpen the official release page to check the Windows SDK .exe download and its published checksums. APK Forge does not download or run it.")
                        .setNegativeButton("Close", null)
                        .setPositiveButton("Open renpy.org", (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result.pageUrl))))
                        .show());
            } catch (Exception error) {
                runOnUiThread(() -> assistantResult.setText("Could not reach the official Ren’Py version page. Check your connection and try again."));
            }
        });
    }

    private void pickExeForHashCheck() {
        if (!RepairAssistantPolicy.maySearchOnline(
                getSharedPreferences("repair-assistant", MODE_PRIVATE).getBoolean("enabled", false),
                getSharedPreferences("repair-assistant", MODE_PRIVATE).getBoolean("online_lookup", false))) {
            Toast.makeText(this, "Turn on both assistant and web lookup in Settings first.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        startActivityForResult(picker, CHECK_EXE);
    }

    private void refreshDiagnostics() {
        if (diagnosticsText == null) return;
        String log = DiagnosticLog.read(getFilesDir());
        diagnosticsText.setText(log.isEmpty() ? "No diagnostic events yet." : log);
    }

    private void exportDiagnostics() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "apk-forge-diagnostics.txt");
        startActivityForResult(intent, SAVE_DIAGNOSTICS);
    }

    private void record(DiagnosticLog.Event event, long metric1, long metric2) {
        DiagnosticLog.record(getFilesDir(), event, metric1, metric2);
        if (showingDiagnostics && diagnosticsText != null) runOnUiThread(this::refreshDiagnostics);
    }

    private View buildBrowserPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(tabletMode ? 16 : 12), dp(10), dp(tabletMode ? 16 : 12), dp(8));
        page.setBackgroundColor(BG);
        page.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(dp(tabletMode ? 16 : 12) + insets.getSystemWindowInsetLeft(),
                    dp(10) + insets.getSystemWindowInsetTop(),
                    dp(tabletMode ? 16 : 12) + insets.getSystemWindowInsetRight(),
                    dp(8) + insets.getSystemWindowInsetBottom());
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button menu = button("", false);
        setButtonGlyph(menu, Glyph.MENU, WHITE);
        menu.setContentDescription("Open app navigation");
        menu.setVisibility(tabletMode || studioMode ? View.GONE : View.VISIBLE);
        menu.setOnClickListener(v -> showNavigationDrawer());
        if (!tabletMode) header.addView(menu, new LinearLayout.LayoutParams(dp(48), dp(48)));
        android.widget.ImageView brandMark = new android.widget.ImageView(this);
        int logoId = getResources().getIdentifier("apkforge_icon", "drawable", getPackageName());
        if (logoId != 0) brandMark.setImageBitmap(browserBrandBitmap(logoId));
        brandMark.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        brandMark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        if (!tabletMode) logoParams.leftMargin = dp(6);
        header.addView(brandMark, logoParams);
        LinearLayout brandCopy = new LinearLayout(this);
        brandCopy.setOrientation(LinearLayout.VERTICAL);
        brandCopy.setPadding(dp(9), 0, 0, 0);
        brandCopy.addView(label("Web Studio", 18, WHITE, true), full());
        header.addView(brandCopy, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(header, fullWithTop(tabletMode ? 4 : 0));

        TextView addressLabel = label(tabletMode ? "GAME WEBSITE OR SEARCH" : "OPEN A GAME PAGE", 10, MUTED, true);
        addressLabel.setPadding(dp(2), 0, 0, dp(4));
        page.addView(addressLabel, fullWithTop(tabletMode ? 12 : 6));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(4), dp(5), dp(4));
        top.setBackground(rounded(INPUT, 15));
        android.widget.ImageView addressGlyph = new android.widget.ImageView(this);
        addressGlyph.setImageDrawable(new BrowserGlyph(Glyph.WEB, AQUA, getResources().getDisplayMetrics().density));
        addressGlyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        top.addView(addressGlyph, new LinearLayout.LayoutParams(dp(22), dp(22)));
        browserAddress = input("Paste a game website or download URL");
        browserAddress.setSingleLine(true);
        browserAddress.setTextSize(15);
        browserAddress.setHint("Paste a link or search phrase");
        browserAddress.setBackgroundColor(Color.TRANSPARENT);
        browserAddress.setPadding(dp(10), dp(8), dp(5), dp(8));
        browserAddress.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        addressParams.leftMargin = dp(6);
        top.addView(browserAddress, addressParams);
        Button go = button("Go", true);
        setLeadingGlyph(go, Glyph.OPEN, BG);
        go.setContentDescription("Open the pasted web address");
        go.setOnClickListener(v -> {
            hideBrowserKeyboard();
            navigateBrowser(browserAddress.getText().toString());
        });
        browserAddress.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO
                    || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                hideBrowserKeyboard();
                navigateBrowser(browserAddress.getText().toString());
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(-2, dp(48));
        goParams.leftMargin = dp(8);
        top.addView(go, goParams);
        page.addView(top, full());
        browserProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        browserProgress.setMax(100);
        browserProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(MINT));
        browserProgress.setVisibility(View.GONE);
        page.addView(browserProgress, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(tabletMode ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        workspace.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(workspace, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout toolsPanel = new LinearLayout(this);
        toolsPanel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout tabletToolsContent = new LinearLayout(this);
        tabletToolsContent.setOrientation(LinearLayout.VERTICAL);
        if (tabletMode) {
            toolsPanel.setPadding(dp(12), dp(12), dp(12), dp(12));
            toolsPanel.setBackground(rounded(CARD, 18));
            tabletInspector = toolsPanel;
            LinearLayout inspectorHeading = new LinearLayout(this);
            inspectorHeading.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout headingCopy = new LinearLayout(this);
            headingCopy.setOrientation(LinearLayout.VERTICAL);
            headingCopy.addView(label("Game inspector", 16, WHITE, true), full());
            tabletInspectorWidthLabel = label("320 dp panel", 11, MUTED, false);
            headingCopy.addView(tabletInspectorWidthLabel, fullWithTop(2));
            inspectorHeading.addView(headingCopy, new LinearLayout.LayoutParams(0, -2, 1));
            Button narrowInspector = button("", false);
            setButtonGlyph(narrowInspector, Glyph.MINUS, WHITE);
            narrowInspector.setContentDescription("Make the game inspector narrower");
            narrowInspector.setTooltipText("Narrow inspector");
            narrowInspector.setOnClickListener(v -> resizeTabletInspector(-48));
            inspectorHeading.addView(narrowInspector, new LinearLayout.LayoutParams(dp(48), dp(48)));
            Button widenInspector = button("", false);
            setButtonGlyph(widenInspector, Glyph.PLUS, WHITE);
            widenInspector.setContentDescription("Make the game inspector wider");
            widenInspector.setTooltipText("Widen inspector");
            widenInspector.setOnClickListener(v -> resizeTabletInspector(48));
            LinearLayout.LayoutParams widenParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            widenParams.leftMargin = dp(4);
            inspectorHeading.addView(widenInspector, widenParams);
            toolsPanel.addView(inspectorHeading, full());
        }

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout navToolbar = new LinearLayout(this);
        navToolbar.setGravity(Gravity.CENTER_VERTICAL);
        navToolbar.setPadding(dp(4), dp(3), dp(4), dp(3));
        navToolbar.setBackground(rounded(CARD, 16));
        LinearLayout actionToolbar = new LinearLayout(this);
        actionToolbar.setGravity(Gravity.CENTER_VERTICAL);
        actionToolbar.setPadding(dp(4), dp(3), dp(4), dp(3));
        actionToolbar.setBackground(rounded(CARD, 16));
        Button back = button("", false);
        setButtonGlyph(back, Glyph.BACK, WHITE);
        back.setContentDescription("Go back in the browser");
        back.setTooltipText("Back");
        back.setOnClickListener(v -> { if (browserWebView != null && browserWebView.canGoBack()) browserWebView.goBack(); });
        navToolbar.addView(back, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button reload = button("", false);
        setButtonGlyph(reload, Glyph.REFRESH, WHITE);
        reload.setContentDescription("Reload this page");
        reload.setTooltipText("Reload");
        reload.setOnClickListener(v -> { if (browserWebView != null) browserWebView.reload(); });
        LinearLayout.LayoutParams reloadParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        reloadParams.leftMargin = dp(6);
        navToolbar.addView(reload, reloadParams);
        Button search = button(tabletMode ? "Search web" : "Search", false);
        setLeadingGlyph(search, Glyph.SEARCH, WHITE);
        search.setContentDescription("Search the web for a game page");
        search.setTooltipText("Search for a game");
        search.setOnClickListener(v -> { hideBrowserKeyboard(); searchBrowser(browserAddress.getText().toString()); });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        searchParams.rightMargin = dp(4);
        actionToolbar.addView(search, searchParams);
        Button detect = button(tabletMode ? "Analyze page" : "Analyze", false);
        setLeadingGlyph(detect, Glyph.SCAN, WHITE);
        detect.setContentDescription("Analyze this page and find game files");
        detect.setTooltipText("Analyze page");
        detect.setOnClickListener(v -> { hideBrowserKeyboard(); detectWebGames(); });
        LinearLayout.LayoutParams detectParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        detectParams.leftMargin = dp(4);
        actionToolbar.addView(detect, detectParams);
        toolbar.addView(navToolbar, full());
        toolbar.addView(actionToolbar, fullWithTop(6));
        TextView controlsHeading = label("Page controls", 14, WHITE, true);
        if (tabletMode) {
            tabletToolsContent.addView(controlsHeading, fullWithTop(12));
            tabletToolsContent.addView(toolbar, fullWithTop(7));
        } else {
            page.addView(toolbar, page.indexOfChild(workspace), fullWithTop(7));
        }

        Button usePastedLink = button("Add a direct download link", false);
        setLeadingGlyph(usePastedLink, Glyph.DOWNLOAD, WHITE);
        usePastedLink.setOnClickListener(v -> { hideBrowserKeyboard(); addManualCandidate(browserAddress.getText().toString()); });
        if (tabletMode) tabletToolsContent.addView(usePastedLink, fullWithTop(7));

        LinearLayout statusStrip = new LinearLayout(this);
        statusStrip.setGravity(Gravity.CENTER_VERTICAL);
        statusStrip.setPadding(dp(10), dp(7), dp(10), dp(7));
        statusStrip.setBackground(rounded(Color.rgb(19, 28, 41), 12));
        browserStatusDot = new View(this);
        browserStatusDot.setBackground(rounded(AQUA, 8));
        statusStrip.addView(browserStatusDot, new LinearLayout.LayoutParams(dp(7), dp(7)));
        browserStatus = label("Ready to browse. Paste a game link above.", 12, MUTED, false);
        browserStatus.setPadding(dp(8), 0, 0, 0);
        statusStrip.addView(browserStatus, new LinearLayout.LayoutParams(0, -2, 1));
        if (tabletMode) tabletToolsContent.addView(statusStrip, fullWithTop(7));

        LinearLayout onlineCard = new LinearLayout(this);
        onlineCard.setOrientation(LinearLayout.VERTICAL);
        onlineCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        onlineCard.setBackground(rounded(CARD, 16));
        onlineCard.addView(label("Build mode", 14, WHITE, true));
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        offlineModeButton = button("Offline", true);
        onlineModeButton = button("Online", false);
        setLeadingGlyph(offlineModeButton, Glyph.DOWNLOAD, BG);
        setLeadingGlyph(onlineModeButton, Glyph.WEB, WHITE);
        modeRow.addView(offlineModeButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams onlineModeParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        onlineModeParams.leftMargin = dp(8);
        modeRow.addView(onlineModeButton, onlineModeParams);
        offlineModeButton.setOnClickListener(v -> { selectedWebsiteMode = WebBuildMode.Mode.OFFLINE; updateWebsiteMode(); });
        onlineModeButton.setOnClickListener(v -> { selectedWebsiteMode = WebBuildMode.Mode.ONLINE; updateWebsiteMode(); });
        onlineCard.addView(modeRow, fullWithTop(8));
        websiteModeDescription = label("", 11, MUTED, false);
        onlineCard.addView(websiteModeDescription, fullWithTop(5));
        websiteBuildButton = button("", true);
        setLeadingGlyph(websiteBuildButton, Glyph.OPEN, BG);
        websiteBuildButton.setMinHeight(dp(50));
        websiteBuildButton.setOnClickListener(v -> {
            hideBrowserKeyboard();
            if (selectedWebsiteMode == WebBuildMode.Mode.ONLINE) wrapCurrentWebsite();
            else captureCurrentWebsite();
        });
        onlineCard.addView(websiteBuildButton, fullWithTop(7));
        updateWebsiteMode();

        browserResults = label("Game links found on this page appear here.", 12, MUTED, false);
        browserResults.setPadding(dp(4), dp(4), dp(4), dp(4));
        browserResultsList = new LinearLayout(this);
        browserResultsList.setOrientation(LinearLayout.VERTICAL);
        browserResultsList.addView(browserResults, full());
        selectedCandidateAction = button("Choose a game link below", true);
        selectedCandidateAction.setEnabled(false);
        selectedCandidateAction.setContentDescription("Choose a detected game link before continuing");
        browserResultsList.addView(selectedCandidateAction, fullWithTop(6));
        browserResultsScroll = new ScrollView(this);
        browserResultsScroll.setFillViewport(false);
        browserResultsScroll.setVerticalScrollBarEnabled(true);
        browserResultsScroll.addView(browserResultsList);

        if (tabletMode) {
            tabletToolsContent.addView(onlineCard, fullWithTop(10));
            browserResultsScroll.setBackground(rounded(INPUT, 12));
            tabletToolsContent.addView(browserResultsScroll, fullWithTop(10));
            ScrollView tabletToolsScroll = new ScrollView(this);
            tabletToolsScroll.setFillViewport(false);
            tabletToolsScroll.setVerticalScrollBarEnabled(true);
            tabletToolsScroll.addView(tabletToolsContent);
            toolsPanel.addView(tabletToolsScroll, new LinearLayout.LayoutParams(-1, 0, 1));
            tabletInspectorWidthDp = BrowserInspectorSizing.tabletWidth(tabletInspectorWidthDp,
                    getResources().getConfiguration().screenWidthDp);
            tabletInspectorWidthLabel.setText(tabletInspectorWidthDp + " dp panel");
            workspace.addView(toolsPanel, new LinearLayout.LayoutParams(dp(tabletInspectorWidthDp), -1));
        } else {
            browserResultsScroll.setVisibility(View.GONE);
            browserResultsScroll.setBackground(rounded(INPUT, 12));
        }

        browserWebView = new WebView(this);
        browserWebView.setBackgroundColor(BG);
        WebSettings settings = browserWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        browserWebView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                browserLoadFailed = false;
                if (browserProgress != null) { browserProgress.setProgress(0); browserProgress.setVisibility(View.VISIBLE); }
                if (pageSelectionOverlay != null) pageSelectionOverlay.setVisibility(View.GONE);
                if (browserSelectedSite != null) browserSelectedSite.setText("Loading selected page…");
                if (browserPreviewTitle != null) browserPreviewTitle.setText("Loading your game page…");
                if (browserPreviewHint != null) browserPreviewHint.setText("The preview will appear as soon as the page is ready.");
                if (browserPreviewEmpty != null) browserPreviewEmpty.setVisibility(View.VISIBLE);
                setBrowserStatus("Loading game page…", BLUE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (browserAddress != null) browserAddress.setText(url);
                if (browserProgress != null) { browserProgress.setProgress(100); browserProgress.setVisibility(View.GONE); }
                if (browserLoadFailed) {
                    if (pageSelectionOverlay != null) pageSelectionOverlay.setVisibility(View.GONE);
                    if (browserPreviewTitle != null) browserPreviewTitle.setText("This page could not be opened");
                    if (browserPreviewHint != null) browserPreviewHint.setText("Check the address or connection, then use Reload to try again.");
                    if (browserPreviewEmpty != null) browserPreviewEmpty.setVisibility(View.VISIBLE);
                    setBrowserStatus("Couldn’t load this page. Check the link or internet connection.", Color.rgb(255, 190, 120));
                    if (aiAutomaticSearchActive && isDuckDuckGoResultsPage(url)) tryNextAiSearchAfterEmpty();
                } else {
                    if (browserPreviewEmpty != null) browserPreviewEmpty.setVisibility(View.GONE);
                    updateBrowserSelectedSite(view);
                    if (pageSelectionOverlay != null && isHttpUrl(url)) pageSelectionOverlay.setVisibility(View.VISIBLE);
                    setBrowserStatus("Page ready · Find files to inspect game downloads.", GREEN);
                    if (aiAutomaticSearchActive && isDuckDuckGoResultsPage(url)) {
                        if (!url.equals(aiAutomaticSearchAnalyzedUrl)) {
                            aiAutomaticSearchAnalyzedUrl = url;
                            view.postDelayed(() -> {
                                if (aiAutomaticSearchActive && url.equals(view.getUrl())) detectWebGames();
                            }, 450);
                        }
                    } else if (aiAutomaticSearchActive && isHttpUrl(url)) {
                        aiAutomaticSearchActive = false;
                        aiDownloadSearchSequence.stopAfterLeads();
                    }
                }
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) browserLoadFailed = true;
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                record(DiagnosticLog.Event.WEBVIEW_RENDERER_GONE, detail != null && detail.didCrash() ? 1 : 0,
                        detail == null ? 0 : detail.rendererPriorityAtExit());
                DiagnosticLog.recordFailure(getFilesDir(), DiagnosticLog.Event.WEBVIEW_RENDERER_GONE, 1,
                        new IllegalStateException("WebView renderer exited"));
                if (browserWebView == view) browserWebView = null;
                view.destroy();
                if (browserPreviewEmpty != null) browserPreviewEmpty.setVisibility(View.VISIBLE);
                setBusy(false, null);
                setBrowserStatus("This game page used more memory than the browser could keep available. APK Forge is still open; reopen the Browser tab and try a smaller page or the game’s ZIP download.", Color.rgb(255, 190, 120));
                return true;
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) { handler.cancel(); }
        });
        browserWebView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int progress) {
                if (browserProgress != null) browserProgress.setProgress(progress);
            }
        });
        browserWebView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String guessed = fileNameFromUrl(url);
            if (contentDisposition != null && contentDisposition.matches("(?is).*filename\\s*=.*")) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)filename\\*?=(?:UTF-8''|\\\")?([^;\\\"]+)").matcher(contentDisposition);
                if (matcher.find()) guessed = matcher.group(1).trim().replaceAll("[\\\\/]", "_");
            }
            if (isZipUrl(url, mimeType, guessed)) confirmDownload(url, guessed, guessed, WebGameLinkAnalyzer.Format.UNKNOWN);
            else Toast.makeText(this, "Only complete ZIP game bundles can be downloaded for offline conversion.", Toast.LENGTH_LONG).show();
        });
        FrameLayout previewFrame = new FrameLayout(this);
        previewFrame.setBackgroundColor(BG);
        previewFrame.addView(browserWebView, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout emptyPreview = new LinearLayout(this);
        emptyPreview.setOrientation(LinearLayout.VERTICAL);
        emptyPreview.setGravity(Gravity.CENTER);
        emptyPreview.setPadding(dp(tabletMode ? 20 : 12), dp(tabletMode ? 20 : 12), dp(tabletMode ? 20 : 12), dp(tabletMode ? 20 : 12));
        emptyPreview.setBackground(rounded(CARD, 16));
        android.widget.ImageView previewLogo = new android.widget.ImageView(this);
        if (logoId != 0) previewLogo.setImageBitmap(browserBrandBitmap(logoId));
        previewLogo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        previewLogo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        emptyPreview.addView(previewLogo, new LinearLayout.LayoutParams(dp(tabletMode ? 104 : 56), dp(tabletMode ? 104 : 56)));
        browserPreviewTitle = label("Your next game starts here", tabletMode ? 18 : 15, WHITE, true);
        browserPreviewTitle.setGravity(Gravity.CENTER);
        emptyPreview.addView(browserPreviewTitle, fullWithTop(tabletMode ? 12 : 7));
        browserPreviewHint = label("Open a page to see it here.", tabletMode ? 13 : 11, MUTED, false);
        browserPreviewHint.setGravity(Gravity.CENTER);
        emptyPreview.addView(browserPreviewHint, fullWithTop(4));
        LinearLayout steps = new LinearLayout(this);
        steps.setGravity(Gravity.CENTER);
        steps.addView(label("01  OPEN", 10, AQUA, true));
        steps.addView(label("     ›     02  INSPECT", 10, MUTED, true));
        steps.addView(label("     ›     03  BUILD", 10, MUTED, true));
        emptyPreview.addView(steps, fullWithTop(tabletMode ? 16 : 8));
        browserPreviewEmpty = emptyPreview;
        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(dp(tabletMode ? 460 : 340), -2, Gravity.CENTER);
        previewFrame.addView(emptyPreview, emptyParams);

        LinearLayout pageSelection = new LinearLayout(this);
        pageSelection.setGravity(Gravity.CENTER_VERTICAL);
        pageSelection.setPadding(dp(12), dp(8), dp(8), dp(8));
        pageSelection.setBackground(rounded(CARD, 16));
        pageSelection.setElevation(dp(8));
        LinearLayout selectionCopy = new LinearLayout(this);
        selectionCopy.setOrientation(LinearLayout.VERTICAL);
        selectionCopy.addView(label("PAGE IN VIEW", 10, AQUA, true), full());
        pageSelectionTitle = label("Open a game page to select it", 12, WHITE, true);
        pageSelectionTitle.setMaxLines(1);
        pageSelectionTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        selectionCopy.addView(pageSelectionTitle, fullWithTop(3));
        pageSelection.addView(selectionCopy, new LinearLayout.LayoutParams(0, -2, 1));
        Button selectPage = button("Select for APK", true);
        selectPage.setOnClickListener(v -> {
            if (browserWebView == null || !isHttpUrl(browserWebView.getUrl())) {
                setBrowserStatus("Open a game page before selecting it for an APK.", Color.rgb(255, 190, 120));
                return;
            }
            selectedWebsiteMode = WebBuildMode.Mode.ONLINE;
            updateWebsiteMode();
            if (!tabletMode) setPhoneInspectorExpanded(true);
            setBrowserStatus("Page selected · choose Online wrapper or Offline snapshot below.", GREEN);
        });
        LinearLayout.LayoutParams selectPageParams = new LinearLayout.LayoutParams(-2, dp(48));
        selectPageParams.leftMargin = dp(8);
        pageSelection.addView(selectPage, selectPageParams);
        pageSelectionOverlay = pageSelection;
        pageSelection.setVisibility(View.GONE);
        FrameLayout.LayoutParams selectionParams = new FrameLayout.LayoutParams(-1, -2,
                Gravity.TOP | Gravity.START);
        selectionParams.setMargins(dp(tabletMode ? 16 : 8), dp(tabletMode ? 16 : 8),
                dp(tabletMode ? 16 : 8), 0);

        if (tabletMode) {
            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(39, 51, 66));
            workspace.addView(divider, new LinearLayout.LayoutParams(dp(1), -1));
            LinearLayout previewColumn = new LinearLayout(this);
            previewColumn.setOrientation(LinearLayout.VERTICAL);
            TextView previewHeader = label("Browser preview     ·     Test the game here", 13, MUTED, true);
            previewHeader.setPadding(dp(18), dp(12), dp(12), dp(10));
            previewColumn.addView(previewHeader, full());
            // Keep the persistent page action in the browser chrome, not on top of game pixels.
            previewColumn.addView(pageSelection, fullWithTop(8));
            previewColumn.addView(previewFrame, new LinearLayout.LayoutParams(-1, 0, 1));
            workspace.addView(previewColumn, new LinearLayout.LayoutParams(0, -1, 1));
        } else {
            previewFrame.addView(pageSelection, selectionParams);
            workspace.addView(previewFrame, new LinearLayout.LayoutParams(-1, 0, 1));
            if (toolbar.getParent() instanceof ViewGroup) ((ViewGroup) toolbar.getParent()).removeView(toolbar);
            phoneInspector = new LinearLayout(this);
            phoneInspector.setOrientation(LinearLayout.VERTICAL);
            phoneInspector.setPadding(dp(14), dp(8), dp(14), dp(10));
            phoneInspector.setBackground(rounded(Color.rgb(24, 34, 50), 24));
            phoneInspector.setElevation(dp(12));

            View sheetHandle = new View(this);
            sheetHandle.setBackground(rounded(Color.rgb(111, 139, 161), 4));
            LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(36), dp(4));
            handleParams.gravity = Gravity.CENTER_HORIZONTAL;
            handleParams.bottomMargin = dp(7);
            phoneInspector.addView(sheetHandle, handleParams);

            LinearLayout inspectorHeader = new LinearLayout(this);
            inspectorHeader.setGravity(Gravity.CENTER_VERTICAL);
            browserSelectedSite = label("Website workspace", 14, WHITE, true);
            browserSelectedSite.setMaxLines(1);
            browserSelectedSite.setEllipsize(android.text.TextUtils.TruncateAt.END);
            statusStrip.removeView(browserStatusDot);
            statusStrip.removeView(browserStatus);
            LinearLayout statusRow = new LinearLayout(this);
            statusRow.setGravity(Gravity.CENTER_VERTICAL);
            statusRow.addView(browserStatusDot, new LinearLayout.LayoutParams(dp(7), dp(7)));
            browserStatus.setMaxLines(1);
            browserStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams statusTextParams = new LinearLayout.LayoutParams(-1, -2);
            statusTextParams.leftMargin = dp(6);
            statusRow.addView(browserStatus, statusTextParams);
            LinearLayout inspectorCopy = new LinearLayout(this);
            inspectorCopy.setOrientation(LinearLayout.VERTICAL);
            inspectorCopy.addView(browserSelectedSite, full());
            inspectorCopy.addView(statusRow, fullWithTop(2));
            inspectorHeader.addView(inspectorCopy, new LinearLayout.LayoutParams(0, -2, 1));

            phoneInspectorToggle = button("Tools", false);
            phoneInspectorToggle.setContentDescription("Expand website tools");
            phoneInspectorToggle.setOnClickListener(v -> setPhoneInspectorExpanded(!phoneInspectorExpanded));
            LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(-2, dp(44));
            toggleParams.leftMargin = dp(8);
            inspectorHeader.addView(phoneInspectorToggle, toggleParams);
            phoneInspector.addView(inspectorHeader, full());

            ScrollView inspectorScroll = new ScrollView(this);
            inspectorScroll.setFillViewport(false);
            inspectorScroll.setVerticalScrollBarEnabled(true);
            phoneInspectorScroll = inspectorScroll;
            phoneInspectorBody = new LinearLayout(this);
            phoneInspectorBody.setOrientation(LinearLayout.VERTICAL);
            phoneInspectorBody.setPadding(0, dp(7), 0, 0);
            TextView inspectLabel = label("BROWSE & INSPECT", 10, AQUA, true);
            phoneInspectorBody.addView(inspectLabel, fullWithTop(2));
            phoneInspectorBody.addView(toolbar, fullWithTop(6));
            phoneInspectorBody.addView(usePastedLink, fullWithTop(6));
            browserResultsScroll.removeView(browserResultsList);
            phoneInspectorBody.addView(browserResultsList, fullWithTop(8));
            inspectorScroll.addView(phoneInspectorBody);
            phoneInspector.addView(inspectorScroll, new LinearLayout.LayoutParams(-1, 0, 1));
            inspectorScroll.setVisibility(View.GONE);
            onlineCard.setPadding(dp(12), dp(9), dp(12), dp(10));
            onlineCard.setBackground(rounded(CARD, 18));
            phoneInspector.addView(onlineCard, fullWithTop(8));
            FrameLayout.LayoutParams inspectorParams = new FrameLayout.LayoutParams(-1,
                    dp(phoneInspectorHeightDp), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            inspectorParams.setMargins(dp(4), 0, dp(4), dp(8));
            previewFrame.addView(phoneInspector, inspectorParams);
            setPhoneInspectorExpanded(false);
        }
        return page;
    }

    private void hideBrowserKeyboard() {
        if (browserAddress == null) return;
        android.view.inputmethod.InputMethodManager inputMethodManager =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) inputMethodManager.hideSoftInputFromWindow(browserAddress.getWindowToken(), 0);
        browserAddress.clearFocus();
    }

    private enum Glyph { MENU, WEB, BACK, REFRESH, SEARCH, SCAN, DOWNLOAD, OPEN, PLUS, MINUS }

    private void setButtonGlyph(Button button, Glyph glyph, int color) {
        Drawable icon = new BrowserGlyph(glyph, color, getResources().getDisplayMetrics().density);
        icon.setBounds(0, 0, dp(24), dp(24));
        button.setCompoundDrawables(null, icon, null, null);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(48));
        button.setMinHeight(dp(48));
    }

    private void setLeadingGlyph(Button button, Glyph glyph, int color) {
        Drawable icon = new BrowserGlyph(glyph, color, getResources().getDisplayMetrics().density);
        icon.setBounds(0, 0, dp(20), dp(20));
        button.setCompoundDrawablesRelative(icon, null, null, null);
        button.setCompoundDrawablePadding(dp(6));
        button.setGravity(Gravity.CENTER);
    }

    private Bitmap browserBrandBitmap(int resourceId) {
        if (browserBrandBitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            browserBrandBitmap = BitmapFactory.decodeResource(getResources(), resourceId, options);
        }
        return browserBrandBitmap;
    }

    private void updateWebsiteMode() {
        if (websiteModeDescription == null || websiteBuildButton == null) return;
        websiteModeDescription.setText(WebBuildMode.description(selectedWebsiteMode));
        websiteBuildButton.setText(WebBuildMode.actionLabel(selectedWebsiteMode));
        boolean offline = selectedWebsiteMode == WebBuildMode.Mode.OFFLINE;
        offlineModeButton.setBackground(gradient(offline ? new int[]{MINT, Color.rgb(109, 211, 204)} : new int[]{Color.rgb(43, 54, 71), Color.rgb(29, 38, 53)}, 15));
        offlineModeButton.setTextColor(offline ? BG : WHITE);
        onlineModeButton.setBackground(gradient(!offline ? new int[]{AMBER, Color.rgb(236, 145, 42)} : new int[]{Color.rgb(43, 54, 71), Color.rgb(29, 38, 53)}, 15));
        onlineModeButton.setTextColor(!offline ? BG : WHITE);
        setLeadingGlyph(offlineModeButton, Glyph.DOWNLOAD, offline ? BG : WHITE);
        setLeadingGlyph(onlineModeButton, Glyph.WEB, !offline ? BG : WHITE);
    }

    private int browserScreenHeightDp() {
        int height = getResources().getConfiguration().screenHeightDp;
        return height > 0 ? height : (int) (getResources().getDisplayMetrics().heightPixels
                / getResources().getDisplayMetrics().density);
    }

    private void setPhoneInspectorExpanded(boolean expanded) {
        if (phoneInspector == null || phoneInspectorBody == null || phoneInspectorScroll == null) return;
        if (expanded && phoneInspectorHeightDp <= 272) {
            phoneInspectorHeightDp = BrowserInspectorSizing.phoneHeight(520, browserScreenHeightDp());
        }
        phoneInspectorExpanded = expanded;
        if (!expanded) phoneInspectorHeightDp = 272;
        else phoneInspectorHeightDp = BrowserInspectorSizing.phoneHeight(phoneInspectorHeightDp,
                browserScreenHeightDp());
        phoneInspectorScroll.setVisibility(expanded ? View.VISIBLE : View.GONE);
        ViewGroup.LayoutParams params = phoneInspector.getLayoutParams();
        if (params != null) {
            params.height = dp(phoneInspectorHeightDp);
            phoneInspector.setLayoutParams(params);
        }
        if (phoneInspectorToggle != null) {
            phoneInspectorToggle.setText(expanded ? "Close tools" : "Tools");
            phoneInspectorToggle.setContentDescription((expanded ? "Collapse" : "Expand") + " the game inspector");
            phoneInspectorToggle.setSelected(expanded);
        }
    }

    private void adjustPhoneInspector(int deltaDp) {
        if (phoneInspector == null) return;
        if (!phoneInspectorExpanded && deltaDp > 0) {
            phoneInspectorHeightDp = BrowserInspectorSizing.phoneHeight(520, browserScreenHeightDp());
            setPhoneInspectorExpanded(true);
            return;
        }
        if (!phoneInspectorExpanded) return;
        int requested = phoneInspectorHeightDp + deltaDp;
        if (requested <= 272) {
            setPhoneInspectorExpanded(false);
            return;
        }
        phoneInspectorHeightDp = BrowserInspectorSizing.phoneHeight(requested, browserScreenHeightDp());
        ViewGroup.LayoutParams params = phoneInspector.getLayoutParams();
        params.height = dp(phoneInspectorHeightDp);
        phoneInspector.setLayoutParams(params);
    }

    private void resizeTabletInspector(int deltaDp) {
        if (!tabletMode || tabletInspector == null) return;
        int width = getResources().getConfiguration().screenWidthDp;
        tabletInspectorWidthDp = BrowserInspectorSizing.tabletWidth(tabletInspectorWidthDp + deltaDp, width);
        ViewGroup.LayoutParams params = tabletInspector.getLayoutParams();
        if (params != null) {
            params.width = dp(tabletInspectorWidthDp);
            tabletInspector.setLayoutParams(params);
        }
        if (tabletInspectorWidthLabel != null) tabletInspectorWidthLabel.setText(tabletInspectorWidthDp + " dp panel");
    }

    private void updateBrowserSelectedSite(WebView view) {
        if (view == null) return;
        String title = view.getTitle() == null ? "" : view.getTitle().trim();
        String host = "";
        try {
            String parsedHost = Uri.parse(view.getUrl()).getHost();
            if (parsedHost != null) host = parsedHost;
        } catch (RuntimeException ignored) { }
        String name = title.isEmpty() ? host : title;
        if (name.isEmpty()) name = "Current game page";
        if (!host.isEmpty() && !name.equalsIgnoreCase(host)) name += " · " + host;
        if (browserSelectedSite != null) browserSelectedSite.setText(name);
        if (pageSelectionTitle != null) pageSelectionTitle.setText(name);
    }

    private void navigateBrowser(String raw) {
        String url = WebGameLinkAnalyzer.normalizeInput(raw);
        if (!url.isEmpty()) { browserWebView.loadUrl(url); return; }
        searchBrowser(raw);
    }

    private void searchBrowser(String query) {
        try {
            browserAddress.setText(query == null ? "" : query.trim());
            browserWebView.loadUrl(WebGameLinkAnalyzer.searchUrl(query));
            setBrowserStatus("Searching the web inside APK Forge. Open a result page, then tap Analyze page.", AQUA);
        } catch (IllegalArgumentException e) {
            setBrowserStatus(e.getMessage(), Color.rgb(255, 190, 120));
        }
    }

    private void detectWebGames() {
        if (browserWebView == null || browserWebView.getUrl() == null) { setBrowserStatus("Open a game website first.", Color.rgb(255, 190, 120)); return; }
        if (!tabletMode) setPhoneInspectorExpanded(true);
        setBrowserStatus("Scanning this page for game downloads…", AQUA);
        browserResultsList.removeViews(2, Math.max(0, browserResultsList.getChildCount() - 2));
        candidateSelection.clear();
        updateCandidateSelection();
        String script = "(function(){let a=[...document.querySelectorAll('a[href],area[href],[data-href]')].map(x=>({url:x.href||x.getAttribute('data-href')||'',text:(x.innerText||x.getAttribute('aria-label')||x.title||x.download||'').trim().slice(0,140)}));a=a.filter(x=>/^https?:/i.test(x.url)&&(/\\.(zip|html?|exe)(?:$|[?#])/i.test(x.url)||/\\b(download|ren.?py|html|browser game|web game|game files|launcher)\\b/i.test(x.text)));let u=new URL(location.href);if(/\\.(zip|html?|exe)(?:$|[?#])/i.test(u.href))a.unshift({url:u.href,text:document.title||u.pathname.split('/').pop()||'Current link'});let seen=new Set();a=a.filter(x=>!seen.has(x.url)&&(seen.add(x.url),true));let r=[...document.querySelectorAll('.result')].slice(0,20).map(c=>{let l=c.querySelector('a.result__a');if(!l)return null;let h=l.href;try{let p=new URL(h);if(p.hostname==='duckduckgo.com'||p.hostname.endsWith('.duckduckgo.com'))h=p.searchParams.get('uddg')||h}catch(e){}return {url:h,text:(l.innerText||'').trim().slice(0,140),snippet:(c.querySelector('.result__snippet')?.innerText||'').trim().slice(0,240)}}).filter(x=>x&&/^https?:/i.test(x.url));return JSON.stringify({title:document.title||'',text:(document.body?document.body.innerText:'').slice(0,5000),links:a.slice(0,120),results:r});})()";
        browserWebView.evaluateJavascript(script, result -> {
            try {
                Object parsed = new org.json.JSONTokener(result).nextValue();
                JSONObject data = parsed instanceof String ? new JSONObject((String) parsed) : (JSONObject) parsed;
                String title = data.optString("title", "");
                String pageText = data.optString("text", "");
                JSONArray links = data.optJSONArray("links");
                JSONArray searchResults = data.optJSONArray("results");
                int archives = 0;
                if (links != null) for (int i = 0; i < links.length(); i++) {
                    JSONObject link = links.optJSONObject(i);
                    if (link != null && isZipUrl(link.optString("url", ""), "", link.optString("text", ""))) archives++;
                }
                int added = 0;
                if (isDuckDuckGoResultsPage(browserWebView.getUrl()) && searchResults != null) {
                    HashSet<String> seenResults = new HashSet<>();
                    for (int i = 0; i < searchResults.length(); i++) {
                        JSONObject item = searchResults.optJSONObject(i);
                        if (item == null) continue;
                        String url = item.optString("url", "");
                        if (!isHttpUrl(url) || !seenResults.add(url)) continue;
                        String resultTitle = item.optString("text", "Search result");
                        String snippet = item.optString("snippet", "");
                        String displayTitle = snippet.isEmpty() ? resultTitle : resultTitle + " — " + snippet;
                        boolean archive = isZipUrl(url, "", resultTitle);
                        WebGameLinkAnalyzer.Format format = WebGameLinkAnalyzer.inferFormat(url, resultTitle, resultTitle + " " + snippet, 1);
                        addCandidateCard(url, displayTitle, format, archive);
                        added++;
                    }
                }
                if (added == 0) {
                if (links != null) for (int i = 0; i < links.length(); i++) {
                    JSONObject link = links.optJSONObject(i);
                    if (link == null) continue;
                    String url = link.optString("url", "");
                    String text = link.optString("text", "Game download");
                    if (!isHttpUrl(url)) continue;
                    final String gameTitle = OfflinePackageFormat.cleanGameTitle(text, fileNameFromUrl(url));
                    boolean archive = isZipUrl(url, "", text);
                    WebGameLinkAnalyzer.Format format = WebGameLinkAnalyzer.inferFormat(url, text, title + " " + pageText, archives);
                    addCandidateCard(url, gameTitle, format, archive);
                    added++;
                }
                }
                if (added == 0) {
                    record(DiagnosticLog.Event.ANALYSIS_COMPLETED, 0, links == null ? 0 : links.length());
                    boolean wasAutomaticAiSearch = aiAutomaticSearchActive;
                    if (wasAutomaticAiSearch && tryNextAiSearchAfterEmpty()) return;
                    browserResults.setText(wasAutomaticAiSearch
                            ? "The local AI tried all " + aiDownloadSearchSequence.size() + " official-download search options but found no usable leads. Try another title, paste a direct link, or inspect the creator’s official site."
                            : "No likely game links found. Search from the address bar, paste a direct link and tap Analyze pasted download link, or open a playable page to make an online HTML APK.");
                    Button assistedSearch = button("Suggest official-download searches with local AI", true);
                    assistedSearch.setOnClickListener(v -> startAiAssistedDownloadSearch());
                    browserResultsList.addView(assistedSearch, fullWithTop(8));
                    addNextAiSearchButton();
                    setBrowserStatus(wasAutomaticAiSearch
                            ? "Tried every local-AI search suggestion · no likely game links found."
                            : "Analysis found no likely download links. You can still paste a direct link or use Search web.", Color.rgb(255, 190, 120));
                } else {
                    boolean automaticAiLeads = aiAutomaticSearchActive;
                    aiAutomaticSearchActive = false;
                    aiDownloadSearchSequence.stopAfterLeads();
                    record(DiagnosticLog.Event.ANALYSIS_COMPLETED, added, links == null ? 0 : links.length());
                    browserResults.setText(added + (isDuckDuckGoResultsPage(browserWebView.getUrl())
                            ? (automaticAiLeads ? " unverified search lead(s) found by local AI search "
                                    + (aiDownloadSearchIndex + 1) + " of " + aiDownloadSearchQueries.size()
                                    : " unverified search result(s)")
                                    + " · choose a promising creator/download page to inspect. Results are leads, not safety checks; nothing was downloaded."
                            : " possible game links · review the title and address, then select one. ZIP contents are checked before packaging."));
                    addNextAiSearchButton();
                    revealBrowserResults();
                    setBrowserStatus("Analyzed " + added + " possible game link(s) · select the right one below.", GREEN);
                }
            } catch (Exception e) {
                record(DiagnosticLog.Event.ANALYSIS_FAILED, 0, 0);
                if (!tryNextAiSearchAfterEmpty()) setBrowserStatus("Page scan failed: " + e.getMessage(), Color.rgb(255, 140, 140));
            }
        });
    }

    private void startAiAssistedDownloadSearch() {
        android.content.SharedPreferences preferences = getSharedPreferences("repair-assistant", MODE_PRIVATE);
        if (!RepairAssistantPolicy.maySearchOnline(preferences.getBoolean("enabled", false),
                preferences.getBoolean("online_lookup", false))) {
            new AlertDialog.Builder(this).setTitle("Enable web-assisted search")
                    .setMessage("Turn on the local assistant and user-started web lookups in Settings first. The model runs on this device; only a search phrase you select will be sent to DuckDuckGo. Page contents and URLs are not sent to the model.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Open Settings", (dialog, which) -> openSettings())
                    .show();
            return;
        }
        if (!RepairAssistantModel.isInstalled(this)) {
            new AlertDialog.Builder(this).setTitle("Local model needed")
                    .setMessage("Download a verified local model in Settings to generate search suggestions. You can still use Search web manually without AI.")
                    .setNegativeButton("Close", null)
                    .setPositiveButton("Open Settings", (dialog, which) -> openSettings())
                    .show();
            return;
        }
        String title = browserWebView == null ? "" : browserWebView.getTitle();
        if (title == null || title.trim().isEmpty()) title = "game";
        final String gameTitle = title.trim();
        setBrowserStatus("Generating search suggestions locally…", AQUA);
        assistantWorker.execute(() -> {
            try {
                List<String> suggestions = DownloadSearchPlanner.parseSuggestions(
                        RepairAssistantModel.generateSearchSuggestions(this, gameTitle), gameTitle);
                runOnUiThread(() -> {
                    if (isFinishing() || suggestions.isEmpty()) return;
                    aiDownloadSearchQueries = new ArrayList<>(suggestions);
                    aiDownloadSearchSequence.start(suggestions);
                    aiDownloadSearchIndex = aiDownloadSearchSequence.index();
                    aiAutomaticSearchActive = aiDownloadSearchSequence.isActive();
                    aiAutomaticSearchAnalyzedUrl = "";
                    if (browserResults != null) browserResults.setText("Local AI used only the page title. APK Forge is trying official Windows, Android/HTML, and Ren’Py searches; you’ll choose which result to inspect. Search leads are unverified and never downloaded automatically.");
                    setBrowserStatus("Searching official game-release options locally assisted by AI…", AQUA);
                    runNextAiDownloadSearch();
                });
            } catch (Exception failure) {
                String message = failure.getMessage() == null ? "Local search suggestions could not be generated." : failure.getMessage();
                runOnUiThread(() -> setBrowserStatus(message, Color.rgb(255, 190, 120)));
            }
        });
    }

    private void addNextAiSearchButton() {
        int next = aiDownloadSearchIndex + 1;
        if (next < 0 || next >= aiDownloadSearchQueries.size()) return;
        Button nextSearch = button("Try next AI search · " + (next + 1) + " of " + aiDownloadSearchQueries.size(), false);
        nextSearch.setOnClickListener(v -> { aiDownloadSearchIndex = next; runNextAiDownloadSearch(); });
        browserResultsList.addView(nextSearch, fullWithTop(8));
    }

    private void runNextAiDownloadSearch() {
        if (aiDownloadSearchIndex < 0 || aiDownloadSearchIndex >= aiDownloadSearchQueries.size()) return;
        String query = aiDownloadSearchQueries.get(aiDownloadSearchIndex);
        aiAutomaticSearchAnalyzedUrl = "";
        if (browserAddress != null) browserAddress.setText(query);
        if (browserResultsList != null && browserResultsList.getChildCount() > 2)
            browserResultsList.removeViews(2, browserResultsList.getChildCount() - 2);
        candidateSelection.clear();
        updateCandidateSelection();
        searchBrowser(query);
    }

    private boolean tryNextAiSearchAfterEmpty() {
        if (!aiAutomaticSearchActive) return false;
        String next = aiDownloadSearchSequence.advanceAfterEmpty();
        if (next == null) {
            aiAutomaticSearchActive = false;
            return false;
        }
        aiDownloadSearchIndex = aiDownloadSearchSequence.index();
        aiAutomaticSearchAnalyzedUrl = "";
        if (browserAddress != null) browserAddress.setText(next);
        if (browserResults != null) browserResults.setText("No likely game links in search "
                + aiDownloadSearchIndex + " of " + aiDownloadSearchSequence.size()
                + ". Trying the next official-release option…");
        setBrowserStatus("No leads yet · automatically trying AI search "
                + (aiDownloadSearchIndex + 1) + " of " + aiDownloadSearchSequence.size() + "…", AQUA);
        if (browserWebView != null) browserWebView.postDelayed(this::runNextAiDownloadSearch, 350);
        return true;
    }

    private boolean isDuckDuckGoResultsPage(String pageUrl) {
        if (pageUrl == null) return false;
        Uri uri = Uri.parse(pageUrl);
        String host = uri.getHost();
        return host != null && (host.equalsIgnoreCase("html.duckduckgo.com")
                || host.equalsIgnoreCase("duckduckgo.com")) && uri.getPath() != null
                && uri.getPath().toLowerCase(Locale.ROOT).contains("html");
    }

    private void confirmReviewExeDownload(String url, String title, DownloadSourceAssessment assessment) {
        if (!assessment.mayOfferReviewDownload()) {
            setBrowserStatus("APK Forge only allows EXE review downloads from HTTPS links.", Color.rgb(255, 190, 120));
            return;
        }
        String host = assessment.host;
        new AlertDialog.Builder(this).setTitle("Download Windows EXE for review?")
                .setMessage("Source host: " + host + "\n" + assessment.evidenceLabel
                        + "\n\nAPK Forge will save the file in private temporary storage, calculate its SHA-256 locally, and let you open a VirusTotal hash report. It will NOT run, install, or convert the EXE. A clean reputation or matching checksum cannot prove a file is safe."
                        + "\n\nOnly continue if you trust the publisher and have rights to the download.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Download & calculate hash", (dialog, which) -> downloadExeForReview(url, title))
                .show();
    }

    private void downloadExeForReview(String rawUrl, String title) {
        if (busy) return;
        setBusy(true, "Downloading EXE into private review storage…");
        setBrowserStatus("Downloading the selected EXE for local hash review…", AQUA);
        record(DiagnosticLog.Event.DOWNLOAD_STARTED, 0, 0);
        worker.execute(() -> {
            File partial = null;
            HttpURLConnection connection = null;
            try {
                URL current = new URL(rawUrl);
                if (!"https".equalsIgnoreCase(current.getProtocol()) || current.getUserInfo() != null)
                    throw new SecurityException("The EXE download must start from a clean HTTPS URL.");
                for (int redirects = 0; redirects <= 5; redirects++) {
                    if (!"https".equalsIgnoreCase(current.getProtocol()) || current.getUserInfo() != null)
                        throw new SecurityException("The download redirected to an unsafe URL; no file was saved.");
                    connection = (HttpURLConnection) current.openConnection();
                    connection.setInstanceFollowRedirects(false);
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(90000);
                    connection.setRequestProperty("User-Agent", "APKForge/3.0 (user-selected EXE hash review)");
                    int response = connection.getResponseCode();
                    if (response >= 300 && response <= 399) {
                        String location = connection.getHeaderField("Location");
                        connection.disconnect();
                        if (location == null || redirects == 5) throw new IllegalStateException("The download redirected too many times or omitted its destination.");
                        current = new URL(current, location);
                        continue;
                    }
                    if (response != HttpURLConnection.HTTP_OK)
                        throw new IllegalStateException("Download server returned HTTP " + response + ".");
                    break;
                }
                if (connection == null || connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                    throw new IllegalStateException("Could not establish a complete EXE download.");
                if (!"https".equalsIgnoreCase(connection.getURL().getProtocol()))
                    throw new SecurityException("The final download connection was not HTTPS.");
                long expected = connection.getContentLengthLong();
                if (expected > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("This EXE exceeds APK Forge's 6 GiB review-download limit.");
                String contentType = connection.getContentType();
                if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("text/html"))
                    throw new IllegalStateException("The server returned an HTML page instead of an EXE file.");
                File reviewDir = new File(getCacheDir(), "exe-review");
                if (!reviewDir.isDirectory() && !reviewDir.mkdirs()) throw new IllegalStateException("Could not create private review storage.");
                long requiredSpace = expected > 0 ? expected + 64L * 1024L * 1024L : 256L * 1024L * 1024L;
                if (reviewDir.getUsableSpace() < requiredSpace) throw new IllegalStateException("Not enough free device storage for this review download.");
                partial = new File(reviewDir, "download-" + System.currentTimeMillis() + ".part");
                long received = 0;
                long lastUpdate = 0;
                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     OutputStream output = new BufferedOutputStream(new FileOutputStream(partial))) {
                    byte[] buffer = new byte[128 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Download cancelled.");
                        received += count;
                        if (received > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("Download exceeded the 6 GiB review limit.");
                        if (reviewDir.getUsableSpace() < 16L * 1024L * 1024L) throw new IllegalStateException("Device storage ran low; the incomplete download was discarded.");
                        output.write(buffer, 0, count);
                        if (System.currentTimeMillis() - lastUpdate > 1200) {
                            lastUpdate = System.currentTimeMillis();
                            long progressBytes = received;
                            runOnUiThread(() -> setBrowserStatus("Downloading EXE · " + readableSize(progressBytes)
                                    + (expected > 0 ? " / " + readableSize(expected) : ""), AQUA));
                        }
                    }
                }
                if (expected > 0 && partial.length() != expected) throw new IllegalStateException("The server ended the download early; the partial file was discarded.");
                if (partial.length() < 4) throw new IllegalStateException("The server returned an empty file.");
                WindowsExecutableHeader.Inspection pe = WindowsExecutableHeader.inspect(partial);
                if (!pe.valid) throw new IllegalStateException("The selected file is not a valid Windows EXE: " + pe.reason);
                String architecture = pe.architecture;
                String sha256 = RepairAssistantModel.sha256(partial);
                String fileLabel = safeName(fileNameFromUrl(rawUrl));
                if (!fileLabel.toLowerCase(Locale.ROOT).endsWith(".exe")) fileLabel += ".exe";
                File reviewed = new File(reviewDir, fileLabel + "-" + System.currentTimeMillis());
                if (!partial.renameTo(reviewed)) throw new IllegalStateException("Could not finish saving the reviewed EXE.");
                File finalReviewed = reviewed;
                String finalHost = connection == null ? "unknown host" : connection.getURL().getHost();
                record(DiagnosticLog.Event.DOWNLOAD_COMPLETED, reviewed.length(), 0);
                runOnUiThread(() -> {
                    setBusy(false, null);
                    setBrowserStatus("EXE saved privately · Windows PE structure and SHA-256 checked · never executed.", GREEN);
                    new AlertDialog.Builder(this).setTitle("EXE downloaded for review")
                            .setMessage("File: " + finalReviewed.getName() + "\nFinal host: " + finalHost
                                    + "\nSize: " + readableSize(finalReviewed.length()) + "\nWindows PE architecture: " + architecture
                                    + "\nSHA-256:\n" + sha256
                                    + "\n\nThe PE check confirms file structure only. It does not verify the publisher or scan for malware. Compare the hash with a checksum published by the game's official creator. APK Forge did not run or convert this file.")
                            .setNegativeButton("Done", null)
                            .setNeutralButton("More checks…", (dialog, which) -> showReviewActions(finalReviewed, sha256))
                            .setPositiveButton("Open VirusTotal hash report", (dialog, which) -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(RepairAssistantPolicy.virusTotalLookupUrl(sha256)));
                                startActivity(intent);
                            }).show();
                });
            } catch (Exception failure) {
                if (partial != null && partial.exists()) partial.delete();
                String message = failure.getMessage() == null ? "The EXE could not be downloaded or verified." : failure.getMessage();
                record(DiagnosticLog.Event.DOWNLOAD_FAILED, 0, 0);
                runOnUiThread(() -> { setBusy(false, null); setBrowserStatus(message, Color.rgb(255, 140, 140)); });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void showReviewActions(File file, String actualSha256) {
        new AlertDialog.Builder(this).setTitle("Review this EXE")
                .setItems(new String[]{"Compare with a publisher checksum…", "Save a copy to Files…"}, (dialog, which) -> {
                    if (which == 0) comparePublishedExeChecksum(actualSha256);
                    else exportReviewedExe(file);
                }).setNegativeButton("Cancel", null).show();
    }

    private void comparePublishedExeChecksum(String actualSha256) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(4));
        form.addView(label("Paste the SHA-256 from the game creator's official download/checksum page. APK Forge only compares the two values; it does not authenticate that website.", 13, MUTED, false), full());
        EditText expectedHash = new EditText(this);
        expectedHash.setHint("64-character SHA-256");
        expectedHash.setSingleLine(true);
        expectedHash.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        expectedHash.setTextColor(WHITE);
        expectedHash.setHintTextColor(MUTED);
        expectedHash.setBackground(rounded(INPUT, 12));
        expectedHash.setPadding(dp(12), dp(10), dp(12), dp(10));
        form.addView(expectedHash, fullWithTop(10));
        android.widget.CheckBox officialSource = new android.widget.CheckBox(this);
        officialSource.setText("I verified that I copied this from the creator's official site");
        officialSource.setTextColor(WHITE);
        form.addView(officialSource, fullWithTop(6));
        new AlertDialog.Builder(this).setTitle("Compare official SHA-256")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Compare", (dialog, which) -> {
                    String expected = expectedHash.getText().toString().trim();
                    RepairAssistantPolicy.BinarySafety assessment = RepairAssistantPolicy.assessBinary(
                            expected, actualSha256, officialSource.isChecked(), officialSource.isChecked());
                    String result;
                    if (assessment == RepairAssistantPolicy.BinarySafety.HASH_MISMATCH)
                        result = "MISMATCH: the downloaded file does not match the checksum you entered. Do not trust or run it.";
                    else if (assessment == RepairAssistantPolicy.BinarySafety.MATCHES_OFFICIAL_CHECKSUM)
                        result = "The file matches the checksum you say came from the creator's official site. APK Forge has not authenticated that site; a matching digest is not a malware-free guarantee.";
                    else if (assessment == RepairAssistantPolicy.BinarySafety.MATCHES_UNTRUSTED_CHECKSUM)
                        result = "The file matches the value entered, but the checksum source was not confirmed as official.";
                    else result = "That is not a valid 64-character SHA-256. No comparison was made.";
                    new AlertDialog.Builder(this).setTitle("Checksum comparison")
                            .setMessage(result + "\n\nSHA-256 of downloaded bytes:\n" + actualSha256)
                            .setPositiveButton("Done", null).show();
                }).show();
    }

    private void exportReviewedExe(File source) {
        if (source == null || !source.isFile()) {
            Toast.makeText(this, "The temporary review download is no longer available.", Toast.LENGTH_LONG).show();
            return;
        }
        pendingReviewExe = source;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, source.getName());
        startActivityForResult(intent, SAVE_REVIEW_EXE);
    }

    private void addAiSearchFallback(String reason) {
        if (browserResults != null) browserResults.setText(reason + " You can ask the local AI for other official game-release searches.");
        if (browserResultsList != null) {
            for (int i = 2; i < browserResultsList.getChildCount(); i++) {
                View child = browserResultsList.getChildAt(i);
                if ("ai-download-search-fallback".equals(child.getTag())) return;
            }
            Button fallback = button("Find other game versions with on-device AI", true);
            fallback.setTag("ai-download-search-fallback");
            fallback.setOnClickListener(v -> startAiAssistedDownloadSearch());
            browserResultsList.addView(fallback, fullWithTop(8));
            revealBrowserResults();
        }
    }

    private void addManualCandidate(String raw) {
        String url = WebGameLinkAnalyzer.normalizeInput(raw);
        if (url.isEmpty()) { setBrowserStatus("Paste a complete http(s) download or game-page URL first.", Color.rgb(255, 190, 120)); return; }
        if (!isHttpUrl(url)) { setBrowserStatus("Only http(s) links are supported.", Color.rgb(255, 140, 140)); return; }
        String name = fileNameFromUrl(url);
        boolean archive = isZipUrl(url, "", name);
        WebGameLinkAnalyzer.Format format = WebGameLinkAnalyzer.inferFormat(url, name, "", 1);
        addCandidateCard(url, OfflinePackageFormat.cleanGameTitle(name, name), format, archive);
        browserResults.setText("Pasted link added · review the title and address, then select it. ZIP contents are validated before packaging.");
        revealBrowserResults();
        setBrowserStatus("Added your pasted link to the selection list.", GREEN);
    }

    private void revealBrowserResults() {
        if (!tabletMode && browserResultsScroll != null) {
            setPhoneInspectorExpanded(true);
            if (browserResultsList != null) browserResultsList.setVisibility(View.VISIBLE);
        }
    }

    private void addCandidateCard(String url, String title, WebGameLinkAnalyzer.Format format, boolean archive) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setBackground(rounded(INPUT, 12));
        WebCandidateSelection.Candidate candidate = new WebCandidateSelection.Candidate(url, title, archive, format);
        card.setTag(candidate);
        String detected = format == WebGameLinkAnalyzer.Format.RENPY ? "Likely Ren’Py" : format == WebGameLinkAnalyzer.Format.HTML ? "Likely HTML" : "Format uncertain";
        card.addView(label((archive ? "ZIP · " : "PAGE/LINK · ") + detected + " · " + title, 12, WHITE, true));
        TextView address = label(url, 10, MUTED, false);
        address.setMaxLines(2);
        card.addView(address, fullWithTop(3));
        DownloadSourceAssessment source = DownloadSourceAssessment.inspect(url,
                browserWebView == null ? null : browserWebView.getUrl(), title);
        card.addView(label(source.evidenceLabel, 10, source.https ? MUTED : Color.rgb(255, 190, 120), false), fullWithTop(3));
        if (source.artifact == DownloadSourceAssessment.Artifact.WINDOWS_EXE) {
            card.addView(label("Windows EXE · not an Android APK. APK Forge never executes it. A Ren’Py launcher EXE alone cannot be converted into a playable Android game.", 11, Color.rgb(255, 190, 120), false), fullWithTop(6));
            if (source.mayOfferReviewDownload()) {
                Button review = button("Download EXE for hash review", false);
                review.setContentDescription("Download this Windows executable into private storage, calculate its SHA-256, and optionally check the hash reputation");
                review.setOnClickListener(v -> confirmReviewExeDownload(url, title, source));
                card.addView(review, fullWithTop(7));
            } else {
                card.addView(label("APK Forge blocks EXE downloads over unencrypted HTTP.", 10, Color.rgb(255, 190, 120), false), fullWithTop(5));
            }
            browserResultsList.addView(card, fullWithTop(6));
            return;
        }
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        if (!archive) {
            Button open = button("Open page", true);
            open.setOnClickListener(v -> browserWebView.loadUrl(url));
            actions.addView(open, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        Button select = button("Select this link", archive);
        select.setContentDescription("Select " + title + (archive ? " game ZIP" : " game page") + " for conversion");
        select.setOnClickListener(v -> {
            candidateSelection.select(candidate);
            updateCandidateSelection();
            setBrowserStatus("Selected “" + candidate.title + "” · continue using the selected-link button.", GREEN);
        });
        LinearLayout.LayoutParams selectParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        if (!archive) selectParams.leftMargin = dp(8);
        actions.addView(select, selectParams);
        card.addView(actions, fullWithTop(6));
        browserResultsList.addView(card, fullWithTop(6));
        updateCandidateSelection();
    }

    private void updateCandidateSelection() {
        WebCandidateSelection.Candidate selected = candidateSelection.getSelected();
        if (selectedCandidateAction == null) return;
        selectedCandidateAction.setEnabled(selected != null);
        if (selected == null) {
            selectedCandidateAction.setText("Choose a game link below");
            selectedCandidateAction.setContentDescription("Choose a detected game link before continuing");
        } else if (selected.archive) {
            selectedCandidateAction.setText("Continue with selected ZIP · " + selected.title);
            selectedCandidateAction.setContentDescription("Choose a format and convert selected ZIP: " + selected.title);
        } else {
            selectedCandidateAction.setText("Open selected game page · " + selected.title);
            selectedCandidateAction.setContentDescription("Open selected game page: " + selected.title);
        }
        for (int i = 2; i < browserResultsList.getChildCount(); i++) {
            View child = browserResultsList.getChildAt(i);
            Object tag = child.getTag();
            if (!(tag instanceof WebCandidateSelection.Candidate)) continue;
            WebCandidateSelection.Candidate candidate = (WebCandidateSelection.Candidate) tag;
            boolean isSelected = candidateSelection.isSelected(candidate.url);
            child.setBackground(rounded(isSelected ? Color.rgb(30, 61, 63) : INPUT, 12));
            child.setSelected(isSelected);
            View actions = ((ViewGroup) child).getChildAt(((ViewGroup) child).getChildCount() - 1);
            if (actions instanceof ViewGroup) {
                for (int j = 0; j < ((ViewGroup) actions).getChildCount(); j++) {
                    View action = ((ViewGroup) actions).getChildAt(j);
                    if (action instanceof Button && ((Button) action).getContentDescription() != null
                            && ((Button) action).getContentDescription().toString().startsWith("Select ")) {
                        ((Button) action).setText(isSelected ? "Selected ✓" : "Select this link");
                        action.setSelected(isSelected);
                    }
                }
            }
        }
        if (selected != null) {
            selectedCandidateAction.setOnClickListener(v -> {
                if (selected.archive) showSelectedArchiveFormat(selected);
                else {
                    browserWebView.loadUrl(selected.url);
                    setBrowserStatus("Opened selected page · choose Online APK or capture an Offline snapshot.", GREEN);
                }
            });
        } else {
            selectedCandidateAction.setOnClickListener(null);
        }
    }

    private void showSelectedArchiveFormat(WebCandidateSelection.Candidate candidate) {
        String[] choices = {"Auto-detect", "Ren’Py archive", "HTML game archive"};
        WebGameLinkAnalyzer.Format[] formats = {
                WebGameLinkAnalyzer.Format.UNKNOWN,
                WebGameLinkAnalyzer.Format.RENPY,
                WebGameLinkAnalyzer.Format.HTML
        };
        new AlertDialog.Builder(this).setTitle("Convert “" + candidate.title + "”")
                .setItems(choices, (dialog, which) -> {
                    WebGameLinkAnalyzer.Format format = formats[which];
                    String description = which == 0 ? "game ZIP" : which == 1 ? "Ren’Py ZIP" : "HTML ZIP";
                    confirmDownload(candidate.url, candidate.title, description, format);
                }).setNegativeButton("Cancel", null).show();
    }

    private void confirmDownload(String url, String title, String description, WebGameLinkAnalyzer.Format format) {
        if (!isHttpUrl(url)) { setBrowserStatus("Only http(s) download links are supported.", Color.rgb(255, 140, 140)); return; }
        if (WebGameLinkAnalyzer.isWindowsExecutable(url)) { setBrowserStatus("A Windows .exe cannot be converted directly. Find the full Ren’Py game ZIP with compiled game/ data, or the original HTML game files.", Color.rgb(255, 190, 120)); return; }
        String choice = format == WebGameLinkAnalyzer.Format.UNKNOWN ? "Auto-detect the game runtime" : "Use the " + format + " runtime (archive contents will be checked)";
            new AlertDialog.Builder(this).setTitle("Download game bundle?").setMessage("APK Forge will download " + description + " from " + Uri.parse(url).getHost() + " (up to 6 GiB compressed). Game data may expand up to 6 GiB, but the finished single Ren'Py APK must stay under 1.9 GiB to install. Larger games require Ren'Py's official downloader and compatible web hosting. " + choice + ". Continue only if you trust this source and have rights to use the game.")
                .setNegativeButton("Cancel", null).setPositiveButton("Download & convert", (d, w) -> downloadAndConvert(url, title, format)).show();
    }

    private void downloadAndConvert(String url, String title, WebGameLinkAnalyzer.Format format) {
        if (busy) return;
        if (!isZipUrl(url, "", fileNameFromUrl(url) + " " + title)) { setBrowserStatus("For automatic offline conversion, select a complete .zip game bundle. For a live HTML page, choose Make online APK.", Color.rgb(255, 190, 120)); return; }
        final String appTitle = OfflinePackageFormat.cleanGameTitle(title, fileNameFromUrl(url));
        final String userAgent = browserWebView == null ? "APKForge/1.0" : browserWebView.getSettings().getUserAgentString();
        final String referer = browserWebView == null ? "" : browserWebView.getUrl();
        final String cookies = CookieManager.getInstance().getCookie(url);
        setBrowserStatus("Downloading game bundle…", AQUA);
        record(DiagnosticLog.Event.DOWNLOAD_STARTED, 0, 0);
        setBusy(true, "Downloading the selected game ZIP…");
        worker.execute(() -> {
            File destination = null;
            try {
                File dir = new File(getFilesDir(), "downloads");
                if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Could not create the downloads folder.");
                destination = new File(dir, safeName(fileNameFromUrl(url)) + "-" + System.currentTimeMillis() + ".zip");
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(20000); connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", userAgent);
                if (referer != null && !referer.isEmpty()) connection.setRequestProperty("Referer", referer);
                if (cookies != null) connection.setRequestProperty("Cookie", cookies);
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("Download server returned HTTP " + code + ".");
                long expected = connection.getContentLengthLong();
            if (expected > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("The compressed ZIP is larger than the 6 GiB download limit.");
                long total = 0;
                final long[] lastStatusUpdate = {0};
                try (InputStream in = new BufferedInputStream(connection.getInputStream()); OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
                    byte[] buffer = new byte[65536]; int count;
                    while ((count = in.read(buffer)) != -1) {
                        total += count;
                if (total > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("Download exceeded the 6 GiB compressed-file limit.");
                        out.write(buffer, 0, count);
                        if (System.currentTimeMillis() - lastStatusUpdate[0] > 1000) {
                            lastStatusUpdate[0] = System.currentTimeMillis();
                            final long transferred = total;
                            runOnUiThread(() -> setBrowserStatus("Downloading game · " + readableSize(transferred) + (expected > 0 ? " / " + readableSize(expected) : " downloaded"), AQUA));
                        }
                    }
                } finally { connection.disconnect(); }
                if (total < 4) throw new IllegalStateException("The server returned an empty or incomplete file.");
                File downloaded = destination;
                record(DiagnosticLog.Event.DOWNLOAD_COMPLETED, total, 0);
                selectedLocalFile = downloaded; selectedFile = null; selectedFolder = null;
                runOnUiThread(() -> {
                    showConvertPage(0);
                    setBusy(false, null);
                    selectedFormatOverride = format;
                    nameField.setText(appTitle);
                    selectedText.setText("Downloaded from website: " + appTitle); selectedText.setTextColor(AQUA);
                    setBrowserStatus("Download complete · " + readableSize(downloaded.length()) + " · starting conversion…", GREEN);
                    convert();
                });
            } catch (Exception e) {
                record(DiagnosticLog.Event.DOWNLOAD_FAILED, 0, 0);
                if (destination != null) destination.delete();
                String error = e.getMessage() == null ? "Download failed." : e.getMessage();
                runOnUiThread(() -> { setBusy(false, null); setBrowserStatus(error, Color.rgb(255, 140, 140)); setStatus(error, Color.rgb(255, 140, 140)); });
            }
        });
    }

    private void wrapCurrentWebsite() {
        if (browserWebView == null || !isHttpUrl(browserWebView.getUrl())) { setBrowserStatus("Open a playable http(s) HTML game page first.", Color.rgb(255, 190, 120)); return; }
        wrapWebsite(browserWebView.getUrl());
    }

    private void captureCurrentWebsite() {
        if (browserWebView == null || !isHttpUrl(browserWebView.getUrl())) {
            setBrowserStatus("Open the playable browser-game page before starting a capture.", Color.rgb(255, 190, 120));
            return;
        }
        final String pageUrl = browserWebView.getUrl();
        final String userAgent = browserWebView.getSettings().getUserAgentString();
        new AlertDialog.Builder(this).setTitle("Capture this browser game?")
                .setMessage("APK Forge will save up to 8 MiB of page markup and 900 browser-loaded resources (maximum 1.75 GiB, streamed to avoid loading large files into memory) and package them for offline play. The finished APK still must fit below 1.9 GiB. Page markup transfers in small chunks to avoid WebView IPC crashes. This is a best-effort snapshot: accounts, multiplayer, server APIs, DRM, and content loaded only later cannot be copied. Play through the parts you need first. Continue only for content you own or have permission to save.")
                .setNegativeButton("Cancel", null).setPositiveButton("Capture page & files", (dialog, which) -> {
                    record(DiagnosticLog.Event.SNAPSHOT_STARTED, 0, 0);
                    setBusy(true, "Inspecting the loaded game page…");
                    String script = "(function(){function abs(v){try{return new URL(v,document.baseURI).href}catch(e){return v}}function css(s){return s.replace(/url\\(\\s*(['\\\"]?)(?!data:|blob:|#)([^)'\\\"]+)\\1\\s*\\)/gi,function(m,q,u){return 'url('+q+abs(u.trim())+q+')'}).replace(/@import\\s+(['\\\"])(.*?)\\1/gi,function(m,q,u){return '@import '+q+abs(u)+q})}for(const e of document.querySelectorAll('script[src],img[src],audio[src],video[src],source[src],track[src],iframe[src],link[href],object[data],embed[src],[data-src],[data-href],[poster],[srcset]')){for(const a of ['src','href','poster','data','data-src','data-href'])if(e.hasAttribute(a))e.setAttribute(a,abs(e.getAttribute(a)))}for(const e of document.querySelectorAll('[style]'))e.setAttribute('style',css(e.getAttribute('style')));for(const e of document.querySelectorAll('style'))e.textContent=css(e.textContent);for(const e of document.querySelectorAll('[srcset]'))e.setAttribute('srcset',e.getAttribute('srcset').split(',').map(x=>{let p=x.trim().split(/\\s+/);p[0]=abs(p[0]);return p.join(' ')}).join(', '));let h='<!doctype html>'+document.documentElement.outerHTML;window.__apkForgeCaptureHtml=h;let r=[...performance.getEntriesByType('resource').map(x=>x.name)];for(const e of document.querySelectorAll('script[src],img[src],audio[src],video[src],source[src],track[src],iframe[src],link[href],object[data],embed[src],[data-src],[data-href],[poster]'))for(const a of ['src','href','poster','data','data-src','data-href'])if(e.hasAttribute(a))r.push(e.getAttribute(a));for(const e of document.querySelectorAll('[srcset]'))for(const x of e.getAttribute('srcset').split(',')){let u=x.trim().split(/\\s+/)[0];if(u)r.push(abs(u))}let u=[...new Set(r.filter(x=>/^https?:/i.test(x)&&x.length<=1024))];return JSON.stringify({htmlLength:h.length,resources:u.slice(0,900),omittedResources:Math.max(0,u.length-900),title:(document.title||'').slice(0,128)})})()";
                    browserWebView.evaluateJavascript(script, result -> beginChunkedWebsiteCapture(result, pageUrl, userAgent));
                }).show();
    }

    private void beginChunkedWebsiteCapture(String result, String pageUrl, String userAgent) {
        try {
            Object parsed = new org.json.JSONTokener(result).nextValue();
            JSONObject data = parsed instanceof String ? new JSONObject((String) parsed) : (JSONObject) parsed;
            int htmlLength = data.optInt("htmlLength", -1);
            int omittedResources = Math.max(0, data.optInt("omittedResources", 0));
            WebCapturePayload payload = new WebCapturePayload(htmlLength);
            String title = data.optString("title", "Saved Game");
            JSONArray array = data.optJSONArray("resources");
            List<String> resources = new ArrayList<>();
            java.util.Map<String, String> cookies = new java.util.HashMap<>();
            if (array != null) for (int i = 0; i < array.length() && resources.size() < 900; i++) {
                String resource = array.optString(i, "");
                if (!isHttpUrl(resource) || resources.contains(resource)) continue;
                resources.add(resource);
                String cookie = CookieManager.getInstance().getCookie(resource);
                if (cookie != null) cookies.put(resource, cookie);
            }
            final String pageTitle = OfflinePackageFormat.cleanGameTitle(title, "Web Game");
            setBrowserStatus("Reading page markup in safe chunks…", AQUA);
            readWebsiteHtmlChunk(payload, 0, htmlLength, pageUrl, pageTitle, userAgent, resources, cookies, omittedResources);
        } catch (Exception e) {
            clearWebCaptureHtml();
            record(DiagnosticLog.Event.SNAPSHOT_FAILED, 0, 0);
            DiagnosticLog.recordFailure(getFilesDir(), DiagnosticLog.Event.SNAPSHOT_FAILED, 1, e);
            setBusy(false, null);
            setBrowserStatus(e.getMessage() == null ? "Could not inspect the page safely." : e.getMessage(), Color.rgb(255, 140, 140));
            addAiSearchFallback("Offline capture could not inspect this page.");
        }
    }

    private void readWebsiteHtmlChunk(WebCapturePayload payload, int offset, int totalLength, String pageUrl,
                                     String pageTitle, String userAgent, List<String> resources,
                                     java.util.Map<String, String> cookies, int omittedResources) {
        int end = Math.min(totalLength, offset + WebCapturePayload.MAX_CHUNK_CHARS);
        String script = "(function(){let h=window.__apkForgeCaptureHtml;return h&&h.length===" + totalLength + "?h.slice(" + offset + "," + end + "):null})()";
        browserWebView.evaluateJavascript(script, result -> {
            try {
                Object parsed = new org.json.JSONTokener(result).nextValue();
                if (!(parsed instanceof String)) throw new IllegalStateException("The browser stopped responding during page capture.");
                payload.append((String) parsed);
                if (end < totalLength) {
                    readWebsiteHtmlChunk(payload, end, totalLength, pageUrl, pageTitle, userAgent, resources, cookies, omittedResources);
                } else {
                    final String html = payload.finish();
                    clearWebCaptureHtml();
                    packageWebsiteSnapshot(pageUrl, pageTitle, userAgent, resources, cookies, html, omittedResources);
                }
            } catch (Exception e) {
                clearWebCaptureHtml();
                record(DiagnosticLog.Event.SNAPSHOT_FAILED, offset, 0);
                DiagnosticLog.recordFailure(getFilesDir(), DiagnosticLog.Event.SNAPSHOT_FAILED, 2, e);
                setBusy(false, null);
                setBrowserStatus(e.getMessage() == null ? "Could not read the page markup." : e.getMessage(), Color.rgb(255, 140, 140));
                addAiSearchFallback("Offline capture could not read the page markup.");
            }
        });
    }

    private void clearWebCaptureHtml() {
        if (browserWebView != null) browserWebView.evaluateJavascript("window.__apkForgeCaptureHtml=null", null);
    }

    private void packageWebsiteSnapshot(String pageUrl, String pageTitle, String userAgent, List<String> resources,
                                        java.util.Map<String, String> cookies, String html, int omittedResources) {
        setBusy(true, "Saving page and browser-loaded game files…");
        worker.execute(() -> {
            File archive = new File(getCacheDir(), "web-capture-" + System.currentTimeMillis() + ".zip");
            try {
                WebGameSnapshot.CaptureResult captureResult = WebGameSnapshot.capture(archive, pageUrl, pageTitle, html, resources, userAgent, cookies);
                int incompleteResources = captureResult.skippedResources + omittedResources;
                record(DiagnosticLog.Event.SNAPSHOT_COMPLETED, captureResult.capturedResources, archive.length());
                selectedLocalFile = archive; selectedFile = null; selectedFolder = null;
                runOnUiThread(() -> {
                    nameField.setText(pageTitle);
                    selectedFormatOverride = WebGameLinkAnalyzer.Format.HTML;
                    setBusy(false, null);
                    if (incompleteResources > 0) {
                        setBrowserStatus("Partial snapshot: " + captureResult.capturedResources + " files captured, " + incompleteResources + " missing or beyond the 900-file limit.", Color.rgb(255, 190, 120));
                        addAiSearchFallback("Offline capture was incomplete.");
                        new AlertDialog.Builder(MainActivity.this).setTitle("Offline capture is incomplete")
                                .setMessage(captureResult.capturedResources + " files were captured; " + incompleteResources
                                        + " were unavailable or beyond the 900-file limit. Building now may leave parts of the game broken."
                                        + " You can return to the game, visit more areas, and capture again.")
                                .setNegativeButton("Discard snapshot", (d, w) -> archive.delete())
                                .setPositiveButton("Build partial APK", (d, w) -> {
                                    selectedLocalFile = archive; selectedFile = null; selectedFolder = null;
                                    nameField.setText(pageTitle);
                                    selectedFormatOverride = WebGameLinkAnalyzer.Format.HTML;
                                    showConvertPage(0);
                                    convert();
                                }).show();
                    } else {
                        setBrowserStatus("Offline snapshot ready · " + captureResult.capturedResources + " game files captured · building APK…", GREEN);
                        selectedLocalFile = archive; selectedFile = null; selectedFolder = null;
                        nameField.setText(pageTitle);
                        selectedFormatOverride = WebGameLinkAnalyzer.Format.HTML;
                        showConvertPage(0);
                        convert();
                    }
                });
            } catch (Exception e) {
                record(DiagnosticLog.Event.SNAPSHOT_FAILED, resources.size(), 0);
                DiagnosticLog.recordFailure(getFilesDir(), DiagnosticLog.Event.SNAPSHOT_FAILED, 3, e);
                archive.delete();
                String message = e.getMessage() == null ? "Could not capture this website." : e.getMessage();
                runOnUiThread(() -> { setBusy(false, null); setBrowserStatus("Capture failed: " + message, Color.rgb(255, 140, 140)); addAiSearchFallback("Offline capture failed."); });
            }
        });
    }

    private void wrapWebsite(String url) {
        if (!isHttpUrl(url)) { setBrowserStatus("Only an http(s) HTML game page can be wrapped online.", Color.rgb(255, 190, 120)); return; }
        new AlertDialog.Builder(this).setTitle("Create online HTML APK?").setMessage("This APK opens " + Uri.parse(url).getHost() + " in its game view. The game stays on the website, so the phone needs internet to play. This does not download the site's files for offline use.")
                .setNegativeButton("Cancel", null).setPositiveButton("Create APK", (d, w) -> {
                    String host = Uri.parse(url).getHost();
                    String title = host == null ? "Web Game" : host.replaceFirst("^www\\.", "");
                    title = title.replaceAll("[^A-Za-z0-9 _-]", " ").trim();
                    if (title.isEmpty()) title = "Web Game";
                    final String appTitle = title.substring(0, Math.min(16, title.length()));
                    setBusy(true, "Preparing an online HTML game wrapper…");
                    try {
                        worker.execute(() -> {
                            File archive = null;
                            try {
                                archive = OnlineGameArchive.create(getCacheDir(), appTitle, url);
                                File completedArchive = archive;
                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) { completedArchive.delete(); return; }
                                    showConvertPage(0);
                                    nameField.setText(appTitle);
                                    selectedText.setText("Online HTML game: " + url); selectedText.setTextColor(BLUE);
                                    selectedLocalFile = completedArchive; selectedFile = null; selectedFolder = null;
                                    selectedFormatOverride = WebGameLinkAnalyzer.Format.HTML;
                                    convert();
                                });
                            } catch (Exception e) {
                                if (archive != null) archive.delete();
                                String error = e.getMessage() == null ? "Could not prepare website APK." : e.getMessage();
                                runOnUiThread(() -> { setBusy(false, null); setBrowserStatus(error, Color.rgb(255, 140, 140)); addAiSearchFallback("The online game wrapper could not be prepared."); });
                            }
                        });
                    } catch (RuntimeException rejected) {
                        setBusy(false, null);
                        setBrowserStatus("Could not start online APK preparation. Please retry.", Color.rgb(255, 140, 140));
                    }
                }).show();
    }

    private boolean isHttpUrl(String value) { return value != null && value.matches("(?i)^https?://[^\\s]+$"); }
    private boolean isZipUrl(String url, String mime, String filename) {
        String lower = (url + " " + filename + " " + mime).toLowerCase(Locale.ROOT);
        return lower.matches("(?s).*\\.zip(?:[?#\\s].*|$).*") || lower.contains("application/zip") || lower.contains("application/x-zip");
    }
    private String fileNameFromUrl(String value) {
        try { String path = Uri.parse(value).getLastPathSegment(); return path == null || path.isEmpty() ? "game.zip" : URLDecoder.decode(path, "UTF-8"); }
        catch (Exception e) { return "game.zip"; }
    }
    private void setBrowserStatus(String text, int color) {
        if (browserStatus != null) { browserStatus.setText(text); browserStatus.setTextColor(color); }
        if (browserStatusDot != null) browserStatusDot.setBackground(rounded(color, 8));
    }

    private void showNavigationDrawer() {
        if (navigationDrawer != null && navigationDrawer.isShowing()) return;
        Dialog dialog = new Dialog(this);
        navigationDrawer = dialog;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(24), dp(18), dp(18));
        panel.setBackground(gradient(new int[]{Color.rgb(35, 30, 48), Color.rgb(21, 22, 28)}, 0));
        panel.addView(label("APK FORGE", 17, AQUA, true), fullWithTop(8));
        panel.addView(label("GAME CONVERTER", 10, MUTED, true), fullWithTop(3));
        addGap(panel, 20);
        addDrawerItem(panel, "⌂     Convert a game", () -> showConvertPage(0));
        addDrawerItem(panel, "◎     Website browser", this::openBrowser);
        addDrawerItem(panel, "▣     Game library", () -> showConvertPage(1));
        addDrawerItem(panel, "▤     Installed games", () -> showConvertPage(2));
        addDrawerItem(panel, "▧     Offline HTML save editor", this::openOfflineSaveEditor);
        addDrawerItem(panel, "☷     Diagnostics / export log", this::openDiagnostics);
        addDrawerItem(panel, "⚙     Settings & local AI", this::openSettings);
        addGap(panel, 14);
        panel.addView(label("Ren’Py: download a complete game ZIP.\nHTML: download its ZIP or wrap the current page online.", 12, MUTED, false), full());
        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) { window.setBackgroundDrawableResource(android.R.color.transparent); window.setLayout(dp(300), -1); window.setGravity(Gravity.START); window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); WindowManager.LayoutParams attrs = window.getAttributes(); attrs.dimAmount = 0.55f; window.setAttributes(attrs); }
        dialog.show();
        if (window != null) { window.setLayout(dp(300), -1); window.setGravity(Gravity.START); }
    }

    private void addDrawerItem(LinearLayout panel, String title, Runnable action) {
        Button item = button(title, false);
        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        item.setOnClickListener(v -> { if (navigationDrawer != null) navigationDrawer.dismiss(); action.run(); });
        panel.addView(item, fullWithTop(8));
    }

    private int visibleConvertSection() {
        if (tabletMode || studioMode) return currentConvertSection;
        if (convertScroll.getScrollY() >= installedSection.getTop()) return 2;
        if (convertScroll.getScrollY() >= librarySection.getTop()) return 1;
        return 0;
    }

    private void showPhoneLibrary(int section) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setPadding(dp(18), dp(16), dp(18), dp(24));
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);
        LinearLayout tabs = new LinearLayout(this);
        String[] names = {"Finished builds", "Installed apps"};
        for (int i = 0; i < names.length; i++) {
            int destination = i + 1;
            Button tab = button(names[i], section == destination);
            tab.setSelected(section == destination);
            tab.setOnClickListener(v -> showConvertPage(destination));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) params.leftMargin = dp(8);
            tabs.addView(tab, params);
        }
        page.addView(tabs, full());
        LinearLayout card = section == 1 ? librarySection : installedSection;
        if (card.getParent() instanceof ViewGroup) ((ViewGroup) card.getParent()).removeView(card);
        page.addView(card, fullWithTop(14));
        showPhonePage(scroll, 2);
    }

    private void showConvertPage(int section) {
        currentConvertSection = section;
        showingBrowser = false;
        showingDiagnostics = false;
        showingSettings = false;
        showingOfflineSaveEditor = false;
        if (tabletMode) {
            if (section == 1) {
                selectTabletNav(tabletLibraryButton);
                showTabletSection(librarySection);
            } else if (section == 2) {
                selectTabletNav(tabletInstalledButton);
                showTabletSection(installedSection);
            } else {
                selectTabletNav(tabletConvertButton);
                if (convertRoot.getParent() instanceof ViewGroup) ((ViewGroup) convertRoot.getParent()).removeView(convertRoot);
                tabletContent.removeAllViews();
                tabletContent.addView(convertRoot, new FrameLayout.LayoutParams(-1, -1));
            }
            return;
        }
        if (studioMode && section != 0) {
            showPhoneLibrary(section);
            return;
        }
        if (!convertRoot.isAttachedToWindow()) {
            showAppContent(convertRoot);
            presentStudioView(convertRoot);
        }
        if (!studioMode) convertScroll.post(() -> {
            View target = section == 1 ? librarySection : section == 2 ? installedSection : convertScroll.getChildAt(0);
            if (target != null) convertScroll.smoothScrollTo(0, target.getTop());
        });
    }

    private LinearLayout card(LinearLayout parent, String heading, String detail) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(studioMode ? 20 : 17), dp(studioMode ? 20 : 16), dp(studioMode ? 20 : 17), dp(studioMode ? 20 : 16));
        GradientDrawable surface = rounded(CARD, 22);
        surface.setStroke(dp(1), Color.rgb(49, 61, 80));
        box.setBackground(surface);
        TextView title = label(heading, 18, WHITE, true);
        if (Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        box.addView(title);
        TextView subtitle = label(detail, 12, MUTED, false);
        subtitle.setPadding(0, dp(7), 0, 0);
        box.addView(subtitle);
        parent.addView(box, full());
        return box;
    }

    private EditText input(String hint) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextSize(14);
        field.setTextColor(WHITE);
        field.setHintTextColor(MUTED);
        field.setHint(hint);
        field.setPadding(dp(13), dp(10), dp(13), dp(10));
        GradientDrawable fieldSurface = rounded(INPUT, 13);
        fieldSurface.setStroke(dp(1), Color.rgb(59, 73, 94));
        field.setBackground(fieldSurface);
        field.setMinHeight(dp(48));
        return field;
    }

    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setTransformationMethod(null);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(48));
        b.setPadding(dp(12), dp(9), dp(12), dp(9));
        b.setStateListAnimator(null);
        styleButton(b, primary);
        return b;
    }

    private void styleButton(Button button, boolean primary) {
        button.setTextColor(new ColorStateList(new int[][]{
                {-android.R.attr.state_enabled}, {}
        }, new int[]{MUTED, primary ? BG : WHITE}));
        GradientDrawable surface = gradient(primary ? new int[]{MINT, Color.rgb(109, 211, 204)}
                : new int[]{Color.rgb(43, 54, 71), Color.rgb(29, 38, 53)}, 16);
        if (!primary) surface.setStroke(dp(1), Color.rgb(62, 76, 96));
        android.graphics.drawable.StateListDrawable backgrounds = new android.graphics.drawable.StateListDrawable();
        backgrounds.addState(new int[]{-android.R.attr.state_enabled}, rounded(INPUT, 16));
        backgrounds.addState(new int[]{}, surface);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(primary ? 0x250D1722 : 0x36BCEAFF),
                backgrounds, null));
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(radius));
        return shape;
    }

    private GradientDrawable gradient(int[] colors, int radius) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private LinearLayout.LayoutParams full() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams fullWithTop(int top) { LinearLayout.LayoutParams p = full(); p.topMargin = dp(top); return p; }
    private void addGap(LinearLayout parent, int height) { View gap = new View(this); parent.addView(gap, new LinearLayout.LayoutParams(1, dp(height))); }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/html", "application/zip", "application/x-zip-compressed"});
        startActivityForResult(intent, PICK_FILE);
    }

    private void pickFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_FOLDER);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UNINSTALL) {
            refreshInstalled();
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Game uninstalled.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode == REQUEST_INSTALL_PERMISSION) {
            if (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls())
                setStatus("Install permission is ready. Tap Install in your library again.", GREEN);
            else
                setStatus("Allow APK Forge to install unknown apps, then retry.", Color.rgb(255, 190, 120));
            return;
        }
        if (requestCode == SAVE_DIAGNOSTICS) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            Uri destination = data.getData();
            worker.execute(() -> {
                try {
                    byte[] bytes = DiagnosticLog.prepareExport(getFilesDir()).getBytes(StandardCharsets.UTF_8);
                    try (OutputStream out = getContentResolver().openOutputStream(destination)) {
                        if (out == null) throw new IllegalStateException("No selected output stream");
                        out.write(bytes);
                    }
                    runOnUiThread(() -> Toast.makeText(this, "Diagnostics exported to the location you chose.", Toast.LENGTH_LONG).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Could not export diagnostics. Choose another save location and retry.", Toast.LENGTH_LONG).show());
                }
            });
            return;
        }
        if (requestCode == OPEN_OFFLINE_SAVE_BACKUP) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) loadOfflineSaveBackup(data.getData());
            return;
        }
        if (requestCode == EXPORT_OFFLINE_SAVE_BACKUP) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) exportOfflineSaveBackup(data.getData());
            return;
        }
        if (requestCode == CHECK_EXE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) checkExeHash(data.getData());
            return;
        }
        if (requestCode == SAVE_REVIEW_EXE) {
            File source = pendingReviewExe;
            pendingReviewExe = null;
            if (resultCode != RESULT_OK || data == null || data.getData() == null || source == null || !source.isFile()) return;
            Uri destination = data.getData();
            worker.execute(() -> {
                try (InputStream input = new BufferedInputStream(new FileInputStream(source));
                     OutputStream output = getContentResolver().openOutputStream(destination)) {
                    if (output == null) throw new IllegalStateException("Could not open the selected save location.");
                    byte[] buffer = new byte[128 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    runOnUiThread(() -> Toast.makeText(this, "EXE copy saved. It has not been run; check the checksum and reputation first.", Toast.LENGTH_LONG).show());
                } catch (Exception error) {
                    runOnUiThread(() -> Toast.makeText(this, "Could not save the EXE copy.", Toast.LENGTH_LONG).show());
                }
            });
            return;
        }
        if (requestCode == IMPORT_REPAIR_MODEL) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) importRepairModel(data.getData());
            return;
        }
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == PICK_FILE) {
            selectedFile = data.getData(); selectedFolder = null; selectedLocalFile = null;
            selectedText.setText("File selected: " + displayName(selectedFile)); selectedText.setTextColor(AQUA);
        } else if (requestCode == PICK_FOLDER) {
            selectedFolder = data.getData(); selectedFile = null; selectedLocalFile = null;
            getContentResolver().takePersistableUriPermission(selectedFolder, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            selectedText.setText("Project folder selected · ready to package"); selectedText.setTextColor(AQUA);
        } else if (requestCode == SAVE_APK && lastApk != null) {
            Uri destination = data.getData();
            worker.execute(() -> {
                try (InputStream in = new BufferedInputStream(new java.io.FileInputStream(lastApk)); OutputStream out = getContentResolver().openOutputStream(destination)) {
                    if (out == null) throw new IllegalStateException("Could not open the selected save location.");
                    byte[] buffer = new byte[65536]; int count;
                    while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
                    runOnUiThread(() -> setStatus("APK saved. You can open it from the Files app to install." , GREEN));
                } catch (Exception e) { runOnUiThread(() -> setStatus("Save failed: " + e.getMessage(), Color.rgb(255, 140, 140))); }
            });
        } else if (requestCode == SAVE_GAME_DATA && pendingGameDataSave != null) {
            Uri destination = data.getData();
            File source = pendingGameDataSave;
            worker.execute(() -> {
                try (InputStream in = new BufferedInputStream(new FileInputStream(source)); OutputStream out = getContentResolver().openOutputStream(destination)) {
                    if (out == null) throw new IllegalStateException("Could not open the selected save location.");
                    byte[] buffer = new byte[1024 * 1024]; int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    runOnUiThread(() -> setStatus("Ren’Py game-data ZIP saved. Keep it with the APK and import it in the installed game.", GREEN));
                } catch (Exception e) { runOnUiThread(() -> setStatus("Game-data export failed: " + e.getMessage(), Color.rgb(255, 140, 140))); }
            });
        }
    }

    private void checkExeHash(Uri fileUri) {
        String name = displayName(fileUri);
        if (!RepairAssistantPolicy.isWindowsExecutableName(name)) {
            Toast.makeText(this, "Choose a Windows .exe file to check.", Toast.LENGTH_LONG).show();
            return;
        }
        assistantResult.setText("Calculating the file hash locally…");
        assistantWorker.execute(() -> {
            try {
                InputStream input = getContentResolver().openInputStream(fileUri);
                if (input == null) throw new IllegalStateException("Could not read the selected file.");
                String hash = RepairAssistantModel.sha256(input);
                String lookupUrl = RepairAssistantPolicy.virusTotalLookupUrl(hash);
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Look up EXE by SHA-256")
                        .setMessage("APK Forge read “" + name + "” locally and calculated its SHA-256:\n\n" + hash
                                + "\n\nOpen its VirusTotal report? It may show version metadata and reputation if this exact file is already known. Only the hash is sent; this does not scan a new file and is not a guarantee of safety.")
                        .setNegativeButton("Cancel", null)
                        .setNeutralButton("Upload for a scan…", (dialog, which) -> confirmVirusTotalUpload())
                        .setPositiveButton("Open hash report", (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lookupUrl))))
                        .show());
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this, "Could not read and hash that EXE.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void confirmVirusTotalUpload() {
        new AlertDialog.Builder(this)
                .setTitle("Publicly share this EXE for scanning?")
                .setMessage("APK Forge will not upload the file. This opens VirusTotal's upload page; you must select the EXE again and confirm there. Standard uploads are shared with VirusTotal's security partners. Do not upload confidential, proprietary, or personal files unless you have permission. A clean result still cannot guarantee a file is safe.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open VirusTotal upload", (dialog, which) -> {
                    Intent upload = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(RepairAssistantPolicy.virusTotalUploadUrl()));
                    try {
                        startActivity(upload);
                    } catch (android.content.ActivityNotFoundException unavailable) {
                        Toast.makeText(this, "No browser is available to open the upload page.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) { }
        return uri.getLastPathSegment() == null ? "Selected file" : uri.getLastPathSegment();
    }

    private void convert() {
        if (busy) return;
        if (selectedFile == null && selectedFolder == null && (selectedLocalFile == null || !selectedLocalFile.isFile())) { setStatus("Choose an HTML/ZIP file or a project folder first.", Color.rgb(255, 190, 120)); return; }
        String appName = nameField.getText().toString().trim();
        String appPackage = packageField.getText().toString().trim();
        if (appName.isEmpty() || appName.length() > 16 || !validPackage(appPackage)) { setStatus("Enter a title of up to 16 characters and an identity seed such as com.studio.mygame.", Color.rgb(255, 190, 120)); return; }
        setBusy(true, "Preparing game assets on this device…");
        record(DiagnosticLog.Event.CONVERSION_STARTED, selectedFolder != null ? 1 : selectedFile != null ? 2 : 3, 0);
        Uri file = selectedFile, folder = selectedFolder;
        File localFile = selectedLocalFile;
        WebGameLinkAnalyzer.Format formatOverride = selectedFormatOverride;
        selectedFormatOverride = WebGameLinkAnalyzer.Format.UNKNOWN;
        final long conversionStartedAt = System.currentTimeMillis();
        worker.execute(() -> {
            File archive = null;
            boolean temporaryArchive = false;
            OfflineApkPackager.Result packaged = null;
            File stagedLibraryApk = null;
            boolean conversionCommitted = false;
            try {
                if (localFile != null && localFile.isFile()) {
                    // A completed website download already is the source ZIP.
                    // Reuse it instead of temporarily duplicating several GiB.
                    archive = localFile;
                } else {
                    archive = File.createTempFile("apkforge-project-", ".zip", getCacheDir());
                    temporaryArchive = true;
                    if (folder != null) zipTree(folder, archive); else zipSingle(file, archive);
                }
                GameSourceInventory.Result inventory = GameSourceInventory.scan(archive, 53_681_283L);
                long totalWorkBytes = inventory.workBytes(archive.length());
                long learnedRate = getSharedPreferences("conversion-timing", MODE_PRIVATE).getLong("bytes_per_second", 0);
                long estimatedSeconds = ConversionEta.estimateSeconds(totalWorkBytes, learnedRate);
                RenPyLargeGameAnalyzer.Result renpyAnalysis = RenPyLargeGameAnalyzer.analyze(archive, 53_681_283L);
                boolean externalRenPyData = renpyAnalysis.isRenPy
                        && RenPyLargeGameAnalyzer.requiresExternalGameData(renpyAnalysis.estimatedApkBytes);
                String largeDataNote = externalRenPyData
                        ? " · large Ren’Py game data will be a separate offline ZIP" : "";
                setStatusFromWorker(inventory.summary() + largeDataNote + " · building and signing · ETA ~" + ConversionEta.format(estimatedSeconds)
                        + " (rough estimate; storage, device speed, and game contents affect it).", AQUA);
                packaged = OfflineApkPackager.build(this, archive, appName, appPackage, formatOverride);
                record(packaged.kind.toLowerCase(Locale.ROOT).contains("ren") ? DiagnosticLog.Event.RENPY_COMPLETED : DiagnosticLog.Event.HTML_COMPLETED, 1, packaged.apk.length());
                File saved = storeInLibrary(packaged.apk, appName);
                stagedLibraryApk = saved;
                File savedGameData = packaged.gameDataBundle == null ? null : storeGameDataInLibrary(packaged.gameDataBundle, saved);
                packaged.apk.delete();
                if (packaged.gameDataBundle != null) packaged.gameDataBundle.delete();
                conversionCommitted = true;
                if (localFile != null && (localFile.getName().startsWith("online-html-") || isManagedDownload(localFile))) localFile.delete();
                long elapsedSeconds = Math.max(1, (System.currentTimeMillis() - conversionStartedAt + 999) / 1000);
                android.content.SharedPreferences timing = getSharedPreferences("conversion-timing", MODE_PRIVATE);
                long measuredRate = totalWorkBytes / elapsedSeconds;
                timing.edit().putLong("bytes_per_second", ConversionEta.updatedRate(learnedRate, measuredRate)).apply();
                OfflineApkPackager.Result completed = packaged;
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    lastApk = saved;
                    lastGameDataBundle = savedGameData;
                    if (playBuiltButton != null) playBuiltButton.setEnabled(true);
                    if (saveApkButton != null) saveApkButton.setEnabled(true);
                    showConvertPage(1); setBusy(false, null); refreshLibrary();
                    if (saveGameDataButton != null) saveGameDataButton.setVisibility(savedGameData == null ? View.GONE : View.VISIBLE);
                    String ready = completed.kind + " APK ready · " + readableSize(saved.length())
                            + (savedGameData == null ? "" : " · separate game data " + readableSize(savedGameData.length()) + " — export both files")
                            + " · built in " + ConversionEta.format(elapsedSeconds) + " · package " + completed.packageName;
                    setStatus(ready, GREEN);
                    Toast.makeText(this, savedGameData == null ? "Your APK is ready" : "APK and separate Ren’Py game data are ready", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                if (stagedLibraryApk != null) stagedLibraryApk.delete();
                record(DiagnosticLog.Event.CONVERSION_FAILED, 0, 0);
                DiagnosticLog.recordFailure(getFilesDir(), DiagnosticLog.Event.CONVERSION_FAILED, 4, e);
                String message = e.getMessage() == null ? "Build failed." : e.getMessage();
                runOnUiThread(() -> { showConvertPage(0); setBusy(false, null); setStatus(message, Color.rgb(255, 140, 140)); });
            } finally {
                if (packaged != null && !conversionCommitted) {
                    if (packaged.apk != null) packaged.apk.delete();
                    if (packaged.gameDataBundle != null) packaged.gameDataBundle.delete();
                }
                if (temporaryArchive && archive != null) archive.delete();
            }
        });
    }

    private boolean isManagedDownload(File file) {
        try {
            return file.getCanonicalFile().toPath().startsWith(new File(getFilesDir(), "downloads").getCanonicalFile().toPath());
        } catch (Exception ignored) { return false; }
    }

    private void copyFile(File source, File target) throws Exception {
        if (source == null || !source.isFile()) throw new IllegalStateException("Downloaded game archive is missing. Download it again.");
        try (InputStream in = new BufferedInputStream(new FileInputStream(source)); OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
            byte[] buffer = new byte[65536]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        }
    }

    private void zipSingle(Uri uri, File target) throws Exception {
        String name = displayName(uri).replace("/", "_").replace("\\", "_");
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("Could not read the selected file.");
            if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                    byte[] buffer = new byte[65536]; int count;
                    while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
                }
                return;
            }
            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)))) {
            zip.putNextEntry(new ZipEntry(name));
            byte[] buffer = new byte[65536]; int count;
            while ((count = in.read(buffer)) >= 0) zip.write(buffer, 0, count);
            zip.closeEntry();
            }
        }
    }

    private void zipTree(Uri tree, File target) throws Exception {
        String rootId = DocumentsContract.getTreeDocumentId(tree);
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)))) {
            zipDirectory(tree, rootId, "", zip, new byte[65536], 0);
        }
    }

    private void zipDirectory(Uri tree, String documentId, String prefix, ZipOutputStream zip, byte[] buffer, int depth) throws Exception {
        if (depth > 80) throw new IllegalStateException("Folder nesting is too deep.");
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId);
        String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};
        try (Cursor cursor = getContentResolver().query(children, columns, null, null, null)) {
            if (cursor == null) throw new IllegalStateException("Could not read the selected folder.");
            int idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
            while (cursor.moveToNext()) {
                String id = cursor.getString(idCol), name = cursor.getString(nameCol), mime = cursor.getString(mimeCol);
                name = name == null ? "file" : name.replace("/", "_").replace("\\", "_");
                String childPath = prefix.isEmpty() ? name : prefix + "/" + name;
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    zip.putNextEntry(new ZipEntry(childPath + "/")); zip.closeEntry();
                    zipDirectory(tree, id, childPath, zip, buffer, depth + 1);
                } else {
                    Uri child = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                    try (InputStream in = getContentResolver().openInputStream(child)) {
                        if (in == null) continue;
                        zip.putNextEntry(new ZipEntry(childPath));
                        int count;
                        while ((count = in.read(buffer)) != -1) zip.write(buffer, 0, count);
                        zip.closeEntry();
                    }
                }
            }
        }
    }

    private void saveApk() {
        if (lastApk == null || !lastApk.isFile()) { setStatus("Build an APK first.", MUTED); return; }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("application/vnd.android.package-archive");
        intent.putExtra(Intent.EXTRA_TITLE, safeName(nameField.getText().toString()) + ".apk");
        startActivityForResult(intent, SAVE_APK);
    }

    private void saveGameData(File bundle) {
        if (bundle == null || !bundle.isFile()) { setStatus("This game does not have a separate data ZIP.", MUTED); return; }
        pendingGameDataSave = bundle;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        String title = bundle.getName().replaceFirst("\\.apk\\.zip$", "-game-data.zip");
        intent.putExtra(Intent.EXTRA_TITLE, title);
        startActivityForResult(intent, SAVE_GAME_DATA);
    }

    private boolean validPackage(String value) { return value.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"); }
    private String safeName(String value) { String name = value.replaceAll("[^A-Za-z0-9._-]+", "_"); return name.isEmpty() ? "game" : name; }
    private String readableSize(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) return String.format(Locale.US, "%.2f GiB", bytes / (1024.0 * 1024.0 * 1024.0));
        return bytes > 1024L * 1024 ? String.format(Locale.US, "%.1f MB", bytes / 1048576.0) : (bytes / 1024) + " KB";
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        buildApkButton.setEnabled(!value);
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        if (message != null) setStatus(message, value ? AQUA : MUTED);
    }

    private void setStatusFromWorker(String text, int color) { runOnUiThread(() -> setStatus(text, color)); }
    private void setStatus(String text, int color) { if (statusText != null) { statusText.setText(text); statusText.setTextColor(color); } }
    private File storeInLibrary(File source, String appName) throws Exception {
        File directory = new File(getFilesDir(), "library");
        if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Could not create the game library.");
        File saved = new File(directory, safeName(appName) + "-" + System.currentTimeMillis() + ".apk");
        if (saved.exists()) throw new IllegalStateException("This library filename is already in use.");
        if (!source.renameTo(saved)) {
            File partial = File.createTempFile("library-", ".partial", directory);
            try {
                copyFile(source, partial);
                if (!partial.renameTo(saved)) throw new IllegalStateException("Could not save the finished APK.");
            } finally {
                partial.delete();
            }
        }
        return saved;
    }

    private File storeGameDataInLibrary(File source, File apk) throws Exception {
        File directory = getExternalFilesDir("library-game-data");
        if (directory == null) directory = new File(getFilesDir(), "library-game-data");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Could not create separate game-data storage.");
        File saved = new File(directory, apk.getName() + ".zip");
        if (saved.exists()) throw new IllegalStateException("A game-data bundle already exists for this library build.");
        if (!source.renameTo(saved)) {
            File partial = new File(directory, saved.getName() + ".partial");
            try {
                copyFile(source, partial);
                if (!partial.renameTo(saved)) throw new IllegalStateException("Could not finish saving the separate game-data bundle.");
                source.delete();
            } catch (Exception failure) {
                partial.delete();
                throw failure;
            }
        }
        return saved;
    }

    private File gameDataForApk(File apk) {
        File directory = getExternalFilesDir("library-game-data");
        File bundle = directory == null ? null : new File(directory, apk.getName() + ".zip");
        if (bundle != null && bundle.isFile()) return bundle;
        return new File(new File(getFilesDir(), "library-game-data"), apk.getName() + ".zip");
    }

    private void refreshLibrary() {
        LinearLayout target = libraryList;
        if (target == null || isDestroyed()) return;
        worker.execute(() -> {
            Map<File, File> builds = readLibrary();
            runOnUiThread(() -> {
                if (!isDestroyed() && libraryList == target) renderLibrary(builds);
            });
        });
    }

    private Map<File, File> readLibrary() {
        Map<File, File> builds = new LinkedHashMap<>();
        File directory = new File(getFilesDir(), "library");
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".apk"));
        if (files == null) return builds;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        Set<String> seenPackageIds = new HashSet<>();
        for (File apk : files) {
            File gameData = gameDataForApk(apk);
            if (LibraryEntryDeduplicator.shouldKeepPackage(seenPackageIds, packageIdFromApk(apk))) {
                builds.put(apk, gameData.isFile() ? gameData : null);
            } else {
                gameData.delete();
                apk.delete();
            }
        }
        return builds;
    }

    private void renderLibrary(Map<File, File> builds) {
        libraryList.removeAllViews();
        if (builds.isEmpty()) {
            libraryList.addView(label("No builds yet. Finished games appear here automatically.", 13, MUTED, false), fullWithTop(8));
        }
        for (Map.Entry<File, File> build : builds.entrySet()) {
            File apk = build.getKey();
            File gameData = build.getValue();
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(8), dp(10), dp(8));
            row.setBackground(rounded(INPUT, 14));
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            String title = apk.getName().replaceFirst("-[0-9]+\\.apk$", "").replace('-', ' ');
            details.addView(label(title, 14, WHITE, true));
            details.addView(label(readableSize(apk.length()), 12, MUTED, false));
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            Button play = button("Play", true);
            play.setContentDescription("Play " + title);
            play.setOnClickListener(v -> playApk(apk));
            row.addView(play, new LinearLayout.LayoutParams(-2, dp(48)));
            Button actions = button("⋯", false);
            actions.setContentDescription("More actions for " + title);
            actions.setOnClickListener(v -> showLibraryActions(actions, apk, gameData));
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            actionParams.leftMargin = dp(6);
            row.addView(actions, actionParams);
            libraryList.addView(row, fullWithTop(8));
        }
    }

    private void showLibraryActions(View anchor, File apk, File gameData) {
        PopupMenu actions = new PopupMenu(this, anchor);
        actions.getMenu().add("Install / update").setOnMenuItemClickListener(item -> {
            installApk(apk);
            return true;
        });
        if (gameData != null) {
            actions.getMenu().add("Export data ZIP").setOnMenuItemClickListener(item -> {
                saveGameData(gameData);
                return true;
            });
        }
        actions.getMenu().add("Remove").setOnMenuItemClickListener(item -> {
            new AlertDialog.Builder(this).setTitle("Remove this build?")
                    .setMessage("Delete the APK and its separate game data from the library? The installed game will stay on this device.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove", (dialog, which) -> {
                        worker.execute(() -> {
                            gameDataForApk(apk).delete();
                            boolean removed = apk.delete() || !apk.exists();
                            if (!removed) runOnUiThread(() -> Toast.makeText(this,
                                    "Could not remove this build.", Toast.LENGTH_SHORT).show());
                        });
                        refreshLibrary();
                    }).show();
            return true;
        });
        actions.show();
    }

    private String packageIdFromApk(File apk) {
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= 33) {
                info = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),
                        PackageManager.PackageInfoFlags.of(0));
            } else {
                info = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
            }
            return info == null ? null : info.packageName;
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    private void refreshInstalled() {
        LinearLayout target = installedList;
        if (target == null || isDestroyed()) return;
        worker.execute(() -> {
            List<InstalledGame> games = new ArrayList<>();
            try {
                PackageManager packages = getPackageManager();
                List<ApplicationInfo> apps = Build.VERSION.SDK_INT >= 33
                        ? packages.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                        : packages.getInstalledApplications(0);
                for (ApplicationInfo app : apps) {
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0 || getPackageName().equals(app.packageName)) continue;
                    games.add(new InstalledGame(app.packageName, packages.getApplicationLabel(app).toString(),
                            packages.getLaunchIntentForPackage(app.packageName)));
                }
                games.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
            } catch (RuntimeException e) {
                runOnUiThread(() -> {
                    if (isDestroyed() || installedList != target) return;
                    target.removeAllViews();
                    target.addView(label("Could not read installed apps.", 13, MUTED, false), full());
                });
                return;
            }
            runOnUiThread(() -> {
                if (!isDestroyed() && installedList == target) renderInstalled(games);
            });
        });
    }

    private static final class InstalledGame {
        final String packageName;
        final String title;
        final Intent launch;

        InstalledGame(String packageName, String title, Intent launch) {
            this.packageName = packageName;
            this.title = title;
            this.launch = launch;
        }
    }

    private void renderInstalled(List<InstalledGame> games) {
        installedList.removeAllViews();
        if (games.isEmpty()) {
            installedList.addView(label("No user-installed apps found.", 13, MUTED, false), fullWithTop(8));
        }
        for (InstalledGame game : games) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(8), dp(10), dp(8));
            row.setBackground(rounded(INPUT, 14));
            TextView name = label(game.title, 13, WHITE, true);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            Button open = button("Play", true);
            open.setContentDescription("Play " + game.title);
            open.setEnabled(game.launch != null);
            open.setOnClickListener(v -> launchGame(game.launch));
            row.addView(open, new LinearLayout.LayoutParams(-2, dp(48)));
            Button uninstall = button("Uninstall", false);
            uninstall.setOnClickListener(v -> requestUninstall(game.packageName));
            LinearLayout.LayoutParams uninstallParams = new LinearLayout.LayoutParams(-2, dp(48));
            uninstallParams.leftMargin = dp(6);
            row.addView(uninstall, uninstallParams);
            installedList.addView(row, fullWithTop(8));
        }
    }

    private void playApk(File apk) {
        prepareGame(apk, true);
    }

    private void launchGame(Intent launch) {
        if (launch == null || isFinishing() || isDestroyed()) return;
        try {
            startActivity(launch);
        } catch (RuntimeException failure) {
            Toast.makeText(this, "Could not launch this game.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean installedBuildMatches(String packageName, File candidate) {
        try {
            ApplicationInfo installed = getPackageManager().getApplicationInfo(packageName, 0);
            File installedApk = new File(installed.sourceDir);
            if (!installedApk.isFile()) return false;
            return !InstalledBuildUpdatePolicy.shouldReplace(
                    RepairAssistantModel.sha256(installedApk), RepairAssistantModel.sha256(candidate));
        } catch (Exception unavailable) {
            return false;
        }
    }

    private void installApk(File apk) {
        prepareGame(apk, false);
    }

    private void prepareGame(File apk, boolean launchAfterInstall) {
        if (preparingGame) return;
        preparingGame = true;
        setStatus("Checking the game build…", AQUA);
        worker.execute(() -> {
            String packageName = apk == null || !apk.isFile() ? null : packageIdFromApk(apk);
            Intent launch = launchAfterInstall && packageName != null
                    ? getPackageManager().getLaunchIntentForPackage(packageName) : null;
            boolean launchInstalled = launch != null && installedBuildMatches(packageName, apk);
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                if (packageName == null) {
                    preparingGame = false;
                    setStatus("Could not read the game APK. Build the game again.", Color.rgb(255, 140, 140));
                    Toast.makeText(this, "Could not read the game APK.", Toast.LENGTH_SHORT).show();
                } else if (launchInstalled) {
                    preparingGame = false;
                    setStatus("Opening the installed game…", AQUA);
                    launchGame(launch);
                } else if (!getPackageManager().canRequestPackageInstalls()) {
                    preparingGame = false;
                    Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(settings, REQUEST_INSTALL_PERMISSION);
                    setStatus("Allow APK Forge to install apps, then tap Install again.", Color.rgb(255, 190, 120));
                } else {
                    setStatus("Preparing the APK for Android installation…", AQUA);
                    worker.execute(() -> writeInstallSession(apk, packageName, launchAfterInstall));
                }
            });
        });
    }

    private void writeInstallSession(File apk, String packageName, boolean launchAfterInstall) {
        int sessionId = -1;
        PackageInstaller.Session session = null;
        try {
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            sessionId = getPackageManager().getPackageInstaller().createSession(params);
            session = getPackageManager().getPackageInstaller().openSession(sessionId);
            try (InputStream in = new BufferedInputStream(new FileInputStream(apk)); OutputStream out = session.openWrite("base.apk", 0, apk.length())) {
                byte[] buffer = new byte[65536]; int count;
                while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
                session.fsync(out);
            }
            Intent callback = new Intent(this, InstallResultReceiver.class)
                    .setAction("com.gamebridge.mobile.INSTALL_RESULT")
                    .putExtra("apkforge.operation", "install")
                    .putExtra("apkforge.launch_after_install", launchAfterInstall)
                    .putExtra("apkforge.package", packageName);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent result = PendingIntent.getBroadcast(this, sessionId, callback, flags);
            session.commit(result.getIntentSender());
            setStatusFromWorker("Confirm installation in the Android prompt.", AQUA);
        } catch (Exception e) {
            record(DiagnosticLog.Event.APK_INSTALL_FAILED, Build.VERSION.SDK_INT, 0);
            setStatusFromWorker("Install failed: " + e.getMessage(), Color.rgb(255, 140, 140));
            if (sessionId >= 0) {
                try { getPackageManager().getPackageInstaller().abandonSession(sessionId); }
                catch (RuntimeException ignored) { }
            }
        } finally {
            try {
                if (session != null) session.close();
            } finally {
                runOnUiThread(() -> preparingGame = false);
            }
        }
    }

    private void requestUninstall(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()
                || getPackageName().equals(packageName)) {
            Toast.makeText(this, "That app cannot be uninstalled from this list.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent uninstall = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + packageName));
            uninstall.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            startActivityForResult(uninstall, REQUEST_UNINSTALL);
            setStatus("Confirm the uninstall in Android's system dialog.", AQUA);
        } catch (Exception error) {
            setStatus("Could not request uninstall. Open Android Settings → Apps to remove this game.",
                    Color.rgb(255, 140, 140));
        }
    }

    public static class InstallResultReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            String operation = intent.getStringExtra("apkforge.operation");
            String packageName = intent.getStringExtra("apkforge.package");
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirm = (Intent) intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm != null) { confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(confirm); }
            } else if (status == PackageInstaller.STATUS_SUCCESS) {
                if ("uninstall".equals(operation)) {
                    Toast.makeText(context, "Game uninstalled.", Toast.LENGTH_LONG).show();
                } else if (intent.getBooleanExtra("apkforge.launch_after_install", false)
                        && packageName != null) {
                    Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(launch);
                    } else {
                        Toast.makeText(context, "Installed, but this APK has no launch screen.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, "Game installed successfully.", Toast.LENGTH_LONG).show();
                }
            } else {
                String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                Toast.makeText(context, "Install failed: " + (message == null ? status : message), Toast.LENGTH_LONG).show();
            }
        }
    }

    private static final class BrowserGlyph extends Drawable {
        private final Glyph glyph;
        private final float density;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        BrowserGlyph(Glyph glyph, int color, float density) {
            this.glyph = glyph;
            this.density = density;
            paint.setColor(color);
            paint.setStrokeWidth(2.1f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            if (bounds.isEmpty()) return;
            canvas.save();
            float scale = Math.min(bounds.width(), bounds.height()) / 24f;
            canvas.translate(bounds.left + (bounds.width() - 24f * scale) / 2f,
                    bounds.top + (bounds.height() - 24f * scale) / 2f);
            canvas.scale(scale, scale);
            Path path = new Path();
            switch (glyph) {
                case MENU:
                    canvas.drawLine(4, 6, 20, 6, paint); canvas.drawLine(4, 12, 20, 12, paint); canvas.drawLine(4, 18, 20, 18, paint);
                    break;
                case WEB:
                    canvas.drawCircle(12, 12, 9, paint); canvas.drawOval(new RectF(8, 3, 16, 21), paint);
                    canvas.drawLine(3, 12, 21, 12, paint);
                    break;
                case BACK:
                    path.moveTo(20, 12); path.lineTo(4, 12); path.moveTo(4, 12); path.lineTo(11, 5); path.moveTo(4, 12); path.lineTo(11, 19);
                    canvas.drawPath(path, paint);
                    break;
                case REFRESH:
                    canvas.drawArc(new RectF(4, 4, 20, 20), -48, 300, false, paint);
                    path.moveTo(16, 4); path.lineTo(20, 4); path.lineTo(20, 8);
                    canvas.drawPath(path, paint);
                    break;
                case SEARCH:
                    canvas.drawCircle(10.5f, 10.5f, 6.5f, paint); canvas.drawLine(15.5f, 15.5f, 21, 21, paint);
                    break;
                case SCAN:
                    canvas.drawRoundRect(new RectF(3.5f, 2.5f, 15.5f, 21.5f), 2, 2, paint);
                    canvas.drawLine(6.5f, 7.5f, 12.5f, 7.5f, paint);
                    canvas.drawLine(6.5f, 11f, 11.5f, 11f, paint);
                    canvas.drawCircle(16.5f, 15.5f, 3.7f, paint);
                    canvas.drawLine(19.2f, 18.2f, 21.5f, 20.5f, paint);
                    break;
                case DOWNLOAD:
                    canvas.drawLine(12, 3, 12, 15, paint); path.moveTo(7, 11); path.lineTo(12, 16); path.lineTo(17, 11);
                    path.moveTo(5, 18); path.lineTo(5, 21); path.lineTo(19, 21); path.lineTo(19, 18); canvas.drawPath(path, paint);
                    break;
                case OPEN:
                    canvas.drawLine(4, 12, 19, 12, paint); path.moveTo(13, 6); path.lineTo(19, 12); path.lineTo(13, 18); canvas.drawPath(path, paint);
                    break;
                case PLUS:
                    canvas.drawLine(12, 5, 12, 19, paint); canvas.drawLine(5, 12, 19, 12, paint);
                    break;
                case MINUS:
                    canvas.drawLine(5, 12, 19, 12, paint);
                    break;
            }
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return Math.round(24 * density); }
        @Override public int getIntrinsicHeight() { return Math.round(24 * density); }
    }
}
