package com.daose.ksanime.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.daose.ksanime.AnimeActivity;
import com.daose.ksanime.R;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.Episode;

import io.realm.RealmResults;
import io.realm.Sort;

public class EpisodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = EpisodeAdapter.class.getSimpleName();

    private static final int HEADER = 0;
    private static final int EPISODE = 1;

    private Anime anime;
    private RealmResults<Episode> episodeList;
    private boolean isUpdating;
    private OnClickListener mListener;

    public interface OnClickListener {
        void onDescriptionClick(String description);
        void onArrowClick();
        void onEpisodeClick(Episode episode, int position);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? HEADER : EPISODE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == HEADER) {
            return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_header, parent, false), mListener);
        } else {
            return new EpisodeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.episode_item, parent, false), mListener);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            final HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.bind(anime, isUpdating);
        } else {
            int offsetPosition = position - 1;
            Episode episode = episodeList.get(offsetPosition);
            final EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) holder;
            episodeViewHolder.bind(episode, episode.name.replace(anime.title, ""), position);
        }
    }

    @Override
    public int getItemCount() {
        return episodeList.size() + 1;
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private View rootView;
        private TextView title;
        private ImageView arrowButton;
        private TextView description;
        private ProgressBar updateProgress;

        private OnClickListener mListener;

        public HeaderViewHolder(View itemView, OnClickListener listener) {
            super(itemView);
            rootView = itemView;
            this.mListener = listener;
            title = (TextView) itemView.findViewById(R.id.title);
            arrowButton = (ImageView) itemView.findViewById(R.id.arrow);
            description = (TextView) itemView.findViewById(R.id.description);
            updateProgress = (ProgressBar) itemView.findViewById(R.id.update_progress);
        }

        public void bind(final Anime anime, final boolean isUpdating) {
            if(anime.description == null) {
                rootView.setVisibility(View.GONE);
                return;
            }

            rootView.setVisibility(View.VISIBLE);
            title.setText(anime.title);
            description.setText(Html.fromHtml(anime.description));

            arrowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onArrowClick();
                }
            });

            description.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onDescriptionClick(anime.description);
                }
            });

            updateProgress.setVisibility(isUpdating ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public static class EpisodeViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;
        private OnClickListener mListener;

        public EpisodeViewHolder(View itemView, OnClickListener listener) {
            super(itemView);
            mListener = listener;
            textView = (TextView) itemView.findViewById(R.id.episode);
        }

        public void bind(final Episode episode, final String displayText, final int position) {
            if(episode.hasWatched) {
                textView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), R.color.trans_base4_inactive));
                textView.setTextColor(ContextCompat.getColor(textView.getContext(), R.color.text_inactive));
            } else {
                textView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), R.color.trans_base4));
                textView.setTextColor(ContextCompat.getColor(textView.getContext(), R.color.text));
            }
            textView.setText(displayText);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), R.color.trans_base4_inactive));
                    textView.setTextColor(ContextCompat.getColor(textView.getContext(), R.color.text_inactive));
                    mListener.onEpisodeClick(episode, position);
                }
            });
        }
    }

    public void setIsUpdating(final boolean isUpdating) {
        this.isUpdating = isUpdating;
        notifyItemChanged(HEADER);
    }

    public EpisodeAdapter(Anime anime, OnClickListener listener) {
        this.anime = anime;
        this.mListener = listener;
        this.isUpdating = true;
        this.episodeList = anime.episodes.sort("name", Sort.DESCENDING);
    }
}
