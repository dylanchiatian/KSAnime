package com.daose.ksanime.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
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
    private Snackbar snackbar;

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
        final NewsAdapter adapter = new NewsAdapter(new ArrayList<News>(), new NewsAdapter.OnClickListener() {
            @Override
            public void onClick(final News news) {
                new AlertDialog.Builder(getContext())
                        .setMessage(Html.fromHtml(news.description))
                        .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.more, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mListener.onNewsItemClick(news.link);
                            }
                        })
                        .setCancelable(true)
                        .show();
            }
        });
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);
        snackbar = Snackbar.make(view, R.string.loading, Snackbar.LENGTH_INDEFINITE);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.trans_base4));
        snackbar.show();

        MAL.getNews(new MAL.Listener() {
            @Override
            public void onSuccess(ArrayList<News> data) {
                adapter.setData(data);
                snackbar.dismiss();
            }

            @Override
            public void onError() {
                Toast.makeText(getContext(), getString(R.string.fail_message), Toast.LENGTH_SHORT).show();
                snackbar.dismiss();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if(snackbar.isShown()) {
            snackbar.dismiss();
        }
    }

    public interface OnFragmentInteractionListener {
        void onNewsItemClick(String url);
    }
}
