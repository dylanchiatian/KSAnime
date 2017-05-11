package com.daose.ksanime.web;

public interface HtmlListener {
    void onPageLoaded(String html, String url);
    void onPageFailed(String error);
}
