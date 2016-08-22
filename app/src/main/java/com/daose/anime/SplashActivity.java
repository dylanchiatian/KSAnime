package com.daose.anime;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.daose.anime.Anime.Anime;
import com.daose.anime.Anime.AnimeList;
import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;
import com.daose.anime.web.Selector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class SplashActivity extends AppCompatActivity implements HtmlListener {

    private static final String LOG_TAG = SplashActivity.class.getSimpleName();

    private ArrayList<Anime> popularList;
    private ArrayList<Anime> hotList;

    private RealmList<Anime> rPopularList;
    private RealmList<Anime> rHotList;

    private Realm realm;

    //TODO:: only get popular/hot list here, image loading in homeactivity?
    //TODO:: only need realmlist, dont need individual transactions of anime

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        load(Browser.BASE_URL);
    }

    private void load(String url) {
        Browser.getInstance(this).setListener(this);
        Browser.getInstance(this).loadUrl(url);
    }

    @Override
    public void onPageLoaded(final String html) {
        Document doc = Jsoup.parse(html);
        Log.d(LOG_TAG, "title: " + doc.title());
        if (doc.title().contains("Please wait")) return;

        Browser.getInstance(SplashActivity.this).reset();

        rPopularList = new RealmList<Anime>();
        rHotList = new RealmList<Anime>();
        //TODO:: get rid of hot_image, should this be in async thread?
        popularList = getAnimeList(doc, Selector.POPULAR_IMAGE + "," + Selector.POPULAR_TITLE);
        hotList = getAnimeList(doc, Selector.HOT_IMAGE + "," + Selector.HOT_TITLE);
        new GetCoverURL().execute();
    }

    private ArrayList<Anime> getAnimeList(Document doc, String query) {
        ArrayList<Anime> animeList = new ArrayList<Anime>();
        Elements mostPopular = doc.select(query);
        if (mostPopular == null) {
            Log.d(LOG_TAG, "doc is wrong");
            Log.d(LOG_TAG, "html: " + doc.html());
        }
        int counter = 0;
        Anime anime = new Anime();
        for (Element animeElement : mostPopular) {
            switch (counter % 3) {
                case 0:
                    anime = new Anime();
                    break;
                case 1:
                    anime.title = animeElement.text();
                    anime.summaryURL = Browser.BASE_URL + animeElement.parentNode().attributes().get("href");
                    animeList.add(anime);
                    break;
                case 2:
                    break;
                default:
                    break;
            }
            counter++;
        }
        return animeList;
    }

    private class GetCoverURL extends AsyncTask<Void, Void, Void> {

        private StringBuilder URLBuilder;

        @Override
        public Void doInBackground(Void... params) {
            realm = Realm.getDefaultInstance();
            URLBuilder = new StringBuilder();
            for (final Anime anime : popularList) {
                Anime rAnime = realm.where(Anime.class).equalTo("title", anime.title).isNotEmpty("coverURL").findFirst();
                if (rAnime == null) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            Anime rAnime = realm.copyToRealmOrUpdate(anime);
                            getURL(rAnime);
                        }
                    });
                } else {
                    Log.d(LOG_TAG, "title: " + anime.title + " already in database with url: " + rAnime.coverURL);
                }
                rPopularList.add(realm.where(Anime.class).equalTo("title", anime.title).isNotEmpty("coverURL").findFirst());
            }

            for (final Anime anime : hotList) {
                Anime rAnime = realm.where(Anime.class).equalTo("title", anime.title).isNotEmpty("coverURL").findFirst();
                if (rAnime == null) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            Anime rAnime = realm.copyToRealmOrUpdate(anime);
                            getURL(rAnime);
                        }
                    });
                } else {
                    Log.d(LOG_TAG, "title: " + anime.title + " already in database with url: " + rAnime.coverURL);
                }
                rHotList.add(realm.where(Anime.class).equalTo("title", anime.title).isNotEmpty("coverURL").findFirst());
            }
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    AnimeList popularAnimeList = new AnimeList("popularList", rPopularList);
                    realm.copyToRealmOrUpdate(popularAnimeList);

                    AnimeList hotAnimeList = new AnimeList("hotList", rHotList);
                    realm.copyToRealmOrUpdate(hotAnimeList);
                }
            });
            realm.close();
            return null;
        }

        private void getURL(Anime anime) {
            try {
                final Document doc = Jsoup.connect(Browser.IMAGE_URL + anime.title).userAgent("Mozilla/5.0").get();
                Uri rawUrl = Uri.parse(doc.select(Selector.MAL_IMAGE).first().attr(Selector.MAL_IMAGE_ATTR));
                URLBuilder.delete(0, URLBuilder.length());
                URLBuilder.append(rawUrl.getScheme()).append("://").append(rawUrl.getHost());
                List<String> pathSegments = rawUrl.getPathSegments();
                if (rawUrl.getPathSegments().size() < 3) {
                    anime.coverURL = "";
                    return;
                } else {
                    for (int i = 2; i < pathSegments.size(); i++) {
                        URLBuilder.append("/");
                        URLBuilder.append(pathSegments.get(i));
                    }
                }
                anime.coverURL = URLBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPostExecute(Void param) {
            Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
            startActivity(intent);
        }
    }
}
