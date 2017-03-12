package com.daose.ksanime.web;

import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

public class HtmlHandler {
    private static final String TAG = HtmlHandler.class.getSimpleName();

    private HtmlListener listener;
    private JSONListener JSONListener;
    private String activeUrl = "";
    private static final String BLANK_HTML = "<head></head><body></body>";

    public HtmlHandler() {}

    public void addHtmlListener(HtmlListener listener) {
        this.listener = listener;
    }

    public void addJSONListener(JSONListener listener){
        this.JSONListener = listener;
    }

    public void removeListeners() {
        this.listener = null;
        this.JSONListener = null;
    }

    public void setActiveUrl(String url) {
        activeUrl = url;
    }

    @JavascriptInterface
    public void handleHtml(String html, String url) {
        if(!activeUrl.equals(url)) {
            // Html fetched too late
            return;
        }

        if(html.length() < 10000){
            handleError("unfinished page");
        }
        if (listener != null) {
            if (html.equals(BLANK_HTML)) {
                listener.onPageFailed();
            } else {
                listener.onPageLoaded(html);
            }
        }
    }

    @JavascriptInterface
    public void handleError(String error) {
        Log.e(TAG, "page failed to load: " + error);
        if(listener != null){
            listener.onPageFailed();
        }
        if(JSONListener != null){
            JSONListener.onPageFailed();
        }
    }

    @JavascriptInterface
    public void handleJSON(String JSONString){
        if(JSONListener != null){
            try {
                JSONListener.onJSONReceived(new JSONObject(JSONString));
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
    }
}
