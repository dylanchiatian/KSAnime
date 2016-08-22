package com.daose.anime.web;

import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by STUDENT on 2016-08-16.
 */
public class CustomWebClient extends WebViewClient {
    private static final String LOG_TAG = "CustomWebClient";

    public CustomWebClient(){
        super();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url){
        //get past cloudflare, please wait 5 seconds
        return false;
    }

    @Override
    public void onPageFinished(WebView view, String url){
        super.onPageFinished(view, url);
        Log.d(LOG_TAG, "onPageFinished: " + url);
        //get full html
        view.loadUrl("javascript:HtmlHandler.handleHtml(document.documentElement.outerHTML);");
    }
}
