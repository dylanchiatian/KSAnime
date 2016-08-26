package com.daose.anime;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;

import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        addContentView(Browser.getInstance(this).getWebView(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Browser.getInstance(this).load("http://kissanime.to//Anime/Orange/Episode-002?id=127623", new HtmlListener() {
            @Override
            public void onPageLoaded(String html) {
                Log.d("test", "DONE");
            }
        });
    }
}
