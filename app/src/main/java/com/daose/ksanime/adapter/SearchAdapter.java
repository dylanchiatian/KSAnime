package com.daose.ksanime.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.daose.ksanime.MainActivity;
import com.daose.ksanime.R;
import com.daose.ksanime.fragment.SearchFragment;

import java.util.ArrayList;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private ArrayList<String> searchList;
    private SearchFragment fragment;
    private MainActivity activity;

    private static final String TAG = SearchAdapter.class.getSimpleName();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final String animeTitle = searchList.get(position);
        holder.title.setText(animeTitle);
        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fragment != null) {
                    fragment.onAnimeClick(v, animeTitle);
                } else if (activity != null) {
                    activity.onAnimeClick(animeTitle);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.anime);
        }
    }

    public SearchAdapter(SearchFragment fragment, ArrayList<String> searchList) {
        this.fragment = fragment;
        this.searchList = searchList;
    }

    public SearchAdapter(MainActivity activity, ArrayList<String> searchList) {
        this.activity = activity;
        this.searchList = searchList;
    }
}
