package com.daose.ksanime;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.daose.ksanime.adapter.EpisodeAdapter;
import com.daose.ksanime.api.KitsuApi;
import com.daose.ksanime.api.ka.KA;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.Episode;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.web.Browser;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import dmax.dialog.SpotsDialog;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import jp.wasabeef.picasso.transformations.BlurTransformation;
import jp.wasabeef.picasso.transformations.GrayscaleTransformation;

public class AnimeActivity extends AppCompatActivity implements EpisodeAdapter.OnClickListener {

    private enum Transformation {
        BLUR, BW
    }

    private static final String TAG = AnimeActivity.class.getSimpleName();

    public static final Pattern pattern = Pattern.compile("[^a-zA-Z0-9.-]");

    private ImageView cover;
    private SpotsDialog loadDialog;
    private RecyclerView rv;
    private EpisodeAdapter adapter;
    private LinearLayout preloadIndicator;
    private FloatingActionButton fabDownload, fabStar, fabRelated;
    private FloatingActionMenu fabMenu;

    private Realm realm;
    private Anime anime;
    private String animeTitle;
    private RealmChangeListener<Anime> animeListener;

    private boolean inDownloadMode;

    private CastContext castContext;
    private CastSession castSession;

    public Anime getAnime() {
        return anime;
    }

