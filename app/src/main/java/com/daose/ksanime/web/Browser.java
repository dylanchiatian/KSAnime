package com.daose.ksanime.web;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.webkit.WebView;

public class Browser {

    private static final String TAG = Browser.class.getSimpleName();

    private static Browser ourInstance;
    public static final String BASE_URL = "http://kissanime.ru/";
    public static final String SEARCH_URL = BASE_URL + "Search/Anime/";
    public static final String IMAGE_URL = "http://myanimelist.net/anime.php?q=";
    public static final String MOST_POPULAR = "AnimeList/MostPopular";
    public static final String NEW_AND_HOT = "AnimeList/NewAndHot";

    private WebView webView;
    private CustomWebClient client;
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
                removeListeners();
                loadUrl("about:blank");
            }
        };
        new Handler(ctx.getMainLooper()).post(reset);
    }

    public void load(String url, HtmlListener listener){
        htmlHandler.addHtmlListener(listener);
        htmlHandler.setActiveUrl(url);
        loadUrl(url);
    }

    public void load(String url, JSONListener listener) {
        htmlHandler.addJSONListener(listener);
        htmlHandler.setActiveUrl(url);
        loadUrl(url);
    }

    public void addHtmlListener(HtmlListener listener){
        htmlHandler.addHtmlListener(listener);
    }

    public void addJSONListener(JSONListener listener){
        htmlHandler.addJSONListener(listener);
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
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1; rv:10.0) Gecko/20100101 Firefox/10.0");
        htmlHandler = new HtmlHandler();
        client = new CustomWebClient();
        webView.setWebViewClient(client);
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
