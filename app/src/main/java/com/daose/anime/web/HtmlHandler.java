package com.daose.anime.web;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class HtmlHandler {
    private static final String TAG = HtmlHandler.class.getSimpleName();

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
        Log.d(TAG, "got html with listener: " + listener);
        if(listener != null) {
            listener.onPageLoaded(html);
        }
    }
}
