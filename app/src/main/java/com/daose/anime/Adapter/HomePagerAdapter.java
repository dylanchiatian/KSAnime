package com.daose.anime.Adapter;

import android.app.Activity;
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

import io.realm.RealmList;

/**
 * Created by billch on 8/22/2016.
 */
public class HomePagerAdapter extends PagerAdapter {

    private final Activity activity;

    public HomePagerAdapter(HomeActivity activity){
        this.activity = activity;
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
        final View view = LayoutInflater.from(activity.getBaseContext()).inflate(R.layout.anime_list, null, false);
        /*
        final AutofitRecyclerView rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
        rv.setHasFixedSize(true);

        switch (position) {
            case 0:
                //TODO:: replace with favourite list
                rv.setAdapter(new AnimeAdapter(HomeActivity.this, new RealmList<Anime>()));
                break;
            case 1:
                rv.setAdapter(new AnimeAdapter(HomeActivity.this, realm.where(AnimeList.class).equalTo("key", "hotList").findFirst().animeList));
                break;
            case 2:
                rv.setAdapter(new AnimeAdapter(HomeActivity.this, realm.where(AnimeList.class).equalTo("key", "popularList").findFirst().animeList));
                break;
            default:
                rv.setAdapter(new AnimeAdapter(HomeActivity.this, new RealmList<Anime>()));
                break;
        }
        container.addView(view);
        */
        return view;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
}
