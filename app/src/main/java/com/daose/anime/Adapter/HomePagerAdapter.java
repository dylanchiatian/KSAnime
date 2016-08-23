package com.daose.anime.Adapter;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daose.anime.Anime.Anime;
import com.daose.anime.Anime.AnimeList;
import com.daose.anime.HomeActivity;
import com.daose.anime.R;
import com.daose.anime.widgets.AutofitRecyclerView;

import io.realm.Realm;
import io.realm.RealmList;

/**
 * Created by billch on 8/22/2016.
 */
public class HomePagerAdapter extends PagerAdapter {

    private final HomeActivity activity;
    private RealmList<Anime> hotList, popularList, starredList;

    public HomePagerAdapter(HomeActivity activity){
        this.activity = activity;
        Realm realm = Realm.getDefaultInstance();
        hotList = realm.where(AnimeList.class).equalTo("key", "hotList").findFirst().animeList;
        popularList = realm.where(AnimeList.class).equalTo("key", "popularList").findFirst().animeList;
        starredList = new RealmList<Anime>();
        realm.close();
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public void destroyItem(final View container, final int position, final Object object) {
        ((ViewPager) container).removeView((View) object);
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {
        Realm realm = Realm.getDefaultInstance();
        final View view = LayoutInflater.from(activity.getBaseContext()).inflate(R.layout.anime_list, null, false);
        final AutofitRecyclerView rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
        rv.setHasFixedSize(true);

        switch (position) {
            case 0:
                //TODO:: replace with favourite list
                rv.setAdapter(new AnimeAdapter(activity, starredList));
                break;
            case 1:
                rv.setAdapter(new AnimeAdapter(activity, hotList));
                break;
            case 2:
                rv.setAdapter(new AnimeAdapter(activity, popularList));
                break;
            default:
                rv.setAdapter(new AnimeAdapter(activity, new RealmList<Anime>()));
                break;
        }
        container.addView(view);
        realm.close();
        return view;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
}
