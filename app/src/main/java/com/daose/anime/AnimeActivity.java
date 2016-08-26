package com.daose.anime;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.daose.anime.adapter.EpisodeAdapter;
import com.daose.anime.model.Anime;
import com.daose.anime.model.Episode;
import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;
import com.daose.anime.web.Selector;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;

import dmax.dialog.SpotsDialog;
import io.realm.Realm;
import io.realm.Sort;
import jp.wasabeef.picasso.transformations.BlurTransformation;

public class AnimeActivity extends AppCompatActivity implements HtmlListener, DialogInterface.OnCancelListener {

    private static final String TAG = AnimeActivity.class.getSimpleName();

    private Anime anime;
    private ImageView cover, star, starAnimation;
    private EpisodeAdapter adapter;

    private Realm realm;

    private Snackbar loadingBar;
    private SpotsDialog loadDialog;

    //TODO:: loading here as well

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime);

        setupDatabase();

        String animeTitle = getIntent().getStringExtra("anime");
        this.anime = realm.where(Anime.class).equalTo("title", animeTitle).findFirst();

        Browser.getInstance(this).load(anime.summaryURL, this);
        initUI();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
    }

    private void initUI() {
        cover = (ImageView) findViewById(R.id.background);
        loadDialog = new SpotsDialog(this, R.style.LoadingTheme);
        loadDialog.setOnCancelListener(this);
        loadingBar = Snackbar.make(cover, "Updating...", Snackbar.LENGTH_INDEFINITE);
        loadingBar.getView().setBackgroundColor(getResources().getColor(R.color.trans_base4_inactive));
        loadingBar.show();

        final Animation buttonAnim = AnimationUtils.loadAnimation(this, R.anim.anim_button);
        star = (ImageView) findViewById(R.id.star);
        star.setSelected(anime.isStarred);
        starAnimation = (ImageView) findViewById(R.id.star_animation);
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

        RecyclerView rv = (RecyclerView) findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        adapter = new EpisodeAdapter(this, anime);
        rv.setAdapter(adapter);
        if (anime.coverURL == null || anime.coverURL.isEmpty()) {
            realm.executeTransactionAsync(new GetCoverURL(anime.title));
        } else {
            Picasso.with(this).load(anime.coverURL)
                    .fit()
                    .centerCrop()
                    .transform(new BlurTransformation(this))
                    .into(cover);
        }
    }

    @Override
    public void onPageLoaded(final String html) {
        Log.d(TAG, "onPageLoaded");
        final Document doc = Jsoup.parse(html);
        if(doc.title().isEmpty()) return;
        Runnable stopLoad = new Runnable() {
            @Override
            public void run() {
                Browser.getInstance(AnimeActivity.this).reset();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Elements elements = doc.select(Selector.EPISODE_LIST);

                        for (Element episodeElement : elements) {
                            String name = episodeElement.text();
                            String URL = Browser.BASE_URL + episodeElement.attributes().get("href");
                            Episode episode = realm.where(Episode.class).equalTo("url", URL).findFirst();
                            if (episode == null) {
                                episode = realm.createObject(Episode.class);
                                episode.name = name;
                                episode.url = URL;
                                anime.episodes.add(episode);
                            }
                        }
                        elements = doc.select(Selector.ANIME_DESCRIPTION);
                        //TODO:: crashes if page returns error, some anime have link at very bottom (Fun Facts)
                        if (elements.size() > 1) {
                            anime.summary = elements.get(elements.size() - 2).text();
                        }
                    }
                });

            }
        };
        new Handler(AnimeActivity.this.getMainLooper()).post(stopLoad);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loadingBar.isShown()) loadingBar.dismiss();
                adapter.setEpisodeList(anime.episodes.sort("name", Sort.DESCENDING));
            }
        });
    }

    public void toggleStar(final boolean isStarred) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                anime.isStarred = isStarred;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Browser.getInstance(this).reset();
    }

    public void requestVideo(final Episode episode) {
        loadDialog.show();
        if (loadingBar.isShown()) loadingBar.dismiss();
        if ((episode.videoURL != null) && (!episode.videoURL.isEmpty())) {
            startVideo(episode.videoURL);
        } else {
            Browser.getInstance(this).load(episode.url, new HtmlListener() {
                @Override
                public void onPageLoaded(String html) {
                    Browser.getInstance(AnimeActivity.this).reset();
                    Document doc = Jsoup.parse(html);
                    final Element videoEle = doc.select(Selector.VIDEO).first();
                    if (videoEle == null) {
                        Log.d(TAG, "couldn't find div");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadDialog.dismiss();
                                Toast.makeText(AnimeActivity.this, "Try again later", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    episode.videoURL = videoEle.attr("abs:src");
                                    episode.hasWatched = true;
                                    startVideo(episode.videoURL);
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private void startVideo(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadDialog.dismiss();
                Intent intent = new Intent(AnimeActivity.this, FullScreenVideoPlayerActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Log.d(TAG, "video load cancelled");
        Browser.getInstance(this).reset();
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
