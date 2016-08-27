package com.daose.anime;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdLoadListener;
import com.applovin.sdk.AppLovinPostbackListener;
import com.applovin.sdk.AppLovinSdk;
import com.daose.anime.adapter.HomePagerAdapter;
import com.daose.anime.adapter.SearchAdapter;
import com.daose.anime.model.Anime;
import com.daose.anime.model.AnimeList;
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

    private List<AppLovinNativeAd> nativeAds;

    private Snackbar loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setupDatabase();
        hotList = getList("hotList");
        popularList = getList("popularList");
        initUI();
        initAds();
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

    private void initAds() {
        AppLovinSdk.getInstance(this).getNativeAdService().loadNativeAds(1, new AppLovinNativeAdLoadListener() {
            @Override
            public void onNativeAdsLoaded(List list) {
                Log.d(TAG, "onNativeAdsLoaded");
                nativeAds = (List<AppLovinNativeAd>) list;
                AppLovinSdk.getInstance(getApplicationContext()).getPostbackService().dispatchPostbackAsync(
                        nativeAds.get(0).getImpressionTrackingUrl(), new AppLovinPostbackListener() {
                            @Override
                            public void onPostbackSuccess(String s) {
                            }

                            @Override
                            public void onPostbackFailure(String s, int i) {
                            }
                        }
                );
            }

            @Override
            public void onNativeAdsFailedToLoad(int i) {
                Log.e(TAG, "onNativeAdsFailedToLoad: " + i);
            }
        });
    }

    public List<AppLovinNativeAd> getNativeAds() {
        return nativeAds;
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

    @Override
    public void onPageLoaded(String html) {
        final Document doc = Jsoup.parse(html);
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
                for (Anime anime : hotList.animeList) {
                    if (anime.coverURL == null || anime.coverURL.isEmpty()) {
                        new GetCoverURL().execute(anime.title);
                    }
                }
                for (Anime anime : popularList.animeList) {
                    if (anime.coverURL == null || anime.coverURL.isEmpty()) {
                        new GetCoverURL().execute(anime.title);
                    }
                }
                if (loadingBar.isShown()) loadingBar.dismiss();
            }
        });
    }

    @Override
    public void onPageFailed() {
        Log.d(TAG, "onPageFailed: HomeActivity");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loadingBar.isShown()) {
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
            }
        });
    }

    //region ads
    public void onNativeAdClick(AppLovinNativeAd ad) {
        ad.launchClickTarget(this);
        Log.d(TAG, "registered click");
    }
    //endregion

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
        if (loadingBar.isShown()) loadingBar.dismiss();
        adapter.getSearchIndicator().setVisibility(View.VISIBLE);
        Browser.getInstance(this).load(Browser.SEARCH_URL + query, new HtmlListener() {
            @Override
            public void onPageLoaded(String html) {
                Browser.getInstance(HomeActivity.this).reset();
                final Document doc = Jsoup.parse(html);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.getSearchIndicator().setVisibility(View.GONE);
                        final Elements animeElements = doc.select(Selector.SEARCH_LIST);
                        final ArrayList<String> searchList = new ArrayList<String>();
                        realm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                for (final Element animeElement : animeElements) {
                                    Anime anime = realm.where(Anime.class).equalTo("title", animeElement.text()).findFirst();
                                    if (anime == null) {
                                        anime = realm.createObject(Anime.class);
                                        anime.title = animeElement.text();
                                        anime.summaryURL = Browser.BASE_URL + animeElement.attributes().get("href");
                                    }
                                    searchList.add(anime.title);
                                }
                            }
                        }, new Realm.Transaction.OnSuccess() {
                            @Override
                            public void onSuccess() {
                                adapter.getSearchView().setAdapter(new SearchAdapter(HomeActivity.this, searchList));
                            }
                        });
                    }
                });
            }

            @Override
            public void onPageFailed() {
                Log.e(TAG, "onPageFailed: Search");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter.getSearchIndicator().getVisibility() == View.VISIBLE) {
                            adapter.getSearchIndicator().setVisibility(View.GONE);
                            Toast.makeText(HomeActivity.this, "Try again later", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        previousQuery = query.toString();
    }

    @Override
    public void onButtonClicked(int i) {
    }
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

    private class GetCoverURL extends AsyncTask<String, Void, String> {

        private StringBuilder URLBuilder;
        private String title;

        @Override
        protected void onPreExecute() {
            URLBuilder = new StringBuilder();

        }

        @Override
        protected String doInBackground(String... titles) {
            String url = "";
            try {
                this.title = titles[0];
                final Document doc = Jsoup.connect(Browser.IMAGE_URL + title).userAgent("Mozilla/5.0").get();
                Uri rawUrl = Uri.parse(doc.select(Selector.MAL_IMAGE).first().attr(Selector.MAL_IMAGE_ATTR));
                URLBuilder.append(rawUrl.getScheme()).append("://").append(rawUrl.getHost());
                List<String> pathSegments = rawUrl.getPathSegments();
                if (rawUrl.getPathSegments().size() < 3) {
                    return url;
                } else {
                    for (int i = 2; i < pathSegments.size(); i++) {
                        URLBuilder.append("/");
                        URLBuilder.append(pathSegments.get(i));
                    }
                }
                url = URLBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return url;
        }

        @Override
        protected void onPostExecute(final String url) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    final Anime anime = realm.where(Anime.class).equalTo("title", title).findFirst();
                    anime.coverURL = url;
                }
            });
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
