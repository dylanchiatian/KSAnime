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

import com.applovin.nativeAds.AppLovinNativeAd;
import com.daose.anime.HomeActivity;
import com.daose.anime.R;
import com.daose.anime.model.Anime;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

public class AnimeAdapter extends RealmRecyclerViewAdapter<Anime, RecyclerView.ViewHolder> {

    private OrderedRealmCollection<Anime> animeList;
    private final Context ctx;
    private final HomeActivity activity;
    private List<AppLovinNativeAd> nativeAds;

    private static final String TAG = AnimeAdapter.class.getSimpleName();
    private int offset;

    private class Type {
        public static final int ANIME = 0;
        public static final int AD = 1;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && nativeAds != null) ? Type.AD : Type.ANIME;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case Type.ANIME:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_item, parent, false);
                return new ViewHolder(v);
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_ad, parent, false);
                return new AdViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AdViewHolder) {
            AppLovinNativeAd ad = nativeAds.get(position);
            AdViewHolder vh = (AdViewHolder) holder;
            vh.title.setText(ad.getTitle());
            vh.cta.setText(ad.getCtaText());
            vh.description.setText(ad.getDescriptionText());
            Picasso.with(ctx).load(ad.getIconUrl()).into(vh.iconImg);
        } else {
            int offsetPosition = position - offset;
            Anime anime = animeList.get(offsetPosition);
            ViewHolder vh = (ViewHolder) holder;
            vh.title.setText(anime.title);
            if (anime.coverURL == null || anime.coverURL.isEmpty()) {
                Picasso.with(ctx).load(R.drawable.placeholder).into(vh.imageView);
            } else {
                Picasso.with(ctx).load(anime.coverURL).placeholder(R.drawable.placeholder).transform(new RoundedCornersTransformation(4, 2, RoundedCornersTransformation.CornerType.ALL)).into(vh.imageView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return animeList.size() + offset;
    }

    private class CycleInterpolator implements android.view.animation.Interpolator {
        private final float mCycles = 0.5f;

        @Override
        public float getInterpolation(final float input) {
            return (float) Math.sin(2.0f * mCycles * Math.PI * input);
        }
    }

    public class AdViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView title, description, cta;
        private ImageView iconImg;
        private View card;

        public AdViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            cta = (TextView) itemView.findViewById(R.id.cta);
            description = (TextView) itemView.findViewById(R.id.description);
            iconImg = (ImageView) itemView.findViewById(R.id.image_view);
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
                            activity.onNativeAdClick(nativeAds.get(0));
                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .withLayer()
                    .start();
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
                            int offsetPosition = getLayoutPosition() - offset;
                            activity.onAnimeSelected(animeList.get(offsetPosition).title);
                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .withLayer()
                    .start();
        }
    }

    public AnimeAdapter(HomeActivity activity, OrderedRealmCollection<Anime> animeList, List<AppLovinNativeAd> nativeAds) {
        super(activity, animeList, true);
        this.activity = activity;
        this.ctx = activity.getBaseContext();
        this.animeList = animeList;
        this.nativeAds = nativeAds;
        if (nativeAds == null) {
            offset = 0;
        } else {
            offset = nativeAds.size();
        }
    }
}
