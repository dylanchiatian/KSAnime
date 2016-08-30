package com.daose.ksanime.web;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class HtmlHandler {
    private static final String TAG = HtmlHandler.class.getSimpleName();

    private HtmlListener listener;
    private static final String BLANK_HTML = "<head></head><body></body>";

    public HtmlHandler() {
    }

    public void setListener(HtmlListener listener) {
        this.listener = listener;
    }

    public void removeListener() {
        this.listener = null;
    }

    @JavascriptInterface
    public void handleHtml(String html) {
        Log.d(TAG, "got html with listener: " + listener);
        if (listener != null) {
            if (html.equals(BLANK_HTML)) {
                listener.onPageFailed();
            } else {
                listener.onPageLoaded(html);
            }
        }
    }

    @JavascriptInterface
    public void handleError() {
        Log.e(TAG, "page failed to load");
        if(listener != null){
            listener.onPageFailed();
        }
    }

    @JavascriptInterface
    public void handleJSON(String JSONString){
        Log.d(TAG, "JSON: " + JSONString);
    }
}
