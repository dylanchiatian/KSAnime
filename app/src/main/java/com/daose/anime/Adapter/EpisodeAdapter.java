package com.daose.anime.Adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.daose.anime.Anime.Anime;
import com.daose.anime.Anime.Episode;
import com.daose.anime.AnimeActivity;
import com.daose.anime.FullScreenVideoPlayerActivity;
import com.daose.anime.R;
import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;
import com.daose.anime.web.Selector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class EpisodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Anime anime;
    private RealmResults<Episode> episodeList;
    private Context ctx;
    private AnimeActivity activity;

    private int num = -1;

    private static final String TAG = "EpisodeAdapter";

    private class Type {
        public static final int HEADER = 0;
        public static final int ODD = 1;
        public static final int EVEN = 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return Type.HEADER;
        return (position % 2 == 0) ? Type.EVEN : Type.ODD;
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
        //TODO:: might be better to just have even/odd layout
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.title.setText(anime.title);
            headerViewHolder.summary.setText(anime.summary);
            headerViewHolder.star.setSelected(anime.isStarred);
        } else {
            int offsetPosition = position - 1;
            Episode episode = episodeList.get(offsetPosition);
            if (episode.hasWatched) {
                ((ViewHolder) holder).title.setBackgroundColor(ctx.getResources().getColor(R.color.trans_base4_inactive));
            } else if (offsetPosition % 2 == 0) {
//                ((ViewHolder) holder).title.setBackgroundColor(ctx.getResources().getColor(R.color.base3));
            } else {
//                ((ViewHolder) holder).title.setBackgroundColor(ctx.getResources().getColor(R.color.base2));
            }
            ((ViewHolder) holder).title.setText(episode.name);
        }
    }

    @Override
    public int getItemCount() {
        return episodeList.size() + 1;
    }

    private class CycleInterpolator implements android.view.animation.Interpolator {
        private final float mCycles = 0.5f;

        @Override
        public float getInterpolation(final float input) {
            return (float) Math.sin(2.0f * mCycles * Math.PI * input);
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        private Animation buttonAnim;
        private TextView title;
        private TextView summary;
        private ImageView star;
        private ImageView starAnimation;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            buttonAnim = AnimationUtils.loadAnimation(ctx, R.anim.anim_button);
            title = (TextView) itemView.findViewById(R.id.title);
            summary = (TextView) itemView.findViewById(R.id.summary);
            star = (ImageView) itemView.findViewById(R.id.star);
            starAnimation = (ImageView) itemView.findViewById(R.id.star_animation);
            starAnimation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    starAnimation.startAnimation(buttonAnim);
                    if(star.isSelected()){
                        star.setSelected(false);
                        activity.toggleStar(false);
                    } else {
                        star.setSelected(true);
                        activity.toggleStar(true);
                    }
                }
            });
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, HtmlListener {

        private TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.episode);
            title.setOnClickListener(this);
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
                            num = getLayoutPosition() - 1;
                            title.setBackgroundColor(ctx.getResources().getColor(R.color.trans_base4_inactive));
                            if ((episodeList.get(num).videoURL != null) && (!episodeList.get(num).videoURL.isEmpty())) {
                                startVideo(episodeList.get(num).videoURL);
                                return;
                            }
                            Browser.getInstance(ctx).setListener(ViewHolder.this);
                            Browser.getInstance(ctx).loadUrl(episodeList.get(num).url);
                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .withLayer()
                    .start();
        }

        @Override
        public void onPageLoaded(String html) {
            Browser.getInstance(ctx).reset();
            Log.d(TAG, "html: " + html);
            Document doc = Jsoup.parse(html);
            final Element videoEle = doc.select(Selector.VIDEO).first();
            if (videoEle == null) {
                Log.d(TAG, "couldn't find div");
                return;
            }

            new Handler(ctx.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Realm realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            episodeList.get(num).videoURL = videoEle.attr("abs:src");
                            //TODO:: set hasWatched in startVideo instead
                            episodeList.get(num).hasWatched = true;
                            startVideo(episodeList.get(num).videoURL);
                        }
                    });
                    realm.close();
                }
            });
        }
    }

    private void startVideo(String URL) {
        Intent intent = new Intent(ctx, FullScreenVideoPlayerActivity.class);
        intent.putExtra("url", URL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public EpisodeAdapter(AnimeActivity activity, Anime anime) {
        this.activity = activity;
        this.ctx = activity.getBaseContext();
        this.anime = anime;
        this.episodeList = anime.episodes.sort("name", Sort.DESCENDING);
    }

    public void setEpisodeList(RealmResults<Episode> episodeList) {
        this.episodeList = episodeList;
        notifyDataSetChanged();
    }
}
