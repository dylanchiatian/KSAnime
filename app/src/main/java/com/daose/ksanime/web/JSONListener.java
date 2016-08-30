package com.daose.ksanime.web;

import org.json.JSONObject;

public interface JSONListener {
    void onPageFailed();
    void onJSONReceived(JSONObject json);
}
