package com.daose.anime.web;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;

public class Browser {

    private static final String TAG = Browser.class.getSimpleName();

    private static Browser ourInstance;
    public static final String BASE_URL = "http://kissanime.to/";
    public static final String SEARCH_URL = "http://kissanime.to/Search/Anime/";
    public static final String IMAGE_URL = "http://myanimelist.net/anime.php?q=";

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

    public void load(String url, HtmlListener listener){
        htmlHandler.setListener(listener);
        webView.loadUrl(url);
    }

    public void setListener(HtmlListener listener){
        htmlHandler.setListener(listener);
    }
    public void removeListener(){
        htmlHandler.removeListener();
    }

    public void loadUrl(String url){
        Log.d(TAG, "load: " + url);
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

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        }

        NetworkInfo[] info = connectivity.getAllNetworkInfo();

        // make sure that there is at least one interface to test against
        if (info != null) {
            // iterate through the interfaces
            for (int i = 0; i < info.length; i++) {
                // check this interface for a connected state
                if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        return false;
    }
}
