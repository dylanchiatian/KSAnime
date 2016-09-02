package com.daose.ksanime.util;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.daose.ksanime.model.Anime;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.Selector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

import io.realm.Realm;

public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public static String PREFS_FILE = "daose";
    public static String FIRST_DOWNLOAD_KEY = "first_download";
    public static String SELECT_QUALITY_KEY = "quality";
    public static String DEFAULT_QUALITY = "720p";

    public static String MARKET_DEEPLINK = "market://details?id=com.daose.ksanime";
    public static String MARKET_URL = "https://play.google.com/store/apps/details?id=com.daose.ksanime";

    public static String URL_KEY = "url";

    public static String USER_AGENT = "Mozilla/5.0";


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

    public Utils() {
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

    //TODO:: blocking the UI thread ?!?!
    public static class GetCoverURL extends AsyncTask<String, Void, String> {

        private String title;

        @Override
        public String doInBackground(String... titles) {
            this.title = titles[0];
            Log.d(TAG, "Start: " + title);
            try {
                final StringBuilder URLBuilder = new StringBuilder();
                final Document doc = Jsoup.connect(Browser.IMAGE_URL + title).userAgent(Utils.USER_AGENT).get();
                Log.d(TAG, "Connected: " + title);
                Uri rawUrl = Uri.parse(doc.select(Selector.MAL_IMAGE).first().attr(Selector.MAL_IMAGE_ATTR));
                URLBuilder.append(rawUrl.getScheme()).append("://").append(rawUrl.getHost());
                List<String> pathSegments = rawUrl.getPathSegments();
                if (rawUrl.getPathSegments().size() < 3) {
                    return "";
                } else {
                    for (int i = 2; i < pathSegments.size(); i++) {
                        URLBuilder.append("/");
                        URLBuilder.append(pathSegments.get(i));
                    }
                }
                return URLBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "End: " + title);
            return "";
        }

        @Override
        public void onPostExecute(final String URL) {
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Anime anime = realm.where(Anime.class).equalTo("title", title).findFirst();
                    anime.coverURL = URL;
                }
            });
            realm.close();
        }
    }
}
