package com.daose.ksanime.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdLoadListener;
import com.applovin.sdk.AppLovinSdk;
import com.daose.ksanime.MainActivity;
import com.daose.ksanime.R;
import com.daose.ksanime.adapter.AnimeAdapter;
import com.daose.ksanime.adapter.HorizontalAdapter;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.AnimeList;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class HomeFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    private static final String TAG = HomeFragment.class.getSimpleName();

    private Realm realm;

    private AnimeList realmPopularList;
    private AnimeList realmTrendingList;

    private RecyclerView popularView;
    private RecyclerView trendingView;
    private RecyclerView updatedView;

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
        popularView = (RecyclerView) view.findViewById(R.id.popular_view);
        trendingView = (RecyclerView) view.findViewById(R.id.trending_view);

        popularView.setHasFixedSize(true);
        trendingView.setHasFixedSize(true);

        popularView.setAdapter(new HorizontalAdapter(this, realmPopularList.animeList, MainActivity.nativeAds));
        trendingView.setAdapter(new HorizontalAdapter(this, realmTrendingList.animeList, MainActivity.nativeAds));

        initAds();
    }
    //TODO:: just use a different adapter, the layouts are all messed up (match_parent for width)

    private void initAds() {
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
        Log.d(TAG, "onNativeAdImpression");
    }

    public void onNativeAdClick(View v, AppLovinNativeAd ad) {
        Log.d(TAG, "onNativeAdClick");
    }

    public void onAnimeClick(View v, String title) {
        Log.d(TAG, "onAnimeClick: " + title);
    }


    public interface OnFragmentInteractionListener {
    }
}
