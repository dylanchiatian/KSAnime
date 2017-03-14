package com.daose.ksanime.web;

import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CustomWebClient extends WebViewClient {
    private static final String TAG = CustomWebClient.class.getSimpleName();

    private static HashSet<String> ignoreUrls;
    private static Map<String, String> headers;
    private static final String[] ignoreKeys = {"/images/", ".png", ".css", ".jpeg", ".jpg", "/ads/", "disqus", "facebook", "favicon"};
    private static final String javascript = "if(document.documentElement===null){HtmlHandler.handleError('null document')}else if(document.title!=='Please wait 5 seconds...'){if(document.documentElement.innerHTML.length<150){HtmlHandler.handleError(document.documentElement.innerHTML)}else if(document.documentElement.innerHTML.length>10000){if(document.getElementById('selectQuality')!==null){var qualities=document.getElementById('selectQuality').options;var dictionary={};for(var i=0;i<qualities.length;i+=1){dictionary[qualities[i].text]=asp.wrap(qualities[i].value)}HtmlHandler.handleJSON(JSON.stringify(dictionary))}else if(window.location.href===currentUrl){HtmlHandler.handleHtml(document.documentElement.innerHTML,window.location.href)}}}";

    public CustomWebClient() {
        super();
        ignoreUrls = new HashSet<String>();
        ignoreUrls.add("about:blank");
        headers = new HashMap<String, String>();
        headers.put("X-Requested-With", "");
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url, headers);
        return true;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Log.e(TAG, "onReceivedError: " + errorCode + " -> " + description + " \nURL: " + failingUrl);
        view.loadUrl("about:blank");
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (ignoreUrls.contains(url)) {
            //Log.d(TAG, "FAIL: " + url);
            return new WebResourceResponse(null, null, null);
        }

        //cloudflare, pass
        if (url.contains("answer")) return null;

        if (url.contains("kissanime") || url.contains("video")) {
            for (String key : ignoreKeys) {
                if (url.contains(key)) {
                    ignoreUrls.add(url);
                    //Log.d(TAG, "FAIL: " + url);
                    return new WebResourceResponse(null, null, null);
                }
            }
            //Log.d(TAG, "PASS: " + url);
            return null;
        } else {
            ignoreUrls.add(url);
            //Log.d(TAG, "FAIL: " + url);
            return new WebResourceResponse(null, null, null);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if(!ignoreUrls.contains(url)) {
            view.loadUrl(injectScript(url));
        }
    }

    private String injectScript(final String currentUrl) {
        return String.format("javascript:var currentUrl=encodeURI('%s');%s", currentUrl, javascript);
    }
}
