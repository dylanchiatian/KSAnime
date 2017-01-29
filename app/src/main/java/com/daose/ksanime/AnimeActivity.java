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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.daose.ksanime.adapter.EpisodeAdapter;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.Episode;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.HtmlListener;
import com.daose.ksanime.web.JSONListener;
import com.daose.ksanime.web.Selector;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import dmax.dialog.SpotsDialog;
import io.realm.Realm;
import io.realm.RealmList;
import jp.wasabeef.picasso.transformations.BlurTransformation;
import jp.wasabeef.picasso.transformations.GrayscaleTransformation;

public class AnimeActivity extends AppCompatActivity {

    private enum Transformation {
        BLUR, BW
    }

    private static final String TAG = AnimeActivity.class.getSimpleName();

    public static final Pattern pattern = Pattern.compile("[^a-zA-Z0-9.-]");

    private ImageView cover;
    private SpotsDialog loadDialog;
    private RecyclerView rv;
    private LinearLayout preloadIndicator;
    private FloatingActionButton fabDownload, fabStar, fabRelated;
    private FloatingActionMenu fabMenu;

    private Realm realm;
    private Anime anime;

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
        setupAnime(getIntent().getStringExtra("anime"));

        castContext = CastContext.getSharedInstance(getApplicationContext());
        castSession = castContext.getSessionManager().getCurrentCastSession();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public void onPause() {
        super.onPause();
        Browser.getInstance(this).reset();
    }

    private void setupAnime(String animeTitle) {
        this.anime = realm.where(Anime.class).equalTo("title", animeTitle).findFirst();
        setupBackground(Transformation.BLUR);
        if (anime.isStarred) {
            fabStar.setImageResource(R.drawable.ic_star_black_24dp);
        }
        if (!anime.episodes.isEmpty()) {
            rv.setAdapter(new EpisodeAdapter(this, anime));
        } else {
            preloadIndicator.setVisibility(View.VISIBLE);
        }
        updateEpisodes();
    }

    private void updateEpisodes() {
        if (!anime.episodes.isEmpty()) {
            preloadIndicator.setVisibility(View.GONE);
        }
        Browser.getInstance(this).load(anime.summaryURL, new HtmlListener() {
            @Override
            public void onPageLoaded(String html) {
                final Document doc = Jsoup.parse(html);
                if (doc.title().isEmpty()) {
                    preloadIndicator.setVisibility(View.GONE);
                    Toast.makeText(AnimeActivity.this, getString(R.string.fail_message), Toast.LENGTH_SHORT).show();
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                Elements elements = doc.select(Selector.EPISODE_LIST);
                                for (Element episodeElement : elements) {
                                    String name = episodeElement.text();
                                    String url = Browser.BASE_URL + episodeElement.attributes().get("href");

                                    Episode episode = realm.where(Episode.class).equalTo("url", url).findFirst();
                                    if (episode == null) {
                                        episode = realm.createObject(Episode.class);
                                        episode.name = name;
                                        episode.url = url;
                                        anime.episodes.add(episode);
                                    }
                                }

                                elements = doc.select(Selector.ANIME_DESCRIPTION);
                                if (elements.size() > 0) {
                                    anime.description = elements.get(0).html();
                                } else if (anime.description == null || anime.description.isEmpty()) {
                                    anime.description = "";
                                }

                                elements = doc.select(Selector.RELATED_ANIME_LIST);
                                if (elements.size() > 0) {
                                    anime.relatedAnimeList = new RealmList<Anime>();
                                    for (Element relatedAnimeElement : elements) {
                                        Uri uri = Uri.parse(Browser.BASE_URL + relatedAnimeElement.attr("href"));
                                        if (uri.getPathSegments().size() > 2) {
                                            //Not an anime, ex: /Anime/Anime_Name/Future_Episode
                                            continue;
                                        }
                                        String title = relatedAnimeElement.text();
                                        String summaryURL = uri.toString();
                                        Anime relatedAnime = realm.where(Anime.class).equalTo("title", title).findFirst();
                                        if (relatedAnime == null) {
                                            relatedAnime = realm.createObject(Anime.class);
                                            relatedAnime.title = title;
                                            relatedAnime.summaryURL = summaryURL;
                                            relatedAnime.relatedAnimeList.add(anime);
                                        }
                                        anime.relatedAnimeList.add(relatedAnime);
                                    }

                                }
                            }
                        });
                        rv.swapAdapter(new EpisodeAdapter(AnimeActivity.this, anime), false);
                        preloadIndicator.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onPageFailed() {
                Log.e(TAG, "onPageFailed: updateEpisodes");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Browser.getInstance(AnimeActivity.this).reset();
                        preloadIndicator.setVisibility(View.GONE);
                        if (loadDialog.isShowing()) loadDialog.dismiss();
                        Toast.makeText(AnimeActivity.this, getString(R.string.update_failed), Toast.LENGTH_SHORT).show();
                    }
                });
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
                    fabStar.setImageResource(R.drawable.ic_star_border_black_24dp);
                } else {
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
            new GetHeaderURL().execute(anime.title);
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

    public void onArrowClick() {
        //TODO:: smooth scroll to last watched position?
        if (rv.getAdapter().getItemCount() > 5) {
            rv.smoothScrollToPosition(5);
        } else if (rv.getAdapter().getItemCount() > 0) {
            rv.smoothScrollToPosition(rv.getAdapter().getItemCount() - 1);
        }
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
        if (loadDialog.isShowing()) loadDialog.dismiss();
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

    //TODO:: clicking this soon after entering this screen results in fail first time around
    public void requestVideo(final Episode episode) {
        loadDialog.show();
        Browser.getInstance(this).addJSONListener(new JSONListener() {
            @Override
            public void onJSONReceived(final JSONObject json) {
                Browser.getInstance(AnimeActivity.this).reset();
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
            public void onPageFailed() {
                Log.e(TAG, "requestVideo: onPageFailed");
                Browser.getInstance(AnimeActivity.this).reset();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadDialog.isShowing()) loadDialog.dismiss();
                        Toast.makeText(AnimeActivity.this, getString(R.string.fail_message), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        Browser.getInstance(this).loadUrl(episode.url);
    }

    private void downloadVideo(final Episode episode, final String downloadURL) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loadDialog.isShowing()) {
                    loadDialog.dismiss();
                }
                if (!Utils.isExternalStorageAvailable()) {
                    Log.e(TAG, "External Storage unavailable");
                    Toast.makeText(AnimeActivity.this, getString(R.string.storage_unavailable), Toast.LENGTH_SHORT).show();
                }

                String filePath = pattern.matcher(anime.title).replaceAll("-") + "/" + pattern.matcher(episode.name).replaceAll("-") + ".mp4";

                //TODO:: broadcast manager for deeplink into download fragment
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm == null) {
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
                if (loadDialog.isShowing()) loadDialog.dismiss();
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

    public void onDescriptionClick() {
        new AlertDialog.Builder(this)
                .setTitle("Description")
                .setMessage(Html.fromHtml(anime.description))
                .setPositiveButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    private class GetHeaderURL extends Utils.GetCoverURL {
        @Override
        public void onPostExecute(String URL) {
            super.onPostExecute(URL);
            if (cover != null && !URL.isEmpty()) {
                Picasso.with(AnimeActivity.this).
                        load(URL)
                        .fit()
                        .centerCrop()
                        .transform(new BlurTransformation(AnimeActivity.this))
                        .into(cover);
            }
        }
    }
}
