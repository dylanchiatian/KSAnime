package com.daose.ksanime;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.EMVideoView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VideoActivity extends AppCompatActivity {

    protected EMVideoView video;

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
}
