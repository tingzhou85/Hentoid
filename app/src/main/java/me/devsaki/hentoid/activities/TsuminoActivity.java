package me.devsaki.hentoid.activities;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import java.net.MalformedURLException;
import java.net.URL;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.abstracts.BaseWebActivity;
import me.devsaki.hentoid.database.enums.Site;
import me.devsaki.hentoid.parser.TsuminoParser;
import me.devsaki.hentoid.util.Helper;

/**
 * Created by Shiro on 1/22/2016.
 * Implements tsumino source
 */
public class TsuminoActivity extends BaseWebActivity {

    private static final String TAG = TsuminoActivity.class.getName();
    private boolean downloadFabPressed = false;
    private int historyIndex;

    @SuppressLint("AddJavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setSite(Site.TSUMINO);
        super.onCreate(savedInstanceState);

        webView.setWebViewClient(new TsuminoWebViewClient());
        webView.addJavascriptInterface(new PageLoadListener(), "HTMLOUT");
    }

    @Override
    public void onDownloadFabClick(View view) {
        downloadFabPressed = true;
        historyIndex = webView.copyBackForwardList().getCurrentIndex();

        String newUrl = webView.getUrl().replace("Book/Info", "Read/View");
        final int index = Helper.ordinalIndexOf(newUrl, '/', 5);
        if (index > 0) newUrl = newUrl.substring(0, index);
        webView.loadUrl(newUrl);
    }

    private class TsuminoWebViewClient extends CustomWebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                URL u = new URL(url);
                return !(u.getHost().endsWith("tsumino.com"));
            } catch (MalformedURLException e) {
                Log.d(TAG, "Malformed URL");
            }
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            if (downloadFabPressed
                    && !(url.contains("//www.tsumino.com/Read/View/")
                    || url.contains("//www.tsumino.com/Read/Auth/")
                    || url.contains("//www.tsumino.com/Read/AuthProcess"))) {
                downloadFabPressed = false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            String js = getResources().getString(R.string.grab_html_from_webview);

            if (url.contains("//www.tsumino.com/Book/Info/")) {
                // following calls PageLoadListener.processHTML(*)
                // Conditional fixes issue with loadUrl("javascript:") on Android 4.4+
                if (Build.VERSION.SDK_INT >= 19) {
                    view.evaluateJavascript(js, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            // Ignored - our js returns null
                        }
                    });
                } else {
                    view.loadUrl(js);
                }
            } else if (url.contains("//www.tsumino.com/Read/View/") && downloadFabPressed) {
                downloadFabPressed = false;

                int currentIndex = webView.copyBackForwardList().getCurrentIndex();
                webView.goBackOrForward(historyIndex - currentIndex);
                processDownload();
            }
        }
    }

    private class PageLoadListener {
        @JavascriptInterface
        public void processHTML(String html) {
            if (html == null) {
                return;
            }
            processContent(TsuminoParser.parseContent(html));
        }
    }
}
