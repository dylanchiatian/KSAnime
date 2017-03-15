package com.daose.ksanime.adapter;


import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daose.ksanime.R;
import com.daose.ksanime.model.News;
import com.daose.ksanime.widget.OverflowView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private ArrayList<News> data;

    public NewsAdapter(ArrayList<News> data) {
        this.data = data;
    }

    public void setData(ArrayList<News> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public NewsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NewsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.news_item, parent, false));
    }

    @Override
    public void onBindViewHolder(NewsViewHolder holder, int position) {
        final News news = data.get(position);
        Picasso.with(holder.thumbnail.getContext()).load(news.thumbnail).into(holder.thumbnail);
        holder.title.setText(Html.fromHtml(news.title));
        holder.description.setText(Html.fromHtml(news.description));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class NewsViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public OverflowView description;
        public ImageView thumbnail;

        public NewsViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.title);
            description = (OverflowView) itemView.findViewById(R.id.description);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
        }
    }
}
