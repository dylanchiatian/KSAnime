package com.daose.anime;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.daose.anime.Adapter.SearchAdapter;
import com.daose.anime.Anime.Anime;
import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;
import com.daose.anime.web.Selector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import io.realm.Realm;

public class SearchActivity extends AppCompatActivity implements HtmlListener {

    private static final String TAG = SearchActivity.class.getSimpleName();
    private Realm realm;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initUI();
        setupDatabase();
        String query = getIntent().getStringExtra("query");
        search(query);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        realm.close();
    }

    private void search(String query) {
        Browser.getInstance(this).setListener(this);
        Browser.getInstance(this).loadUrl(Browser.SEARCH_URL + query);
    }

    private void initUI() {
        rv = (RecyclerView) findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onPageLoaded(final String html) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Browser.getInstance(SearchActivity.this).reset();
                Document doc = Jsoup.parse(html);
                final Elements animeElements = doc.select(Selector.SEARCH_LIST);
                final ArrayList<String> searchList = new ArrayList<String>();
                for (final Element animeElement : animeElements) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            Anime anime = realm.where(Anime.class).equalTo("title", animeElement.text()).findFirst();
                            if (anime == null) {
                                anime = realm.createObject(Anime.class);
                                anime.title = animeElement.text();
                                anime.summaryURL = Browser.BASE_URL + animeElement.attributes().get("href");
                            }
                            Log.d(TAG, "summaryURL: " + anime.summaryURL);
                            searchList.add(anime.title);
                        }
                    });
                }
                rv.setAdapter(new SearchAdapter(SearchActivity.this, searchList));
            }
        });
    }
}
