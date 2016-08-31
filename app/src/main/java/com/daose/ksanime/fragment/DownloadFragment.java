package com.daose.ksanime.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daose.ksanime.R;
import com.daose.ksanime.adapter.DownloadAdapter;
import com.daose.ksanime.adapter.SectionAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class DownloadFragment extends Fragment {

    private static final String TAG = DownloadFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;

    public DownloadFragment() {
    }

    public static DownloadFragment newInstance() {
        DownloadFragment fragment = new DownloadFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        int animeIndex = 0;
        List<SectionAdapter.Section> sections = new ArrayList<SectionAdapter.Section>();
        ArrayList<File> downloadedEpisodes = new ArrayList<File>();
        File movies = getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if(movies == null) return; //TODO:: better fail case here
        File[] animes = movies.listFiles();
        for(File animeFolder : animes){
            //TODO:: if it's an empty folder then delete it
            sections.add(new SectionAdapter.Section(animeIndex, animeFolder.getName()));
            File[] downloadedFiles = animeFolder.listFiles();
            animeIndex += downloadedFiles.length;
            downloadedEpisodes.addAll(Arrays.asList(downloadedFiles));
        }

        DownloadAdapter adapter = new DownloadAdapter(this, downloadedEpisodes);

        SectionAdapter.Section[] sectionArray = new SectionAdapter.Section[sections.size()];
        SectionAdapter sectionAdapter = new SectionAdapter(getContext(), R.layout.section_header, R.id.title, adapter);
        sectionAdapter.setSections(sections.toArray(sectionArray));
        rv.setAdapter(sectionAdapter);
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
