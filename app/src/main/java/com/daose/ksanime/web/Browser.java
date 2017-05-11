package com.daose.ksanime.web;

import android.content.Context;
import android.os.Handler;
import android.webkit.WebView;

public class Browser {

    private static final String TAG = Browser.class.getSimpleName();

    private static Browser ourInstance;
    public static final String IMAGE_URL = "http://myanimelist.net/anime.php?q=";

    private WebView webView;
    private CustomWebClient client;
    private HtmlHandler htmlHandler;
    private Context ctx;

    public WebView getWebView(){
        return webView;
    }

    public static Browser getInstance(Context ctx) {
        if(ourInstance == null){
            ourInstance = new Browser(ctx.getApplicationContext());
        }
        return ourInstance;
    }

    public void reset(){
        Runnable reset = new Runnable() {
            @Override
            public void run() {
                removeListeners();
                loadUrl("about:blank");
            }
        };
        new Handler(ctx.getMainLooper()).post(reset);
    }

    public void load(String url, HtmlListener listener){
        htmlHandler.addHtmlListener(listener);
        loadUrl(url);
    }

    public void load(String url, JSONListener listener) {
        htmlHandler.addJSONListener(listener);
        loadUrl(url);
    }

    public void answerCaptcha(CaptchaListener listener) {
        htmlHandler.addCaptchaListener(listener);
    }

    public void removeListeners(){
        htmlHandler.removeListeners();
    }

    public void loadUrl(String url){
        webView.loadUrl(url, client.getHeaders());
    }

    private Browser(Context ctx) {
        this.ctx = ctx;
        webView = new WebView(ctx);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true); // Required for JW Player
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1; rv:10.0) Gecko/20100101 Firefox/10.0");
        htmlHandler = new HtmlHandler();
        client = new CustomWebClient();
        webView.setWebViewClient(client);
        webView.addJavascriptInterface(htmlHandler, "HtmlHandler");
    }
}
