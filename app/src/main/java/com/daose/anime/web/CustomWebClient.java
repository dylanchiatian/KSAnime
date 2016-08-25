package com.daose.anime.web;

import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CustomWebClient extends WebViewClient {
    private static final String TAG = CustomWebClient.class.getSimpleName();

    public CustomWebClient() {
        super();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        //get past cloudflare, please wait 5 seconds
        Log.d(TAG, "shouldOverrideUrlLoading: " + url);
        return false;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Log.e(TAG, "onReceivedError: " + errorCode + " -> " + description + " \nURL: " + failingUrl);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url){
        //TODO:: block ads here: http://www.hidroh.com/2016/05/19/hacking-up-ad-blocker-android/
        Log.d(TAG, "shouldInterceptRequest: " + url);
        return null;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Log.d(TAG, "onPageFinished: " + url);

        //get post-javascript html and pass it to HtmlHandler.handleHtml()
        view.loadUrl("javascript:HtmlHandler.handleHtml(document.documentElement.outerHTML);");
    }
}
