package com.daose.ksanime.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.daose.ksanime.R;

import io.realm.Realm;

public class SettingsFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {

    private OnFragmentInteractionListener mListener;
    private SharedPreferences prefs;

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        RadioGroup qualities = (RadioGroup) view.findViewById(R.id.qualities);
        Button resetButton = (Button) view.findViewById(R.id.reset);
        prefs = getActivity().getSharedPreferences("daose", Context.MODE_PRIVATE);
        String quality = prefs.getString("quality", "720p");
        switch (quality) {
            case "1080p":
                qualities.check(R.id.ten);
                break;
            case "720p":
                qualities.check(R.id.seven);
                break;
            case "480p":
                qualities.check(R.id.four);
                break;
            case "360p":
                qualities.check(R.id.three);
                break;
            default:
                qualities.check(R.id.seven);
                break;
        }
        qualities.setOnCheckedChangeListener(this);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetDialog();
            }
        });
    }

    private void showResetDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Reset?")
                .setMessage("Reset will erase favorites and all cached data")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Realm realm = Realm.getDefaultInstance();
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.deleteAll();
                                Toast.makeText(getContext(), "Reset success", Toast.LENGTH_SHORT).show();
                            }
                        });
                        realm.close();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        */
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        SharedPreferences.Editor editor = prefs.edit();
        switch (checkedId) {
            case R.id.ten:
                editor.putString("quality", "1080p");
                break;
            case R.id.seven:
                editor.putString("quality", "720p");
                break;
            case R.id.four:
                editor.putString("quality", "480p");
                break;
            case R.id.three:
                editor.putString("quality", "360p");
                break;
            default:
                editor.putString("quality", "720p");
                break;
        }
        editor.apply();
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction();
    }
}
