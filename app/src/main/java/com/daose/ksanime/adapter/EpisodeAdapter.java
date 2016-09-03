package com.daose.ksanime.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daose.ksanime.AnimeActivity;
import com.daose.ksanime.R;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.Episode;

import io.realm.RealmResults;
import io.realm.Sort;

public class EpisodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Anime anime;
    private RealmResults<Episode> episodeList;
    private Context ctx;
    private AnimeActivity activity;

    private static final String TAG = EpisodeAdapter.class.getSimpleName();

    private class Type {
        public static final int HEADER = 0;
        public static final int EPISODE = 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? Type.HEADER : Type.EPISODE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case Type.HEADER:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_header, parent, false);
                return new HeaderViewHolder(v);
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.episode_item, parent, false);
                return new ViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.title.setText(anime.title);
            headerViewHolder.description.setText(anime.description);
        } else {
            int offsetPosition = position - 1;
            Episode episode = episodeList.get(offsetPosition);
            if (episode.hasWatched) {
                ((ViewHolder) holder).title.setBackgroundColor(ctx.getResources().getColor(R.color.trans_base4_inactive));
                ((ViewHolder) holder).title.setTextColor(ctx.getResources().getColor(R.color.text_inactive));
            } else {
                ((ViewHolder) holder).title.setBackgroundColor(ctx.getResources().getColor(R.color.trans_base4));
                ((ViewHolder) holder).title.setTextColor(ctx.getResources().getColor(R.color.text));
            }
            ((ViewHolder) holder).title.setText(episode.name);
        }
    }

    @Override
    public int getItemCount() {
        return episodeList.size() + 1;
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView title;
        private ImageView arrowButton;
        private TextView description;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            arrowButton = (ImageView) itemView.findViewById(R.id.arrow);
            arrowButton.setOnClickListener(this);
            description = (TextView) itemView.findViewById(R.id.description);
        }

        @Override
        public void onClick(View v){
            activity.onArrowClick();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.episode);
            title.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int num = getLayoutPosition() - 1;
            title.setBackgroundColor(ctx.getResources().getColor(R.color.trans_base4_inactive));
            title.setTextColor(ctx.getResources().getColor(R.color.text_inactive));
            activity.requestVideo(episodeList.get(num));
        }
    }

    public EpisodeAdapter(AnimeActivity activity, Anime anime) {
        this.activity = activity;
        this.ctx = activity.getBaseContext();
        this.anime = anime;
        this.episodeList = anime.episodes.sort("name", Sort.DESCENDING);
    }
}
