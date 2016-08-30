package com.daose.ksanime.web;

import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CustomWebClient extends WebViewClient {
    private static final String TAG = CustomWebClient.class.getSimpleName();
    private static final WebResourceResponse dud = new WebResourceResponse("text/plain", "utf-8", null);

    private static HashSet<String> ignoreUrls;
    private static Map<String, String> headers;
    private static final String[] ignoreKeys = {"/images/", ".png", ".css", ".jpeg", ".jpg", "/ads/", "disqus", "facebook"};
    private static final String javascript = "javascript:" +
            "if(document.documentElement == null || document.body.innerHTML === \"The service is unavailable.\"){" +
            "HtmlHandler.handleError(); " +
            "}else if(document.title === \"Please wait 5 seconds...\")" +
            "{}else{" +
            "HtmlHandler.handleHtml(document.documentElement.innerHTML);" +
            "}";

    //TODO:: select quality
    //select quality:
    //console.log(asp.wrap("options value"));

    public CustomWebClient() {
        super();
        ignoreUrls = new HashSet<String>();
        headers = new HashMap<String, String>();
        headers.put("X-Requested-With", "");
    }

    public Map<String, String> getHeaders(){
        return headers;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.d(TAG, "url: " + url);
        view.loadUrl(url, headers);
        return true;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Log.e(TAG, "onReceivedError: " + errorCode + " -> " + description + " \nURL: " + failingUrl);
        view.loadUrl("about:blank");
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        Log.d(TAG, "request: " + url);
        if (ignoreUrls.contains(url)) {
            return dud;
        }

        //cloudflare, pass
        if (url.contains("answer")) return null;

        if (url.contains("kissanime")) {
            for (String key : ignoreKeys) {
                if (url.contains(key)) {
                    //Log.d(TAG, "IGNORE: " + url);
                    ignoreUrls.add(url);
                    return dud;
                }
            }
            //Log.d(TAG, "PASS: " + url);
            return null;
        } else {
            ignoreUrls.add(url);
            return dud;
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        //get post-javascript html and pass it to HtmlHandler.handleHtml()
        view.loadUrl(javascript);
    }
}
