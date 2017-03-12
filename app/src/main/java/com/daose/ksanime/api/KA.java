package com.daose.ksanime.api;

import android.content.Context;
import android.util.Log;

import com.daose.ksanime.R;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.HtmlListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class KA {
    private static final String TAG = KA.class.getSimpleName();

    private static final String BASE_URL = "http://kissanime.ru/";
    private static final String SEARCH_URL = BASE_URL + "Search/Anime/";
    private static final String IMAGE_URL = "http://myanimelist.net/anime.php?q=";
    private static final String MOST_POPULAR = "AnimeList/MostPopular";
    private static final String NEW_AND_HOT = "AnimeList/NewAndHot";

    private static final String POPULAR_TITLE = "div#tab-mostview span.title";
    private static final String POPULAR_IMAGE = "div#tab-mostview img";
    private static final String POPULAR = POPULAR_IMAGE + "," + POPULAR_TITLE;
    private static final String TRENDING_TITLE = "div#tab-trending span.title";
    private static final String TRENDING_IMAGE = "div#tab-trending img";
    private static final String TRENDING = TRENDING_IMAGE + "," + TRENDING_TITLE;
    private static final String UPDATED = "div.barContent";
    private static final String MAL_IMAGE = "div.picSurround img";
    private static final String MAL_IMAGE_ATTR = "data-src";
    private static final String EPISODE_LIST = "table.listing tbody tr td a";
    private static final String ANIME_DESCRIPTION = "div.barContent p:not(:has(span.info))";
    private static final String VIDEO = "div#divContentVideo video";
    private static final String RELATED_ANIME_LIST = "div.rightBox a";
    private static final String ANIME_LIST = "table.listing td:eq(0) a";
    private static final String SEARCH_CHECK = "div.bigBarContainer div.barTitle";

    public static final String UPDATED_LIST = "updated_list";
    public static final String POPULAR_LIST = "popular_list";
    public static final String TRENDING_LIST = "trending_list";

    public static void getHomePage(final Context context, final OnPageLoaded callback) {
        if(!Utils.isNetworkAvailable(context)) {
            callback.onError(context.getResources().getString(R.string.fail_internet));
            return;
        }

        Browser.getInstance(context).load(BASE_URL, new HtmlListener() {
            @Override
            public void onPageLoaded(final String html) {
                try {
                    final Document doc = Jsoup.parse(html);
                    final JSONObject ret = new JSONObject();

                    final Elements updatedElements = doc.select(UPDATED).last().select("a");
                    if(updatedElements == null || updatedElements.size() == 0) {
                        callback.onError(context.getResources().getString(R.string.fail_message));
                        return;
                    }

                    final Elements trendingElements = doc.select(TRENDING);
                    if(trendingElements.size() == 0) {
                        callback.onError(context.getResources().getString(R.string.fail_message));
                        return;
                    }

                    final Elements popularElements = doc.select(POPULAR);
                    if(popularElements.size() == 0) {
                        callback.onError(context.getResources().getString(R.string.fail_message));
                        return;
                    }

                    final JSONArray updatedList = new JSONArray();
                    for (int i = 0; i < updatedElements.size(); i += 2) {
                        final String title = updatedElements.get(i).text();
                        final String summaryURL = updatedElements.get(i).attr("href");
                        if (title.equals("More...")) {
                            break;
                        }

                        final JSONObject anime = new JSONObject();
                        anime.put(Anime.TITLE, updatedElements.get(i).text());
                        anime.put(Anime.SUMMARY_URL, updatedElements.get(i).attr("href"));
                        updatedList.put(anime);
                    }
                    ret.put(UPDATED_LIST, updatedList);
                    ret.put(TRENDING_LIST, getAnimeList(doc, TRENDING));
                    ret.put(POPULAR_LIST, getAnimeList(doc, POPULAR));

                    callback.onSuccess(ret);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed getting home page", e);
                    callback.onError(context.getResources().getString(R.string.fail_message));
                } catch (Exception e) {
                    Log.d(TAG, "Failed getting home page", e);
                    callback.onError(context.getResources().getString(R.string.fail_message));
                }
            }

            @Override
            public void onPageFailed() {
                callback.onError(context.getResources().getString(R.string.fail_message));
            }
        });
    }

    private static JSONArray getAnimeList(final Document doc, final String query) throws JSONException, Exception {
        final Elements elements = doc.select(query);
        if(elements.size() == 0) {
            throw new Exception("0 elements");
        }

        final JSONArray list = new JSONArray();

        int counter = 0;
        for(final Element element : elements) {
            if(counter % 3 == 1) {
                final JSONObject anime = new JSONObject();
                anime.put(Anime.TITLE, element.text());
                anime.put(Anime.SUMMARY_URL, element.parentNode().attributes().get("href"));
                list.put(anime);
            }
            counter++;
        }

        return list;
    }

    public interface OnPageLoaded {
        void onSuccess(final JSONObject json);
        void onError(final String error);
    }
}
