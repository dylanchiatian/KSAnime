package com.daose.ksanime.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daose.ksanime.HomeActivity;
import com.daose.ksanime.R;
import com.daose.ksanime.model.Anime;

import io.realm.OrderedRealmCollection;

public class StarAdapter extends AnimeAdapter {
    public StarAdapter(HomeActivity activity, OrderedRealmCollection<Anime> animeList) {
        super(activity, animeList, null);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.star_item, parent, false);
        return new ViewHolder(v);
    }
}
