package com.daose.ksanime.web;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.daose.ksanime.api.ka.CaptchaActivity;
import com.daose.ksanime.api.ka.KA;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CustomWebClient extends WebViewClient {
    private static final String TAG = CustomWebClient.class.getSimpleName();

    private static HashSet<String> ignoreUrls;
    private static Map<String, String> headers;
    private static final String[] ignoreKeys = {"/images/", ".png", ".css", ".jpeg", ".jpg", "/ads/", "disqus", "facebook", "video.js"};
    private static final String[] whiteKeys = {
            "answer", // cloudflare
            "openload",
            "rapidvideo",
            "sweetcaptcha",
            "jwpcdn" // JW Player required for rapidvideo
    };
    private static final String javascript = "if(document.documentElement===null){HtmlHandler.handleError('null document')}else if(document.title!=='Please wait 5 seconds...'){if(document.documentElement.innerHTML.length<150){}else if(typeof playerInstance!=='undefined'){var rapidVideo=playerInstance.getConfig().sources[playerInstance.getCurrentQuality()].file;var link={RapidVideo:rapidVideo};HtmlHandler.handleJSON(JSON.stringify(link))}else if(document.documentElement.innerHTML.length>10000){if(~window.location.href.indexOf('AreYouHuman')){HtmlHandler.handleError('captcha')}else if(document.getElementById('slcQualix')!==null){var qualities=document.getElementById('slcQualix').options;var dictionary={};for(var i=0;i<qualities.length;i+=1){dictionary[qualities[i].text]=ovelWrap(qualities[i].value)}HtmlHandler.handleJSON(JSON.stringify(dictionary))}else if(document.getElementById('selectServer')!==null){var serverSelector=document.getElementById('selectServer');var server=serverSelector.options[serverSelector.selectedIndex].text;if(server==='Openload'||server==='RapidVideo'){var redirect=document.getElementById('divContentVideo').getElementsByTagName('iframe')[0].src;window.location=redirect}else{HtmlHandler.handleError('No server available')}}else if(document.getElementById('streamurl')!==null){var id=document.getElementById('streamurl').innerHTML;var link={Openload:('https://openload.co/stream/'+id+'?mime=true')};HtmlHandler.handleJSON(JSON.stringify(link))}else if(window.location.href===currentUrl){HtmlHandler.handleHtml(document.documentElement.innerHTML,window.location.href)}}else{}}";

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
        //Log.d(TAG, "PASS?: " + url);
        if (ignoreUrls.contains(url)) {
            //Log.d(TAG, "FAIL: " + url);
            return new WebResourceResponse(null, null, null);
        }

        for (String key : whiteKeys) {
            if (url.contains(key)) {
                return null;
            }
        }

        if (url.contains("kissanime") || url.contains("video")) {
            for (String key : ignoreKeys) {
                if (url.contains(key)) {
                    ignoreUrls.add(url);
                    //Log.d(TAG, "FAIL: " + url);
                    return new WebResourceResponse(null, null, null);
                }
            }
            return null;
        } else {
            ignoreUrls.add(url);
            //Log.d(TAG, "FAIL: " + url);
            return new WebResourceResponse(null, null, null);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if(url.equals(KA.CAPTCHA_ENDPOINT)) {
            view.loadUrl("javascript:HtmlHandler.finish()");
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
