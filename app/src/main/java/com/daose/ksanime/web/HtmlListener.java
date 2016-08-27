package com.daose.ksanime.web;

/**
 * Created by STUDENT on 2016-08-16.
 */
public interface HtmlListener {
    void onPageLoaded(String html);
    void onPageFailed();
}
