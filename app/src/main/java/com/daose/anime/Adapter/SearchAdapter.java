package com.daose.anime.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.daose.anime.AnimeActivity;
import com.daose.anime.R;

import java.util.ArrayList;

/**
 * Created by STUDENT on 2016-08-21.
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private ArrayList<String> searchList;
    private Context ctx;

    private static final String TAG = SearchAdapter.class.getSimpleName();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.title.setText(searchList.get(position));
    }

    @Override
    public int getItemCount() {
        return searchList.size();
    }

    private class CycleInterpolator implements android.view.animation.Interpolator {
        private final float mCycles = 0.5f;

        @Override
        public float getInterpolation(final float input) {
            return (float) Math.sin(2.0f * mCycles * Math.PI * input);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.anime);
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
                            Intent intent = new Intent(ctx, AnimeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("anime", searchList.get(getLayoutPosition()));
                            ctx.startActivity(intent);
                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .withLayer()
                    .start();
        }
    }

    public SearchAdapter(Context ctx, ArrayList<String> searchList) {
        this.ctx = ctx;
        this.searchList = searchList;
    }
}
