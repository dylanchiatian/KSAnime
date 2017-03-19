package com.daose.ksanime;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.daose.ksanime.service.KAService;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    @Override
    public void onCreate(){
        super.onCreate();

        SharedPreferences settings = getSharedPreferences("daose", MODE_PRIVATE);

        if (settings.getBoolean("first_install", true)) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("first_install", false);
            editor.apply();
        }

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this).deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(realmConfiguration);

        startKAService();
    }

    private void startKAService() {
        final Intent intent = new Intent(this, KAService.class);
        final PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                AlarmManager.INTERVAL_DAY / 3,
                pendingIntent
        );
    }
}
