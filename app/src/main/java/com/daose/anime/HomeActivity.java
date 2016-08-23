package com.daose.anime;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.daose.anime.Adapter.HomePagerAdapter;
import com.daose.anime.Anime.Anime;
import com.daose.anime.Anime.AnimeList;
import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;
import com.daose.anime.web.Selector;
import com.gigamole.navigationtabbar.ntb.NavigationTabBar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class HomeActivity extends AppCompatActivity implements TextView.OnEditorActionListener, HtmlListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private Realm realm;
    private AnimeList hotList, popularList;
    private ViewPager viewPager;

    //TODO:: get rid of splash activity and just load here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setupDatabase();
        hotList = getList("hotList");
        popularList = getList("popularList");
        initUI();
        Browser.getInstance(this).load(Browser.BASE_URL, this);
    }

    private AnimeList getList(final String list) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (realm.where(AnimeList.class).equalTo("key", list).findFirst() == null) {
                    AnimeList animeList = realm.createObject(AnimeList.class);
                    animeList.key = list;
                }
            }
        });
        return realm.where(AnimeList.class).equalTo("key", list).findFirst();
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            search(v.getText().toString());
        }
        return false;
    }

    private void search(String query) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("query", query);
        startActivity(intent);
    }

    private void initUI() {
        EditText search = (EditText) findViewById(R.id.search);
        search.setOnEditorActionListener(this);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new HomePagerAdapter(this));

        final String[] colors = getResources().getStringArray(R.array.nav_colors);
        final NavigationTabBar ntb = (NavigationTabBar) findViewById(R.id.ntb);
        final ArrayList<NavigationTabBar.Model> models = new ArrayList<NavigationTabBar.Model>();
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_star_border_black_24dp),
                        Color.parseColor(colors[0]))
                        .title("Starred")
                        .build()
        );
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_whatshot_black_24dp),
                        Color.parseColor(colors[1]))
                        .title("Hot")
                        .build()
        );
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_trending_up_black_24dp),
                        Color.parseColor(colors[2]))
                        .title("Popular")
                        .build()
        );
        ntb.setModels(models);
        ntb.setViewPager(viewPager, 1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPageLoaded(String html) {
        final Document doc = Jsoup.parse(html);
        Log.d(TAG, "title: " + doc.title());
        if (doc.title().contains("Please wait")) return;
        if (doc.title().isEmpty()) return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Browser.getInstance(HomeActivity.this).reset();
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        popularList.animeList = getAnimeList(doc, Selector.POPULAR_IMAGE + "," + Selector.POPULAR_TITLE);
                        hotList.animeList = getAnimeList(doc, Selector.HOT_IMAGE + "," + Selector.HOT_TITLE);
                    }
                });

                realm.executeTransactionAsync(new GetCoverURL());
            }
        });
    }

    private class GetCoverURL implements Realm.Transaction {

        private StringBuilder URLBuilder;

        public GetCoverURL() {
        }

        @Override
        public void execute(Realm realm) {
            URLBuilder = new StringBuilder();
            RealmList<Anime> asyncPopularList = realm.where(AnimeList.class).equalTo("key", "popularList").findFirst().animeList;
            RealmList<Anime> asyncHotList = realm.where(AnimeList.class).equalTo("key", "hotList").findFirst().animeList;
            for (final Anime anime : asyncPopularList) {
                if(anime.coverURL == null) {
                    getURL(anime);
                    realm.copyToRealmOrUpdate(anime);
                }
            }

            for (final Anime anime : asyncHotList) {
                if(anime.coverURL == null) {
                    getURL(anime);
                    realm.copyToRealmOrUpdate(anime);
                }
            }
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
    }

    private RealmList<Anime> getAnimeList(Document doc, String query) {
        RealmList<Anime> animeList = new RealmList<Anime>();
        Elements elements = doc.select(query);
        if (elements == null) {
            Log.d(TAG, "doc is wrong");
            Log.d(TAG, "html: " + doc.html());
            return new RealmList<Anime>();
        }

        int counter = 0;

        for (Element animeElement : elements) {
            switch (counter % 3) {
                case 0:
                    break;
                case 1:
                    Anime anime = realm.where(Anime.class).equalTo("title", animeElement.text()).findFirst();
                    if(anime == null){
                        anime = realm.createObject(Anime.class);
                        anime.title = animeElement.text();
                        anime.summaryURL = Browser.BASE_URL + animeElement.parentNode().attributes().get("href");
                    }
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
}
