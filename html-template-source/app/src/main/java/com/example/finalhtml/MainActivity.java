package com.example.finalhtml;

import com.apkforge.input.WebGameInputMapper;
import com.gamebridge.mobile.LocalGameStateStore;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int EXPORT_SAVE_REQUEST = 41;
    private static final int IMPORT_SAVE_REQUEST = 42;
    private static final int MAX_SAVE_BACKUP_BYTES = 8 * 1024 * 1024 + 128 * 1024;
    private WebView gameView;
    private TextView saveButton;
    private volatile String allowedSaveOrigin = "";
    private String pendingSaveOrigin;
    private int horizontalDirection;
    private int verticalDirection;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        gameView = new WebView(this);
        gameView.setFocusable(true);
        gameView.setFocusableInTouchMode(true);
        gameView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        gameView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings settings = gameView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        gameView.addJavascriptInterface(new TextInputBridge(), "APKForgeTextInput");
        gameView.addJavascriptInterface(new LocalSaveBridge(), "APKForgeLocalSaves");
        gameView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                allowedSaveOrigin = normalizedOrigin(url);
                setInputToolsExpanded(false);
                installLocalSaveSync(view);
                view.evaluateJavascript("(function(){"
                        + "if(window.__apkForgeTextInputInstalled)return;"
                        + "window.__apkForgeTextInputInstalled=true;"
                        + "document.addEventListener('focusin',function(event){"
                        + "var field=event.target;if(!field)return;"
                        + "try{window.APKForgeTextInput.onFocus(String(field.tagName||''),"
                        + "String(field.type||''),!!field.isContentEditable||"
                        + "String(field.getAttribute('contenteditable')).toLowerCase()==='true');}"
                        + "catch(ignored){}},true);})()", null);
                String touchAdapter = readAsset("apkforge-touch-adapter.js");
                if (!touchAdapter.isEmpty()) view.evaluateJavascript(touchAdapter, null);
                view.postDelayed(() -> view.evaluateJavascript(
                        "!!document.querySelector('[data-apkforge-tools-toggle]')", initialized -> {
                            if (!"true".equals(initialized)) {
                                Toast.makeText(MainActivity.this,
                                        "Game input tools did not initialize. Export diagnostics from APK Forge.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }), 250);
            }
        });
        gameView.setWebChromeClient(new WebChromeClient());
        gameView.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) view.requestFocus();
            return false;
        });
        FrameLayout gameFrame = new FrameLayout(this);
        gameFrame.addView(gameView, new FrameLayout.LayoutParams(-1, -1));
        saveButton = new TextView(this);
        saveButton.setText("Save");
        saveButton.setTextSize(14);
        saveButton.setTextColor(Color.WHITE);
        saveButton.setGravity(Gravity.CENTER);
        saveButton.setPadding(dp(18), 0, dp(18), 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(20, 43, 48));
        background.setCornerRadius(dp(20));
        background.setStroke(dp(1), Color.rgb(20, 198, 164));
        saveButton.setBackground(background);
        saveButton.setElevation(dp(8));
        saveButton.setContentDescription("Save game progress locally");
        saveButton.setOnClickListener(v -> showLocalSaveMenu());
        FrameLayout.LayoutParams saveParams = new FrameLayout.LayoutParams(-2, dp(48), Gravity.RIGHT | Gravity.BOTTOM);
        saveParams.rightMargin = dp(14);
        saveParams.bottomMargin = dp(144);
        gameFrame.addView(saveButton, saveParams);
        setContentView(gameFrame);
        gameView.requestFocus(View.FOCUS_DOWN);
        gameView.loadUrl("file:///android_asset/www/index.html");
    }

    private void installLocalSaveSync(WebView view) {
        String script = "(function(){try{"
                + "var origin=location.origin==='null'?'file://offline':location.origin;"
                + "var read=function(){var o={};for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);o[k]=localStorage.getItem(k)}return JSON.stringify(o)};"
                + "var saved=window.APKForgeLocalSaves.loadState(origin);"
                + "if(saved&&localStorage.length===0&&!sessionStorage.getItem('__apkforge_save_restored')){"
                + "var values=JSON.parse(saved);Object.keys(values).forEach(function(k){localStorage.setItem(k,String(values[k]))});"
                + "sessionStorage.setItem('__apkforge_save_restored','1');location.reload();return;}"
                + "var lastCheckpoint=null;var push=function(){var state=read();if(state==='{}'&&lastCheckpoint&&lastCheckpoint!=='{}')return;if(state!==lastCheckpoint){lastCheckpoint=state;window.APKForgeLocalSaves.saveState(origin,state)}};"
                + "window.__apkForgeSaveNow=push;window.__apkForgeSaveSnapshot=read;window.addEventListener('pagehide',push);"
                + "if(!window.__apkForgeSaveTimer)window.__apkForgeSaveTimer=setInterval(push,2000);push();"
                + "}catch(e){}})();";
        view.evaluateJavascript(script, null);
    }

    private void showLocalSaveMenu() {
        new AlertDialog.Builder(this).setTitle("Game saves · this device")
                .setItems(new String[]{"Save progress now", "Export save backup…", "Import save backup…", "Restore last local save", "About local saves"}, (dialog, which) -> {
                    if (which == 0) {
                        checkpointNow();
                    } else if (which == 1) {
                        exportSaveBackup();
                    } else if (which == 2) {
                        startImportSaveBackup();
                    } else if (which == 3) {
                        restoreLocalSave();
                    } else {
                        new AlertDialog.Builder(this).setTitle("Where progress is saved")
                                .setMessage("HTML localStorage is checkpointed privately on this device and restored when the game opens. Export a save backup before uninstalling or moving the game to another device. Installing an update over the same app keeps its private files when Android accepts the same package and signing key. The game must use localStorage; website-account saves stay with the website, and IndexedDB/cookies are not included.")
                                .setPositiveButton("OK", null).show();
                    }
                }).show();
    }

    private void checkpointNow() {
        if (gameView == null || allowedSaveOrigin.isEmpty()) return;
        String script = "(function(){try{var o={};for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);o[k]=localStorage.getItem(k)}"
                + "var s=JSON.stringify(o);window.APKForgeLocalSaves.saveState(location.origin==='null'?'file://offline':location.origin,s);return 'ok';}catch(e){return 'error';}})()";
        gameView.evaluateJavascript(script, result -> runOnUiThread(() -> Toast.makeText(this,
                "\"ok\"".equals(result) ? "Game progress saved on this device." : "Could not save this page’s localStorage progress.",
                Toast.LENGTH_SHORT).show()));
    }

    private void exportSaveBackup() {
        WebView view = gameView;
        String origin = allowedSaveOrigin;
        if (view == null || origin.isEmpty()) {
            Toast.makeText(this, "Wait for the game page to finish loading first.", Toast.LENGTH_LONG).show();
            return;
        }
        String script = "(function(){try{var o={};for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);o[k]=localStorage.getItem(k)}"
                + "var s=JSON.stringify(o);window.APKForgeLocalSaves.saveState(" + JSONObject.quote(origin) + ",s);return s;}catch(e){return '';}})()";
        view.evaluateJavascript(script, encoded -> {
            try {
                Object decoded = new org.json.JSONTokener(encoded == null ? "null" : encoded).nextValue();
                if (!(decoded instanceof String) || ((String) decoded).isEmpty()) throw new IOException("No localStorage save could be read from this game.");
                LocalGameStateStore.save(getFilesDir(), origin, (String) decoded);
                pendingSaveOrigin = origin;
                String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, "APKForge-game-save-" + timestamp + ".apkforge-save");
                startActivityForResult(intent, EXPORT_SAVE_REQUEST);
            } catch (Exception failure) {
                runOnUiThread(() -> Toast.makeText(this, "Save export failed: " + safeError(failure), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void startImportSaveBackup() {
        if (allowedSaveOrigin.isEmpty()) {
            Toast.makeText(this, "Wait for the game page to finish loading first.", Toast.LENGTH_LONG).show();
            return;
        }
        pendingSaveOrigin = allowedSaveOrigin;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        startActivityForResult(intent, IMPORT_SAVE_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != EXPORT_SAVE_REQUEST && requestCode != IMPORT_SAVE_REQUEST) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            pendingSaveOrigin = null;
            return;
        }
        Uri selected = data.getData();
        String origin = pendingSaveOrigin;
        pendingSaveOrigin = null;
        if (origin == null || origin.isEmpty() || !origin.equals(allowedSaveOrigin)) {
            Toast.makeText(this, "The game page changed; reopen its save menu and try again.", Toast.LENGTH_LONG).show();
            return;
        }
        if (requestCode == EXPORT_SAVE_REQUEST) {
            try (OutputStream output = getContentResolver().openOutputStream(selected, "w")) {
                if (output == null) throw new IOException("The selected location could not be opened.");
                output.write(LocalGameStateStore.exportBackup(getFilesDir(), origin));
                output.flush();
                Toast.makeText(this, "Save backup exported. Keep it somewhere safe.", Toast.LENGTH_LONG).show();
            } catch (Exception failure) {
                Toast.makeText(this, "Save export failed: " + safeError(failure), Toast.LENGTH_LONG).show();
            }
            return;
        }
        try (InputStream input = getContentResolver().openInputStream(selected)) {
            if (input == null) throw new IOException("The selected backup could not be opened.");
            LocalGameStateStore.restoreBackup(getFilesDir(), origin, readBounded(input, MAX_SAVE_BACKUP_BYTES));
            restoreLocalSave();
            Toast.makeText(this, "Save backup restored for this game page.", Toast.LENGTH_LONG).show();
        } catch (Exception failure) {
            Toast.makeText(this, "Save import failed: " + safeError(failure), Toast.LENGTH_LONG).show();
        }
    }

    private void restoreLocalSave() {
        if (gameView == null || allowedSaveOrigin.isEmpty()) return;
        String script = "(function(){try{var o=location.origin==='null'?'file://offline':location.origin;"
                + "var s=window.APKForgeLocalSaves.loadState(o);if(!s){window.APKForgeLocalSaves.report('No local save exists for this page yet.');return;}"
                + "var v=JSON.parse(s);localStorage.clear();Object.keys(v).forEach(function(k){localStorage.setItem(k,String(v[k]))});location.reload();"
                + "}catch(e){window.APKForgeLocalSaves.report('Could not restore this page’s local save.');}})()";
        gameView.evaluateJavascript(script, null);
    }

    private byte[] readBounded(InputStream input, int maximum) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (count > maximum - output.size()) throw new IOException("The save backup is larger than the supported limit.");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private String safeError(Exception failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty() ? "check the selected save file and try again" : message;
    }

    private void setInputToolsExpanded(boolean expanded) {
        if (saveButton != null) saveButton.setVisibility(expanded ? View.GONE : View.VISIBLE);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String normalizedOrigin(String url) {
        Uri uri = Uri.parse(url == null ? "" : url);
        String scheme = uri.getScheme();
        if (scheme == null) return "";
        scheme = scheme.toLowerCase(java.util.Locale.ROOT);
        if ("file".equals(scheme)) return "file://offline";
        if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null) return "";
        return scheme + "://" + uri.getAuthority().toLowerCase(java.util.Locale.ROOT);
    }

    private final class LocalSaveBridge {
        @JavascriptInterface public String loadState(String origin) {
            try {
                if (!normalizedOrigin(origin).equals(allowedSaveOrigin) || allowedSaveOrigin.isEmpty()) return "";
                String state = LocalGameStateStore.load(getFilesDir(), origin);
                return state == null ? "" : state;
            } catch (Exception ignored) { return ""; }
        }

        @JavascriptInterface public void saveState(String origin, String json) {
            try {
                if (!normalizedOrigin(origin).equals(allowedSaveOrigin) || allowedSaveOrigin.isEmpty()) return;
                LocalGameStateStore.save(getFilesDir(), origin, json);
            } catch (Exception failure) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, failure.getMessage(), Toast.LENGTH_LONG).show());
            }
        }

        @JavascriptInterface public void report(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
        }

        @JavascriptInterface public void onToolsExpanded(boolean expanded) {
            runOnUiThread(() -> setInputToolsExpanded(expanded));
        }
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (gameView != null) {
            gameView.requestLayout();
            gameView.invalidate();
        }
    }

    @Override protected void onPause() {
        if (gameView != null) gameView.evaluateJavascript("if(window.__apkForgeSaveNow)window.__apkForgeSaveNow()", null);
        super.onPause();
    }

    private final class TextInputBridge {
        @JavascriptInterface public void onFocus(String tagName, String inputType, boolean contentEditable) {
            if (!com.gamebridge.mobile.HtmlKeyboardSupport.shouldShowKeyboard(tagName, inputType, contentEditable)) return;
            runOnUiThread(() -> {
                WebView view = gameView;
                if (view == null) return;
                view.requestFocus();
                view.post(() -> {
                    android.view.inputmethod.InputMethodManager inputMethodManager =
                            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null && gameView == view) {
                        inputMethodManager.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            });
        }
    }

    private String readAsset(String name) {
        try (InputStream input = getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException unavailable) {
            return "";
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_UP) {
            boolean controller = event.isFromSource(InputDevice.SOURCE_GAMEPAD)
                    || event.isFromSource(InputDevice.SOURCE_DPAD);
            if (controller) {
                WebGameInputMapper.Key mapped = WebGameInputMapper.fromAndroidKeyCode(event.getKeyCode());
                if (mapped != null && gameView != null) {
                    dispatchGameKey(mapped, event.getAction() == KeyEvent.ACTION_UP, event.getRepeatCount() > 0);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE && event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
            float x = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            float y = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
            if (Math.abs(x) < 0.15f) x = event.getAxisValue(MotionEvent.AXIS_X);
            if (Math.abs(y) < 0.15f) y = event.getAxisValue(MotionEvent.AXIS_Y);
            boolean changedX = updateAxis(x, true);
            boolean changedY = updateAxis(y, false);
            if (changedX || changedY) return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private boolean updateAxis(float value, boolean horizontal) {
        int next = value <= -0.55f ? -1 : value >= 0.55f ? 1 : 0;
        int previous = horizontal ? horizontalDirection : verticalDirection;
        if (next == previous) return false;
        if (horizontal) horizontalDirection = next;
        else verticalDirection = next;
        if (previous != 0) dispatchAxisKey(horizontal, previous, true);
        if (next != 0) dispatchAxisKey(horizontal, next, false);
        return true;
    }

    private void dispatchAxisKey(boolean horizontal, int direction, boolean up) {
        int keyCode = horizontal ? (direction < 0 ? 21 : 22) : (direction < 0 ? 19 : 20);
        WebGameInputMapper.Key mapped = WebGameInputMapper.fromAndroidKeyCode(keyCode);
        if (mapped != null) dispatchGameKey(mapped, up, false);
    }

    private void dispatchGameKey(WebGameInputMapper.Key key, boolean up, boolean repeat) {
        if (gameView == null) return;
        String type = up ? "keyup" : "keydown";
        String script = "(function(){var t=document;"
                + "t.dispatchEvent(new KeyboardEvent(" + JSONObject.quote(type) + ",{" 
                + "key:" + JSONObject.quote(key.key) + ",code:" + JSONObject.quote(key.code)
                + ",keyCode:" + key.keyCode + ",which:" + key.keyCode
                + ",bubbles:true,cancelable:true,repeat:" + repeat + "}));})()";
        gameView.evaluateJavascript(script, null);
    }

    @Override protected void onDestroy() {
        if (gameView != null) {
            gameView.setOnTouchListener(null);
            gameView.destroy();
            gameView = null;
        }
        super.onDestroy();
    }
}
