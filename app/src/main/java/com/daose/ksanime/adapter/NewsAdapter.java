package com.daose.ksanime.adapter;


import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daose.ksanime.R;
import com.daose.ksanime.model.News;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private ArrayList<News> data;
    private OnClickListener listener;

    public NewsAdapter(ArrayList<News> data, OnClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    public void setData(ArrayList<News> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public NewsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NewsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.news_item, parent, false), listener);
    }

    @Override
    public void onBindViewHolder(NewsViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        public News news;
        public TextView title;
        public ImageView thumbnail;
        public View background;

        public NewsViewHolder(View itemView, final OnClickListener listener) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.title);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            background = itemView.findViewById(R.id.background);

            background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(news);
                }
            });
        }

        public void bind(News news) {
            this.news = news;
            Picasso.with(thumbnail.getContext()).load(news.thumbnail).into(thumbnail);
            title.setText(Html.fromHtml(news.title));
        }
    }

    public interface OnClickListener {
        void onClick(News news);
    }
}
