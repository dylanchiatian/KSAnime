package com.daose.ksanime.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.daose.ksanime.R;
import com.daose.ksanime.adapter.NewsAdapter;
import com.daose.ksanime.api.MAL;
import com.daose.ksanime.model.News;

import java.util.ArrayList;

public class NewsFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    private ArrayList<News> newsList = new ArrayList<News>();

    public NewsFragment() {
        // Required empty public constructor
    }

    public static NewsFragment newInstance() {
        return new NewsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final RecyclerView rv = (RecyclerView) view.findViewById(R.id.recycler_view);
        final NewsAdapter adapter = new NewsAdapter(newsList);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        MAL.getNews(new MAL.Listener() {
            @Override
            public void onSuccess(ArrayList<News> data) {
                adapter.setData(data);
            }

            @Override
            public void onError() {
                Toast.makeText(getContext(), getString(R.string.fail_message), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            /*
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
                    */
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onNewsItemClick(String url);
    }
}
