package com.daose.ksanime.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daose.ksanime.R;
import com.daose.ksanime.adapter.DownloadAdapter;
import com.daose.ksanime.adapter.SectionAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DownloadFragment extends Fragment {

    private static final String TAG = DownloadFragment.class.getSimpleName();

    private RecyclerView rv;

    private File moviesFolder;

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
        moviesFolder = getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        if (moviesFolder == null || moviesFolder.list().length == 0) {
            view = inflater.inflate(R.layout.fragment_download_default, container, false);
        } else {
            view = inflater.inflate(R.layout.fragment_download, container, false);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedBundleInstance) {
        if (moviesFolder != null && moviesFolder.list().length != 0) {
            rv = (RecyclerView) view.findViewById(R.id.recycler_view);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setHasFixedSize(true);
            refreshAdapter();
        }
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

    public void onVideoRemove(String path, int position) {
        File file = new File(path);
        File parent = file.getParentFile();
        file.delete();
        if (parent.list().length == 0) {
            parent.delete();
        }
        refreshAdapter();
    }

    private void refreshAdapter() {
        int animeIndex = 0;
        List<SectionAdapter.Section> sections = new ArrayList<SectionAdapter.Section>();
        ArrayList<File> downloadedEpisodes = new ArrayList<File>();
        if (moviesFolder == null) return;
        File[] animes = moviesFolder.listFiles();
        for (File animeFolder : animes) {
            sections.add(new SectionAdapter.Section(animeIndex, animeFolder.getName().replaceAll("-", " ")));
            File[] downloadedFiles = animeFolder.listFiles();
            animeIndex += downloadedFiles.length;
            downloadedEpisodes.addAll(Arrays.asList(downloadedFiles));
        }

        DownloadAdapter adapter = new DownloadAdapter(this, downloadedEpisodes);

        SectionAdapter.Section[] sectionArray = new SectionAdapter.Section[sections.size()];
        SectionAdapter sectionAdapter = new SectionAdapter(getContext(), R.layout.section_header, R.id.title, adapter);
        sectionAdapter.setSections(sections.toArray(sectionArray));
        if (rv.getAdapter() == null) {
            rv.setAdapter(sectionAdapter);
        } else {
            rv.swapAdapter(sectionAdapter, false);
        }
    }


    public interface OnFragmentInteractionListener {
        void onVideoClick(String path);
    }
}
