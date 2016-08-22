package com.daose.anime.web;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * Created by STUDENT on 2016-08-16.
 */
public class HtmlHandler {
    private static final String LOG_TAG = "HtmlHandler";

    private HtmlListener listener;

    public HtmlHandler(){

    }

    public void setListener(HtmlListener listener){
        this.listener = listener;
    }
    public void removeListener(){
        this.listener = null;
    }

    @JavascriptInterface
    public void handleHtml(String html){
        Log.d(LOG_TAG, "got html");
        if(listener != null) {
            listener.onPageLoaded(html);
        }
    }
}
