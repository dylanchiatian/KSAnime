package com.daose.ksanime.api;

import android.os.AsyncTask;
import android.util.Log;

import com.daose.ksanime.model.News;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class MAL {
    private static final String TAG = MAL.class.getSimpleName();
    private static final String BASE_URL = "https://myanimelist.net";
    private static final String NEWS_ENDPOINT = "/rss/news.xml";

    // TODO:: check that we have network first
    public static void getNews(Listener listener) {
        new FetchNews(listener).execute();
    }

    public interface Listener {
        void onSuccess(ArrayList<News> data);
        void onError();
    }

    private static class FetchNews extends AsyncTask<Void, Void, ArrayList<News>> {

        private Listener listener;

        public FetchNews(Listener listener) {
            this.listener = listener;
        }

        @Override
        protected ArrayList<News> doInBackground(Void ...voids) {
            final ArrayList<News> newsList = new ArrayList<News>();
            try {
                final String endpoint = BASE_URL + NEWS_ENDPOINT;
                final Document doc = Jsoup.connect(endpoint).parser(Parser.xmlParser()).get();
                final Elements elements = doc.select("item");
                for (final Element element : elements) {
                    final News news = new News();
                    news.title = element.select("title").text();
                    news.description = element.select("description").text();
                    news.thumbnail = element.select("media|thumbnail").text();
                    news.pubDate = element.select("pubDate").text();
                    news.link = element.select("link").text();
                    newsList.add(news);
                }
            } catch (IOException e){
                Log.e(TAG, "FetchNews error", e);
            }
            return newsList;
        }

        @Override
        protected void onPostExecute(ArrayList<News> data) {
            if(data.size() > 0) {
                listener.onSuccess(data);
            } else {
                listener.onError();
            }
        }
    }
}
