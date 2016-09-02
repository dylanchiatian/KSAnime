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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.daose.ksanime.R;
import com.daose.ksanime.adapter.SearchAdapter;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.HtmlListener;
import com.daose.ksanime.web.Selector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import io.realm.Realm;

public class SearchFragment extends Fragment implements HtmlListener {
    private static final String TAG = SearchFragment.class.getSimpleName();
    private static final String KEY = "key";

    private String query = "";

    private Realm realm;
    private RecyclerView rv;

    private ProgressBar searchIndicator;

    private OnFragmentInteractionListener mListener;

    public SearchFragment() {
    }

    public static SearchFragment newInstance(String key) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            query = getArguments().getString(KEY);
            realm = Realm.getDefaultInstance();
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
        return inflater.inflate(R.layout.fragment_search, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        rv = (RecyclerView) view.findViewById(R.id.recycler_view);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new SearchAdapter(this, new ArrayList<String>()));
        searchIndicator = (ProgressBar) view.findViewById(R.id.search_indicator);
        initAds();
        search();
    }

    private void search() {
        if (Browser.getInstance(getContext()).isNetworkAvailable()) {
            Browser.getInstance(getContext()).load(Browser.SEARCH_URL + query, this);
        } else {
            searchIndicator.setVisibility(View.GONE);
            Toast.makeText(getContext(), "No internet", Toast.LENGTH_SHORT).show();
        }
    }

    private void initAds() {
        //TODO:: ads
    }

    @Override
    public void onPageLoaded(String html) {
        Browser.getInstance(getContext()).reset();
        if (getActivity() == null) {
            Log.e(TAG, "getActivity returned NULL");
            return;
        }
        final Document doc = Jsoup.parse(html);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Elements animeElements = doc.select(Selector.ANIME_LIST);
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
                        rv.swapAdapter(new SearchAdapter(SearchFragment.this, searchList), false);
                        searchIndicator.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    @Override
    public void onPageFailed() {
        if (getActivity() == null) {
            Log.e(TAG, "getActivity is NULL");
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                searchIndicator.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Try again later", Toast.LENGTH_SHORT).show();
            }
        });
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
                        mListener.onAnimeClick(anime);
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
    }
}
