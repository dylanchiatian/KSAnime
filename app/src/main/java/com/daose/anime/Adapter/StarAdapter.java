package com.daose.anime.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daose.anime.Anime.Anime;
import com.daose.anime.HomeActivity;
import com.daose.anime.R;

import io.realm.OrderedRealmCollection;

public class StarAdapter extends AnimeAdapter {
    public StarAdapter(HomeActivity activity, OrderedRealmCollection<Anime> animeList) {
        super(activity, animeList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.star_item, parent, false);
        return new ViewHolder(v);
    }
}
