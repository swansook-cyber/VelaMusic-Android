package net.velalab.music;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.app.UiModeManager;
import android.content.Context;
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
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebResourceError;
import android.webkit.WebResourceResponse;

public final class MainActivity extends Activity {
    private static final String HOME = "https://music.velalab.net/";
    private WebView web;
    private boolean isTelevision;
    private ProgressBar progress;
    private FrameLayout root;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        UiModeManager uiMode = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        isTelevision = uiMode != null && uiMode.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
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
        // Respect the page viewport on phones while allowing responsive desktop/tablet layouts.
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
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
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.getUrl() != null && request.getUrl().getPath() != null && request.getUrl().getPath().matches("(?i).*(\\.mp3|\\.m4a|\\.aac|\\.ogg|\\.wav|/stream/|/audio/).*")) {
                    Log.w("VelaMusicMedia", "Audio request failed: " + error.getErrorCode() + " " + error.getDescription());
                }
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request != null && request.getUrl() != null && request.getUrl().getPath() != null && request.getUrl().getPath().matches("(?i).*(\\.mp3|\\.m4a|\\.aac|\\.ogg|\\.wav|/stream/|/audio/).*")) {
                    Log.w("VelaMusicMedia", "Audio HTTP error: " + response.getStatusCode());
                }
            }
            @Override public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                if (isTelevision && url != null && url.startsWith(HOME)) {
                    installTvRemoteSupport(view);
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage message) {
                if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    Log.w("VelaMusicMedia", "Web console error: " + message.message());
                }
                return false;
            }
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

    // Scoped to Android TV only. No page content, login state or playback APIs are changed.
    // This is an enhancement for web controls that do not expose native D-pad focus.
    private void installTvRemoteSupport(WebView view) {
        final String script = "(function(){"
            + "if(window.__velaTvRemoteInstalled)return;"
            + "window.__velaTvRemoteInstalled=true;"
            + "var style=document.createElement('style');"
            + "style.textContent='a:focus,button:focus,input:focus,select:focus,textarea:focus,[role=button]:focus,[tabindex]:focus{outline:4px solid #ffb000!important;outline-offset:4px!important;box-shadow:0 0 0 5px rgba(0,0,0,.55)!important;scroll-margin:80px!important}';"
            + "(document.head||document.documentElement).appendChild(style);"
            + "function eligible(el){if(!el||!el.getBoundingClientRect)return false;"
            + "var r=el.getBoundingClientRect(),s=getComputedStyle(el);"
            + "return r.width>0&&r.height>0&&s.visibility!=='hidden'&&s.display!=='none'&&!el.disabled&&el.getAttribute('aria-disabled')!=='true'}"
            + "function targets(){return Array.from(document.querySelectorAll('a[href],button,input,select,textarea,[role=button],[tabindex]:not([tabindex=-1])')).filter(eligible)}"
            + "function edit(el){return el&&(el.isContentEditable||/^(INPUT|TEXTAREA|SELECT)$/.test(el.tagName))}"
            + "document.addEventListener('keydown',function(e){"
            + "if(e.defaultPrevented||!/^Arrow(Up|Down|Left|Right)$/.test(e.key)||e.altKey||e.ctrlKey||e.metaKey||edit(document.activeElement))return;"
            + "var list=targets();if(!list.length)return;"
            + "var a=document.activeElement,base=a&&eligible(a)?a.getBoundingClientRect():null;"
            + "if(!base){list[0].focus();e.preventDefault();return}"
            + "var cx=base.left+base.width/2,cy=base.top+base.height/2,best=null,score=Infinity;"
            + "list.forEach(function(n){if(n===a)return;var r=n.getBoundingClientRect(),dx=r.left+r.width/2-cx,dy=r.top+r.height/2-cy;"
            + "var primary=e.key==='ArrowRight'?dx:e.key==='ArrowLeft'?-dx:e.key==='ArrowDown'?dy:-dy;"
            + "if(primary<=2)return;var cross=/Left|Right/.test(e.key)?Math.abs(dy):Math.abs(dx);"
            + "var v=primary+cross*2.5;if(v<score){best=n;score=v}});"
            + "if(best){best.focus();best.scrollIntoView({block:'nearest',inline:'nearest'});e.preventDefault()}"
            + "},false);"
            + "})();";
        view.evaluateJavascript(script, null);
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
