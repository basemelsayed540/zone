package com.zone.finder;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQ_FILE = 1;

    private WebView web;
    private ValueCallback<Uri[]> filePathCallback;
    private String appUrl;

    // Turns blob: downloads (Excel export, backup) into a call to the native side.
    private static final String HOOK_JS =
        "(function(){if(window.__zb)return;window.__zb=1;"
        + "function h(a){try{if(a&&a.tagName==='A'&&a.hasAttribute('download')&&/^blob:/.test(a.href)){"
        + "fetch(a.href).then(function(r){return r.blob()}).then(function(b){var f=new FileReader();"
        + "f.onload=function(){var s=String(f.result);ZoneApp.save(a.getAttribute('download')||'file',"
        + "b.type||'application/octet-stream',s.slice(s.indexOf(',')+1))};f.readAsDataURL(b)});return true}}catch(e){}return false}"
        + "var c=HTMLAnchorElement.prototype.click;"
        + "HTMLAnchorElement.prototype.click=function(){if(!h(this))c.apply(this,arguments)};"
        + "var d=EventTarget.prototype.dispatchEvent;"
        + "EventTarget.prototype.dispatchEvent=function(ev){if(this instanceof HTMLAnchorElement&&ev&&ev.type==='click'&&h(this))return true;return d.apply(this,arguments)};"
        + "})();";

    private static final String OFFLINE_HTML =
        "<html dir='rtl'><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
        + "<body style='font-family:sans-serif;text-align:center;padding:40px 20px'>"
        + "<h2>لا يوجد اتصال بالإنترنت</h2><p>اتأكد من النت وجرّب تاني.</p>"
        + "<button style='font-size:18px;padding:10px 30px' onclick=\"location.href='%s'\">إعادة المحاولة</button>"
        + "</body></html>";

    public class Bridge {
        @JavascriptInterface
        public void save(String name, String mime, String b64) {
            try {
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_");
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, safe);
                v.put(MediaStore.Downloads.MIME_TYPE, mime);
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new Exception("insert failed");
                try (OutputStream o = getContentResolver().openOutputStream(uri)) {
                    o.write(data);
                }
                toast("اتحفظ في التنزيلات: " + safe);
            } catch (Exception e) {
                toast("فشل حفظ الملف");
            }
        }
    }

    private void toast(final String m) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appUrl = getString(R.string.app_url);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.addJavascriptInterface(new Bridge(), "ZoneApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView v, String url, Bitmap icon) {
                v.evaluateJavascript(HOOK_JS, null);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                v.evaluateJavascript(HOOK_JS, null);
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    v.loadDataWithBaseURL(appUrl, String.format(OFFLINE_HTML, appUrl), "text/html", "utf-8", null);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith(appUrl) || u.startsWith("blob:") || u.startsWith("about:")) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl()));
                } catch (Exception ignored) {
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                try {
                    startActivityForResult(p.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(appUrl);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE && filePathCallback != null) {
            filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            filePathCallback = null;
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
