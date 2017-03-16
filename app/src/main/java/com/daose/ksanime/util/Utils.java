package com.daose.ksanime.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import com.daose.ksanime.model.Anime;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.Selector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.List;

import io.realm.Realm;

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

    public static class GetCoverURL extends AsyncTask<String, Void, String> {

        private String title;

        @Override
        public String doInBackground(String... titles) {
            this.title = titles[0];
            if (title.length() > 60) {
                title = title.substring(0, 59);
            }
            String malTitle = title.replace(' ', '+').replace("(Sub)", "").replace("(Dub)", "");
            try {
                final StringBuilder URLBuilder = new StringBuilder();
                final Document doc = Jsoup.connect(Browser.IMAGE_URL + malTitle).userAgent(Utils.USER_AGENT).get();

                Element imageElement = doc.select(Selector.MAL_IMAGE).first();
                if (imageElement == null) return "";

                Uri rawUrl = Uri.parse(imageElement.attr(Selector.MAL_IMAGE_ATTR));
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
            return "";
        }

        @Override
        public void onPostExecute(final String URL) {
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Anime anime = realm.where(Anime.class).equalTo("title", title).findFirst();
                    if (anime == null) {
                        anime = realm.createObject(Anime.class);
                        anime.title = title;
                    }
                    anime.coverURL = URL;
                }
            });
            realm.close();
        }
    }
}
