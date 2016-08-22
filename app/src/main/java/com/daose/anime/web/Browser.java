package com.daose.anime.web;

import android.content.Context;
import android.os.Handler;
import android.webkit.WebView;

/**
 * Created by STUDENT on 2016-08-17.
 */
public class Browser {
    private static Browser ourInstance;
    public static final String BASE_URL = "http://kissanime.to/";
    public static final String SEARCH_URL = "http://kissanime.to/Search/Anime/";

    private WebView webView;
    private HtmlHandler htmlHandler;
    private Context ctx;

    public WebView getWebView(){
        return webView;
    }

    public static Browser getInstance(Context ctx) {
        if(ourInstance == null){
            ourInstance = new Browser(ctx);
        }
        return ourInstance;
    }

    public void reset(){
        Runnable reset = new Runnable() {
            @Override
            public void run() {
                removeListener();
                loadUrl("about:blank");
            }
        };
        new Handler(ctx.getMainLooper()).post(reset);
    }

    public void setListener(HtmlListener listener){
        htmlHandler.setListener(listener);
    }
    public void removeListener(){
        htmlHandler.removeListener();
    }

    public void loadUrl(String url){
        webView.loadUrl(url);
    }

    private Browser(Context ctx) {
        this.ctx = ctx;
        webView = new WebView(ctx);
        webView.getSettings().setJavaScriptEnabled(true);
        htmlHandler = new HtmlHandler();
        webView.setWebViewClient(new CustomWebClient());
        webView.addJavascriptInterface(htmlHandler, "HtmlHandler");
    }
}
