package com.daose.ksanime;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.daose.ksanime.model.Anime;
import com.daose.ksanime.util.Utils;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.EMVideoView;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import io.realm.Realm;

public class VideoActivity extends AppCompatActivity {

    private final static String TAG = VideoActivity.class.getSimpleName();

    protected EMVideoView video;
    private Uri uri;

    private CastContext castContext;
    private CastSession castSession;
    private RemoteMediaClient mediaClient;

    private Anime anime;

    private Realm realm;

    //TODO:: look at alternative: https://github.com/afollestad/easy-video-player
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video);
        setupDatabase();
        setupCast();
        setupAnime();

        video = (EMVideoView) findViewById(R.id.video);
        video.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared() {
                if (castSession != null && castSession.isConnected()) {
                    final RemoteMediaClient mediaClient = castSession.getRemoteMediaClient();
                    MediaMetadata animeMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
                    animeMetadata.putString(MediaMetadata.KEY_TITLE, anime.title);
                    if(anime.coverURL != null && !anime.coverURL.isEmpty()) {
                        animeMetadata.addImage(new WebImage(Uri.parse(anime.coverURL)));
                    }
                    MediaInfo animeInfo = new MediaInfo.Builder(getIntent().getStringExtra(Utils.URL_KEY))
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setContentType("video/mp4")
                            .setStreamDuration(video.getDuration())
                            .setMetadata(animeMetadata)
                            .build();

                    mediaClient.addListener(new RemoteMediaClient.Listener() {
                        @Override
                        public void onStatusUpdated() {
                            Intent intent = new Intent(VideoActivity.this, CastActivity.class);
                            startActivity(intent);
                            mediaClient.removeListener(this);
                            finish();
                        }
                        @Override
                        public void onMetadataUpdated() {}
                        @Override
                        public void onQueueStatusUpdated() {}
                        @Override
                        public void onPreloadStatusUpdated() {}
                        @Override
                        public void onSendingRemoteMediaRequest() {}
                    });
                    mediaClient.load(animeInfo, true);
                } else {
                    video.start();
                }
            }
        });

        uri = Uri.parse(getIntent().getStringExtra("url"));
        video.setVideoURI(uri);
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        realm.close();
    }

    private void setupCast() {
        castContext = CastContext.getSharedInstance(this);
        castSession = castContext.getSessionManager().getCurrentCastSession();
    }

    private void setupAnime() {
        anime = realm.where(Anime.class).equalTo(Utils.ANIME_KEY, getIntent().getStringExtra(Utils.ANIME_KEY)).findFirst();
    }

    @Override
    public void onPause() {
        super.onPause();
        video.pause();
    }
}
