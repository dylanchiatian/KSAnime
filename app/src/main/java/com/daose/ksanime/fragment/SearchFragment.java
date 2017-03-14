package com.daose.ksanime.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.daose.ksanime.R;
import com.daose.ksanime.adapter.FullSearchAdapter;
import com.daose.ksanime.api.KA;
import com.daose.ksanime.helper.ApiHelper;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.AnimeList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmList;

public class SearchFragment extends Fragment {
    private static final String TAG = SearchFragment.class.getSimpleName();
    private static final String KEY = "key";

    private String query = "";

    private Realm realm;
    private RecyclerView rv;

    private ProgressBar searchIndicator;

    private OnFragmentInteractionListener mListener;

    public SearchFragment() {}

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
        realm = Realm.getDefaultInstance();
        rv = (RecyclerView) view.findViewById(R.id.recycler_view);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new FullSearchAdapter(getContext(), getList(AnimeList.SEARCH_RESULTS).animeList, new FullSearchAdapter.OnClickListener() {
            @Override
            public void onClick(String title) {
                mListener.onAnimeClick(title);
            }
        }));
        rv.setVisibility(View.GONE);
        searchIndicator = (ProgressBar) view.findViewById(R.id.search_indicator);
        search();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (realm != null) {
            realm.close();
        }
    }

    private AnimeList getList(final String key) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (realm.where(AnimeList.class).equalTo(AnimeList.KEY, key).findFirst() == null) {
                    AnimeList animeList = realm.createObject(AnimeList.class);
                    animeList.key = key;
                    animeList.animeList = new RealmList<Anime>();
                }
            }
        });
        return realm.where(AnimeList.class).equalTo(AnimeList.KEY, key).findFirst();
    }

    private void hideSearchIndicator() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                searchIndicator.setVisibility(View.GONE);
            }
        });
    }

    private void handleSuccess() {
        hideSearchIndicator();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rv.setVisibility(View.VISIBLE);
            }
        });
    }

    private void handleError(final String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        hideSearchIndicator();
    }

    private void search() {
        KA.search(getContext(), query, new KA.OnPageLoaded() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    final boolean redirected = json.getBoolean(KA.REDIRECTED);
                    if (redirected) {
                        ApiHelper.saveAnimeToRealm(json.getJSONObject(KA.ANIME));
                        hideSearchIndicator();
                        mListener.onAnimeClick(json.getJSONObject(KA.ANIME).getString(Anime.TITLE));
                    } else {
                        final JSONArray list = json.getJSONArray(AnimeList.LIST);
                        ApiHelper.saveListToRealm(list, AnimeList.SEARCH_RESULTS);
                        handleSuccess();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "search onSuccess json error for query: " + query, e);
                    handleError(getString(R.string.fail_search));
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, error);
                handleError(error);
            }
        });
    }

    public interface OnFragmentInteractionListener {
        void onAnimeClick(String animeTitle);
    }
}
