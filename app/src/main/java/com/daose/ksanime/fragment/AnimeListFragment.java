package com.daose.ksanime.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdLoadListener;
import com.applovin.sdk.AppLovinSdk;
import com.daose.ksanime.MainActivity;
import com.daose.ksanime.R;
import com.daose.ksanime.adapter.AnimeAdapter;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.AnimeList;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.HtmlListener;
import com.daose.ksanime.web.Selector;
import com.daose.ksanime.widget.AutofitRecyclerView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmList;

public class AnimeListFragment extends Fragment implements AppLovinNativeAdLoadListener {
    private static final String TAG = AnimeListFragment.class.getSimpleName();
    private static final String KEY = "key";

    public enum Type {
        Popular, Trending, Starred
    }

    private String value;

    private Realm realm;

    private AnimeList realmAnimeList;
    private OrderedRealmCollection<Anime> animeList;

    private AutofitRecyclerView rv;

    private Snackbar refreshBar;
    private Type type;

    private OnFragmentInteractionListener mListener;

    public AnimeListFragment() {
    }

    public static AnimeListFragment newInstance(String key) {
        AnimeListFragment fragment = new AnimeListFragment();
        Bundle args = new Bundle();
        args.putString(KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            value = getArguments().getString(KEY);
            type = Type.valueOf(getArguments().getString(KEY));
            realm = Realm.getDefaultInstance();
            switch (type) {
                case Starred:
                    animeList = realm.where(Anime.class).equalTo("isStarred", true).findAll();
                    break;
                default:
                    realmAnimeList = getList(value);
                    animeList = realmAnimeList.animeList;
                    break;
            }
        }
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (type == Type.Starred && animeList.isEmpty()) {
            return inflater.inflate(R.layout.star_list_default, container, false);
        } else {
            return inflater.inflate(R.layout.fragment_anime_list, container, false);
        }
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (type == Type.Starred && animeList.isEmpty()) return;
        rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
        rv.setHasFixedSize(true);
        rv.setAdapter(new AnimeAdapter(this, animeList, MainActivity.nativeAds));
        refreshBar = Snackbar.make(rv, getString(R.string.snackbar_refresh), Snackbar.LENGTH_INDEFINITE);
        refreshBar.getView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.trans_base4));
        initAds();
        if (type != Type.Starred) {
            update();
        }
    }

    private void initAds() {
        AppLovinSdk.getInstance(getContext()).getNativeAdService().loadNativeAds(1, this);
    }

    private void update() {

        if (!Browser.getInstance(getContext()).isNetworkAvailable()) {
            Toast.makeText(getContext(), "No internet", Toast.LENGTH_SHORT).show();
            return;
        }
        refreshBar.show();
        String URL = Browser.BASE_URL;
        if (type == Type.Popular) {
            URL += Browser.MOST_POPULAR;
        } else if (type == Type.Trending) {
            URL += Browser.NEW_AND_HOT;
        }
        Browser.getInstance(getContext()).load(URL, new HtmlListener() {
            @Override
            public void onPageLoaded(String html) {
                Browser.getInstance(getContext()).reset();
                final Document doc = Jsoup.parse(html);
                if (getActivity() == null) {
                    Log.e(TAG, "getActivity refresh is null");
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                final Elements animeElements = doc.select(Selector.ANIME_LIST);
                                final RealmList<Anime> animeList = new RealmList<Anime>();
                                for (final Element animeElement : animeElements) {
                                    Anime anime = realm.where(Anime.class).equalTo("title", animeElement.text()).findFirst();
                                    if (anime == null) {
                                        anime = realm.createObject(Anime.class);
                                        anime.title = animeElement.text();
                                        anime.summaryURL = Browser.BASE_URL + animeElement.attributes().get("href");
                                    }
                                    animeList.add(anime);
                                }
                                realmAnimeList.animeList = animeList;
                            }
                        });

                        for (Anime anime : realmAnimeList.animeList) {
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
                Log.d(TAG, "onPageFailed");
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (refreshBar.isShown()) {
                            refreshBar.dismiss();
                            Browser.getInstance(getContext()).reset();
                            Snackbar retryBar = Snackbar
                                    .make(rv, "Refresh Failed", Snackbar.LENGTH_LONG)
                                    .setAction("Retry", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            update();
                                        }
                                    })
                                    .setActionTextColor(getResources().getColor(R.color.colorAccent));
                            retryBar.getView().setBackgroundColor(getResources().getColor(R.color.trans_base4_inactive));
                            retryBar.show();
                        }
                    }
                });
            }
        });
    }

    private AnimeList getList(final String list) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (realm.where(AnimeList.class).equalTo(KEY, list).findFirst() == null) {
                    AnimeList animeList = realm.createObject(AnimeList.class);
                    animeList.key = list;
                    animeList.animeList = new RealmList<Anime>();
                }
            }
        });
        return realm.where(AnimeList.class).equalTo(KEY, list).findFirst();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (refreshBar != null && refreshBar.isShown()) refreshBar.dismiss();
        realm.close();
    }


    public interface OnFragmentInteractionListener {
        void onNativeAdClick(AppLovinNativeAd ad);

        void onAnimeClick(String anime);
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


    public void onNativeAdImpression(AppLovinNativeAd ad) {
        AppLovinSdk.getInstance(getContext()).getPostbackService().dispatchPostbackAsync(
                ad.getImpressionTrackingUrl(), null
        );
    }

    @Override
    public void onNativeAdsLoaded(final List list) {
        MainActivity.nativeAds = (List<AppLovinNativeAd>) list;
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rv.swapAdapter(new AnimeAdapter(AnimeListFragment.this, animeList, MainActivity.nativeAds), false);
            }
        });
    }

    @Override
    public void onNativeAdsFailedToLoad(int i) {
        Log.e(TAG, "onNativeAdsFailedToLoad: " + i);
    }

    public void onAnimeClick(View v, final String anime) {
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
                        mListener.onAnimeClick(anime);
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                    }
                })
                .withLayer()
                .start();
    }
}
