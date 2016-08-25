package com.daose.anime;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.daose.anime.Adapter.HomePagerAdapter;
import com.daose.anime.Adapter.SearchAdapter;
import com.daose.anime.Anime.Anime;
import com.daose.anime.Anime.AnimeList;
import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;
import com.daose.anime.web.Selector;
import com.gigamole.navigationtabbar.ntb.NavigationTabBar;
import com.mancj.materialsearchbar.MaterialSearchBar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class HomeActivity extends AppCompatActivity implements HtmlListener, MaterialSearchBar.OnSearchActionListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private Realm realm;
    private AnimeList hotList, popularList;
    private ViewPager viewPager;
    private HomePagerAdapter adapter;
    private NavigationTabBar ntb;
    private String previousQuery;

    private Snackbar loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setupDatabase();
        hotList = getList("hotList");
        popularList = getList("popularList");
        initUI();
        if (Browser.getInstance(this).isNetworkAvailable()) {
            Browser.getInstance(this).load(Browser.BASE_URL, this);
            loadingBar.show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    private void initUI() {
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        adapter = new HomePagerAdapter(this);
        viewPager.setAdapter(adapter);

        ntb = (NavigationTabBar) findViewById(R.id.ntb);
        final String[] colors = getResources().getStringArray(R.array.nav_colors);
        final ArrayList<NavigationTabBar.Model> models = new ArrayList<NavigationTabBar.Model>();
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_star_border_black_24dp),
                        Color.parseColor(colors[0]))
                        .selectedIcon(getResources().getDrawable(R.drawable.ic_star_black_24dp))
                        .build()
        );
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_whatshot_black_24dp),
                        Color.parseColor(colors[1]))
                        .build()
        );
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_trending_up_black_24dp),
                        Color.parseColor(colors[2]))
                        .build()
        );
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_search_black_24dp),
                        Color.parseColor(colors[3]))
                        .build()
        );
        ntb.setModels(models);
        ntb.setBehaviorEnabled(true);
        ntb.setViewPager(viewPager, 1);

        loadingBar = Snackbar.make(ntb, "Refreshing...", Snackbar.LENGTH_INDEFINITE);
        loadingBar.getView().setBackgroundColor(getResources().getColor(R.color.base1));
    }

    public void onAnimeSelected(String title) {
        if (loadingBar.isShown()) loadingBar.dismiss();
        Intent intent = new Intent(this, AnimeActivity.class);
        intent.putExtra("anime", title);
        startActivity(intent);
    }

    //Connected to http://kissanime.to
    @Override
    public void onPageLoaded(String html) {
        final Document doc = Jsoup.parse(html);
        Log.d(TAG, "title: " + doc.title());
        if (doc.title().contains("Please wait") || doc.title().contains("not available")) return;
        if (doc.title().isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingBar.dismiss();
                    Browser.getInstance(HomeActivity.this).reset();
                    Snackbar retryBar = Snackbar
                            .make(ntb, "Refresh Failed", Snackbar.LENGTH_LONG)
                            .setAction("Retry", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Browser.getInstance(HomeActivity.this).load(Browser.BASE_URL, HomeActivity.this);
                                    loadingBar.show();
                                }
                            })
                            .setActionTextColor(HomeActivity.this.getResources().getColor(R.color.colorAccent));
                    retryBar.getView().setBackgroundColor(getResources().getColor(R.color.base1));
                    retryBar.show();
                }
            });
            return;
        }

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
                loadingBar.dismiss();
            }
        });
    }

    //region search
    @Override
    public void onSearchStateChanged(boolean b) {
        Log.d(TAG, "onSearchStateChanged: " + b);
    }

    @Override
    public void onSearchConfirmed(CharSequence charSequence) {
        Log.d(TAG, "onSearchConfirmed: " + charSequence);
        //TODO:: breaks if they try to search the same thing twice (first time internet bad or something), use onSearchStateChanged to determine
        //onSearchConfirmed gets called twice in a row (bug)
        if (previousQuery != null && previousQuery.equals(charSequence.toString())) return;
        search(charSequence);
    }

    private void search(CharSequence query) {
        //TODO:: snack bar for search loading
        if (loadingBar.isShown()) loadingBar.dismiss();
        Browser.getInstance(this).load(Browser.SEARCH_URL + query, new HtmlListener() {
            @Override
            public void onPageLoaded(String html) {
                Browser.getInstance(HomeActivity.this).reset();
                final Document doc = Jsoup.parse(html);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Elements animeElements = doc.select(Selector.SEARCH_LIST);
                        final ArrayList<String> searchList = new ArrayList<String>();
                        for (final Element animeElement : animeElements) {
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    Anime anime = realm.where(Anime.class).equalTo("title", animeElement.text()).findFirst();
                                    if (anime == null) {
                                        anime = realm.createObject(Anime.class);
                                        anime.title = animeElement.text();
                                        anime.summaryURL = Browser.BASE_URL + animeElement.attributes().get("href");
                                    }
                                    Log.d(TAG, "summaryURL: " + anime.summaryURL);
                                    searchList.add(anime.title);
                                }
                            });
                        }
                        adapter.getSearchView().setAdapter(new SearchAdapter(HomeActivity.this, searchList));
                    }
                });
            }
        });
        previousQuery = query.toString();
    }

    @Override
    public void onButtonClicked(int i) {}
    //endregion

    //region Helper functions
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
                if (anime.coverURL == null) {
                    getURL(anime);
                    realm.copyToRealmOrUpdate(anime);
                }
            }

            for (final Anime anime : asyncHotList) {
                if (anime.coverURL == null) {
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
                    if (anime == null) {
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
    //endregion
}
