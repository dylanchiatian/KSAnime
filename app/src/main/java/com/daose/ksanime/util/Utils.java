package com.daose.ksanime.util;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AlertDialog;

import com.daose.ksanime.model.Anime;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.Selector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.List;

import io.realm.Realm;

import static android.content.Context.MODE_PRIVATE;

public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public static final String PREFS_FILE = "daose";
    public static final String FIRST_DOWNLOAD_KEY = "first_download";
    public static final String SELECT_QUALITY_KEY = "quality";
    public static final String DEFAULT_QUALITY = "720p";

    public static final String MARKET_DEEPLINK = "market://details?id=com.daose.ksanime";
    public static final String MARKET_URL = "https://play.google.com/store/apps/details?id=com.daose.ksanime";

    public static final String URL_KEY = "url";
    public static final String ANIME_KEY = "title";

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0";

    public Utils() {}

    public static boolean containsIgnoreCase(String src, String what) {
        final int length = what.length();
        if (length == 0)
            return true; // Empty string is contained

        final char firstLo = Character.toLowerCase(what.charAt(0));
        final char firstUp = Character.toUpperCase(what.charAt(0));

        for (int i = src.length() - length; i >= 0; i--) {
            // Quick check before calling the more expensive regionMatches() method:
            final char ch = src.charAt(i);
            if (ch != firstLo && ch != firstUp)
                continue;

            if (src.regionMatches(true, i, what, 0, length))
                return true;
        }

        return false;
    }

    public static void showOneTimeDialog(String key, AlertDialog dialog) {
        SharedPreferences settings = dialog.getContext().getSharedPreferences("daose", MODE_PRIVATE);
        if(!settings.getBoolean(key, false)) {
            dialog.show();
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(key, true);
            editor.apply();
        }
    }

    public static void dismissDialog(Dialog dialog) {
        if(dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    public static class CycleInterpolator implements android.view.animation.Interpolator {
        private final float mCycles = 0.5f;

        @Override
        public float getInterpolation(final float input) {
            return (float) Math.sin(2.0f * mCycles * Math.PI * input);
        }
    }

    public static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static boolean isNetworkAvailable(Context ctx) {
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
