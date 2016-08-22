package com.daose.anime.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.daose.anime.Anime.Anime;
import com.daose.anime.Anime.Episode;
import com.daose.anime.FullScreenVideoPlayerActivity;
import com.daose.anime.R;
import com.daose.anime.VideoActivity;
import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;
import com.daose.anime.web.Selector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by STUDENT on 2016-08-17.
 */
public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private RealmResults<Episode> episodeList;
    private Context ctx;

    private int num = -1;

    private static final String LOG_TAG = "EpisodeAdapter";

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.episode_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Episode episode = episodeList.get(position);
        if(episode.hasWatched){
            holder.title.setBackgroundColor(ctx.getResources().getColor(R.color.colorPrimaryDark));
        }
        holder.title.setText(episode.name);
    }

    @Override
    public int getItemCount() {
        return episodeList.size();
    }

    private class CycleInterpolator implements android.view.animation.Interpolator {
        private final float mCycles = 0.5f;

        @Override
        public float getInterpolation(final float input) {
            return (float) Math.sin(2.0f * mCycles * Math.PI * input);
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
                            num = getLayoutPosition();
                            title.setBackgroundColor(ctx.getResources().getColor(R.color.colorPrimaryDark));
                            if((episodeList.get(num).videoURL != null) && (!episodeList.get(num).videoURL.isEmpty())){
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
        public void onPageLoaded(String html){
            Browser.getInstance(ctx).reset();
            Log.d(LOG_TAG, "html: " + html);
            Document doc = Jsoup.parse(html);
            final Element videoEle = doc.select(Selector.VIDEO).first();
            if(videoEle == null){
                Log.d(LOG_TAG, "couldn't find div");
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
                            episodeList.get(num).hasWatched = true;
                            startVideo(episodeList.get(num).videoURL);
                        }
                    });
                    realm.close();
                }
            });
        }
    }

    private void startVideo(String URL){
        Intent intent = new Intent(ctx, FullScreenVideoPlayerActivity.class);
        intent.putExtra("url", URL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public EpisodeAdapter(Context ctx, RealmResults<Episode> episodeList) {
        this.ctx = ctx;
        this.episodeList = episodeList;
    }

    public void setEpisodeList(RealmResults<Episode> episodeList){
        this.episodeList = episodeList;
        notifyDataSetChanged();
    }
}