    //TODO:: group episodes in 50s
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime);

        setupDatabase();
        initUI();
        setupAnime();

        castContext = CastContext.getSharedInstance(getApplicationContext());
        castSession = castContext.getSessionManager().getCurrentCastSession();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(realm != null) {
            anime.removeChangeListener(animeListener);
            realm.close();
        }
    }

    private void setupAnime() {
        animeTitle = getIntent().getStringExtra("anime");
        this.anime = realm.where(Anime.class).equalTo(Anime.TITLE, animeTitle).findFirst();

        adapter = new EpisodeAdapter(anime, this);
        rv.setAdapter(adapter);

        setupBackground(Transformation.BLUR);
        animeListener = new RealmChangeListener<Anime>() {
            @Override
            public void onChange(Anime element) {
                if (element.coverURL != null) {
                    setupBackground(Transformation.BLUR);
                }
            }
        };
        anime.addChangeListener(animeListener);

        if (anime.isStarred) {
            fabStar.setImageResource(R.drawable.ic_star_black_24dp);
        }
        if (anime.episodes.isEmpty()) {
            preloadIndicator.setVisibility(View.VISIBLE);
        }
        updateEpisodes();
    }

    private void handleFail(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.setIsUpdating(false);
                preloadIndicator.setVisibility(View.GONE);
                Utils.dismissDialog(loadDialog);
                Toast.makeText(AnimeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCaptchaError(final String url) {
        handleFail(getString(R.string.captcha));
        KA.openCaptcha(AnimeActivity.this, url);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == KA.CAPTCHA_CODE && resultCode == KA.CAPTCHA_CODE) {
            final String url = data.getStringExtra(Episode.URL);
            Realm r = Realm.getDefaultInstance();
            Episode episode = r.where(Episode.class).equalTo(Episode.URL, url).findFirst();
            getVideo(episode);
            if (loadDialog != null && !loadDialog.isShowing()) loadDialog.show();
            r.close();
        }
    }

    private void updateEpisodes() {
        if (!anime.episodes.isEmpty()) {
            preloadIndicator.setVisibility(View.GONE);
        }

        KA.getAnime(this, anime.summaryURL, new KA.OnPageLoaded() {
            @Override
            public void onSuccess(final JSONObject json) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    try {
                                        final Anime anime = realm.where(Anime.class).equalTo(Anime.TITLE, animeTitle).findFirst();
                                        final JSONArray episodeList = json.getJSONArray(Anime.EPISODES);
                                        final RealmList<Episode> episodeRealmList = new RealmList<Episode>();
                                        for (int i = 0; i < episodeList.length(); i++) {
                                            final JSONObject obj = episodeList.getJSONObject(i);
                                            final Episode episode = realm.createOrUpdateObjectFromJson(Episode.class, obj);
                                            episodeRealmList.add(episode);
                                        }
                                        anime.episodes = episodeRealmList;

                                        final JSONArray relatedList = json.getJSONArray(Anime.RELATED_LIST);
                                        final RealmList<Anime> relatedRealmList = new RealmList<Anime>();
                                        for (int i = 0; i < relatedList.length(); i++) {
                                            final JSONObject obj = relatedList.getJSONObject(i);
                                            final Anime relatedAnime = realm.createOrUpdateObjectFromJson(Anime.class, obj);
                                            relatedAnime.relatedAnimeList.add(anime);
                                            relatedRealmList.add(relatedAnime);
                                        }
                                        anime.relatedAnimeList = relatedRealmList;
                                        anime.description = json.getString(Anime.DESCRIPTION);

                                        adapter.notifyDataSetChanged();
                                        adapter.setIsUpdating(false);
                                        preloadIndicator.setVisibility(View.GONE);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "updateEpisodes json error", e);
                                        handleFail(getString(R.string.update_failed));
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "realm error", e);
                            handleFail(getString(R.string.update_failed));
                        }
                    }
                });
            }
            @Override
            public void onError(final String error) {
                Log.e(TAG, error);
                handleFail(error);
            }
        });
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
    }

    private void initUI() {
        cover = (ImageView) findViewById(R.id.background);

        loadDialog = new SpotsDialog(this, R.style.LoadingTheme);
        preloadIndicator = (LinearLayout) findViewById(R.id.preload);
        preloadIndicator.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //prevent touches from going through
                return true;
            }
        });

        rv = (RecyclerView) findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);

        fabMenu = (FloatingActionMenu) findViewById(R.id.fab_menu);
        fabMenu.setClosedOnTouchOutside(true);
        fabDownload = (FloatingActionButton) findViewById(R.id.fab_download);
        fabRelated = (FloatingActionButton) findViewById(R.id.fab_related);
        fabStar = (FloatingActionButton) findViewById(R.id.fab_star);

        fabRelated.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRelatedDialog();
                fabMenu.close(true);
            }
        });

        fabDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inDownloadMode) {
                    inDownloadMode = false;
                    setupBackground(Transformation.BLUR);
                    fabMenu.close(true);
                    fabDownload.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            fabDownload.setImageDrawable(ContextCompat.getDrawable(AnimeActivity.this, R.drawable.ic_file_download_black_24dp));
                            fabDownload.setLabelText("Download Mode");
                        }
                    }, 500);
                } else {
                    inDownloadMode = true;
                    setupBackground(Transformation.BW);
                    fabMenu.close(true);
                    fabDownload.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            fabDownload.setImageDrawable(ContextCompat.getDrawable(AnimeActivity.this, R.drawable.ic_remove_red_eye_black_24dp));
                            fabDownload.setLabelText("Watch Mode");
                        }
                    }, 500);
                }
            }
        });

        fabStar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (anime.isStarred) {
                    // Unstar
                    fabStar.setImageResource(R.drawable.ic_star_border_black_24dp);
                } else {
                    // Star
                    fabStar.setImageResource(R.drawable.ic_star_black_24dp);
                }
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        anime.isStarred = !anime.isStarred;
                    }
                });
                fabMenu.close(true);
            }
        });

        loadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                rv.getAdapter().notifyDataSetChanged();
                Browser.getInstance(AnimeActivity.this).reset();
            }
        });
    }

    private void setupBackground(Transformation type) {
        if (anime.coverURL == null || anime.coverURL.isEmpty()) {
            KitsuApi.getInstance().fetchCoverUrl(anime.title);
        } else {
            if (type.equals(Transformation.BW)) {
                Picasso.with(this).load(anime.coverURL)
                        .fit()
                        .centerCrop()
                        .transform(new BlurTransformation(this))
                        .transform(new GrayscaleTransformation())
                        .into(cover);
            } else {
                Picasso.with(this).load(anime.coverURL)
                        .fit()
                        .centerCrop()
                        .transform(new BlurTransformation(this))
                        .into(cover);
            }
        }
    }

    @Override
    public void onDescriptionClick(String description) {
        new AlertDialog.Builder(this)
                .setTitle("Description")
                .setMessage(Html.fromHtml(description))
                .setPositiveButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    public void onArrowClick() {
        //TODO:: smooth scroll to last watched position?
        if (rv.getAdapter().getItemCount() > 5) {
            rv.smoothScrollToPosition(5);
        } else if (rv.getAdapter().getItemCount() > 0) {
            rv.smoothScrollToPosition(rv.getAdapter().getItemCount() - 1);
        }
    }

    public void getVideo(final Episode episode) {
        KA.getVideo(this, episode.url, new KA.OnPageLoaded() {
            @Override
            public void onSuccess(JSONObject json) {
                if (inDownloadMode) {
                    showSelectQualityDialog(episode, json);
                } else {
                    try {
                        SharedPreferences prefs = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
                        String resolution = prefs.getString(Utils.SELECT_QUALITY_KEY, Utils.DEFAULT_QUALITY);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        episode.hasWatched = true;
                                        rv.getAdapter().notifyDataSetChanged();
                                    }
                                });
                            }
                        });
                        startVideo(json.getString(resolution));
                    } catch (JSONException e) {
                        showSelectQualityDialog(episode, json);
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (KA.CAPTCHA_ERROR.equals(error)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleCaptchaError(episode.url);
                        }
                    });
                } else {
                    handleFail(error);
                }
            }
        });
    }

    @Override
    public void onEpisodeClick(final Episode episode, final int position) {
        loadDialog.show();
        getVideo(episode);
    }

    private void showRelatedDialog() {
        if (anime.relatedAnimeList.size() == 0) {
            Toast.makeText(this, "No Related Animes", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] relatedAnimeTitles = new CharSequence[anime.relatedAnimeList.size()];
        for (int i = 0; i < anime.relatedAnimeList.size(); i++) {
            relatedAnimeTitles[i] = anime.relatedAnimeList.get(i).title;
        }

        new AlertDialog.Builder(this)
                .setTitle("Related Animes")
                .setSingleChoiceItems(relatedAnimeTitles, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent(AnimeActivity.this, AnimeActivity.class);
                        intent.putExtra("anime", anime.relatedAnimeList.get(which).title);
                        startActivity(intent);
                    }
                })
                .create()
                .show();
    }

    private void showSelectQualityDialog(final Episode episode, final JSONObject json) {
        Utils.dismissDialog(loadDialog);
        Iterator<String> it = json.keys();
        final ArrayList<String> qualities = new ArrayList<String>();
        while (it.hasNext()) {
            qualities.add(it.next());
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.quality_title))
                .setSingleChoiceItems(qualities.toArray(new CharSequence[qualities.size()]), -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        episode.hasWatched = true;
                                    }
                                });
                            }
                        });
                        if (inDownloadMode) {
                            if (isFirstDownload()) {
                                showRatingDialog();
                            }
                            downloadVideo(episode, json.optString(qualities.get(which)));
                        } else {
                            startVideo(json.optString(qualities.get(which)));
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rv.getAdapter().notifyDataSetChanged();
                            }
                        });
                    }
                })
                .create()
                .show();
    }

    public boolean isFirstDownload() {
        SharedPreferences pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (pref.getBoolean(Utils.FIRST_DOWNLOAD_KEY, true)) {
            editor.putBoolean(Utils.FIRST_DOWNLOAD_KEY, false);
            editor.apply();
            return true;
        } else {
            return false;
        }
    }

    public void showRatingDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.rating_title))
                .setMessage(getString(R.string.rating_message))
                .setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.MARKET_DEEPLINK)));
                        } catch (android.content.ActivityNotFoundException exception) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.MARKET_URL)));
                        }
                    }
                })
                .create()
                .show();
    }

    private void downloadVideo(final Episode episode, final String downloadURL) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utils.dismissDialog(loadDialog);
                if (!Utils.isExternalStorageAvailable()) {
                    Log.e(TAG, "External Storage unavailable");
                    Toast.makeText(AnimeActivity.this, getString(R.string.storage_unavailable), Toast.LENGTH_SHORT).show();
                }

                String filePath = pattern.matcher(anime.title).replaceAll("-") + "/" + pattern.matcher(episode.name).replaceAll("-") + ".mp4";

                //TODO:: broadcast manager for deeplink into download fragment
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm == null || !URLUtil.isNetworkUrl(downloadURL)) {
                    Toast.makeText(AnimeActivity.this, getString(R.string.download_unavailable), Toast.LENGTH_SHORT).show();
                    return;
                }

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadURL));
                request.setDestinationInExternalFilesDir(AnimeActivity.this, Environment.DIRECTORY_MOVIES, filePath);
                request.allowScanningByMediaScanner();
                request.setVisibleInDownloadsUi(true);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                dm.enqueue(request);
                Toast.makeText(AnimeActivity.this, getString(R.string.download_started), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void startVideo(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utils.dismissDialog(loadDialog);

                if (!URLUtil.isNetworkUrl(url)) {
                    Log.e(TAG, url);
                    Toast.makeText(AnimeActivity.this, "Bad video link", Toast.LENGTH_SHORT).show();
                    return;
                }

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Anime prevAnime = realm.where(Anime.class).equalTo("isLastWatched", true).findFirst();
                        if (prevAnime != null) prevAnime.isLastWatched = false;
                        anime.isLastWatched = true;

                    }
                });

                Uri uri = Uri.parse(url);
                Intent extIntent = new Intent(Intent.ACTION_VIEW, uri);
                extIntent.setDataAndType(uri, "video/mp4");

                if(castSession != null && castSession.isConnected()){
                    castVideo(anime, url);
                    return;
                }

                if(extIntent.resolveActivity(getPackageManager()) != null &&
                        !getSharedPreferences(getString(R.string.shared_preferences_key), Context.MODE_PRIVATE).getBoolean(getString(R.string.use_internal_player), false)) {
                    startActivity(extIntent);
                    return;
                }

                Intent intent = new Intent(AnimeActivity.this, FullScreenVideoPlayerActivity.class);
                intent.putExtra(Utils.URL_KEY, url);
                intent.putExtra(Utils.ANIME_KEY, anime.title);
                startActivity(intent);
            }
        });
    }

    private void castVideo(Anime anime, String url){
        final RemoteMediaClient mediaClient = castSession.getRemoteMediaClient();
        MediaMetadata animeMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        animeMetadata.putString(MediaMetadata.KEY_TITLE, anime.title);
        if(anime.coverURL != null && !anime.coverURL.isEmpty()) {
            animeMetadata.addImage(new WebImage(Uri.parse(anime.coverURL)));
        }
        MediaInfo animeInfo = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("video/mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(animeMetadata)
                .build();

        mediaClient.addListener(new RemoteMediaClient.Listener() {
            @Override
            public void onStatusUpdated() {
                Intent intent = new Intent(AnimeActivity.this, CastActivity.class);
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
        mediaClient.load(animeInfo, true, 0);
    }
}
