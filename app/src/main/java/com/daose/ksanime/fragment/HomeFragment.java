package com.daose.ksanime.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdLoadListener;
import com.applovin.sdk.AppLovinSdk;
import com.daose.ksanime.MainActivity;
import com.daose.ksanime.R;
import com.daose.ksanime.adapter.HorizontalAdapter;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.AnimeList;
import com.daose.ksanime.model.Episode;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.HtmlListener;
import com.daose.ksanime.web.Selector;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;

public class HomeFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    private static final String TAG = HomeFragment.class.getSimpleName();

    private Realm realm;

    private AnimeList realmPopularList;
    private AnimeList realmTrendingList;
    private AnimeList realmUpdatedList;

    private RecyclerView popularView;
    private RecyclerView trendingView;
    private RecyclerView updatedView;

    private Button morePopular;
    private Button moreTrending;

    private RelativeLayout recentView;

    private Snackbar refreshBar;

    private Anime recentAnime;

    private AppLovinNativeAd trendingAd, popularAd, updatedAd;

    public HomeFragment() {
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDatabase();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        trendingView = (RecyclerView) view.findViewById(R.id.trending_view);
        popularView = (RecyclerView) view.findViewById(R.id.popular_view);
        updatedView = (RecyclerView) view.findViewById(R.id.updated_view);

        trendingView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        popularView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        updatedView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        trendingView.setHasFixedSize(true);
        popularView.setHasFixedSize(true);
        updatedView.setHasFixedSize(true);

        trendingView.setNestedScrollingEnabled(false);
        popularView.setNestedScrollingEnabled(false);
        updatedView.setNestedScrollingEnabled(false);

        recentView = (RelativeLayout) view.findViewById(R.id.recent_view);
        recentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAnimeClick(v, recentAnime.title);
            }
        });

        refreshBar = Snackbar.make(view, getString(R.string.snackbar_refresh), Snackbar.LENGTH_INDEFINITE);
        refreshBar.getView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.trans_base4));

        moreTrending = (Button) view.findViewById(R.id.more_trending);
        morePopular = (Button) view.findViewById(R.id.more_popular);

        moreTrending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onShowMore(AnimeListFragment.Type.Trending.name());
            }
        });

        morePopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onShowMore(AnimeListFragment.Type.Popular.name());
            }
        });

        initAds();
        trendingView.setAdapter(new HorizontalAdapter(this, realmTrendingList.animeList, trendingAd));
        popularView.setAdapter(new HorizontalAdapter(this, realmPopularList.animeList, popularAd));
        updatedView.setAdapter(new HorizontalAdapter(this, realmUpdatedList.animeList, updatedAd));

        refresh();
    }

    private void refresh() {
        if (Browser.getInstance(getContext()).isNetworkAvailable()) {
            refreshBar.show();
            Browser.getInstance(getContext()).load(Browser.BASE_URL, new HtmlListener() {
                @Override
                public void onPageLoaded(String html) {
                    Browser.getInstance(getContext()).reset();
                    final Document doc = Jsoup.parse(html);
                    //trending, popular, updated
                    if (getActivity() == null) {
                        Log.e(TAG, "getActivity: refresh was null");
                        return;
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    updateAnimeList(realmTrendingList, doc, Selector.TRENDING);
                                    updateAnimeList(realmPopularList, doc, Selector.POPULAR);

                                    RealmList<Anime> updatedList = new RealmList<Anime>();
                                    Elements elements = doc.select(Selector.UPDATED).last().select("a");
                                    for (int i = 0; i < elements.size(); i += 2) {
                                        String title = elements.get(i).text();
                                        String summaryURL = elements.get(i).attr("href");
                                        if (title.equals("More...")) {
                                            break;
                                        }

                                        Anime anime = realm.where(Anime.class).equalTo("title", title).findFirst();
                                        if (anime == null) {
                                            anime = realm.createObject(Anime.class);
                                            anime.title = title;
                                            anime.summaryURL = Browser.BASE_URL + summaryURL;
                                        }
                                        updatedList.add(anime);
                                    }
                                    realmUpdatedList.animeList = updatedList;
                                }
                            });

                            for (Anime anime : realmPopularList.animeList) {
                                if (anime.coverURL == null || anime.coverURL.isEmpty()) {
                                    new Utils.GetCoverURL().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, anime.title);
                                }
                            }

                            for (Anime anime : realmTrendingList.animeList) {
                                if (anime.coverURL == null || anime.coverURL.isEmpty()) {
                                    new Utils.GetCoverURL().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, anime.title);
                                }
                            }

                            for (Anime anime : realmUpdatedList.animeList) {
                                if (anime.coverURL == null || anime.coverURL.isEmpty()) {
                                    new Utils.GetCoverURL().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, anime.title);
                                }
                            }

                            if (refreshBar.isShown()) refreshBar.dismiss();
                        }
                    });
                }

                @Override
                public void onPageFailed() {
                    Browser.getInstance(getContext()).reset();
                    if (refreshBar.isShown()) refreshBar.dismiss();
                    Toast.makeText(getContext(), "Refresh failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "No internet", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupRecentView();
    }

    private void setupRecentView() {
        //TODO:: what about a horizontal blurred image that expands into AnimeActivity?
        recentAnime = realm.where(Anime.class).equalTo("isLastWatched", true).findFirst();
        if (recentAnime != null) {
            final ImageView cover = (ImageView) recentView.findViewById(R.id.recent_anime_cover);
            if (recentAnime.coverURL == null || recentAnime.coverURL.isEmpty()) {
                new Utils.GetCoverURL().execute(recentAnime.title);
            } else {
                Picasso.with(getContext()).load(recentAnime.coverURL).placeholder(R.drawable.placeholder).into(cover);
            }

            //TODO:: could have lastwatched in anime field and set a listener for anime in animeactivity that updates when episode -> hasWatched
            RealmResults<Episode> watchedEpisodes = recentAnime.episodes.where().equalTo("hasWatched", true).findAllSorted("name", Sort.DESCENDING);
            if (watchedEpisodes.size() > 0) {
                TextView episodeName = (TextView) recentView.findViewById(R.id.recent_episode_name);
                episodeName.setText(watchedEpisodes.first().name);
            }

            recentView.setVisibility(View.VISIBLE);
        } else {
            recentView.setVisibility(View.GONE);
        }
    }

    private void initAds() {
        if (MainActivity.nativeAds == null) {
            trendingAd = null;
            popularAd = null;
            updatedAd = null;
        } else {
            switch (MainActivity.nativeAds.size()) {
                case 1:
                    updatedAd = MainActivity.nativeAds.get(0);
                    trendingAd = null;
                    popularAd = null;
                    break;
                case 2:
                    updatedAd = MainActivity.nativeAds.get(0);
                    trendingAd = MainActivity.nativeAds.get(1);
                    popularAd = null;
                    break;
                case 3:
                    updatedAd = MainActivity.nativeAds.get(0);
                    trendingAd = MainActivity.nativeAds.get(1);
                    popularAd = MainActivity.nativeAds.get(2);
                    break;
                default:
                    trendingAd = popularAd = updatedAd = null;
                    break;
            }
        }

        AppLovinSdk.getInstance(getContext()).getNativeAdService().loadNativeAds(3, new AppLovinNativeAdLoadListener() {
            @Override
            public void onNativeAdsLoaded(final List list) {
                MainActivity.nativeAds = (List<AppLovinNativeAd>) list;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updatedView.swapAdapter(new HorizontalAdapter(HomeFragment.this, realmUpdatedList.animeList, (AppLovinNativeAd) list.get(0)), false);
                            trendingView.swapAdapter(new HorizontalAdapter(HomeFragment.this, realmTrendingList.animeList, (AppLovinNativeAd) list.get(1)), false);
                            popularView.swapAdapter(new HorizontalAdapter(HomeFragment.this, realmPopularList.animeList, (AppLovinNativeAd) list.get(2)), false);
                        }
                    });

                }
            }

            @Override
            public void onNativeAdsFailedToLoad(int i) {
                Log.e(TAG, "onNativeAdsFailedToLoad: " + i);
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(refreshBar.isShown()) refreshBar.dismiss();
        mListener = null;
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
        realmPopularList = getList("home_popular");
        realmTrendingList = getList("home_trending");
        realmUpdatedList = getList("home_updated");
    }

    private AnimeList getList(final String list) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (realm.where(AnimeList.class).equalTo("key", list).findFirst() == null) {
                    AnimeList animeList = realm.createObject(AnimeList.class);
                    animeList.key = list;
                    animeList.animeList = new RealmList<Anime>();
                }
            }
        });
        return realm.where(AnimeList.class).equalTo("key", list).findFirst();
    }

    public void onNativeAdImpression(AppLovinNativeAd ad) {
        AppLovinSdk.getInstance(getContext()).getPostbackService().dispatchPostbackAsync(
                ad.getImpressionTrackingUrl(), null
        );
    }

    public void onNativeAdClick(View v, final AppLovinNativeAd ad) {
        ViewCompat.animate(v)
                .setDuration(200)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setInterpolator(new Utils.CycleInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                        if (refreshBar.isShown()) refreshBar.dismiss();
                        Browser.getInstance(getActivity()).reset();
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        mListener.onNativeAdClick(ad);
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                    }
                })
                .withLayer()
                .start();
    }

    public void onAnimeClick(View v, final String animeTitle) {
        ViewCompat.animate(v)
                .setDuration(200)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setInterpolator(new Utils.CycleInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                        if (refreshBar.isShown()) refreshBar.dismiss();
                        Browser.getInstance(getActivity()).reset();
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        mListener.onAnimeClick(animeTitle);
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                    }
                })
                .withLayer()
                .start();
    }

    private void updateAnimeList(final AnimeList realmAnimeList, Document doc, String query) {
        final Elements elements = doc.select(query);
        if (elements == null || elements.size() == 0) {
            Log.d(TAG, "doc is wrong");
            Log.d(TAG, "html: " + doc.html());
            Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            return;
        }

        final RealmList<Anime> animeList = new RealmList<Anime>();
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

        realmAnimeList.animeList = animeList;
    }

    public interface OnFragmentInteractionListener {
        void onAnimeClick(String animeTitle);
        void onNativeAdClick(AppLovinNativeAd ad);

        void onShowMore(String key);
    }
}
