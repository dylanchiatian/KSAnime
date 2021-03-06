package com.daose.ksanime.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.daose.ksanime.R;
import com.daose.ksanime.fragment.DownloadFragment;

import java.io.File;
import java.util.ArrayList;


public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

    private static final String TAG = DownloadAdapter.class.getSimpleName();

    private DownloadFragment fragment;
    private ArrayList<File> downloadedList;

    @Override
    public DownloadAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(DownloadAdapter.ViewHolder holder, final int position) {
        final File episode = downloadedList.get(position);
        holder.title.setText(episode.getName().replaceAll("-", " "));
        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.onVideoClick(episode.getPath());
            }
        });
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.onVideoRemove(episode.getPath(), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return downloadedList.size();
    }

    public DownloadAdapter(DownloadFragment fragment, ArrayList<File> downloadedList) {
        this.fragment = fragment;
        this.downloadedList = downloadedList;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private ImageButton delete;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.episode);
            delete = (ImageButton) itemView.findViewById(R.id.delete);
        }
    }

}
