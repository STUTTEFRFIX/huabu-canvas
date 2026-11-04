package io.github.stuttefrfix.huabu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String PAPER = "#F4EFE4";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor(PAPER));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setDefaultTextEncodingName("utf-8");
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton("确定", (dialog, which) -> result.confirm())
                        .setNegativeButton("取消", (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton("好", (dialog, which) -> result.confirm())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }
        });
        webView.addJavascriptInterface(new AndroidBridge(), "HuabuAndroid");
        webView.loadUrl("file:///android_asset/index.html");

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor(PAPER));
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(
                    0,
                    insets.getSystemWindowInsetTop(),
                    0,
                    insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(appearance, appearance);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void copyText(final String text) {
            runOnUiThread(() -> {
                ClipboardManager manager =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                manager.setPrimaryClip(ClipData.newPlainText("画布 JSON", text));
                Toast.makeText(MainActivity.this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            });
        }

        @JavascriptInterface
        public void saveText(final String filename, final String text) {
            runOnUiThread(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveToDownloads(filename, text);
                    } else {
                        saveToAppFiles(filename, text);
                    }
                } catch (Exception error) {
                    Toast.makeText(
                            MainActivity.this,
                            "保存失败：" + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void saveToDownloads(String filename, String text) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
        values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/画布");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IllegalStateException("无法创建文件");

        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) throw new IllegalStateException("无法写入文件");
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }

        ContentValues done = new ContentValues();
        done.put(MediaStore.MediaColumns.IS_PENDING, 0);
        getContentResolver().update(uri, done, null, null);
        Toast.makeText(this, "已保存到下载/画布", Toast.LENGTH_SHORT).show();
    }

    private void saveToAppFiles(String filename, String text) throws Exception {
        File dir = new File(getExternalFilesDir(null), "exports");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("无法创建目录");
        File file = new File(dir, filename);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
        Toast.makeText(this, "已保存到 " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
    }
}
