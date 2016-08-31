package com.daose.ksanime.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.daose.ksanime.R;
import com.daose.ksanime.fragment.AnimeListFragment;
import com.daose.ksanime.fragment.DownloadFragment;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.Episode;

import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;

public class DownloadAdapter extends RealmRecyclerViewAdapter<Episode, RecyclerView.ViewHolder> {

    private static final String TAG = DownloadAdapter.class.getSimpleName();

    private DownloadFragment fragment;
    private OrderedRealmCollection<Episode> downloadedList;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //TODO:: change to another layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.episode_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder)holder).title.setText(downloadedList.get(position).name);
    }

    @Override
    public int getItemCount(){
        return downloadedList.size();
    }

    public DownloadAdapter(DownloadFragment fragment, OrderedRealmCollection<Episode> downloadedList) {
        super(fragment.getContext(), downloadedList, true);
        this.fragment = fragment;
        this.downloadedList = downloadedList;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView title;

        public ViewHolder(View itemView){
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.episode);
            title.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            fragment.onVideoClick(downloadedList.get(getLayoutPosition()).localFilePath);
        }
    }

}
