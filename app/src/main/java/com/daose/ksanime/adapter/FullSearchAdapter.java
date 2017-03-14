package com.daose.ksanime.adapter;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.daose.ksanime.R;
import com.daose.ksanime.model.Anime;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;

public class FullSearchAdapter extends RealmRecyclerViewAdapter<Anime, RecyclerView.ViewHolder> {

    private OrderedRealmCollection<Anime> list;
    private FullSearchAdapter.OnClickListener listener;

    public FullSearchAdapter(@NonNull Context context, @Nullable OrderedRealmCollection<Anime> data, FullSearchAdapter.OnClickListener onClickListener) {
        super(context, data, true);
        this.list = data;
        this.listener = onClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final Anime anime = list.get(position);
        ViewHolder vh = (ViewHolder) holder;
        vh.title.setText(anime.title);
        vh.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClick(anime.title);
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.anime);
        }
    }

    public interface OnClickListener {
        void onClick(String title);
    }
}
