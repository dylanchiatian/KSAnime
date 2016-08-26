package com.daose.anime.adapter;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daose.anime.HomeActivity;
import com.daose.anime.R;
import com.daose.anime.model.Anime;
import com.daose.anime.model.AnimeList;
import com.daose.anime.widget.AutofitRecyclerView;
import com.mancj.materialsearchbar.MaterialSearchBar;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class HomePagerAdapter extends PagerAdapter {

    private enum Page {
        STARRED, HOT, POPULAR, SEARCH
    }

    private final HomeActivity activity;
    private RealmList<Anime> hotList, popularList;
    private RealmResults<Anime> starredList;
    private RecyclerView searchView;
    private static final String TAG = HomePagerAdapter.class.getSimpleName();

    public HomePagerAdapter(HomeActivity activity) {
        this.activity = activity;
        Realm realm = Realm.getDefaultInstance();
        hotList = realm.where(AnimeList.class).equalTo("key", "hotList").findFirst().animeList;
        popularList = realm.where(AnimeList.class).equalTo("key", "popularList").findFirst().animeList;
        starredList = realm.where(Anime.class).equalTo("isStarred", true).findAll();
        realm.close();
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public void destroyItem(final View container, final int position, final Object object) {
        ((ViewPager) container).removeView((View) object);
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {
        View view;
        AutofitRecyclerView rv;
        Page pageType = Page.values()[position];

        switch (pageType) {
            case STARRED:
                view = LayoutInflater.from(activity.getBaseContext()).inflate(R.layout.star_list, null, false);
                rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
                rv.setHasFixedSize(true);
                rv.setAdapter(new StarAdapter(activity, starredList));
                break;
            case HOT:
                view = LayoutInflater.from(activity.getBaseContext()).inflate(R.layout.anime_list, null, false);
                rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
                rv.setHasFixedSize(true);
                rv.setAdapter(new AnimeAdapter(activity, hotList));
                break;
            case POPULAR:
                view = LayoutInflater.from(activity.getBaseContext()).inflate(R.layout.anime_list, null, false);
                rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
                rv.setHasFixedSize(true);
                rv.setAdapter(new AnimeAdapter(activity, popularList));
                break;
            case SEARCH:
                view = LayoutInflater.from(activity.getBaseContext()).inflate(R.layout.search_list, null, false);
                searchView = (RecyclerView) view.findViewById(R.id.recycler_view);
                MaterialSearchBar searchBar = (MaterialSearchBar) view.findViewById(R.id.search_bar);
                searchBar.setOnSearchActionListener(activity);
                searchView.setLayoutManager(new LinearLayoutManager(activity));
                break;
            default:
                view = LayoutInflater.from(activity.getBaseContext()).inflate(R.layout.anime_list, null, false);
                rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
                rv.setHasFixedSize(true);
                rv.setAdapter(new AnimeAdapter(activity, new RealmList<Anime>()));
                break;
        }
        container.addView(view);
        return view;
    }

    public RecyclerView getSearchView() {
        return searchView;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
}
