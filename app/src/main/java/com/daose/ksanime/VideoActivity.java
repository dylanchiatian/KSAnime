package com.daose.ksanime;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.EMVideoView;

public class VideoActivity extends AppCompatActivity {

    private final static String TAG = VideoActivity.class.getSimpleName();

    protected EMVideoView video;
    //TODO:: look at alternative: https://github.com/afollestad/easy-video-player

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video);

        video = (EMVideoView) findViewById(R.id.video);
        video.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared() {
                video.start();
            }
        });

        video.setVideoURI(Uri.parse(getIntent().getStringExtra("url")));
    }

    @Override
    public void onPause() {
        super.onPause();
        video.pause();
    }
}
