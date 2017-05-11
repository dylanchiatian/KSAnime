package com.daose.ksanime.fragment;

import android.content.Context;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daose.ksanime.R;
import com.daose.ksanime.adapter.HorizontalAdapter;
import com.daose.ksanime.api.ka.KA;
import com.daose.ksanime.api.KitsuApi;
import com.daose.ksanime.helper.ApiHelper;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.AnimeList;
import com.daose.ksanime.model.Episode;
import com.daose.ksanime.util.Utils;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public HomeFragment() {}
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
                if(refreshBar.isShown()) refreshBar.dismiss();
                mListener.onShowMore(AnimeList.MORE_TRENDING);
            }
        });

        morePopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(refreshBar.isShown()) refreshBar.dismiss();
                mListener.onShowMore(AnimeList.MORE_POPULAR);
            }
        });

        trendingView.setAdapter(new HorizontalAdapter(this, realmTrendingList.animeList));
        popularView.setAdapter(new HorizontalAdapter(this, realmPopularList.animeList));
        updatedView.setAdapter(new HorizontalAdapter(this, realmUpdatedList.animeList));

        refresh();
    }

    private void fetchThumbnails() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fetchThumbnails(realmUpdatedList.animeList);
                fetchThumbnails(realmTrendingList.animeList);
                fetchThumbnails(realmPopularList.animeList);
            }
        });
    }

    private void fetchThumbnails(RealmList<Anime> list) {
        for(Anime anime : list) {
            if(anime.coverURL == null || anime.coverURL.isEmpty()) {
                KitsuApi.getInstance().fetchCoverUrl(anime.title);
            }
        }
    }

    private void refresh() {
        refreshBar.show();
        KA.getHomePage(getContext(), new KA.OnPageLoaded() {
            @Override
            public void onSuccess(final JSONObject json) {
                try {
                    final JSONArray updatedList = json.getJSONArray(AnimeList.UPDATED);
                    ApiHelper.saveListToRealm(updatedList, AnimeList.UPDATED);

                    final JSONArray trendingList = json.getJSONArray(AnimeList.TRENDING);
                    ApiHelper.saveListToRealm(trendingList, AnimeList.TRENDING);

                    final JSONArray popularList = json.getJSONArray(AnimeList.POPULAR);
                    ApiHelper.saveListToRealm(popularList, AnimeList.POPULAR);

                    fetchThumbnails();
                } catch (JSONException e) {
                    Log.e(TAG, "Home page onSuccess error", e);
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
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(getString(R.string.home));
        setupRecentView();
    }

    private void setupRecentView() {
        //TODO:: what about a horizontal blurred image that expands into AnimeActivity?
        recentAnime = realm.where(Anime.class).equalTo("isLastWatched", true).findFirst();
        if (recentAnime != null) {
            final ImageView cover = (ImageView) recentView.findViewById(R.id.recent_anime_cover);
            if (recentAnime.coverURL == null || recentAnime.coverURL.isEmpty()) {
                KitsuApi.getInstance().fetchCoverUrl(recentAnime.title);
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

    @Override
    public void onDetach() {
        super.onDetach();
        if(refreshBar.isShown()) refreshBar.dismiss();
        mListener = null;
        if(realm != null) {
            realm.close();
        }
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
        realmPopularList = getList("home_popular");
        realmTrendingList = getList("home_trending");
        realmUpdatedList = getList("home_updated");
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

    public void onAnimeClick(View v, final String animeTitle) {
        if(refreshBar.isShown()) refreshBar.dismiss();

        ViewCompat.animate(v)
                .setDuration(200)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setInterpolator(new Utils.CycleInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                        if (refreshBar.isShown()) refreshBar.dismiss();
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
        void onShowMore(String key);
    }
}
