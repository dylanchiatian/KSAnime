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

import com.daose.ksanime.R;
import com.daose.ksanime.adapter.DownloadAdapter;
import com.daose.ksanime.model.Episode;

import io.realm.Realm;

public class DownloadFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private Realm realm;

    public DownloadFragment() {
    }

    public static DownloadFragment newInstance() {
        DownloadFragment fragment = new DownloadFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_download, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedBundleInstance) {
        RecyclerView rv = (RecyclerView) view.findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setHasFixedSize(true);
        rv.setAdapter(new DownloadAdapter(this, realm.where(Episode.class).isNotNull("localFilePath").findAll()));
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
    }

    public void onVideoClick(String path) {
        mListener.onVideoClick(path);
    }

    public interface OnFragmentInteractionListener {
        void onVideoClick(String path);
    }
}
