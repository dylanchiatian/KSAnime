package com.daose.ksanime.web;

import org.json.JSONObject;

public interface JSONListener {
    void onJSONReceived(JSONObject json);
    void onPageFailed(String error);
}
