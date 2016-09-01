package com.daose.ksanime;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.applovin.adview.AppLovinInterstitialAd;
import com.daose.ksanime.adapter.EpisodeAdapter;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.Episode;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.HtmlListener;
import com.daose.ksanime.web.JSONListener;
import com.daose.ksanime.web.Selector;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import dmax.dialog.SpotsDialog;
import io.realm.Realm;
import io.realm.Sort;
import jp.wasabeef.picasso.transformations.BlurTransformation;
import jp.wasabeef.picasso.transformations.GrayscaleTransformation;

public class AnimeActivity extends AppCompatActivity {

    private enum Transformation {
        BLUR, BW
    }

    private static final String TAG = AnimeActivity.class.getSimpleName();

    private Anime anime;
    private ImageView cover, star, starAnimation;
    private RecyclerView rv;

    private Realm realm;

    private Snackbar updateBar;
    private SpotsDialog loadDialog;

    private FloatingActionButton fab;

    private boolean inDownloadMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime);

        initAds();
        setupDatabase();
        initUI();
        updateEpisodes();
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

    private void updateEpisodes() {
        if (anime.episodes.isEmpty()) {
            updateBar.show();
        } else {
            fab.show();
        }
        Browser.getInstance(this).load(anime.summaryURL, new HtmlListener() {
            @Override
            public void onPageLoaded(String html) {
                //Log.d(TAG, "onPageLoaded");
                final Document doc = Jsoup.parse(html);
                if (doc.title().isEmpty()) {
                    Log.e(TAG, "this shouldn't happen: " + html);
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Browser.getInstance(AnimeActivity.this).reset();

                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {

                                Elements elements = doc.select(Selector.EPISODE_LIST);

                                for (Element episodeElement : elements) {
                                    String name = episodeElement.text();
                                    String url = Browser.BASE_URL + episodeElement.attributes().get("href");

                                    //GOOGLE PLAY
                                    if (Utils.containsIgnoreCase(name, "censored"))
                                        continue;

                                    Episode episode = realm.where(Episode.class).equalTo("url", url).findFirst();
                                    if (episode == null) {
                                        episode = realm.createObject(Episode.class);
                                        episode.name = name;
                                        episode.url = url;
                                        anime.episodes.add(episode);
                                    }
                                }
                                /*
                                elements = doc.select(Selector.ANIME_DESCRIPTION);
                                //TODO:: crashes if page returns error, some anime have link at very bottom (Fun Facts)
                                if (elements.size() > 1) {
                                    anime.summary = elements.get(elements.size() - 2).text();
                                }
                                */
                            }
                        });
                        ((EpisodeAdapter) rv.getAdapter()).setEpisodeList(anime.episodes.sort("name", Sort.DESCENDING));
                        if (updateBar.isShownOrQueued()) {
                            updateBar.dismiss();
                        }
                        fab.show();
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
                        if (updateBar.isShown()) {
                            updateBar.dismiss();
                        }
                        fab.show();
                        Toast.makeText(AnimeActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void initAds() {
        if (AppLovinInterstitialAd.isAdReadyToDisplay(this)) {
            if (Math.random() > 0.5) {
                AppLovinInterstitialAd.show(this);
            }
        }
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
        String animeTitle = getIntent().getStringExtra("anime");
        this.anime = realm.where(Anime.class).equalTo("title", animeTitle).findFirst();
    }

    private void initUI() {
        cover = (ImageView) findViewById(R.id.background);
        updateBar = Snackbar.make(cover, "Updating...", Snackbar.LENGTH_INDEFINITE);
        updateBar.getView().setBackgroundColor(getResources().getColor(R.color.trans_base4_inactive));
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.hide();
        loadDialog = new SpotsDialog(this, R.style.LoadingTheme);

        final Animation buttonAnim = AnimationUtils.loadAnimation(this, R.anim.anim_button);
        star = (ImageView) findViewById(R.id.star);
        star.setSelected(anime.isStarred);
        starAnimation = (ImageView) findViewById(R.id.star_animation);

        rv = (RecyclerView) findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new EpisodeAdapter(this, anime));
        setupBackground(Transformation.BLUR);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inDownloadMode) {
                    inDownloadMode = false;
                    setupBackground(Transformation.BLUR);
                    fab.setImageDrawable(ContextCompat.getDrawable(AnimeActivity.this, R.drawable.ic_file_download_black_24dp));
                } else {
                    inDownloadMode = true;
                    setupBackground(Transformation.BW);
                    fab.setImageDrawable(ContextCompat.getDrawable(AnimeActivity.this, R.drawable.ic_remove_red_eye_black_24dp));
                }
            }
        });

        starAnimation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                starAnimation.startAnimation(buttonAnim);
                if (star.isSelected()) {
                    star.setSelected(false);
                    toggleStar(false);
                } else {
                    star.setSelected(true);
                    toggleStar(true);
                }
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
            realm.executeTransactionAsync(new GetCoverURL(anime.title));
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

    public void toggleStar(final boolean isStarred) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                anime.isStarred = isStarred;
            }
        });
    }

    private void showSelectQualityDialog(final Episode episode, final JSONObject json) {
        if (loadDialog.isShowing()) loadDialog.dismiss();
        Iterator<String> it = json.keys();
        final ArrayList<String> qualities = new ArrayList<String>();
        while (it.hasNext()) {
            qualities.add(it.next());
        }

        new AlertDialog.Builder(AnimeActivity.this)
                .setTitle("Select Quality")
                .setSingleChoiceItems(qualities.toArray(new CharSequence[qualities.size()]), -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (inDownloadMode) {
                            downloadVideo(episode, json.optString(qualities.get(which)));
                        } else {
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

    public void requestVideo(final Episode episode) {
        loadDialog.show();
        if (updateBar.isShown()) updateBar.dismiss();

        Browser.getInstance(this).addJSONListener(new JSONListener() {
            @Override
            public void onJSONReceived(final JSONObject json) {
                Browser.getInstance(AnimeActivity.this).reset();
                if (inDownloadMode) {
                    showSelectQualityDialog(episode, json);
                } else {
                    try {
                        SharedPreferences prefs = getSharedPreferences("daose", MODE_PRIVATE);
                        String resolution = prefs.getString("quality", "720p");
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
                        Toast.makeText(AnimeActivity.this, "Try again later", Toast.LENGTH_SHORT).show();
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
                    Log.e(TAG, "External storage not available");
                    Toast.makeText(AnimeActivity.this, "Cannot save file", Toast.LENGTH_SHORT).show();
                }
                String filePath = anime.title.replaceAll("[^a-zA-Z0-9.-]", "-");
                try {
                    filePath += "/" + episode.name.replaceAll("[^a-zA-Z0-9]", "-");
                } catch (Exception e) {
                    e.printStackTrace();
                    filePath += "/" + UUID.randomUUID().toString();
                }
                Log.d(TAG, "filename: " + filePath);

                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadURL));
                request.setDestinationInExternalFilesDir(AnimeActivity.this, Environment.DIRECTORY_MOVIES, filePath + ".mp4");
                request.allowScanningByMediaScanner();
                request.setVisibleInDownloadsUi(true);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                dm.enqueue(request);
                Toast.makeText(AnimeActivity.this, "Download started", Toast.LENGTH_SHORT).show();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        episode.hasWatched = true;
                    }
                });
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
                Intent intent = new Intent(AnimeActivity.this, FullScreenVideoPlayerActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);
            }
        });
    }

    private class GetCoverURL implements Realm.Transaction {

        private String title;

        public GetCoverURL(String title) {
            this.title = title;
        }

        @Override
        public void execute(Realm realm) {
            try {
                final StringBuilder URLBuilder = new StringBuilder();
                Anime anime = realm.where(Anime.class).equalTo("title", title).findFirst();
                final Document doc = Jsoup.connect(Browser.IMAGE_URL + anime.title).userAgent("Mozilla/5.0").get();
                Uri rawUrl = Uri.parse(doc.select(Selector.MAL_IMAGE).first().attr(Selector.MAL_IMAGE_ATTR));
                URLBuilder.append(rawUrl.getScheme()).append("://").append(rawUrl.getHost());
                List<String> pathSegments = rawUrl.getPathSegments();
                if (rawUrl.getPathSegments().size() < 3) {
                    anime.coverURL = "";
                    return;
                } else {
                    for (int i = 2; i < pathSegments.size(); i++) {
                        URLBuilder.append("/");
                        URLBuilder.append(pathSegments.get(i));
                    }
                }
                anime.coverURL = URLBuilder.toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Picasso.with(AnimeActivity.this).
                                load(URLBuilder.toString())
                                .fit()
                                .centerCrop()
                                .transform(new BlurTransformation(AnimeActivity.this))
                                .into(cover);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
