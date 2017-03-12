package com.daose.ksanime.web;

public interface HtmlListener {
    void onPageLoaded(String html);
    void onPageFailed();
}
