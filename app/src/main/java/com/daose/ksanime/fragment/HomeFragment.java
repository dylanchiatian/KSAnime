package com.daose.ksanime.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

    private RecyclerView popularView;
    private RecyclerView trendingView;
    private RecyclerView updatedView;

    private RelativeLayout recentView;

    private Anime recentAnime;

    private AppLovinNativeAd trendingAd, popularAd;

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

        trendingView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        popularView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        trendingView.setHasFixedSize(true);
        popularView.setHasFixedSize(true);

        trendingView.setNestedScrollingEnabled(false);
        popularView.setNestedScrollingEnabled(false);

        //---
        if(recentAnime != null) {
            recentView = (RelativeLayout) view.findViewById(R.id.recent_view);
            TextView title = (TextView) recentView.findViewById(R.id.recent_anime_title);
            title.setText(recentAnime.title);

            RealmResults<Episode> watchedEpisodes = recentAnime.episodes.where().equalTo("hasWatched", true).findAllSorted("name", Sort.DESCENDING);
            if(watchedEpisodes.size() > 0){
                TextView episodeName = (TextView) recentView.findViewById(R.id.recent_episode_name);
                episodeName.setText(watchedEpisodes.first().name);
            }
            recentView.setVisibility(View.VISIBLE);
        }


        initAds();
        trendingView.setAdapter(new HorizontalAdapter(this, realmTrendingList.animeList, trendingAd));
        popularView.setAdapter(new HorizontalAdapter(this, realmPopularList.animeList, popularAd));
    }

    private void initAds() {
        if (MainActivity.nativeAds == null) {
            trendingAd = null;
            popularAd = null;
        } else {
            if (MainActivity.nativeAds.size() > 1) {
                trendingAd = MainActivity.nativeAds.get(0);
                popularAd = MainActivity.nativeAds.get(1);
            } else {
                trendingAd = MainActivity.nativeAds.get(0);
                popularAd = null;
            }
        }

        AppLovinSdk.getInstance(getContext()).getNativeAdService().loadNativeAds(2, new AppLovinNativeAdLoadListener() {
            @Override
            public void onNativeAdsLoaded(final List list) {
                MainActivity.nativeAds = (List<AppLovinNativeAd>) list;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            trendingView.swapAdapter(new HorizontalAdapter(HomeFragment.this, realmTrendingList.animeList, (AppLovinNativeAd) list.get(0)), false);
                            popularView.swapAdapter(new HorizontalAdapter(HomeFragment.this, realmPopularList.animeList, (AppLovinNativeAd) list.get(1)), false);
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
        mListener = null;
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
        realmPopularList = getList("Popular");
        realmTrendingList = getList("Trending");
        recentAnime = realm.where(Anime.class).equalTo("isLastWatched", true).findFirst();
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
                        //todo:: update bar dismiss
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
                        //todo:: update bar dismiss
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


    public interface OnFragmentInteractionListener {
        void onAnimeClick(String animeTitle);

        void onNativeAdClick(AppLovinNativeAd ad);
    }
}
