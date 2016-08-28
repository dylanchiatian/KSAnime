package com.daose.ksanime.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdLoadListener;
import com.applovin.sdk.AppLovinSdk;
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

import java.io.IOException;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class AnimeListFragment extends Fragment implements AppLovinNativeAdLoadListener, HtmlListener {
    private static final String TAG = AnimeListFragment.class.getSimpleName();
    private static final String KEY = "key";

    private Realm realm;
    private RealmList<Anime> animeList;

    private AutofitRecyclerView rv;

    private Snackbar refreshBar;

    private OnFragmentInteractionListener mListener;

    public AnimeListFragment() {
        // Required empty public constructor
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
            String value = getArguments().getString(KEY);
            realm = Realm.getDefaultInstance();
            animeList = realm.where(AnimeList.class).equalTo(KEY, value).findFirst().animeList;
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_anime_list, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
        rv.setHasFixedSize(true);
        rv.setAdapter(new AnimeAdapter(this, animeList, null));
        refreshBar = Snackbar.make(rv, "Refreshing...", Snackbar.LENGTH_INDEFINITE);
        refreshBar.getView().setBackgroundColor(getResources().getColor(R.color.base1));
        initAds();
        update();
    }

    private void initAds() {
        AppLovinSdk.getInstance(getContext()).getNativeAdService().loadNativeAds(1, this);
    }

    private void update() {
        if (Browser.getInstance(getContext()).isNetworkAvailable()) {
            refreshBar.show();
            Browser.getInstance(getContext()).load(Browser.BASE_URL, this);
        } else {
            Toast.makeText(getContext(), "No internet", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }


    public interface OnFragmentInteractionListener {
        void onNativeAdClick(AppLovinNativeAd ad);

        void onAnimeClick(String anime);
    }

    //region listeners
    public void onNativeAdClick(View v, final AppLovinNativeAd ad) {
        ViewCompat.animate(v)
                .setDuration(200)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setInterpolator(new Utils.CycleInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rv.swapAdapter(new AnimeAdapter(AnimeListFragment.this, animeList, (List<AppLovinNativeAd>) list), false);
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
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        if (refreshBar.isShown()) refreshBar.dismiss();
                        mListener.onAnimeClick(anime);
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                    }
                })
                .withLayer()
                .start();
    }

    @Override
    public void onPageLoaded(String html) {
        final Document doc = Jsoup.parse(html);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Browser.getInstance(getContext()).reset();
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        animeList = getAnimeList(doc, Selector.POPULAR_IMAGE + "," + Selector.POPULAR_TITLE);
                    }
                });
                for (Anime anime : animeList) {
                    if (anime.coverURL == null || anime.coverURL.isEmpty()) {
                        new GetCoverURL().execute(anime.title);
                    }
                }
                if (refreshBar.isShown()) refreshBar.dismiss();
            }
        });
    }

    @Override
    public void onPageFailed() {
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
                    retryBar.getView().setBackgroundColor(getResources().getColor(R.color.base1));
                    retryBar.show();
                }
            }
        });
    }
    //endregion

    //region jsoup
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