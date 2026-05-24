package com.snapbox.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private Vibrator vibrator;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Full screen / edge-to-edge ──────────────────────────
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insetsController =
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.hide(WindowInsetsCompat.Type.systemBars());
        insetsController.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);

        // ── Vibrator ────────────────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        // ── WebView settings ────────────────────────────────────
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);           // localStorage support
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        // Hardware acceleration for smooth animations
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // ── JavaScript Bridge ───────────────────────────────────
        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        // ── WebView clients ─────────────────────────────────────
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Stay within the app — don't open external URLs
                String url = request.getUrl().toString();
                if (url.startsWith("file://") || url.startsWith("about:")) {
                    return false;
                }
                return true; // block external navigation
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // ── Load the game ───────────────────────────────────────
        webView.loadUrl("file:///android_asset/index.html");
    }

    // ── Android Bridge (called from JavaScript) ─────────────────
    private class AndroidBridge {

        /**
         * Vibrate the device.
         * Call from JS: Android.vibrate(50)           // single pulse ms
         * Call from JS: Android.vibratePattern([30,10,30]) // pattern
         */
        @JavascriptInterface
        public void vibrate(int ms) {
            if (vibrator == null || ms <= 0) return;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(ms);
                }
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void vibratePattern(String pattern) {
            // pattern is JSON array string like "[30,10,30]"
            if (vibrator == null) return;
            try {
                pattern = pattern.replace("[","").replace("]","").trim();
                String[] parts = pattern.split(",");
                long[] timings = new long[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    timings[i] = Long.parseLong(parts[i].trim());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1));
                } else {
                    vibrator.vibrate(timings, -1);
                }
            } catch (Exception ignored) {}
        }

        /** Get device info */
        @JavascriptInterface
        public String getDeviceInfo() {
            return Build.MODEL + " / Android " + Build.VERSION.RELEASE;
        }
    }

    // ── Lifecycle ───────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        webView.pauseTimers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        // Let the game handle back button via JS if possible
        webView.evaluateJavascript(
            "if(window.__onBackPressed) window.__onBackPressed(); else true;",
            result -> {
                if ("true".equals(result)) {
                    super.onBackPressed();
                }
            }
        );
    }
}
