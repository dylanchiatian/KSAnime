package com.daose.anime.adapter;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daose.anime.HomeActivity;
import com.daose.anime.R;
import com.daose.anime.model.Anime;
import com.squareup.picasso.Picasso;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;

public class AnimeAdapter extends RealmRecyclerViewAdapter<Anime, AnimeAdapter.ViewHolder> {

    private OrderedRealmCollection<Anime> animeList;
    private final Context ctx;
    private final HomeActivity activity;

    private static final String TAG = AnimeAdapter.class.getSimpleName();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Anime anime = animeList.get(position);
        holder.title.setText(anime.title);
        if(anime.coverURL == null || anime.coverURL.isEmpty()){
            Picasso.with(ctx).load(R.drawable.placeholder).into(holder.imageView);
        } else {
            Picasso.with(ctx).load(anime.coverURL).placeholder(R.drawable.placeholder).into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return animeList.size();
    }

    private class CycleInterpolator implements android.view.animation.Interpolator {
        private final float mCycles = 0.5f;

        @Override
        public float getInterpolation(final float input) {
            return (float) Math.sin(2.0f * mCycles * Math.PI * input);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView title;
        private ImageView imageView;
        private View card;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            imageView = (ImageView) itemView.findViewById(R.id.image_view);
            card = itemView.findViewById(R.id.card);
            card.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            ViewCompat.animate(v)
                    .setDuration(200)
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setInterpolator(new CycleInterpolator())
                    .setListener(new ViewPropertyAnimatorListener() {
                        @Override
                        public void onAnimationStart(View view) {

                        }

                        @Override
                        public void onAnimationEnd(View view) {
                            activity.onAnimeSelected(animeList.get(getLayoutPosition()).title);
                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .withLayer()
                    .start();
        }
    }

    public AnimeAdapter(HomeActivity activity, OrderedRealmCollection<Anime> animeList) {
        super(activity, animeList, true);
        this.activity = activity;
        this.ctx = activity.getBaseContext();
        this.animeList = animeList;
    }
}
