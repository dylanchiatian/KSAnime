package com.daose.ksanime.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daose.ksanime.R;
import com.daose.ksanime.fragment.AnimeListFragment;
import com.daose.ksanime.model.Anime;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

public class AnimeAdapter extends RealmRecyclerViewAdapter<Anime, RecyclerView.ViewHolder> {

    private OrderedRealmCollection<Anime> animeList;
    private AnimeListFragment fragment;
    private Context ctx;

    private static final String TAG = AnimeAdapter.class.getSimpleName();

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_item, parent, false);
                return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Anime anime = animeList.get(position);
        ViewHolder vh = (ViewHolder) holder;
        vh.title.setText(anime.title);
        vh.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.onAnimeClick(v, anime.title);
            }
        });
        if (anime.coverURL == null || anime.coverURL.isEmpty()) {
            Picasso.with(ctx).load(R.drawable.placeholder).into(vh.imageView);
        } else {
            Picasso.with(ctx).load(anime.coverURL).placeholder(R.drawable.placeholder).transform(new RoundedCornersTransformation(4, 2, RoundedCornersTransformation.CornerType.ALL)).into(vh.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return animeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private ImageView imageView;
        private View card;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            imageView = (ImageView) itemView.findViewById(R.id.image_view);
            card = itemView.findViewById(R.id.card);
        }

    }

    public AnimeAdapter(AnimeListFragment fragment, OrderedRealmCollection<Anime> animeList) {
        super(fragment.getContext(), animeList, true);
        this.fragment = fragment;
        this.ctx = fragment.getContext();
        this.animeList = animeList;
    }

}
