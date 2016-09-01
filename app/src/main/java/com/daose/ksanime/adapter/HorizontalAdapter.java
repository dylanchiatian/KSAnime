package com.daose.ksanime.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.daose.ksanime.R;
import com.daose.ksanime.fragment.HomeFragment;
import com.daose.ksanime.model.Anime;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

public class HorizontalAdapter extends RealmRecyclerViewAdapter<Anime, RecyclerView.ViewHolder> {

    private OrderedRealmCollection<Anime> animeList;
    private HomeFragment fragment;
    private Context ctx;
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
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_horizontal_item, parent, false);
                return new ViewHolder(v);
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_horizontal_ad, parent, false);
                return new AdViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AdViewHolder) {
            final AppLovinNativeAd ad = nativeAds.get(position);
            AdViewHolder vh = (AdViewHolder) holder;
            vh.title.setText(ad.getTitle());
            vh.card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.onNativeAdClick(v, ad);
                }
            });
            Picasso.with(ctx).load(ad.getIconUrl()).placeholder(R.drawable.ad_placeholder).into(vh.iconImg);
        } else {
            int offsetPosition = position - offset;
            final Anime anime = animeList.get(offsetPosition);
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
    }

    @Override
    public int getItemCount() {
        return animeList.size() + offset;
    }


    public class AdViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private ImageView iconImg;
        private View card;

        public AdViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            iconImg = (ImageView) itemView.findViewById(R.id.image_view);
            card = itemView.findViewById(R.id.card);
        }
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

    public HorizontalAdapter(HomeFragment fragment, OrderedRealmCollection<Anime> animeList, List<AppLovinNativeAd> nativeAds) {
        super(fragment.getContext(), animeList, true);
        this.fragment = fragment;
        this.ctx = fragment.getContext();
        this.animeList = animeList;
        this.nativeAds = nativeAds;
        if (nativeAds == null) {
            offset = 0;
        } else {
            fragment.onNativeAdImpression(nativeAds.get(0));
            offset = nativeAds.size();
        }
    }
}
