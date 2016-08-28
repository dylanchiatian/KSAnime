package com.daose.ksanime.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.daose.ksanime.R;
import com.daose.ksanime.adapter.AnimeAdapter;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.AnimeList;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.widget.AutofitRecyclerView;

import io.realm.Realm;
import io.realm.RealmList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AnimeListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AnimeListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AnimeListFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = AnimeListFragment.class.getSimpleName();
    private static final String KEY = "key";

    // TODO: Rename and change types of parameters
    private RealmList<Anime> animeList;
    private OnFragmentInteractionListener mListener;

    private Realm realm;

    public AnimeListFragment() {
        // Required empty public constructor
    }

    public static AnimeListFragment newInstance(String key) {
        AnimeListFragment fragment = new AnimeListFragment();
        Bundle args = new Bundle();
        args.putString(KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String value = getArguments().getString(KEY);
            realm = Realm.getDefaultInstance();
            animeList = realm.where(AnimeList.class).equalTo(KEY, value).findFirst().animeList;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_anime_list, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        //TODO:: get native ads
        AutofitRecyclerView rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
        rv.setHasFixedSize(true);
        rv.setAdapter(new AnimeAdapter(this, animeList, null));
    }

    //startregion listeners
    public void onNativeAdClick(View v, final AppLovinNativeAd ad) {
        ViewCompat.animate(v)
                .setDuration(200)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setInterpolator(new Utils.CycleInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        mListener.onNativeAdClick(ad);
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                    }
                })
                .withLayer()
                .start();
    }

    public void onAnimeClick(View v, final String anime) {
        ViewCompat.animate(v)
                .setDuration(200)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setInterpolator(new Utils.CycleInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        mListener.onAnimeClick(anime);
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                    }
                })
                .withLayer()
                .start();
    }
    //endregion

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    public interface OnFragmentInteractionListener {
        void onNativeAdClick(AppLovinNativeAd ad);

        void onAnimeClick(String anime);
    }
}
