package com.daose.anime;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.daose.anime.Adapter.EpisodeAdapter;
import com.daose.anime.Anime.Anime;
import com.daose.anime.Anime.Episode;
import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;
import com.daose.anime.web.Selector;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import io.realm.Realm;
import io.realm.Sort;

public class AnimeActivity extends AppCompatActivity implements HtmlListener {

    private static final String TAG = AnimeActivity.class.getSimpleName();

    private Anime anime;
    private RecyclerView rv;
    private TextView title, summary;
    private ImageView cover;
    private EpisodeAdapter adapter;

    private Realm realm;

    private boolean isFetching = false;

    //TODO:: loading here as well

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime);

        setupDatabase();

        String animeTitle = getIntent().getStringExtra("anime");
        this.anime = realm.where(Anime.class).equalTo("title", animeTitle).findFirst();
        assert anime != null;

        Browser.getInstance(this).setListener(this);
        Browser.getInstance(this).loadUrl(anime.summaryURL);
        initUI();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        realm.close();
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
    }

    private void initUI() {
        rv = (RecyclerView) findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        adapter = new EpisodeAdapter(this, anime.episodes.sort("name", Sort.DESCENDING));
        rv.setAdapter(adapter);

        title = (TextView) findViewById(R.id.title);
        title.setText(anime.title);
        summary = (TextView) findViewById(R.id.description);
        summary.setMovementMethod(new ScrollingMovementMethod());
        cover = (ImageView) findViewById(R.id.cover);
        cover.post(new Runnable() {
            @Override
            public void run() {
                summary.setMaxHeight(cover.getHeight() - title.getHeight());
            }
        });
        if(anime.coverURL != null) {
            Picasso.with(this).load(anime.coverURL).placeholder(R.drawable.placeholder).into(cover);
        }

        if (anime.summary != null) {
            summary.setText(anime.summary);
        }
    }

    @Override
    public void onPageLoaded(final String html) {
        Log.d(TAG, "onPageLoaded");
        if (isFetching) return;
        Runnable stopLoad = new Runnable() {
            @Override
            public void run() {
                Browser.getInstance(AnimeActivity.this).reset();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        final Document doc = Jsoup.parse(html);
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
                        //TODO:: crashes if page returns error
                        anime.summary = elements.get(elements.size() - 2).text();
                        Log.d(TAG, elements.get(elements.size() - 2).text());
                    }
                });

            }
        };
        new Handler(AnimeActivity.this.getMainLooper()).post(stopLoad);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.setEpisodeList(anime.episodes.sort("name", Sort.DESCENDING));
                summary.setMaxHeight(cover.getHeight() - title.getHeight());
                summary.setText(anime.summary);
                summary.invalidate();
            }
        });
        isFetching = true;
    }
}
