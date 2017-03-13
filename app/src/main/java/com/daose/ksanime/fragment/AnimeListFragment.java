package com.daose.ksanime.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.daose.ksanime.R;
import com.daose.ksanime.adapter.AnimeAdapter;
import com.daose.ksanime.api.KA;
import com.daose.ksanime.helper.ApiHelper;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.AnimeList;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.HtmlListener;
import com.daose.ksanime.web.Selector;
import com.daose.ksanime.widget.AutofitRecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmList;

public class AnimeListFragment extends Fragment implements KA.OnPageLoaded {
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
        rv.setAdapter(new AnimeAdapter(this, animeList));
        refreshBar = Snackbar.make(rv, getString(R.string.snackbar_refresh), Snackbar.LENGTH_INDEFINITE);
        refreshBar.getView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.trans_base4));
        if (type != Type.Starred) {
            update();
        }
    }

    @Override
    public void onSuccess(JSONObject json) {
        try {
            final JSONArray list = json.getJSONArray(AnimeList.LIST);
            ApiHelper.saveListToRealm(list, type.name());

            final Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    final AnimeList list = realm.where(AnimeList.class).equalTo(AnimeList.KEY, type.name()).findFirst();
                    for(final Anime anime : list.animeList) {
                        if(anime.coverURL == null || anime.coverURL.isEmpty()) {
                            new Utils.GetCoverURL().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, anime.title);
                        }
                    }
                }
            });
            realm.close();
        } catch (JSONException e) {
            Log.e(TAG, "AnimeList onSuccess error for type: " + type.name(), e);
            Toast.makeText(getContext(), getString(R.string.fail_message), Toast.LENGTH_SHORT).show();
        } finally {
            if(refreshBar.isShown()) refreshBar.dismiss();
        }
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, error);
        if(refreshBar.isShown()) refreshBar.dismiss();
        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
    }

    private void update() {
        refreshBar.show();
        if(type == Type.Popular) {
            KA.getPopularList(getContext(), this);
        } else if(type == Type.Trending) {
            KA.getTrendingList(getContext(), this);
        }
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
        if(realm != null) {
            realm.close();
        }
    }


    public interface OnFragmentInteractionListener {
        void onAnimeClick(String anime);
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
