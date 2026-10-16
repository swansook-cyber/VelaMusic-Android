package net.velalab.music;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.net.Uri;

public final class MainActivity extends Activity {
    private static final String HOME = "https://music.velalab.net/";
    private WebView web;
    private ProgressBar progress;
    private FrameLayout root;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new FrameLayout(this);
        web = new WebView(this);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, -2);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        root.addView(progress, p);
        setContentView(root);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportZoom(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.requestFocus();
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                if ("https".equals(uri.getScheme()) &&
                    "music.velalab.net".equalsIgnoreCase(uri.getHost())) return false;
                // Open external destinations outside the app rather than exposing JS bridge.
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (Exception e) { Toast.makeText(MainActivity.this, "Cannot open link", Toast.LENGTH_SHORT).show(); }
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
            @Override public void onShowCustomView(View view, CustomViewCallback cb) {
                if (fullscreenView != null) { cb.onCustomViewHidden(); return; }
                fullscreenView = view;
                fullscreenCallback = cb;
                web.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
            }
            @Override public void onHideCustomView() { closeFullscreen(); }
        });
        web.loadUrl(HOME);
    }

    private void closeFullscreen() {
        if (fullscreenView == null) return;
        root.removeView(fullscreenView);
        fullscreenView = null;
        web.setVisibility(View.VISIBLE);
        if (fullscreenCallback != null) { fullscreenCallback.onCustomViewHidden(); fullscreenCallback = null; }
    }

    // Android TV D-pad can focus web links and buttons; handling remains WebView's
    // native key navigation, so no simulated screen taps or forced selection.
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (fullscreenView != null) { closeFullscreen(); return true; }
            if (web.canGoBack()) { web.goBack(); return true; }
            return super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() {
        if (fullscreenView != null) { closeFullscreen(); return; }
        if (web.canGoBack()) { web.goBack(); return; }
        super.onBackPressed();
    }
    @Override protected void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }
    @Override protected void onDestroy() {
        if (web != null) { root.removeView(web); web.destroy(); web = null; }
        super.onDestroy();
    }
}
