package com.daose.ksanime.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.WebView;

import com.daose.ksanime.MainActivity;
import com.daose.ksanime.R;
import com.daose.ksanime.api.ka.KA;
import com.daose.ksanime.fragment.AnimeListFragment;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.web.CustomWebClient;
import com.daose.ksanime.web.HtmlHandler;
import com.daose.ksanime.web.HtmlListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import static com.daose.ksanime.web.Selector.EPISODE_LIST;


public class KAService extends Service {
    private static final String TAG = KAService.class.getSimpleName();
    private static final String LIST = "list";
    private static final String INDEX = "index";
    private static final String MESSAGES = "messages";
    private static final int NOTIFICATION_ID = 2;

    private WebView webView;
    private CustomWebClient client;
    private HtmlHandler htmlHandler;

    private ArrayList<String> list;
    private ArrayList<String> messages;
    private int index;

    public KAService() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(index < list.size()) {
            Intent intent = new Intent(this, KAService.class);
            intent.putExtra(LIST, list);
            intent.putExtra(INDEX, index);
            intent.putExtra(MESSAGES, messages);
            startService(intent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) {
            list = null;
            index = 0;
            messages = null;
        } else {
            list = intent.getStringArrayListExtra(LIST);
            index = intent.getIntExtra(INDEX, 0);
            messages = intent.getStringArrayListExtra(MESSAGES);
        }

        if(messages == null) {
            messages = new ArrayList<String>();
        }

        if(list == null) {
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this).deleteRealmIfMigrationNeeded().build();
            Realm.setDefaultConfiguration(realmConfiguration);
            Realm realm = Realm.getDefaultInstance();
            final List<Anime> fullList = realm.copyFromRealm(realm.where(Anime.class).equalTo(Anime.IS_STARRED, true).findAll(), 0);
            list = new ArrayList<String>();
            for (final Anime anime : fullList) {
                list.add(anime.title);
            }
            realm.close();
        }
        if(list.size() > 0) {
            checkAnime(list.get(index));
            index++;
        } else {
            stopSelf();
        }

        return START_STICKY;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1; rv:10.0) Gecko/20100101 Firefox/10.0");
        client = new CustomWebClient();
        htmlHandler = new HtmlHandler();
        webView.setWebViewClient(client);
        webView.addJavascriptInterface(htmlHandler, "HtmlHandler");
    }

    private void checkAnime(final String title) {
        if(title == null) {
            stopSelf();
            return;
        }

        final Realm realm = Realm.getDefaultInstance();
        final Anime anime = realm.where(Anime.class).equalTo(Anime.TITLE, title).findFirst();
        final int numOfEpisodes = anime.episodes.size();
        final String checkUrl = anime.summaryURL.startsWith("http") ? anime.summaryURL : KA.BASE_URL + anime.summaryURL;
        realm.close();

        htmlHandler.addHtmlListener(new HtmlListener() {
            @Override
            public void onPageLoaded(String html, String url) {
                final Document doc = Jsoup.parse(html);
                if (doc.title().isEmpty()) {
                    return;
                }

                if(doc.select(EPISODE_LIST).size() > numOfEpisodes) {
                    sendNotification(title);
                }
                stopSelf();
            }

            @Override
            public void onPageFailed(String error) {
                Log.e(TAG, "onPageFailed");
            }
        });
        webView.loadUrl(checkUrl, client.getHeaders());
    }

    private String getOneLiner(final String title) {
        return messages.size() > 1 ? String.format(Locale.US, "%s and %d more", title, messages.size() - 1) : title;
    }

    private void sendNotification(final String title) {
        messages.add(title);

        final Intent intent = new Intent(KAService.this, MainActivity.class);
        intent.setAction(AnimeListFragment.Type.Starred.name());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(KAService.this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(KAService.this)
                        .setContentTitle(getString(R.string.notification_summary))
                        .setContentText(getOneLiner(title))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setAutoCancel(true)
                        .setNumber(messages.size())
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setStyle(getNotificationList())
                        .setColor(0x0)
                        .setContentIntent(pendingIntent);
        Notification notification = builder.build();
        notification.defaults |= Notification.DEFAULT_ALL;
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    private NotificationCompat.InboxStyle getNotificationList() {
        NotificationCompat.InboxStyle notifications = new NotificationCompat.InboxStyle();
        notifications.setBigContentTitle(getString(R.string.notification_summary));
        for(int i = 0; i < messages.size(); i++) {
            notifications.addLine(messages.get(i));
        }
        return notifications;
    }
}
